package world.phantasmal.lib.asm

import mu.KotlinLogging
import kotlin.math.min

private val logger = KotlinLogging.logger {}

private const val INDENT_WIDTH = 4
private val INDENT = " ".repeat(INDENT_WIDTH)

/**
 * @param inlineStackArgs If true, will output stack arguments inline instead of outputting stack
 * management instructions (argpush variants).
 */
fun disassemble(bytecodeIr: BytecodeIr, inlineStackArgs: Boolean = true): List<String> {
    logger.trace {
        "Disassembling ${bytecodeIr.segments.size} segments with ${
            if (inlineStackArgs) "inline stack arguments" else "stack push instructions"
        }."
    }

    val lines = mutableListOf<String>()
    val stack = mutableListOf<ArgWithType>()
    var sectionType: SegmentType? = null

    for (segment in bytecodeIr.segments) {
        // Section marker (.code, .data or .string).
        if (sectionType != segment.type) {
            sectionType = segment.type

            if (lines.isNotEmpty()) {
                lines.add("")
            }

            val sectionMarker = when (segment) {
                is InstructionSegment -> ".code"
                is DataSegment -> ".data"
                is StringSegment -> ".string"
            }

            lines.add(sectionMarker)
            lines.add("")
        }

        // Labels.
        for (label in segment.labels) {
            lines.add("$label:")
        }

        // Code or data lines.
        when (segment) {
            is InstructionSegment -> {
                var inVaList = false

                segment.instructions.forEachIndexed { i, instruction ->
                    val opcode = instruction.opcode

                    if (opcode.code == OP_VA_START.code) {
                        inVaList = true
                    } else if (opcode.code == OP_VA_END.code) {
                        inVaList = false
                    }

                    if (inlineStackArgs &&
                        !inVaList &&
                        opcode.stack == StackInteraction.Push &&
                        canInlinePushedArg(segment, i)
                    ) {
                        stack.addAll(addTypeToArgs(opcode.params, instruction.args))
                    } else {
                        val sb = StringBuilder(INDENT)
                        sb.append(opcode.mnemonic)

                        if (opcode.stack == StackInteraction.Pop) {
                            if (inlineStackArgs) {
                                sb.appendArgs(
                                    opcode.params,
                                    stack.takeLast(opcode.params.size),
                                    stack = true,
                                )
                            }
                        } else {
                            sb.appendArgs(
                                opcode.params,
                                addTypeToArgs(opcode.params, instruction.args),
                                stack = false
                            )
                        }

                        if (opcode.stack != StackInteraction.Push) {
                            stack.clear()
                        }

                        lines.add(sb.toString())
                    }
                }
            }

            is DataSegment -> {
                val sb = StringBuilder(INDENT)

                for (i in 0 until segment.data.size) {
                    sb.append("0x")
                    sb.append(segment.data.getUByte(i).toString(16).padStart(2, '0'))

                    when {
                        // Last line.
                        i == segment.data.size - 1 -> {
                            lines.add(sb.toString())
                        }
                        // Start a new line after every 16 bytes.
                        i % 16 == 15 -> {
                            lines.add(sb.toString())
                            sb.setLength(0)
                            sb.append(INDENT)
                        }
                        // Add a space between each byte.
                        else -> {
                            sb.append(" ")
                        }
                    }
                }
            }

            is StringSegment -> {
                lines.add(StringBuilder(INDENT).appendStringSegment(segment.value).toString())
            }
        }
    }

    // Ensure newline at the end.
    lines.add("")

    logger.trace { "Disassembly finished, line count: ${lines.size}." }

    return lines
}

private data class ArgWithType(val arg: Arg, val type: AnyType)

private fun canInlinePushedArg(segment: InstructionSegment, index: Int): Boolean {
    var pushedArgCount = 0

    for (i in index until segment.instructions.size) {
        val opcode = segment.instructions[i].opcode

        when (opcode.stack) {
            StackInteraction.Push -> pushedArgCount++

            StackInteraction.Pop -> {
                var paramCount = 0
                var varArgs = false

                for (param in opcode.params) {
                    when (param.type) {
                        is ILabelVarType -> varArgs = true
                        is RegVarType -> varArgs = true
                        else -> paramCount++
                    }
                }

                return pushedArgCount <= paramCount || (pushedArgCount > paramCount && varArgs)
            }

            null -> return false
        }
    }

    return false
}

private fun addTypeToArgs(params: List<Param>, args: List<Arg>): List<ArgWithType> {
    val argsWithType = mutableListOf<ArgWithType>()

    for (i in 0 until min(params.size, args.size)) {
        argsWithType.add(ArgWithType(args[i], params[i].type))
    }

    // Deal with varargs.
    val lastParam = params.lastOrNull()

    if (lastParam?.varargs == true) {
        for (i in argsWithType.size until args.size) {
            argsWithType.add(ArgWithType(args[i], lastParam.type))
        }
    }

    return argsWithType
}

private fun StringBuilder.appendArgs(params: List<Param>, args: List<ArgWithType>, stack: Boolean) {
    var i = 0

    while (i < params.size) {
        val paramType = params[i].type

        if (i == 0) {
            append(" ")
        } else {
            append(", ")
        }

        if (i < args.size) {
            val (arg, argType) = args[i]

            if (argType is RegType) {
                append("r")
                append(arg.value)
            } else {
                when (paramType) {
                    FloatType -> {
                        // Floats are pushed onto the stack as integers with arg_pushl.
                        if (stack) {
                            append(Float.fromBits((arg as IntArg).value))
                        } else {
                            append(arg.value)
                        }
                    }

                    ILabelVarType -> {
                        while (i < args.size) {
                            append(args[i].arg.value)
                            if (i < args.lastIndex) append(", ")
                            i++
                        }
                    }

                    RegVarType -> {
                        while (i < args.size) {
                            append("r")
                            append(args[i].arg.value)
                            if (i < args.lastIndex) append(", ")
                            i++
                        }
                    }

                    is RegType -> {
                        append("r")
                        append(arg.value)
                    }

                    StringType -> {
                        appendStringArg((arg as StringArg).value)
                    }

                    else -> {
                        append(arg.value)
                    }
                }
            }
        }

        i++
    }
}

private fun StringBuilder.appendStringArg(value: String): StringBuilder {
    append("\"")

    for (char in value) {
        when (char) {
            '\r' -> append("\\r")
            '\n' -> append("\\n")
            '\t' -> append("\\t")
            '"' -> append("\\\"")
            else -> append(char)
        }
    }

    append("\"")
    return this
}

private fun StringBuilder.appendStringSegment(value: String): StringBuilder {
    append("\"")

    var i = 0

    while (i < value.length) {
        when (val char = value[i]) {
            // Replace <cr> with \n.
            '<' -> {
                if (i + 3 < value.length &&
                    value[i + 1] == 'c' &&
                    value[i + 2] == 'r' &&
                    value[i + 3] == '>'
                ) {
                    append("\\n")
                    i += 3
                } else {
                    append(char)
                }
            }
            '\r' -> append("\\r")
            '\n' -> append("\\n")
            '\t' -> append("\\t")
            '"' -> append("\\\"")
            else -> append(char)
        }

        i++
    }

    append("\"")
    return this
}

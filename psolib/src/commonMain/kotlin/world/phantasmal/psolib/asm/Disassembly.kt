package world.phantasmal.psolib.asm

import mu.KotlinLogging
import kotlin.math.min

private val logger = KotlinLogging.logger {}

private const val INDENT_WIDTH = 4
private val INDENT = " ".repeat(INDENT_WIDTH)

enum class IntFormat {
    HEX,
    DECIMAL,
}

/**
 * @param intFormat How to format integer arguments: [IntFormat.HEX] for 0x prefixed hexadecimal,
 * [IntFormat.DECIMAL] for decimal. Labels and registers are not affected.
 * @param hideNops If true, NOP instructions will be omitted from the output.
 */
fun disassemble(
    bytecodeIr: BytecodeIr,
    intFormat: IntFormat = IntFormat.DECIMAL,
    hideNops: Boolean = false,
): List<String> {
    logger.trace {
        "Disassembling ${bytecodeIr.segments.size} segments."
    }

    val lines = mutableListOf<String>()
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
                for (instruction in segment.instructions) {
                    val opcode = instruction.opcode

                    if (hideNops && opcode.code == OP_NOP.code) {
                        continue
                    }

                    val sb = StringBuilder(INDENT)
                    sb.append(opcode.mnemonic)

                    val useHex = intFormat == IntFormat.HEX

                    // All instructions (including Pop) now have inlined args in normalized IR.
                    sb.appendArgs(
                        opcode.params,
                        addTypeToArgs(opcode.params, instruction.args),
                        useHex = useHex,
                    )

                    lines.add(sb.toString())
                }
            }

            is DataSegment -> {
                if (segment.data.size == 0) {
                    // Zero-byte segment: emit an empty line so the label is not orphaned
                    // and the segment round-trips correctly through the assembler.
                    lines.add(INDENT)
                } else {
                    val sb = StringBuilder(INDENT)

                    for (i in 0 until segment.data.size) {
                        sb.append("0x")
                        sb.append(segment.data.getUByte(i).toString(16).uppercase().padStart(2, '0'))

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

private fun StringBuilder.appendArgs(
    params: List<Param>,
    args: List<ArgWithType>,
    useHex: Boolean = false,
) {
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

            if (argType is RegType || (arg is IntArg && arg.isRegRef)) {
                append("r")
                append(arg.value)
            } else {
                when (paramType) {
                    FloatType -> {
                        append(arg.coerceFloat())
                    }

                    ILabelVarType -> {
                        i = appendVarArgs(args, i, prefix = "")
                    }

                    RegVarType -> {
                        i = appendVarArgs(args, i, prefix = "r")
                    }

                    is RegType -> {
                        append("r")
                        append(arg.value)
                    }

                    StringType -> {
                        appendStringArg((arg as StringArg).value)
                    }

                    else -> {
                        appendValueArg(arg, paramType, useHex)
                    }
                }
            }
        }

        i++
    }
}

/**
 * Appends all remaining varargs starting at [startIndex], separated by commas. Each arg value is
 * prefixed with [prefix] (e.g. "r" for register references, "" for labels). Returns the index of
 * the last consumed arg (the caller will increment it once more).
 */
private fun StringBuilder.appendVarArgs(
    args: List<ArgWithType>,
    startIndex: Int,
    prefix: String,
): Int {
    var i = startIndex

    while (i < args.size) {
        append(prefix)
        append(args[i].arg.value)
        if (i < args.lastIndex) append(", ")
        i++
    }

    // Return the last index consumed; the caller will increment once more.
    return i - 1
}

/**
 * Appends a value-type argument, using hex formatting when [useHex] is true and the parameter type
 * is a non-label value type.
 */
private fun StringBuilder.appendValueArg(arg: Arg, paramType: AnyType, useHex: Boolean) {
    if (useHex && paramType is ValueType && paramType !is LabelType) {
        val bitSize = when (paramType) {
            ByteType -> 8
            ShortType -> 16
            else -> 32
        }
        appendHexInt(arg.value, bitSize)
    } else {
        append(arg.value)
    }
}

private fun StringBuilder.appendHexInt(value: Any?, bitSize: Int = 32) {
    val intVal = (value as? Number)?.toInt() ?: 0
    val unsigned = intVal.toUInt()
    val mask = if (bitSize >= 32) UInt.MAX_VALUE else ((1u shl bitSize) - 1u)
    val masked = unsigned and mask
    val hexDigits = bitSize / 4
    append("0x")
    append(masked.toString(16).uppercase().padStart(hexDigits, '0'))
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

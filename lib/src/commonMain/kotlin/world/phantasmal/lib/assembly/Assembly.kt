package world.phantasmal.lib.assembly

import mu.KotlinLogging
import world.phantasmal.core.Problem
import world.phantasmal.core.PwResult
import world.phantasmal.core.Severity
import world.phantasmal.lib.buffer.Buffer

private val logger = KotlinLogging.logger {}

class AssemblyProblem(
    severity: Severity,
    uiMessage: String,
    message: String? = null,
    cause: Throwable? = null,
    val lineNo: Int,
    val col: Int,
    val length: Int,
) : Problem(severity, uiMessage, message, cause)

fun assemble(
    assembly: List<String>,
    inlineStackArgs: Boolean = true,
): PwResult<List<Segment>> {
    logger.trace {
        "Assembling ${assembly.size} lines with ${
            if (inlineStackArgs) "inline stack arguments" else "stack push instructions"
        }."
    }

    val result = Assembler(assembly, inlineStackArgs).assemble()

    logger.trace {
        val warnings = result.problems.count { it.severity == Severity.Warning }
        val errors = result.problems.count { it.severity == Severity.Error }

        "Assembly finished with $warnings warnings and $errors errors."
    }

    return result
}

private class Assembler(private val assembly: List<String>, private val inlineStackArgs: Boolean) {
    private var lineNo = 1
    private lateinit var tokens: MutableList<Token>
    private var ir: MutableList<Segment> = mutableListOf()

    /**
     * The current segment.
     */
    private var segment: Segment? = null

    /**
     * Encountered labels.
     */
    private val labels: MutableSet<Int> = mutableSetOf()
    private var section: SegmentType = SegmentType.Instructions
    private var firstSectionMarker = true
    private var prevLineHadLabel = false

    private val result = PwResult.build<List<Segment>>(logger)

    fun assemble(): PwResult<List<Segment>> {
        // Tokenize and assemble line by line.
        for (line in assembly) {
            tokens = tokenizeLine(line)

            if (tokens.isNotEmpty()) {
                val token = tokens.removeFirst()
                var hasLabel = false

                when (token) {
                    is Token.Label -> {
                        parseLabel(token)
                        hasLabel = true
                    }
                    is Token.Section -> {
                        parseSection(token)
                    }
                    is Token.Int32 -> {
                        if (section == SegmentType.Data) {
                            parseBytes(token)
                        } else {
                            addUnexpectedTokenError(token)
                        }
                    }
                    is Token.Str -> {
                        if (section == SegmentType.String) {
                            parseString(token)
                        } else {
                            addUnexpectedTokenError(token)
                        }
                    }
                    is Token.Ident -> {
                        if (section === SegmentType.Instructions) {
                            parseInstruction(token)
                        } else {
                            addUnexpectedTokenError(token)
                        }
                    }
                    is Token.InvalidSection -> {
                        addError(token, "Invalid section type.")
                    }
                    is Token.InvalidIdent -> {
                        addError(token, "Invalid identifier.")
                    }
                    else -> {
                        addUnexpectedTokenError(token)
                    }
                }

                prevLineHadLabel = hasLabel
            }

            lineNo++
        }

        return result.success(ir)
    }

    private fun addInstruction(
        opcode: Opcode,
        args: List<Arg>,
        stackArgs: List<Arg>,
        token: Token?,
        argTokens: List<Token>,
        stackArgTokens: List<Token>,
    ) {
        when (val seg = segment) {
            null -> {
                // Unreachable code, technically valid.
                segment = InstructionSegment(
                    labels = mutableListOf(),
                    instructions = mutableListOf(),
                    srcLoc = SegmentSrcLoc()
                )

                ir.add(segment!!)
            }

            is InstructionSegment -> {
                seg.instructions.add(
                    Instruction(
                        opcode,
                        args,
                        InstructionSrcLoc(
                            mnemonic = token?.let {
                                SrcLoc(lineNo, token.col, token.len)
                            },
                            args = argTokens.map {
                                SrcLoc(lineNo, it.col, it.len)
                            },
                            stackArgs = stackArgTokens.mapIndexed { i, sat ->
                                StackArgSrcLoc(lineNo, sat.col, sat.len, stackArgs[i].value)
                            },
                        )
                    )
                )
            }

            else -> {
                logger.error { "Line $lineNo: Expected instructions segment." }
            }
        }
    }

    private fun addBytes(bytes: ByteArray) {
        when (val seg = segment) {
            null -> {
                // Unaddressable data, technically valid.
                segment = DataSegment(
                    labels = mutableListOf(),
                    data = Buffer.fromByteArray(bytes),
                    srcLoc = SegmentSrcLoc()
                )

                ir.add(segment!!)
            }

            is DataSegment -> {
                val oldSize = seg.data.size
                seg.data.size += bytes.size

                for (i in bytes.indices) {
                    seg.data.setByte(i + oldSize, bytes[i])
                }
            }

            else -> {
                logger.error { "Line $lineNo: Expected data segment." }
            }
        }
    }

    private fun addString(str: String) {
        when (val seg = segment) {
            null -> {
                // Unaddressable data, technically valid.
                segment = StringSegment(
                    labels = mutableListOf(),
                    value = str,
                    srcLoc = SegmentSrcLoc()
                )

                ir.add(segment!!)
            }

            is StringSegment -> {
                seg.value += str
            }

            else -> {
                logger.error { "Line $lineNo: Expected string segment." }
            }
        }
    }

    private fun addError(col: Int, length: Int, uiMessage: String, message: String? = null) {
        result.addProblem(
            AssemblyProblem(
                Severity.Error,
                uiMessage,
                message ?: "$uiMessage At $lineNo:$col.",
                lineNo = lineNo,
                col = col,
                length = length
            )
        )
    }

    private fun addError(token: Token, uiMessage: String, message: String? = null) {
        addError(token.col, token.len, uiMessage, message)
    }

    private fun addUnexpectedTokenError(token: Token) {
        addError(
            token,
            "Unexpected token.",
            "Unexpected ${token::class.simpleName} at ${token.srcLoc()}.",
        )
    }

    private fun addWarning(token: Token, uiMessage: String) {
        result.addProblem(
            AssemblyProblem(
                Severity.Warning,
                uiMessage,
                lineNo = lineNo,
                col = token.col,
                length = token.len,
            )
        )
    }

    private fun parseLabel(token: Token.Label) {
        val label = token.value

        if (!labels.add(label)) {
            addError(token, "Duplicate label.")
        }

        val nextToken = tokens.removeFirstOrNull()

        val srcLoc = SrcLoc(lineNo, token.col, token.len)

        if (prevLineHadLabel) {
            val segment = ir.last()
            segment.labels.add(label)
            segment.srcLoc.labels.add(srcLoc)
        }

        when (section) {
            SegmentType.Instructions -> {
                if (!prevLineHadLabel) {
                    segment = InstructionSegment(
                        labels = mutableListOf(label),
                        instructions = mutableListOf(),
                        srcLoc = SegmentSrcLoc(labels = mutableListOf(srcLoc)),
                    )

                    ir.add(segment!!)
                }

                if (nextToken != null) {
                    if (nextToken is Token.Ident) {
                        parseInstruction(nextToken)
                    } else {
                        addError(nextToken, "Expected opcode mnemonic.")
                    }
                }
            }

            SegmentType.Data -> {
                if (!prevLineHadLabel) {
                    segment = DataSegment(
                        labels = mutableListOf(label),
                        data = Buffer.withCapacity(0),
                        srcLoc = SegmentSrcLoc(labels = mutableListOf(srcLoc)),
                    )
                    ir.add(segment!!)
                }

                if (nextToken != null) {
                    if (nextToken is Token.Int32) {
                        parseBytes(nextToken)
                    } else {
                        addError(nextToken, "Expected bytes.")
                    }
                }
            }

            SegmentType.String -> {
                if (!prevLineHadLabel) {
                    segment = StringSegment(
                        labels = mutableListOf(label),
                        value = "",
                        srcLoc = SegmentSrcLoc(labels = mutableListOf(srcLoc)),
                    )
                    ir.add(segment!!)
                }

                if (nextToken != null) {
                    if (nextToken is Token.Str) {
                        parseString(nextToken)
                    } else {
                        addError(nextToken, "Expected a string.")
                    }
                }
            }
        }
    }

    private fun parseSection(token: Token.Section) {
        val section = when (token) {
            is Token.Section.Code -> SegmentType.Instructions
            is Token.Section.Data -> SegmentType.Data
            is Token.Section.Str -> SegmentType.String
        }

        if (this.section == section && !firstSectionMarker) {
            addWarning(token, "Unnecessary section marker.")
        }

        this.section = section
        firstSectionMarker = false

        tokens.removeFirstOrNull()?.let { nextToken ->
            addUnexpectedTokenError(nextToken)
        }
    }

    private fun parseInstruction(identToken: Token.Ident) {
        val opcode = mnemonicToOpcode(identToken.value)

        if (opcode == null) {
            addError(identToken, "Unknown opcode.")
        } else {
            val varargs = opcode.params.any {
                it.type is ILabelVarType || it.type is RegRefVarType
            }

            val paramCount =
                if (!inlineStackArgs && opcode.stack == StackInteraction.Pop) 0
                else opcode.params.size

            val argCount = tokens.count { it !is Token.ArgSeparator }

            val lastToken = tokens.lastOrNull()
            val errorLength = lastToken?.let { it.col + it.len - identToken.col } ?: 0
            // Inline arguments.
            val insArgAndTokens = mutableListOf<Pair<Arg, Token>>()
            // Stack arguments.
            val stackArgAndTokens = mutableListOf<Pair<Arg, Token>>()

            if (!varargs && argCount != paramCount) {
                addError(
                    identToken.col,
                    errorLength,
                    "Expected $paramCount argument ${if (paramCount == 1) "" else "s"}, got $argCount.",
                )

                return
            } else if (varargs && argCount < paramCount) {
                // TODO: This check assumes we want at least 1 argument for a vararg parameter.
                //       Is this correct?
                addError(
                    identToken.col,
                    errorLength,
                    "Expected at least $paramCount argument ${if (paramCount == 1) "" else "s"}, got $argCount.",
                )

                return
            } else if (opcode.stack !== StackInteraction.Pop) {
                // Arguments should be inlined right after the opcode.
                if (!parseArgs(opcode.params, insArgAndTokens, stack = false)) {
                    return
                }
            } else {
                // Arguments should be passed to the opcode via the stack.
                if (!parseArgs(opcode.params, stackArgAndTokens, stack = true)) {
                    return
                }

                for (i in opcode.params.indices) {
                    val param = opcode.params[i]
                    val argAndToken = stackArgAndTokens.getOrNull(i) ?: continue
                    val (arg, argToken) = argAndToken

                    if (argToken is Token.Register) {
                        if (param.type is RegTupRefType) {
                            addInstruction(
                                OP_ARG_PUSHB,
                                listOf(arg),
                                emptyList(),
                                null,
                                listOf(argToken),
                                emptyList(),
                            )
                        } else {
                            addInstruction(
                                OP_ARG_PUSHR,
                                listOf(arg),
                                emptyList(),
                                null,
                                listOf(argToken),
                                emptyList(),
                            )
                        }
                    } else {
                        when (param.type) {
                            is ByteType,
                            is RegRefType,
                            is RegTupRefType,
                            -> {
                                addInstruction(
                                    OP_ARG_PUSHB,
                                    listOf(arg),
                                    emptyList(),
                                    null,
                                    listOf(argToken),
                                    emptyList(),
                                )
                            }

                            is ShortType,
                            is LabelType,
                            is ILabelType,
                            is DLabelType,
                            is SLabelType,
                            -> {
                                addInstruction(
                                    OP_ARG_PUSHW,
                                    listOf(arg),
                                    emptyList(),
                                    null,
                                    listOf(argToken),
                                    emptyList(),
                                )
                            }

                            is IntType -> {
                                addInstruction(
                                    OP_ARG_PUSHL,
                                    listOf(arg),
                                    emptyList(),
                                    null,
                                    listOf(argToken),
                                    emptyList(),
                                )
                            }

                            is FloatType -> {
                                addInstruction(
                                    OP_ARG_PUSHL,
                                    listOf(Arg((arg.value as Float).toRawBits())),
                                    emptyList(),
                                    null,
                                    listOf(argToken),
                                    emptyList(),
                                )
                            }

                            is StringType -> {
                                addInstruction(
                                    OP_ARG_PUSHS,
                                    listOf(arg),
                                    emptyList(),
                                    null,
                                    listOf(argToken),
                                    emptyList(),
                                )
                            }

                            else -> {
                                logger.error {
                                    "Line $lineNo: Type ${param.type::class} not implemented."
                                }
                            }
                        }
                    }
                }
            }

            val (args, argTokens) = insArgAndTokens.unzip()
            val (stackArgs, stackArgTokens) = stackArgAndTokens.unzip()

            addInstruction(
                opcode,
                args,
                stackArgs,
                identToken,
                argTokens,
                stackArgTokens,
            )
        }
    }

    /**
     * Returns true iff arguments can be translated to byte code, possibly after truncation.
     */
    private fun parseArgs(
        params: List<Param>,
        argAndTokens: MutableList<Pair<Arg, Token>>,
        stack: Boolean,
    ): Boolean {
        var semiValid = true
        var shouldBeArg = true
        var paramI = 0

        for (i in 0 until tokens.size) {
            val token = tokens[i]
            val param = params[paramI]

            if (token is Token.ArgSeparator) {
                if (shouldBeArg) {
                    addError(token, "Expected an argument.")
                } else if (
                    param.type !is ILabelVarType &&
                    param.type !is RegRefVarType
                ) {
                    paramI++
                }

                shouldBeArg = true
            } else {
                if (!shouldBeArg) {
                    val prevToken = tokens[i - 1]
                    val col = prevToken.col + prevToken.len

                    addError(col, token.col - col, "Expected a comma.")
                }

                shouldBeArg = false

                var match: Boolean

                when (token) {
                    is Token.Int32 -> {
                        when (param.type) {
                            is ByteType -> {
                                match = true
                                parseInt(1, token, argAndTokens)
                            }
                            is ShortType,
                            is LabelType,
                            is ILabelType,
                            is DLabelType,
                            is SLabelType,
                            is ILabelVarType,
                            -> {
                                match = true
                                parseInt(2, token, argAndTokens)
                            }
                            is IntType -> {
                                match = true
                                parseInt(4, token, argAndTokens)
                            }
                            is FloatType -> {
                                match = true
                                argAndTokens.add(Pair(Arg(token.value), token))
                            }
                            else -> {
                                match = false
                            }
                        }
                    }

                    is Token.Float32 -> {
                        match = param.type == FloatType

                        if (match) {
                            argAndTokens.add(Pair(Arg(token.value), token))
                        }
                    }

                    is Token.Register -> {
                        match = stack ||
                                param.type is RegRefType ||
                                param.type is RegRefVarType ||
                                param.type is RegTupRefType

                        parseRegister(token, argAndTokens)
                    }

                    is Token.Str -> {
                        match = param.type is StringType

                        if (match) {
                            argAndTokens.add(Pair(Arg(token.value), token))
                        }
                    }

                    else -> {
                        match = false
                    }
                }

                if (!match) {
                    semiValid = false

                    val typeStr: String? = when (param.type) {
                        is ByteType -> "an 8-bit integer"
                        is ShortType -> "a 16-bit integer"
                        is IntType -> "a 32-bit integer"
                        is FloatType -> "a float"
                        is LabelType -> "a label"

                        is ILabelType,
                        is ILabelVarType,
                        -> "an instruction label"

                        is DLabelType -> "a data label"
                        is SLabelType -> "a string label"
                        is StringType -> "a string"

                        is RegRefType,
                        is RegRefVarType,
                        is RegTupRefType,
                        -> "a register reference"

                        else -> null
                    }

                    addError(
                        token,
                        if (typeStr == null) "Unexpected token." else "Expected ${typeStr}."
                    )
                }
            }
        }

        tokens.clear()
        return semiValid
    }

    private fun parseInt(
        size: Int,
        token: Token.Int32,
        argAndTokens: MutableList<Pair<Arg, Token>>,
    ) {
        val value = token.value
        val bitSize = 8 * size
        // Minimum of the signed version of this integer type.
        val minValue = -(1 shl (bitSize - 1))
        // Maximum of the unsigned version of this integer type.
        val maxValue = (1L shl (bitSize)) - 1L

        when {
            value < minValue -> {
                addError(token, "${bitSize}-Bit integer can't be less than ${minValue}.")
            }
            value > maxValue -> {
                addError(token, "${bitSize}-Bit integer can't be greater than ${maxValue}.")
            }
            else -> {
                argAndTokens.add(Pair(Arg(value), token))
            }
        }
    }

    private fun parseRegister(token: Token.Register, argAndTokens: MutableList<Pair<Arg, Token>>) {
        val value = token.value

        if (value > 255) {
            addError(token, "Invalid register reference, expected r0-r255.")
        } else {
            argAndTokens.add(Pair(Arg(value), token))
        }
    }

    private fun parseBytes(firstToken: Token.Int32) {
        val bytes = mutableListOf<Byte>()
        var token: Token = firstToken
        var i = 0

        while (token is Token.Int32) {
            if (token.value < 0) {
                addError(token, "Unsigned 8-bit integer can't be less than 0.")
            } else if (token.value > 255) {
                addError(token, "Unsigned 8-bit integer can't be greater than 255.")
            }

            bytes.add(token.value.toByte())

            if (i < tokens.size) {
                token = tokens[i++]
            } else {
                break
            }
        }

        if (i < tokens.size) {
            addError(token, "Expected an unsigned 8-bit integer.")
        }

        addBytes(bytes.toByteArray())
    }

    private fun parseString(token: Token.Str) {
        tokens.removeFirstOrNull()?.let { nextToken ->
            addUnexpectedTokenError(nextToken)
        }

        addString(token.value.replace("\n", "<cr>"))
    }

    private fun Token.srcLoc(): String = "$lineNo:$col"
}

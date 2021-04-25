package world.phantasmal.lib.asm

import mu.KotlinLogging
import world.phantasmal.core.Problem
import world.phantasmal.core.PwResult
import world.phantasmal.core.Severity
import world.phantasmal.lib.buffer.Buffer
import kotlin.time.measureTimedValue

private val logger = KotlinLogging.logger {}

class AssemblyProblem(
    severity: Severity,
    uiMessage: String,
    message: String? = null,
    cause: Throwable? = null,
    val lineNo: Int,
    val col: Int,
    val len: Int,
) : Problem(severity, uiMessage, message, cause)

fun assemble(
    asm: List<String>,
    inlineStackArgs: Boolean = true,
): PwResult<BytecodeIr> {
    logger.trace {
        "Assembling ${asm.size} lines with ${
            if (inlineStackArgs) "inline stack arguments" else "stack push instructions"
        }."
    }

    val (result, time) = measureTimedValue { Assembler(asm, inlineStackArgs).assemble() }

    logger.trace {
        val warnings = result.problems.count { it.severity == Severity.Warning }
        val errors = result.problems.count { it.severity == Severity.Error }

        "Assembly finished in ${time.inMilliseconds}ms with $warnings warnings and $errors errors."
    }

    return result
}

private class Assembler(private val asm: List<String>, private val inlineStackArgs: Boolean) {
    private var lineNo = 1
    private val tokenizer = LineTokenizer()
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

    private val result = PwResult.build<BytecodeIr>(logger)

    fun assemble(): PwResult<BytecodeIr> {
        // Tokenize and assemble line by line.
        for (line in asm) {
            tokenizer.tokenize(line)
            tokenizer.nextToken()

            if (tokenizer.type != null) {
                var hasLabel = false

                // Token type checks are ordered from most frequent to least frequent for increased
                // perf.
                when (tokenizer.type) {
                    Token.Ident -> {
                        if (section === SegmentType.Instructions) {
                            parseInstruction()
                        } else {
                            addUnexpectedTokenError()
                        }
                    }
                    Token.Label -> {
                        parseLabel()
                        hasLabel = true
                    }
                    Token.CodeSection -> {
                        parseCodeSection()
                    }
                    Token.DataSection -> {
                        parseDataSection()
                    }
                    Token.StrSection -> {
                        parseStrSection()
                    }
                    Token.Int32 -> {
                        if (section === SegmentType.Data) {
                            parseBytes()
                        } else {
                            addUnexpectedTokenError()
                        }
                    }
                    Token.Str -> {
                        if (section === SegmentType.String) {
                            parseString()
                        } else {
                            addUnexpectedTokenError()
                        }
                    }
                    Token.InvalidSection -> {
                        addError("Invalid section type.")
                    }
                    Token.InvalidIdent -> {
                        addError("Invalid identifier.")
                    }
                    else -> {
                        addUnexpectedTokenError()
                    }
                }

                prevLineHadLabel = hasLabel
            }

            lineNo++
        }

        return result.success(BytecodeIr(ir))
    }

    private fun addInstruction(
        opcode: Opcode,
        args: List<Arg>,
        mnemonicSrcLoc: SrcLoc?,
        valid: Boolean,
        argSrcLocs: List<ArgSrcLoc>,
        trailingArgSeparator: Boolean,
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
                        valid,
                        InstructionSrcLoc(
                            mnemonic = mnemonicSrcLoc,
                            args = argSrcLocs,
                            trailingArgSeparator,
                        ),
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
                    bytecodeSize = null,
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

    private fun addError(col: Int, len: Int, uiMessage: String, message: String? = null) {
        result.addProblem(
            AssemblyProblem(
                Severity.Error,
                uiMessage,
                message ?: "$uiMessage At $lineNo:$col.",
                lineNo = lineNo,
                col = col,
                len = len
            )
        )
    }

    private fun addError(uiMessage: String, message: String? = null) {
        addError(tokenizer.col, tokenizer.len, uiMessage, message)
    }

    private fun addUnexpectedTokenError() {
        addError(
            "Unexpected token.",
            "Unexpected ${tokenizer.type?.name} at $lineNo:${tokenizer.col}.",
        )
    }

    private fun addWarning(uiMessage: String) {
        result.addProblem(
            AssemblyProblem(
                Severity.Warning,
                uiMessage,
                lineNo = lineNo,
                col = tokenizer.col,
                len = tokenizer.len,
            )
        )
    }

    private fun parseLabel() {
        val label = tokenizer.intValue

        if (!labels.add(label)) {
            addError("Duplicate label.")
        }

        val srcLoc = srcLocFromTokenizer()

        if (prevLineHadLabel) {
            val segment = ir.last()
            segment.labels.add(label)
            segment.srcLoc.labels.add(srcLoc)
        }

        tokenizer.nextToken()

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

                if (tokenizer.type === Token.Ident) {
                    parseInstruction()
                } else if (tokenizer.type != null) {
                    addError("Expected opcode mnemonic.")
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

                if (tokenizer.type === Token.Int32) {
                    parseBytes()
                } else if (tokenizer.type != null) {
                    addError("Expected bytes.")
                }
            }

            SegmentType.String -> {
                if (!prevLineHadLabel) {
                    segment = StringSegment(
                        labels = mutableListOf(label),
                        value = "",
                        bytecodeSize = null,
                        srcLoc = SegmentSrcLoc(labels = mutableListOf(srcLoc)),
                    )
                    ir.add(segment!!)
                }

                if (tokenizer.type === Token.Str) {
                    parseString()
                } else if (tokenizer.type != null) {
                    addError("Expected a string.")
                }
            }
        }
    }

    private fun parseCodeSection() {
        parseSection(SegmentType.Instructions)
    }

    private fun parseDataSection() {
        parseSection(SegmentType.Data)
    }

    private fun parseStrSection() {
        parseSection(SegmentType.String)
    }

    private fun parseSection(section: SegmentType) {
        if (this.section == section && !firstSectionMarker) {
            addWarning("Unnecessary section marker.")
        }

        this.section = section
        firstSectionMarker = false

        if (tokenizer.nextToken()) {
            addUnexpectedTokenError()
        }
    }

    private fun parseInstruction() {
        val opcode = mnemonicToOpcode(tokenizer.strValue)
        val mnemonicSrcLoc = srcLocFromTokenizer()

        if (opcode == null) {
            addError("Unknown opcode.")
        } else {
            if (opcode.stack !== StackInteraction.Pop) {
                // Arguments should be inlined immediately after the opcode.
                parseArgs(
                    opcode,
                    mnemonicSrcLoc,
                    stack = false,
                )
            } else {
                // Arguments should be passed to the opcode via the stack.
                parseArgs(
                    opcode,
                    mnemonicSrcLoc,
                    stack = true,
                )
            }
        }
    }

    private fun parseArgs(opcode: Opcode, mnemonicSrcLoc: SrcLoc, stack: Boolean) {
        val immediateArgs = mutableListOf<Arg>()
        val srcLocs = mutableListOf<ArgSrcLoc>()
        var argCount = 0
        var valid = true
        var shouldBeArg = true
        var paramI = 0
        var prevToken: Token?
        var prevCol: Int
        var prevLen: Int
        var token = tokenizer.type
        var col = tokenizer.col
        var len = tokenizer.len

        tokenizer.nextToken()

        while (true) {
            // Previous token data.
            prevToken = token
            prevCol = col
            prevLen = len

            // Current token data.
            token = tokenizer.type
            col = tokenizer.col
            len = tokenizer.len
            val value = tokenizer.value

            if (token == null) {
                break
            }

            // Next token data.
            tokenizer.nextToken()
            val nextToken = tokenizer.type
            val nextCol = tokenizer.col
            val nextLen = tokenizer.len

            val param = opcode.params.getOrNull(paramI)
            val paramType = param?.type

            // Coarse source position, including surrounding whitespace.
            val coarseCol = prevCol + prevLen
            val coarseLen =
                if (nextToken === Token.ArgSeparator) nextCol + nextLen - coarseCol
                else nextCol - coarseCol

            if (token === Token.ArgSeparator) {
                if (shouldBeArg) {
                    addError("Expected an argument.")
                } else if (param == null || !param.varargs) {
                    paramI++
                }

                shouldBeArg = true
            } else {
                if (!shouldBeArg) {
                    addError(coarseCol, col - coarseCol, "Expected a comma.")
                }

                shouldBeArg = false

                argCount++

                // Try to match token type to parameter type.
                var typeMatch: Boolean

                // If arg is nonnull, types match and argument is syntactically valid.
                val arg: Arg = when (token) {
                    Token.Int32 -> {
                        value as Int

                        when (paramType) {
                            ByteType -> {
                                typeMatch = true
                                checkIntValue(col, len, value, 1)
                            }
                            ShortType,
                            is LabelType,
                            -> {
                                typeMatch = true
                                checkIntValue(col, len, value, 2)
                            }
                            IntType -> {
                                typeMatch = true
                                checkIntValue(col, len, value, 4)
                            }
                            FloatType -> {
                                typeMatch = true
                                FloatArg(value.toFloat())
                            }
                            else -> {
                                typeMatch = false
                                IntArg(value)
                            }
                        }
                    }

                    Token.Float32 -> {
                        typeMatch = paramType === FloatType
                        FloatArg(value as Float)
                    }

                    Token.Register -> {
                        typeMatch = stack ||
                                paramType === RegVarType ||
                                paramType is RegType

                        value as Int

                        if (value > 255) {
                            addError(col, len, "Invalid register reference, expected r0-r255.")
                        }

                        IntArg(value)
                    }

                    Token.Str -> {
                        typeMatch = paramType === StringType
                        StringArg(value as String)
                    }

                    else -> {
                        typeMatch = false
                        UnknownArg(value!!)
                    }
                }

                val srcLoc = ArgSrcLoc(
                    precise = SrcLoc(lineNo, col, len),
                    coarse = SrcLoc(lineNo, coarseCol, coarseLen),
                )

                if (!stack) {
                    immediateArgs.add(arg)
                }

                srcLocs.add(srcLoc)

                if (!typeMatch) {
                    valid = false

                    // Don't add a type errors for surplus arguments.
                    if (param != null) {
                        val typeStr = when (param.type) {
                            ByteType -> "an 8-bit integer"
                            ShortType -> "a 16-bit integer"
                            IntType -> "a 32-bit integer"
                            FloatType -> "a float"

                            ILabelType,
                            ILabelVarType,
                            -> "an instruction label"

                            DLabelType -> "a data label"
                            SLabelType -> "a string label"

                            is LabelType -> "a label"

                            StringType -> "a string"

                            RegVarType,
                            is RegType,
                            -> "a register reference"

                            PointerType -> "a pointer" // No known opcodes directly take a pointer.

                            AnyType.Instance -> "an argument" // Should never happen.
                        }

                        addError(col, len, "Expected ${typeStr}.")
                    }
                } else if (stack) {
                    // Inject stack push instructions if necessary.
                    checkNotNull(paramType)

                    // If the token is a register, push it as a register, otherwise coerce type.
                    if (token === Token.Register) {
                        if (paramType is RegType) {
                            addInstruction(
                                OP_ARG_PUSHB,
                                listOf(arg),
                                mnemonicSrcLoc = null,
                                valid = true,
                                listOf(srcLoc),
                                trailingArgSeparator = false,
                            )
                        } else {
                            addInstruction(
                                OP_ARG_PUSHR,
                                listOf(arg),
                                mnemonicSrcLoc = null,
                                valid = true,
                                listOf(srcLoc),
                                trailingArgSeparator = false,
                            )
                        }
                    } else {
                        when (paramType) {
                            ByteType,
                            is RegType,
                            -> {
                                addInstruction(
                                    OP_ARG_PUSHB,
                                    listOf(arg),
                                    mnemonicSrcLoc = null,
                                    valid = true,
                                    listOf(srcLoc),
                                    trailingArgSeparator = false,
                                )
                            }

                            ShortType,
                            is LabelType,
                            -> {
                                addInstruction(
                                    OP_ARG_PUSHW,
                                    listOf(arg),
                                    mnemonicSrcLoc = null,
                                    valid = true,
                                    listOf(srcLoc),
                                    trailingArgSeparator = false,
                                )
                            }

                            IntType -> {
                                addInstruction(
                                    OP_ARG_PUSHL,
                                    listOf(arg),
                                    mnemonicSrcLoc = null,
                                    valid = true,
                                    listOf(srcLoc),
                                    trailingArgSeparator = false,
                                )
                            }

                            // Floats are pushed as ints.
                            FloatType -> {
                                addInstruction(
                                    OP_ARG_PUSHL,
                                    listOf(IntArg((arg as FloatArg).value.toRawBits())),
                                    mnemonicSrcLoc = null,
                                    valid = true,
                                    listOf(srcLoc),
                                    trailingArgSeparator = false,
                                )
                            }

                            StringType -> {
                                addInstruction(
                                    OP_ARG_PUSHS,
                                    listOf(arg),
                                    mnemonicSrcLoc = null,
                                    valid = true,
                                    listOf(srcLoc),
                                    trailingArgSeparator = false,
                                )
                            }

                            else -> {
                                logger.error {
                                    "Line $lineNo: Type ${paramType::class} not implemented."
                                }
                            }
                        }
                    }
                }
            }
        }

        val paramCount =
            if (!inlineStackArgs && opcode.stack === StackInteraction.Pop) 0
            else opcode.params.size

        val trailingArgSeparator = prevToken === Token.ArgSeparator

        // Length from the start of the mnemonic until the end of the last token.
        val errorLength = prevCol + prevLen - mnemonicSrcLoc.col

        if (opcode.varargs) {
            // Argument count should be equal to or greater than the amount of parameters for variadic
            // opcodes.
            if (argCount < paramCount) {
                valid = false
                addError(
                    mnemonicSrcLoc.col,
                    errorLength,
                    "Expected at least $paramCount argument${
                        if (paramCount == 1) "" else "s"
                    }, got $argCount.",
                )
            }
        } else {
            // Argument count should match parameter count exactly for non-variadic opcodes.
            if (argCount != paramCount) {
                valid = false
                addError(
                    mnemonicSrcLoc.col,
                    errorLength,
                    "Expected $paramCount argument${
                        if (paramCount == 1) "" else "s"
                    }, got $argCount.",
                )
            }
        }

        // Trailing argument separators are not allowed.
        if (trailingArgSeparator) {
            addError(prevCol, prevLen, "Unexpected comma.")
        }

        addInstruction(opcode, immediateArgs, mnemonicSrcLoc, valid, srcLocs, trailingArgSeparator)
    }

    private fun checkIntValue(col: Int, len: Int, value: Int, size: Int): Arg {
        // Fast-path 32-bit ints for improved JS perf. Otherwise maxValue would have to be a Long
        // or UInt, which incurs a perf hit in JS.
        if (size != 4) {
            val bitSize = 8 * size
            // Minimum of the signed version of this integer type.
            val minValue = -(1 shl (bitSize - 1))
            // Maximum of the unsigned version of this integer type.
            val maxValue = (1 shl (bitSize)) - 1

            when {
                value < minValue -> {
                    addError(col, len, "${bitSize}-Bit integer can't be less than ${minValue}.")
                }
                value > maxValue -> {
                    addError(col, len, "${bitSize}-Bit integer can't be greater than ${maxValue}.")
                }
            }
        }

        return IntArg(value)
    }

    private fun parseBytes() {
        val bytes = mutableListOf<Byte>()

        while (tokenizer.type === Token.Int32) {
            val value = tokenizer.intValue

            if (value < 0) {
                addError("Unsigned 8-bit integer can't be less than 0.")
            } else if (value > 255) {
                addError("Unsigned 8-bit integer can't be greater than 255.")
            }

            bytes.add(value.toByte())

            tokenizer.nextToken()
        }

        if (tokenizer.type != null) {
            addError("Expected an unsigned 8-bit integer.")
        }

        addBytes(bytes.toByteArray())
    }

    private fun parseString() {
        addString(tokenizer.strValue.replace("\n", "<cr>"))

        if (tokenizer.nextToken()) {
            addUnexpectedTokenError()
        }
    }

    private fun srcLocFromTokenizer(): SrcLoc = SrcLoc(lineNo, tokenizer.col, tokenizer.len)
}

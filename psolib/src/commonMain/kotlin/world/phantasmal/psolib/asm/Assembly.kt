package world.phantasmal.psolib.asm

import mu.KotlinLogging
import world.phantasmal.core.Problem
import world.phantasmal.core.PwResult
import world.phantasmal.core.Severity
import world.phantasmal.psolib.buffer.Buffer
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
): PwResult<BytecodeIr> {
    logger.trace {
        "Assembling ${asm.size} lines."
    }

    val (result, time) = measureTimedValue { Assembler(asm).assemble() }

    logger.trace {
        val warnings = result.problems.count { it.severity == Severity.Warning }
        val errors = result.problems.count { it.severity == Severity.Error }

        "Assembly finished in ${time.inWholeMilliseconds}ms with $warnings warnings and $errors errors."
    }

    return result
}

private class Assembler(private val asm: List<String>) {
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
        val instruction = Instruction(
            opcode,
            args,
            valid,
            InstructionSrcLoc(
                mnemonic = mnemonicSrcLoc,
                args = argSrcLocs,
                trailingArgSeparator,
            ),
        )

        when (val seg = segment) {
            null -> {
                // Unreachable code, technically valid.
                segment = InstructionSegment(
                    labels = mutableListOf(),
                    instructions = mutableListOf(instruction),
                    srcLoc = SegmentSrcLoc()
                )

                ir.add(segment!!)
            }

            is InstructionSegment -> {
                seg.instructions.add(instruction)
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

        if (prevLineHadLabel && ir.isNotEmpty()) {
            val segment = ir.last()
            segment.labels.add(label)
            segment.srcLoc.labels.add(srcLoc)
        }

        tokenizer.nextToken()

        when (section) {
            SegmentType.Instructions -> {
                createSegmentAndParseInline(Token.Ident, ::parseInstruction, "Expected opcode mnemonic.") {
                    InstructionSegment(
                        labels = mutableListOf(label),
                        instructions = mutableListOf(),
                        srcLoc = SegmentSrcLoc(labels = mutableListOf(srcLoc)),
                    )
                }
            }

            SegmentType.Data -> {
                createSegmentAndParseInline(Token.Int32, ::parseBytes, "Expected bytes.") {
                    DataSegment(
                        labels = mutableListOf(label),
                        data = Buffer.withCapacity(0),
                        srcLoc = SegmentSrcLoc(labels = mutableListOf(srcLoc)),
                    )
                }
            }

            SegmentType.String -> {
                createSegmentAndParseInline(Token.Str, ::parseString, "Expected a string.") {
                    StringSegment(
                        labels = mutableListOf(label),
                        value = "",
                        bytecodeSize = null,
                        srcLoc = SegmentSrcLoc(labels = mutableListOf(srcLoc)),
                    )
                }
            }
        }
    }

    /**
     * Creates a new segment (if the previous line didn't have a label) and optionally parses inline
     * content on the same line as the label.
     */
    private fun createSegmentAndParseInline(
        inlineTokenType: Token,
        parseInline: () -> Unit,
        errorMessage: String,
        segmentFactory: () -> Segment,
    ) {
        if (!prevLineHadLabel) {
            segment = segmentFactory()
            ir.add(segment!!)
        }

        if (tokenizer.type === inlineTokenType) {
            parseInline()
        } else if (tokenizer.type != null) {
            addError(errorMessage)
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
            // All arguments are inlined into the instruction (normalized IR).
            parseArgs(opcode, mnemonicSrcLoc)
        }
    }

    private fun parseArgs(opcode: Opcode, mnemonicSrcLoc: SrcLoc) {
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
            prevToken = token
            prevCol = col
            prevLen = len

            token = tokenizer.type
            col = tokenizer.col
            len = tokenizer.len
            val value = tokenizer.value

            if (token == null) break

            tokenizer.nextToken()
            val nextToken = tokenizer.type
            val nextCol = tokenizer.col
            val nextLen = tokenizer.len

            val param = opcode.params.getOrNull(paramI)
            val paramType = param?.type

            // Coarse source position, including surrounding whitespace.
            val coarseCol = prevCol + prevLen
            val coarseLen = when (nextToken) {
                Token.ArgSeparator -> nextCol + nextLen - coarseCol
                null -> nextCol - coarseCol + 1
                else -> nextCol - coarseCol
            }

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

                val (arg, typeMatch) = parseArgToken(token, value, paramType, opcode, col, len)

                srcLocs.add(ArgSrcLoc(
                    precise = SrcLoc(lineNo, col, len),
                    coarse = SrcLoc(lineNo, coarseCol, coarseLen),
                ))
                immediateArgs.add(arg)

                if (!typeMatch) {
                    valid = false
                    if (param != null) {
                        addError(col, len, "Expected ${describeParamType(param.type)}.")
                    }
                }
            }
        }

        valid = validateArgCount(opcode, argCount, mnemonicSrcLoc, prevCol, prevLen, valid)

        val trailingArgSeparator = prevToken === Token.ArgSeparator
        if (trailingArgSeparator) {
            addError(prevCol, prevLen, "Unexpected comma.")
        }

        addInstruction(opcode, immediateArgs, mnemonicSrcLoc, valid, srcLocs, trailingArgSeparator)
    }

    /**
     * Converts a token to an [Arg] and returns whether the type matched the expected parameter type.
     */
    private fun parseArgToken(
        token: Token,
        value: Any?,
        paramType: AnyType?,
        opcode: Opcode,
        col: Int,
        len: Int,
    ): Pair<Arg, Boolean> = when (token) {
        Token.Int32 -> {
            value as Int
            when (paramType) {
                ByteType -> Pair(checkIntValue(col, len, value, 1), true)
                ShortType, is LabelType -> Pair(checkIntValue(col, len, value, 2), true)
                IntType -> Pair(checkIntValue(col, len, value, 4), true)
                FloatType -> Pair(FloatArg(value.toFloat()), true)
                else -> Pair(IntArg(value), false)
            }
        }

        Token.Float32 -> Pair(FloatArg(value as Float), paramType === FloatType)

        Token.Register -> {
            val typeMatch = paramType === RegVarType ||
                    paramType is RegType ||
                    opcode.stack === StackInteraction.Pop
            value as Int
            if (value > 255) {
                addError(col, len, "Invalid register reference, expected r0-r255.")
            }
            val arg = if (opcode.stack === StackInteraction.Pop && paramType !is RegType) {
                IntArg(value, isRegRef = true)
            } else {
                IntArg(value)
            }
            Pair(arg, typeMatch)
        }

        Token.Str -> Pair(StringArg(value as String), paramType === StringType)

        else -> Pair(UnknownArg(value), false)
    }

    private fun describeParamType(type: AnyType): String = when (type) {
        ByteType -> "an 8-bit integer"
        ShortType -> "a 16-bit integer"
        IntType -> "a 32-bit integer"
        FloatType -> "a float"
        ILabelType, ILabelVarType -> "an instruction label"
        DLabelType -> "a data label"
        SLabelType -> "a string label"
        is LabelType -> "a label"
        StringType -> "a string"
        RegVarType, is RegType -> "a register reference"
        PointerType -> "a pointer"
        AnyType.Instance -> "an argument"
    }

    private fun validateArgCount(
        opcode: Opcode,
        argCount: Int,
        mnemonicSrcLoc: SrcLoc,
        prevCol: Int,
        prevLen: Int,
        wasValid: Boolean,
    ): Boolean {
        val paramCount = opcode.params.size
        val errorLength = prevCol + prevLen - mnemonicSrcLoc.col

        val countOk = if (opcode.varargs) argCount >= paramCount else argCount == paramCount

        if (!countOk) {
            val atLeast = if (opcode.varargs) "at least " else ""
            addError(
                mnemonicSrcLoc.col,
                errorLength,
                "Expected ${atLeast}$paramCount argument${if (paramCount == 1) "" else "s"}, got $argCount.",
            )
            return false
        }

        return wasValid
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

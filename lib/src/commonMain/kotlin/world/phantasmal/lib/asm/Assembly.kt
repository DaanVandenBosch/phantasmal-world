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
        argSrcLocs: List<SrcLoc>,
        stackArgSrcLocs: List<SrcLoc>,
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
                            mnemonic = mnemonicSrcLoc,
                            args = argSrcLocs,
                            stackArgs = stackArgSrcLocs,
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
            // Inline arguments.
            val inlineArgs = mutableListOf<Arg>()
            val inlineArgSrcLocs = mutableListOf<SrcLoc>()
            // Stack arguments.
            val stackArgs = mutableListOf<Arg>()
            val stackArgSrcLocs = mutableListOf<SrcLoc>()

            if (opcode.stack !== StackInteraction.Pop) {
                // Arguments should be inlined right after the opcode.
                if (!parseArgs(
                        opcode,
                        mnemonicSrcLoc.col,
                        inlineArgs,
                        inlineArgSrcLocs,
                        stack = false,
                    )
                ) {
                    return
                }
            } else {
                // Arguments should be passed to the opcode via the stack.
                if (!parseArgs(
                        opcode,
                        mnemonicSrcLoc.col,
                        stackArgs,
                        stackArgSrcLocs,
                        stack = true,
                    )
                ) {
                    return
                }
            }

            addInstruction(
                opcode,
                inlineArgs,
                mnemonicSrcLoc,
                inlineArgSrcLocs,
                stackArgSrcLocs,
            )
        }
    }

    /**
     * Returns true iff arguments can be translated to byte code, possibly after truncation.
     */
    private fun parseArgs(
        opcode: Opcode,
        startCol: Int,
        args: MutableList<Arg>,
        srcLocs: MutableList<SrcLoc>,
        stack: Boolean,
    ): Boolean {
        var varargs = false
        var argCount = 0
        var semiValid = true
        var shouldBeArg = true
        var paramI = 0
        var prevCol = 0
        var prevLen = 0

        while (tokenizer.nextToken()) {
            if (tokenizer.type !== Token.ArgSeparator) {
                argCount++
            }

            if (paramI < opcode.params.size) {
                val param = opcode.params[paramI]

                if (param.type === ILabelVarType || param.type === RegRefVarType) {
                    // A varargs parameter is always the last parameter.
                    varargs = true
                }

                if (tokenizer.type === Token.ArgSeparator) {
                    if (shouldBeArg) {
                        addError("Expected an argument.")
                    } else if (!varargs) {
                        paramI++
                    }

                    shouldBeArg = true
                } else {
                    if (!shouldBeArg) {
                        val col = prevCol + prevLen
                        addError(col, tokenizer.col - col, "Expected a comma.")
                    }

                    shouldBeArg = false

                    // Try to match token type parameter type.
                    var typeMatch: Boolean

                    // If arg is nonnull, types match and argument is syntactically valid.
                    val arg: Arg? = when (tokenizer.type) {
                        Token.Int32 -> {
                            when (param.type) {
                                ByteType -> {
                                    typeMatch = true
                                    parseInt(1)
                                }
                                ShortType,
                                is LabelType,
                                -> {
                                    typeMatch = true
                                    parseInt(2)
                                }
                                IntType -> {
                                    typeMatch = true
                                    parseInt(4)
                                }
                                FloatType -> {
                                    typeMatch = true
                                    Arg(tokenizer.intValue.toFloat())
                                }
                                else -> {
                                    typeMatch = false
                                    null
                                }
                            }
                        }

                        Token.Float32 -> {
                            typeMatch = param.type === FloatType

                            if (typeMatch) {
                                Arg(tokenizer.floatValue)
                            } else {
                                null
                            }
                        }

                        Token.Register -> {
                            typeMatch = stack ||
                                    param.type === RegRefType ||
                                    param.type === RegRefVarType ||
                                    param.type is RegTupRefType

                            parseRegister()
                        }

                        Token.Str -> {
                            typeMatch = param.type === StringType

                            if (typeMatch) {
                                Arg(tokenizer.strValue)
                            } else {
                                null
                            }
                        }

                        else -> {
                            typeMatch = false
                            null
                        }
                    }

                    val srcLoc = srcLocFromTokenizer()

                    if (arg != null) {
                        args.add(arg)
                        srcLocs.add(srcLoc)
                    }

                    if (!typeMatch) {
                        semiValid = false

                        val typeStr: String? = when (param.type) {
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

                            RegRefType,
                            RegRefVarType,
                            is RegTupRefType,
                            -> "a register reference"

                            else -> null
                        }

                        addError(
                            if (typeStr == null) "Unexpected token." else "Expected ${typeStr}."
                        )
                    } else if (stack && arg != null) {
                        // Inject stack push instructions if necessary.
                        // If the token is a register, push it as a register, otherwise coerce type.
                        if (tokenizer.type === Token.Register) {
                            if (param.type is RegTupRefType) {
                                addInstruction(
                                    OP_ARG_PUSHB,
                                    listOf(arg),
                                    null,
                                    listOf(srcLoc),
                                    emptyList(),
                                )
                            } else {
                                addInstruction(
                                    OP_ARG_PUSHR,
                                    listOf(arg),
                                    null,
                                    listOf(srcLoc),
                                    emptyList(),
                                )
                            }
                        } else {
                            when (param.type) {
                                ByteType,
                                RegRefType,
                                is RegTupRefType,
                                -> {
                                    addInstruction(
                                        OP_ARG_PUSHB,
                                        listOf(arg),
                                        null,
                                        listOf(srcLoc),
                                        emptyList(),
                                    )
                                }

                                ShortType,
                                is LabelType,
                                -> {
                                    addInstruction(
                                        OP_ARG_PUSHW,
                                        listOf(arg),
                                        null,
                                        listOf(srcLoc),
                                        emptyList(),
                                    )
                                }

                                IntType -> {
                                    addInstruction(
                                        OP_ARG_PUSHL,
                                        listOf(arg),
                                        null,
                                        listOf(srcLoc),
                                        emptyList(),
                                    )
                                }

                                FloatType -> {
                                    addInstruction(
                                        OP_ARG_PUSHL,
                                        listOf(Arg((arg.value as Float).toRawBits())),
                                        null,
                                        listOf(srcLoc),
                                        emptyList(),
                                    )
                                }

                                StringType -> {
                                    addInstruction(
                                        OP_ARG_PUSHS,
                                        listOf(arg),
                                        null,
                                        listOf(srcLoc),
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
            }

            prevCol = tokenizer.col
            prevLen = tokenizer.len
        }

        val paramCount =
            if (!inlineStackArgs && opcode.stack === StackInteraction.Pop) 0
            else opcode.params.size

        val errorLength = prevCol + prevLen - startCol

        if (!varargs && argCount != paramCount) {
            addError(
                startCol,
                errorLength,
                "Expected $paramCount argument${
                    if (paramCount == 1) "" else "s"
                }, got $argCount.",
            )
        } else if (varargs && argCount < paramCount) {
            // TODO: This check assumes we want at least 1 argument for a vararg parameter.
            //       Is this correct?
            addError(
                startCol,
                errorLength,
                "Expected at least $paramCount argument${
                    if (paramCount == 1) "" else "s"
                }, got $argCount.",
            )
        }

        return semiValid
    }

    private fun parseInt(size: Int): Arg? {
        val value = tokenizer.intValue

        // Fast-path 32-bit ints for improved JS perf. Otherwise maxValue would have to be a Long
        // or UInt, which incurs a perf hit in JS.
        if (size == 4) {
            return Arg(value)
        } else {
            val bitSize = 8 * size
            // Minimum of the signed version of this integer type.
            val minValue = -(1 shl (bitSize - 1))
            // Maximum of the unsigned version of this integer type.
            val maxValue = (1 shl (bitSize)) - 1

            return when {
                value < minValue -> {
                    addError("${bitSize}-Bit integer can't be less than ${minValue}.")
                    null
                }
                value > maxValue -> {
                    addError("${bitSize}-Bit integer can't be greater than ${maxValue}.")
                    null
                }
                else -> {
                    Arg(value)
                }
            }
        }
    }

    private fun parseRegister(): Arg? {
        val value = tokenizer.intValue

        return if (value > 255) {
            addError("Invalid register reference, expected r0-r255.")
            null
        } else {
            Arg(value)
        }
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

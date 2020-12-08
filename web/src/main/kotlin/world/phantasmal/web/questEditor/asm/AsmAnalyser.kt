package world.phantasmal.web.questEditor.asm

import world.phantasmal.core.*
import world.phantasmal.lib.asm.*
import world.phantasmal.lib.asm.dataFlowAnalysis.getMapDesignations
import world.phantasmal.observable.value.Val
import world.phantasmal.observable.value.list.ListVal
import world.phantasmal.observable.value.list.mutableListVal
import world.phantasmal.observable.value.mutableVal
import kotlin.math.min

// TODO: Delegate to web worker?
@Suppress("ObjectPropertyName") // Suppress warnings about private properties starting with "_".
object AsmAnalyser {
    private val KEYWORD_REGEX = Regex("""^\s*\.[a-z]+${'$'}""")
    private val KEYWORD_SUGGESTIONS: List<CompletionItem> =
        listOf(
            CompletionItem(
                label = ".code",
                type = CompletionItemType.Keyword,
                insertText = "code",
            ),
            CompletionItem(
                label = ".data",
                type = CompletionItemType.Keyword,
                insertText = "data",
            ),
            CompletionItem(
                label = ".string",
                type = CompletionItemType.Keyword,
                insertText = "string",
            ),
        )

    private val INSTRUCTION_REGEX = Regex("""^\s*([a-z][a-z0-9_=<>!]*)?${'$'}""")
    private val INSTRUCTION_SUGGESTIONS: List<CompletionItem> =
        (OPCODES + OPCODES_F8 + OPCODES_F9)
            .filterNotNull()
            .map { opcode ->
                CompletionItem(
                    label = opcode.mnemonic,
                    // TODO: Add signature?
                    type = CompletionItemType.Opcode,
                    insertText = opcode.mnemonic,
                )
            }

    private var inlineStackArgs: Boolean = true
    private val asm: JsArray<String> = jsArrayOf()
    private var _bytecodeIr = mutableVal(BytecodeIr(emptyList()))
    private var _mapDesignations = mutableVal<Map<Int, Int>>(emptyMap())
    private val _problems = mutableListVal<AssemblyProblem>()

    val bytecodeIr: Val<BytecodeIr> = _bytecodeIr
    val mapDesignations: Val<Map<Int, Int>> = _mapDesignations
    val problems: ListVal<AssemblyProblem> = _problems

    suspend fun setAsm(asm: List<String>, inlineStackArgs: Boolean) {
        this.inlineStackArgs = inlineStackArgs
        this.asm.splice(0, this.asm.length, *asm.toTypedArray())

        processAsm()
    }

    suspend fun updateAssembly(changes: List<AsmChange>) {
        for (change in changes) {
            val (startLineNo, startCol, endLineNo, endCol) = change.range
            val linesChanged = endLineNo - startLineNo + 1
            val newLines = change.newAsm.split("\n").toJsArray()

            when {
                linesChanged == 1 -> {
                    replaceLinePart(startLineNo, startCol, endCol, newLines)
                }

                newLines.length == 1 -> {
                    replaceLinesAndMergeLineParts(
                        startLineNo,
                        endLineNo,
                        startCol,
                        endCol,
                        newLines[0],
                    )
                }

                else -> {
                    // Keep the left part of the first changed line.
                    replaceLinePartRight(startLineNo, startCol, newLines[0])

                    // Keep the right part of the last changed line.
                    replaceLinePartLeft(endLineNo, endCol, newLines[newLines.length - 1])

                    // Replace all the lines in between.
                    // It's important that we do this last.
                    replaceLines(
                        startLineNo + 1,
                        endLineNo - 1,
                        newLines.slice(1, newLines.length - 1),
                    )
                }
            }
        }

        processAsm()
    }

    private fun replaceLinePart(
        lineNo: Int,
        startCol: Int,
        endCol: Int,
        newLineParts: JsArray<String>,
    ) {
        val line = asm[lineNo - 1]
        // We keep the parts of the line that weren't affected by the edit.
        val lineStart = line.substring(0, startCol - 1)
        val lineEnd = line.substring(endCol - 1)

        if (newLineParts.length == 1) {
            asm[lineNo - 1] = lineStart + newLineParts[0] + lineEnd
        } else {
            asm.splice(
                lineNo - 1,
                1,
                lineStart + newLineParts[0],
                *newLineParts.slice(1, newLineParts.length - 1).asArray(),
                newLineParts[newLineParts.length - 1] + lineEnd,
            )
        }
    }

    private fun replaceLinePartLeft(lineNo: Int, endCol: Int, newLinePart: String) {
        asm[lineNo - 1] = newLinePart + asm[lineNo - 1].substring(endCol - 1)
    }

    private fun replaceLinePartRight(lineNo: Int, startCol: Int, newLinePart: String) {
        asm[lineNo - 1] = asm[lineNo - 1].substring(0, startCol - 1) + newLinePart
    }

    private fun replaceLines(startLineNo: Int, endLineNo: Int, newLines: JsArray<String>) {
        asm.splice(startLineNo - 1, endLineNo - startLineNo + 1, *newLines.asArray())
    }

    private fun replaceLinesAndMergeLineParts(
        startLineNo: Int,
        endLineNo: Int,
        startCol: Int,
        endCol: Int,
        newLinePart: String,
    ) {
        val startLine = asm[startLineNo - 1]
        val endLine = asm[endLineNo - 1]
        // We keep the parts of the lines that weren't affected by the edit.
        val startLineStart = startLine.substring(0, startCol - 1)
        val endLineEnd = endLine.substring(endCol - 1)

        asm.splice(
            startLineNo - 1,
            endLineNo - startLineNo + 1,
            startLineStart + newLinePart + endLineEnd,
        )
    }

    private fun processAsm() {
        val assemblyResult = assemble(asm.asArray().toList(), inlineStackArgs)

        @Suppress("UNCHECKED_CAST")
        _problems.value = assemblyResult.problems as List<AssemblyProblem>

        if (assemblyResult is Success) {
            val bytecodeIr = assemblyResult.value
            _bytecodeIr.value = bytecodeIr

            val instructionSegments = bytecodeIr.instructionSegments()

            instructionSegments.find { 0 in it.labels }?.let { label0Segment ->
                _mapDesignations.value = getMapDesignations(instructionSegments, label0Segment)
            }
        }
    }

    suspend fun getCompletions(lineNo: Int, col: Int): List<CompletionItem> {
        val text = getLine(lineNo)?.take(col) ?: ""

        return when {
            KEYWORD_REGEX.matches(text) -> KEYWORD_SUGGESTIONS
            INSTRUCTION_REGEX.matches(text) -> INSTRUCTION_SUGGESTIONS
            else -> emptyList()
        }
    }

    suspend fun getSignatureHelp(lineNo: Int, col: Int): SignatureHelp? {
        // Hacky way of providing parameter hints.
        // We just tokenize the current line and look for the first identifier and check whether
        // it's a valid opcode.
        var signature: Signature? = null
        var activeParam = -1

        getLine(lineNo)?.let { text ->
            val tokens = tokenizeLine(text)

            tokens.find { it is Token.Ident }?.let { ident ->
                ident as Token.Ident

                mnemonicToOpcode(ident.value)?.let { opcode ->
                    signature = getSignature(opcode)

                    for (tkn in tokens) {
                        if (tkn.col + tkn.len > col) {
                            break
                        } else if (tkn is Token.Ident && activeParam == -1) {
                            activeParam = 0
                        } else if (tkn is Token.ArgSeparator) {
                            activeParam++
                        }
                    }
                }
            }
        }

        return signature?.let { sig ->
            SignatureHelp(
                signature = sig,
                activeParameter = activeParam,
            )
        }
    }

    private fun getSignature(opcode: Opcode): Signature {
        var signature = opcode.mnemonic + " "
        val params = mutableListOf<Parameter>()
        var first = true

        for (param in opcode.params) {
            if (first) {
                first = false
            } else {
                signature += ", "
            }

            val paramTypeStr = when (param.type) {
                ByteType -> "Byte"
                ShortType -> "Short"
                IntType -> "Int"
                FloatType -> "Float"
                ILabelType -> "&Function"
                DLabelType -> "&Data"
                SLabelType -> "&String"
                ILabelVarType -> "...&Function"
                StringType -> "String"
                RegRefType, is RegTupRefType -> "Register"
                RegRefVarType -> "...Register"
                PointerType -> "Pointer"
                else -> "Any"
            }

            params.add(
                Parameter(
                    labelStart = signature.length,
                    labelEnd = signature.length + paramTypeStr.length,
                    documentation = param.doc,
                )
            )

            signature += paramTypeStr
        }

        return Signature(
            label = signature,
            documentation = opcode.doc,
            parameters = params,
        )
    }

    suspend fun getHover(lineNo: Int, col: Int): Hover? {
        val help = getSignatureHelp(lineNo, col)
            ?: return null

        val sig = help.signature
        val param = sig.parameters.getOrNull(help.activeParameter)

        val contents = mutableListOf<String>()

        // Instruction signature. Parameter highlighted if possible.
        contents.add(
            if (param == null) {
                sig.label
            } else {
                // TODO: Figure out how to underline the active parameter in addition to
                //  bolding it to make it match the look of the signature help.
                sig.label.substring(0, param.labelStart) +
                        "__" +
                        sig.label.substring(param.labelStart, param.labelEnd) +
                        "__" +
                        sig.label.substring(param.labelEnd)
            }
        )

        // Put the parameter doc and the instruction doc in the same string to match the look of the
        // signature help.
        var doc = ""

        // Parameter doc.
        if (param?.documentation != null) {
            doc += param.documentation

            // TODO: Figure out how add an empty line here to make it match the look of the
            //  signature help.
            doc += "\n\n"
        }

        // Instruction doc.
        sig.documentation?.let { doc += it }

        if (doc.isNotEmpty()) {
            contents.add(doc)
        }

        return Hover(contents)
    }

    suspend fun getDefinition(lineNo: Int, col: Int): List<AsmRange> {
        getInstruction(lineNo, col)?.let { inst ->
            for ((paramIdx, param) in inst.opcode.params.withIndex()) {
                if (param.type is LabelType) {
                    if (inst.opcode.stack != StackInteraction.Pop) {
                        // Immediate arguments.
                        val args = inst.getArgs(paramIdx)
                        val argSrcLocs = inst.getArgSrcLocs(paramIdx)

                        for (i in 0 until min(args.size, argSrcLocs.size)) {
                            val arg = args[i]
                            val srcLoc = argSrcLocs[i]

                            if (positionInside(lineNo, col, srcLoc)) {
                                val label = arg.value as Int
                                return getLabelDefinitions(label)
                            }
                        }
                    } else {
                        // Stack arguments.
                        val argSrcLocs = inst.getStackArgSrcLocs(paramIdx)

                        for (srcLoc in argSrcLocs) {
                            if (positionInside(lineNo, col, srcLoc)) {
                                val label = srcLoc.value as Int
                                return getLabelDefinitions(label)
                            }
                        }
                    }
                }
            }
        }

        return emptyList()
    }

    private fun getInstruction(lineNo: Int, col: Int): Instruction? {
        for (segment in bytecodeIr.value.segments) {
            if (segment is InstructionSegment) {
                // Loop over instructions in reverse order so stack popping instructions will be
                // handled before the related stack pushing instructions when inlineStackArgs is on.
                for (i in segment.instructions.lastIndex downTo 0) {
                    val inst = segment.instructions[i]

                    inst.srcLoc?.let { srcLoc ->
                        if (positionInside(lineNo, col, srcLoc.mnemonic)) {
                            return inst
                        }

                        for (argSrcLoc in srcLoc.args) {
                            if (positionInside(lineNo, col, argSrcLoc)) {
                                return inst
                            }
                        }

                        if (inlineStackArgs) {
                            for (argSrcLoc in srcLoc.stackArgs) {
                                if (positionInside(lineNo, col, argSrcLoc)) {
                                    return inst
                                }
                            }
                        }
                    }
                }
            }
        }

        return null
    }

    private fun getLabelDefinitions(label: Int): List<AsmRange> =
        bytecodeIr.value.segments.asSequence()
            .filter { label in it.labels }
            .mapNotNull { segment ->
                val labelIdx = segment.labels.indexOf(label)

                segment.srcLoc.labels.getOrNull(labelIdx)?.let { labelSrcLoc ->
                    AsmRange(
                        startLineNo = labelSrcLoc.lineNo,
                        startCol = labelSrcLoc.col,
                        endLineNo = labelSrcLoc.lineNo,
                        endCol = labelSrcLoc.col + labelSrcLoc.len,
                    )
                }
            }
            .toList()

    private fun positionInside(lineNo: Int, col: Int, srcLoc: SrcLoc?): Boolean =
        if (srcLoc == null) {
            false
        } else {
            lineNo == srcLoc.lineNo && col >= srcLoc.col && col <= srcLoc.col + srcLoc.len
        }

    @Suppress("RedundantNullableReturnType") // Can return undefined.
    private fun getLine(lineNo: Int): String? = asm[lineNo - 1]
}

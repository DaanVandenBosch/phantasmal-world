package world.phantasmal.web.assemblyWorker

import world.phantasmal.core.*
import world.phantasmal.lib.asm.*
import world.phantasmal.lib.asm.dataFlowAnalysis.ControlFlowGraph
import world.phantasmal.lib.asm.dataFlowAnalysis.getMapDesignations
import world.phantasmal.lib.asm.dataFlowAnalysis.getStackValue
import world.phantasmal.web.shared.messages.*
import world.phantasmal.web.shared.messages.AssemblyProblem
import kotlin.math.min
import world.phantasmal.lib.asm.AssemblyProblem as LibAssemblyProblem

class AsmAnalyser {
    // User input.
    private var inlineStackArgs: Boolean = true
    private val asm: JsArray<String> = jsArrayOf()

    // Output.
    private var bytecodeIr = BytecodeIr(emptyList())
    private var problems: List<AssemblyProblem>? = null

    // Derived data.
    private var _cfg: ControlFlowGraph? = null
    private val cfg: ControlFlowGraph
        get() {
            if (_cfg == null) _cfg = ControlFlowGraph.create(bytecodeIr)
            return _cfg!!
        }

    private var mapDesignations: Map<Int, Int>? = null

    fun setAsm(asm: List<String>, inlineStackArgs: Boolean) {
        this.inlineStackArgs = inlineStackArgs
        this.asm.splice(0, this.asm.length, *asm.toTypedArray())
        mapDesignations = null
    }

    fun updateAsm(changes: List<AsmChange>) {
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

    fun processAsm(): List<ServerNotification> {
        _cfg = null

        val notifications = mutableListOf<ServerNotification>()
        val assemblyResult = assemble(asm.asArray().toList(), inlineStackArgs)

        @Suppress("UNCHECKED_CAST")
        val problems =
            (assemblyResult.problems as List<LibAssemblyProblem>).map {
                AssemblyProblem(it.severity, it.uiMessage, it.lineNo, it.col, it.len)
            }

        if (problems != this.problems) {
            this.problems = problems
            notifications.add(ServerNotification.Problems(problems))
        }

        if (assemblyResult is Success) {
            bytecodeIr = assemblyResult.value

            val instructionSegments = bytecodeIr.instructionSegments()

            instructionSegments.find { 0 in it.labels }?.let { label0Segment ->
                val designations = getMapDesignations(label0Segment) { cfg }

                if (designations != mapDesignations) {
                    mapDesignations = designations
                    notifications.add(
                        ServerNotification.MapDesignations(designations)
                    )
                }
            }
        }

        return notifications
    }

    fun getCompletions(requestId: Int, lineNo: Int, col: Int): Response.GetCompletions {
        val text = getLine(lineNo)?.take(col)?.trim()?.toLowerCase() ?: ""

        val completions: List<CompletionItem> = when {
            KEYWORD_REGEX.matches(text) -> KEYWORD_SUGGESTIONS

            INSTRUCTION_REGEX.matches(text) -> {
                val suggestions = INSTRUCTION_SUGGESTIONS.asSequence()
                val startingWith = suggestions.filter { it.label.startsWith(text) }
                val containing = suggestions.filter { it.label.contains(text) }

                (startingWith + containing)
                    .take(20)
                    .toList()
            }

            else -> emptyList()
        }

        return Response.GetCompletions(requestId, completions)
    }

    fun getSignatureHelp(requestId: Int, lineNo: Int, col: Int): Response.GetSignatureHelp =
        Response.GetSignatureHelp(requestId, signatureHelp(lineNo, col))

    private fun signatureHelp(lineNo: Int, col: Int): SignatureHelp? {
        var signature: Signature? = null
        var activeParam = -1

        getInstructionForSrcLoc(lineNo, col)?.let { (inst, argIdx) ->
            signature = getSignature(inst.opcode)
            activeParam = argIdx
        }

        return signature?.let { sig ->
            SignatureHelp(
                signature = sig,
                activeParameter = activeParam,
            )
        }
    }

    fun getHover(requestId: Int, lineNo: Int, col: Int): Response.GetHover {
        val hover = signatureHelp(lineNo, col)?.let { help ->
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

            Hover(contents)
        }

        return Response.GetHover(requestId, hover)
    }

    fun getDefinition(requestId: Int, lineNo: Int, col: Int): Response.GetDefinition {
        var result = emptyList<AsmRange>()

        getInstructionForSrcLoc(lineNo, col)?.inst?.let { inst ->
            loop@
            for ((paramIdx, param) in inst.opcode.params.withIndex()) {
                if (param.type is LabelType) {
                    if (inst.opcode.stack != StackInteraction.Pop) {
                        // Immediate arguments.
                        val args = inst.getArgs(paramIdx)
                        val argSrcLocs = inst.getArgSrcLocs(paramIdx)

                        for (i in 0 until min(args.size, argSrcLocs.size)) {
                            val arg = args[i]
                            val srcLoc = argSrcLocs[i].coarse

                            if (positionInside(lineNo, col, srcLoc)) {
                                val label = arg.value as Int
                                result = getLabelDefinitions(label)
                                break@loop
                            }
                        }
                    } else {
                        // Stack arguments.
                        val argSrcLocs = inst.getStackArgSrcLocs(paramIdx)

                        for ((i, argSrcLoc) in argSrcLocs.withIndex()) {
                            if (positionInside(lineNo, col, argSrcLoc.coarse)) {
                                val labelValues = getStackValue(cfg, inst, argSrcLocs.lastIndex - i)

                                if (labelValues.size <= 5) {
                                    result = labelValues.flatMap(::getLabelDefinitions)
                                }

                                break@loop
                            }
                        }
                    }
                }
            }
        }

        return Response.GetDefinition(requestId, result)
    }

    private fun getLabelDefinitions(label: Int): List<AsmRange> =
        bytecodeIr.segments.asSequence()
            .filter { label in it.labels }
            .mapNotNull { segment ->
                val labelIdx = segment.labels.indexOf(label)
                segment.srcLoc.labels.getOrNull(labelIdx)?.toAsmRange()
            }
            .toList()

    fun getLabels(requestId: Int): Response.GetLabels {
        val result = bytecodeIr.segments.asSequence()
            .flatMap { segment ->
                segment.labels.mapIndexed { labelIdx, label ->
                    val range = segment.srcLoc.labels.getOrNull(labelIdx)?.toAsmRange()
                    Label(name = label, range)
                }
            }
            .toList()

        return Response.GetLabels(requestId, result)
    }

    fun getHighlights(requestId: Int, lineNo: Int, col: Int): Response.GetHighlights {
        val result = mutableListOf<AsmRange>()

        when (val ir = getIrForSrcLoc(lineNo, col)) {
            is Ir.Inst -> {
                val srcLoc = ir.inst.srcLoc?.mnemonic

                if (ir.argIdx == -1 ||
                    // Also return this instruction if we're right past the mnemonic. E.g. at the
                    // first whitespace character preceding the first argument.
                    (srcLoc != null && col <= srcLoc.col + srcLoc.len)
                ) {
                    for (segment in bytecodeIr.segments) {
                        if (segment is InstructionSegment) {
                            for (inst in segment.instructions) {
                                if (inst.opcode.code == ir.inst.opcode.code) {
                                    inst.srcLoc?.mnemonic?.toAsmRange()?.let(result::add)
                                }
                            }
                        }
                    }
                }
            }
        }

        return Response.GetHighlights(requestId, result)
    }

    private fun getInstructionForSrcLoc(lineNo: Int, col: Int): Ir.Inst? =
        getIrForSrcLoc(lineNo, col) as? Ir.Inst

    private fun getIrForSrcLoc(lineNo: Int, col: Int): Ir? {
        for (segment in bytecodeIr.segments) {
            if (segment is InstructionSegment) {
                // Loop over instructions in reverse order so stack popping instructions will be
                // handled before the related stack pushing instructions when inlineStackArgs is on.
                for (i in segment.instructions.lastIndex downTo 0) {
                    val inst = segment.instructions[i]

                    inst.srcLoc?.let { srcLoc ->
                        var instLineNo = -1
                        var lastCol = -1

                        srcLoc.mnemonic?.let { mnemonicSrcLoc ->
                            instLineNo = mnemonicSrcLoc.lineNo
                            lastCol = mnemonicSrcLoc.col + mnemonicSrcLoc.len

                            if (positionInside(lineNo, col, mnemonicSrcLoc)) {
                                return Ir.Inst(inst, argIdx = -1)
                            }
                        }

                        for ((argIdx, argSrcLoc) in srcLoc.args.withIndex()) {
                            instLineNo = argSrcLoc.coarse.lineNo
                            lastCol = argSrcLoc.coarse.col + argSrcLoc.coarse.len

                            if (positionInside(lineNo, col, argSrcLoc.coarse)) {
                                return Ir.Inst(inst, argIdx)
                            }
                        }

                        if (inlineStackArgs) {
                            for ((argIdx, argSrcLoc) in srcLoc.stackArgs.withIndex()) {
                                instLineNo = argSrcLoc.coarse.lineNo
                                lastCol = argSrcLoc.coarse.col + argSrcLoc.coarse.len

                                if (positionInside(lineNo, col, argSrcLoc.coarse)) {
                                    return Ir.Inst(inst, argIdx)
                                }
                            }
                        }

                        if (lineNo == instLineNo && col >= lastCol) {
                            return Ir.Inst(
                                inst,
                                if (inlineStackArgs && inst.opcode.stack === StackInteraction.Pop) {
                                    srcLoc.stackArgs.lastIndex
                                } else {
                                    srcLoc.args.lastIndex
                                },
                            )
                        }
                    }
                }
            }
        }

        return null
    }

    private fun positionInside(lineNo: Int, col: Int, srcLoc: SrcLoc?): Boolean =
        if (srcLoc == null) {
            false
        } else {
            lineNo == srcLoc.lineNo && col >= srcLoc.col && col < srcLoc.col + srcLoc.len
        }

    @Suppress("RedundantNullableReturnType") // Can return undefined.
    private fun getLine(lineNo: Int): String? = asm[lineNo - 1]

    private fun SrcLoc.toAsmRange(): AsmRange =
        AsmRange(
            startLineNo = lineNo,
            startCol = col,
            endLineNo = lineNo,
            endCol = col + len,
        )

    private sealed class Ir {
        data class Inst(val inst: Instruction, val argIdx: Int) : Ir()
    }

    companion object {
        private val KEYWORD_REGEX = Regex("""^\s*\.[a-z]+${'$'}""")
        private val KEYWORD_SUGGESTIONS: List<CompletionItem> =
            listOf(
                CompletionItem(
                    label = ".code",
                    type = CompletionItemType.Keyword,
                    detail = null,
                    documentation = "Start of a code segment",
                    insertText = "code",
                ),
                CompletionItem(
                    label = ".data",
                    type = CompletionItemType.Keyword,
                    detail = null,
                    documentation = "Start of a data segment",
                    insertText = "data",
                ),
                CompletionItem(
                    label = ".string",
                    type = CompletionItemType.Keyword,
                    detail = null,
                    documentation = "Start of a string data segment",
                    insertText = "string",
                ),
            )

        private val INSTRUCTION_REGEX = Regex("""^\s*([a-z][a-z0-9_=<>!]*)?${'$'}""")
        private val INSTRUCTION_SUGGESTIONS: List<CompletionItem> =
            (OPCODES.asSequence() + OPCODES_F8.asSequence() + OPCODES_F9.asSequence())
                .filterNotNull()
                .map { opcode ->
                    val sig = getSignature(opcode)
                    CompletionItem(
                        label = opcode.mnemonic,
                        type = CompletionItemType.Opcode,
                        detail = sig.label,
                        documentation = sig.documentation,
                        insertText = "${opcode.mnemonic} ",
                    )
                }
                .sortedBy { it.label }
                .toList()

        private fun getSignature(opcode: Opcode): Signature {
            val signature = StringBuilder(opcode.mnemonic).append(" ")
            val params = mutableListOf<Parameter>()
            var first = true

            for (param in opcode.params) {
                if (first) {
                    first = false
                } else {
                    signature.append(", ")
                }

                val labelStart = signature.length

                signature.appendParam(param)

                params.add(
                    Parameter(
                        labelStart,
                        labelEnd = signature.length,
                        documentation = param.doc,
                    )
                )
            }

            return Signature(
                label = signature.toString(),
                documentation = opcode.doc,
                parameters = params,
            )
        }

        private fun StringBuilder.appendParam(param: Param) {
            if (param.read || param.write) {
                if (param.read) append("in")
                if (param.write) append("out")
                append(" ")
            }

            when (val type = param.type) {
                AnyType.Instance -> append("Any")
                ByteType -> append("Byte")
                ShortType -> append("Short")
                IntType -> append("Int")
                FloatType -> append("Float")
                LabelType.Instance -> append("Label")
                ILabelType -> append("ILabel")
                DLabelType -> append("DLabel")
                SLabelType -> append("SLabel")
                ILabelVarType -> append("...ILabel")
                StringType -> append("String")
                is RegType -> {
                    append("Reg")

                    type.registers?.let { registers ->
                        append("<")

                        var first = true

                        for (register in registers) {
                            if (first) {
                                first = false
                            } else {
                                append(", ")
                            }

                            appendParam(register)
                        }

                        append(">")
                    }
                }
                RegVarType -> append("...Reg")
                PointerType -> append("Pointer")
            }

            param.name?.let {
                append(" ")
                append(param.name)
            }
        }
    }
}

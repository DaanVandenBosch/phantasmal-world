package world.phantasmal.web.assemblyWorker

import world.phantasmal.core.*
import world.phantasmal.psolib.asm.*
import world.phantasmal.psolib.asm.dataFlowAnalysis.ControlFlowGraph
import world.phantasmal.psolib.asm.dataFlowAnalysis.ValueSet
import world.phantasmal.psolib.asm.dataFlowAnalysis.getMapDesignations
import world.phantasmal.psolib.asm.dataFlowAnalysis.getStackValue
import world.phantasmal.web.shared.messages.*
import world.phantasmal.web.shared.messages.AssemblyProblem
import kotlin.math.max
import kotlin.math.min
import world.phantasmal.psolib.asm.AssemblyProblem as LibAssemblyProblem

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

    private var mapDesignations: Map<Int, Set<Int>>? = null

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
        val text = getLine(lineNo)?.take(col)?.trim()?.lowercase() ?: ""

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

        getInstructionForSrcLoc(lineNo, col)?.let { result ->
            signature = getSignature(result.inst.opcode)
            activeParam = result.paramIdx
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

            // Put the parameter doc and the instruction doc in the same string to match the look of
            // the signature help.
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
            visitLabelArguments(
                inst,
                accept = { argSrcLoc -> positionInside(lineNo, col, argSrcLoc.coarse) },
                processImmediateArg = { label, _ ->
                    result = getLabelDefinitionsAndReferences(label, references = false)
                    VisitAction.Return
                },
                processStackArg = { labels, _, _ ->
                    if (labels.size <= 5) {
                        result = labels.flatMap {
                            getLabelDefinitionsAndReferences(it, references = false)
                        }
                    }

                    VisitAction.Return
                },
            )
        }

        return Response.GetDefinition(requestId, result)
    }

    fun getLabels(requestId: Int): Response.GetLabels {
        val result = bytecodeIr.segments.asSequence()
            .flatMap { segment ->
                segment.labels.mapIndexed { labelIdx, label ->
                    val range = segment.srcLoc.labels[labelIdx].toAsmRange()
                    Label(name = label, range)
                }
            }
            .toList()

        return Response.GetLabels(requestId, result)
    }

    fun getHighlights(requestId: Int, lineNo: Int, col: Int): Response.GetHighlights {
        val results = mutableListOf<AsmRange>()

        when (val ir = getIrForSrcLoc(lineNo, col)) {
            is Ir.Label -> {
                results.addAll(getLabelDefinitionsAndReferences(ir.label))
            }

            is Ir.Inst -> {
                val srcLoc = ir.inst.srcLoc?.mnemonic

                if (ir.paramIdx == -1 ||
                    // Also return this instruction if we're right past the mnemonic. E.g. at the
                    // first whitespace character preceding the first argument.
                    (srcLoc != null && col <= srcLoc.col + srcLoc.len)
                ) {
                    // Find all instructions with the same opcode.
                    for (segment in bytecodeIr.segments) {
                        if (segment is InstructionSegment) {
                            for (inst in segment.instructions) {
                                if (inst.opcode.code == ir.inst.opcode.code) {
                                    inst.srcLoc?.mnemonic?.toAsmRange()?.let(results::add)
                                }
                            }
                        }
                    }
                } else {
                    visitArgs(
                        ir.inst,
                        processParam = { VisitAction.Go },
                        processImmediateArg = { param, arg, argSrcLoc ->
                            if (positionInside(lineNo, col, argSrcLoc.coarse)) {
                                (arg as? IntArg)?.let {
                                    when (param.type) {
                                        is LabelType -> {
                                            results.addAll(
                                                getLabelDefinitionsAndReferences(arg.value)
                                            )
                                        }
                                        is RegRefType -> {
                                            results.addAll(getRegisterReferences(arg.value))
                                        }
                                        else -> Unit
                                    }
                                }

                                VisitAction.Return
                            } else {
                                VisitAction.Continue
                            }
                        },
                        processStackArgSrcLoc = { _, argSrcLoc ->
                            if (positionInside(lineNo, col, argSrcLoc.coarse)) {
                                VisitAction.Go
                            } else {
                                VisitAction.Continue
                            }
                        },
                        processStackArg = { param, _, pushInst, _ ->
                            if (pushInst != null) {
                                val pushArg = pushInst.args.firstOrNull()

                                if (pushArg is IntArg) {
                                    if (pushInst.opcode.code == OP_ARG_PUSHR.code ||
                                        param.type is RegRefType
                                    ) {
                                        results.addAll(getRegisterReferences(pushArg.value))
                                    } else if (param.type is LabelType) {
                                        results.addAll(
                                            getLabelDefinitionsAndReferences(pushArg.value)
                                        )
                                    }
                                }
                            }

                            VisitAction.Return
                        }
                    )
                }
            }

            else -> {}
        }

        return Response.GetHighlights(requestId, results)
    }

    private fun getInstructionForSrcLoc(lineNo: Int, col: Int): Ir.Inst? =
        getIrForSrcLoc(lineNo, col) as? Ir.Inst

    private fun getIrForSrcLoc(lineNo: Int, col: Int): Ir? {
        for (segment in bytecodeIr.segments) {
            for ((index, srcLoc) in segment.srcLoc.labels.withIndex()) {
                if (srcLoc.lineNo == lineNo &&
                    col >= srcLoc.col &&
                    col < srcLoc.col + srcLoc.len
                ) {
                    return Ir.Label(segment.labels[index])
                }
            }

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
                                return Ir.Inst(inst, paramIdx = -1)
                            }
                        }

                        for ((argIdx, argSrcLoc) in srcLoc.args.withIndex()) {
                            instLineNo = argSrcLoc.coarse.lineNo
                            lastCol = argSrcLoc.coarse.col + argSrcLoc.coarse.len

                            if (positionInside(lineNo, col, argSrcLoc.coarse)) {
                                return Ir.Inst(inst, argIdx)
                            }
                        }

                        if (lineNo == instLineNo && col >= lastCol) {
                            val argIdx = max(0, srcLoc.args.lastIndex) +
                                    (if (srcLoc.trailingArgSeparator) 1 else 0)

                            val paramIdx = min(argIdx, inst.opcode.params.lastIndex)

                            return Ir.Inst(inst, paramIdx)
                        }
                    }
                }
            }
        }

        return null
    }

    private fun getRegisterReferences(register: Int): List<AsmRange> {
        val results = mutableListOf<AsmRange>()

        for (segment in bytecodeIr.segments) {
            if (segment is InstructionSegment) {
                for (inst in segment.instructions) {
                    visitArgs(
                        inst,
                        processParam = { VisitAction.Go },
                        processImmediateArg = { param, arg, argSrcLoc ->
                            if (param.type is RegRefType &&
                                arg is IntArg &&
                                arg.value == register
                            ) {
                                results.add(argSrcLoc.precise.toAsmRange())
                            }

                            VisitAction.Go
                        },
                        processStackArgSrcLoc = { param, _ ->
                            if (param.type is RegRefType) VisitAction.Go
                            else VisitAction.Continue
                        },
                        processStackArg = { _, _, pushInst, argSrcLoc ->
                            if (pushInst != null &&
                                pushInst.opcode.code != OP_ARG_PUSHR.code
                            ) {
                                val pushArg = pushInst.args.firstOrNull()

                                if (pushArg is IntArg && pushArg.value == register) {
                                    results.add(argSrcLoc.precise.toAsmRange())
                                }
                            }

                            VisitAction.Go
                        }
                    )
                }
            }
        }

        return results
    }

    /**
     * Returns all definitions and all arguments that references the given [label].
     */
    private fun getLabelDefinitionsAndReferences(
        label: Int,
        definitions: Boolean = true,
        references: Boolean = true,
    ): List<AsmRange> {
        val results = mutableListOf<AsmRange>()

        for (segment in bytecodeIr.segments) {
            // Add label definitions to the results.
            if (definitions) {
                val labelIdx = segment.labels.indexOf(label)

                if (labelIdx != -1) {
                    segment.srcLoc.labels.getOrNull(labelIdx)?.let { srcLoc ->
                        results.add(
                            AsmRange(
                                startLineNo = srcLoc.lineNo,
                                startCol = srcLoc.col,
                                endLineNo = srcLoc.lineNo,
                                // Exclude the trailing ":" character.
                                endCol = srcLoc.col + srcLoc.len - 1,
                            )
                        )
                    }
                }
            }

            // Find all instruction arguments that reference the label.
            if (references) {
                if (segment is InstructionSegment) {
                    for (inst in segment.instructions) {
                        visitLabelArguments(
                            inst,
                            accept = { true },
                            processImmediateArg = { labelArg, argSrcLoc ->
                                if (labelArg == label) {
                                    results.add(argSrcLoc.precise.toAsmRange())
                                }

                                VisitAction.Go
                            },
                            processStackArg = { labelArg, pushInst, argSrcLoc ->
                                // Filter out arg_pushr labels, because register values could be
                                // used for anything.
                                if (pushInst != null &&
                                    pushInst.opcode.code != OP_ARG_PUSHR.code &&
                                    labelArg.size == 1L &&
                                    label in labelArg
                                ) {
                                    results.add(argSrcLoc.precise.toAsmRange())
                                }

                                VisitAction.Go
                            },
                        )
                    }
                }
            }
        }

        return results
    }

    /**
     * Visits all label arguments of [instruction] with their value.
     */
    private fun visitLabelArguments(
        instruction: Instruction,
        accept: (ArgSrcLoc) -> Boolean,
        processImmediateArg: (label: Int, ArgSrcLoc) -> VisitAction,
        processStackArg: (label: ValueSet, Instruction?, ArgSrcLoc) -> VisitAction,
    ) {
        visitArgs(
            instruction,
            processParam = { if (it.type is LabelType) VisitAction.Go else VisitAction.Continue },
            processImmediateArg = { _, arg, srcLoc ->
                if (accept(srcLoc) && arg is IntArg) {
                    processImmediateArg(arg.value, srcLoc)
                } else VisitAction.Continue
            },
            processStackArgSrcLoc = { _, srcLoc ->
                if (accept(srcLoc)) VisitAction.Go
                else VisitAction.Continue
            },
            processStackArg = { _, value, pushInst, srcLoc ->
                processStackArg(value, pushInst, srcLoc)
            }
        )
    }

    private enum class VisitAction {
        Go, Break, Continue, Return
    }

    /**
     * Visits all arguments of [instruction], including stack arguments.
     */
    private fun visitArgs(
        instruction: Instruction,
        processParam: (Param) -> VisitAction,
        processImmediateArg: (Param, Arg, ArgSrcLoc) -> VisitAction,
        processStackArgSrcLoc: (Param, ArgSrcLoc) -> VisitAction,
        processStackArg: (Param, ValueSet, Instruction?, ArgSrcLoc) -> VisitAction,
    ) {
        for ((paramIdx, param) in instruction.opcode.params.withIndex()) {
            when (processParam(param)) {
                VisitAction.Go -> Unit // Keep going.
                VisitAction.Break -> break // Same as Stop.
                VisitAction.Continue -> continue
                VisitAction.Return -> return
            }

            if (instruction.opcode.stack !== StackInteraction.Pop) {
                // Immediate arguments.
                val args = instruction.getArgs(paramIdx)
                val argSrcLocs = instruction.getArgSrcLocs(paramIdx)

                for (i in 0 until min(args.size, argSrcLocs.size)) {
                    val arg = args[i]
                    val srcLoc = argSrcLocs[i]

                    when (processImmediateArg(param, arg, srcLoc)) {
                        VisitAction.Go -> Unit // Keep going.
                        VisitAction.Break -> break
                        VisitAction.Continue -> continue // Same as Down.
                        VisitAction.Return -> return
                    }
                }
            } else {
                // Stack arguments.
                val argSrcLocs = instruction.getArgSrcLocs(paramIdx)

                // Never varargs.
                for (srcLoc in argSrcLocs) {
                    when (processStackArgSrcLoc(param, srcLoc)) {
                        VisitAction.Go -> Unit // Keep going.
                        VisitAction.Break -> break
                        VisitAction.Continue -> continue
                        VisitAction.Return -> return
                    }

                    val (labelValues, pushInstruction) = getStackValue(
                        cfg,
                        instruction,
                        instruction.opcode.params.lastIndex - paramIdx,
                    )

                    when (processStackArg(param, labelValues, pushInstruction, srcLoc)) {
                        VisitAction.Go -> Unit // Keep going.
                        VisitAction.Break -> break
                        VisitAction.Continue -> continue // Same as Down.
                        VisitAction.Return -> return
                    }
                }
            }
        }
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
        class Label(val label: Int) : Ir()
        class Inst(val inst: Instruction, val paramIdx: Int) : Ir()
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

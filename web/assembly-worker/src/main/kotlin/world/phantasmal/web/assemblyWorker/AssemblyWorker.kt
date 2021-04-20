package world.phantasmal.web.assemblyWorker

import mu.KotlinLogging
import world.phantasmal.core.*
import world.phantasmal.lib.asm.*
import world.phantasmal.lib.asm.dataFlowAnalysis.ControlFlowGraph
import world.phantasmal.lib.asm.dataFlowAnalysis.getMapDesignations
import world.phantasmal.lib.asm.dataFlowAnalysis.getStackValue
import world.phantasmal.web.shared.Throttle
import world.phantasmal.web.shared.messages.*
import kotlin.math.min
import kotlin.time.measureTime
import world.phantasmal.lib.asm.AssemblyProblem as AssemblerAssemblyProblem

private val logger = KotlinLogging.logger {}

class AssemblyWorker(private val sendMessage: (ServerMessage) -> Unit) {
    private val messageQueue: MutableList<ClientMessage> = mutableListOf()
    private val messageProcessingThrottle = Throttle(wait = 100)
    private val tokenizer = LineTokenizer()

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

    fun receiveMessage(message: ClientMessage) {
        messageQueue.add(message)
        messageProcessingThrottle(::processMessages)
    }

    private fun processMessages() {
        // Split messages into ASM changes and other messages. Remove useless/duplicate
        // notifications.
        val asmChanges = mutableListOf<ClientNotification>()
        val otherMessages = mutableListOf<ClientMessage>()

        for (message in messageQueue) {
            when (message) {
                is ClientNotification.SetAsm -> {
                    // All previous ASM change messages can be discarded when the entire ASM has
                    // changed.
                    asmChanges.clear()
                    asmChanges.add(message)
                }

                is ClientNotification.UpdateAsm ->
                    asmChanges.add(message)

                else ->
                    otherMessages.add(message)
            }
        }

        messageQueue.clear()

        // Process ASM changes first.
        processAsmChanges(asmChanges)
        otherMessages.forEach(::processMessage)
    }

    private fun processAsmChanges(messages: List<ClientNotification>) {
        if (messages.isNotEmpty()) {
            val time = measureTime {
                for (message in messages) {
                    when (message) {
                        is ClientNotification.SetAsm ->
                            setAsm(message.asm, message.inlineStackArgs)

                        is ClientNotification.UpdateAsm ->
                            updateAsm(message.changes)
                    }
                }

                processAsm()
            }

            logger.trace {
                "Processed ${messages.size} assembly changes in ${time.inMilliseconds}ms."
            }
        }
    }

    private fun processMessage(message: ClientMessage) {
        val time = measureTime {
            when (message) {
                is ClientNotification.SetAsm,
                is ClientNotification.UpdateAsm ->
                    logger.error { "Unexpected ${message::class.simpleName}." }

                is Request.GetCompletions ->
                    getCompletions(message.id, message.lineNo, message.col)

                is Request.GetSignatureHelp ->
                    getSignatureHelp(message.id, message.lineNo, message.col)

                is Request.GetHover ->
                    getHover(message.id, message.lineNo, message.col)

                is Request.GetDefinition ->
                    getDefinition(message.id, message.lineNo, message.col)
            }
        }

        logger.trace { "Processed ${message::class.simpleName} in ${time.inMilliseconds}ms." }
    }

    private fun setAsm(asm: List<String>, inlineStackArgs: Boolean) {
        this.inlineStackArgs = inlineStackArgs
        this.asm.splice(0, this.asm.length, *asm.toTypedArray())
        mapDesignations = null
    }

    private fun updateAsm(changes: List<AsmChange>) {
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

    private fun processAsm() {
        _cfg = null

        val assemblyResult = assemble(asm.asArray().toList(), inlineStackArgs)

        @Suppress("UNCHECKED_CAST")
        val problems = (assemblyResult.problems as List<AssemblerAssemblyProblem>).map {
            AssemblyProblem(it.severity, it.uiMessage, it.lineNo, it.col, it.len)
        }

        if (problems != this.problems) {
            this.problems = problems
            sendMessage(ServerNotification.Problems(problems))
        }

        if (assemblyResult is Success) {
            bytecodeIr = assemblyResult.value

            val instructionSegments = bytecodeIr.instructionSegments()

            instructionSegments.find { 0 in it.labels }?.let { label0Segment ->
                val designations = getMapDesignations(label0Segment) { cfg }

                if (designations != mapDesignations) {
                    mapDesignations = designations
                    sendMessage(
                        ServerNotification.MapDesignations(
                            designations
                        )
                    )
                }
            }
        }
    }

    private fun getCompletions(requestId: Int, lineNo: Int, col: Int) {
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

        sendMessage(Response.GetCompletions(requestId, completions))
    }

    private fun getSignatureHelp(requestId: Int, lineNo: Int, col: Int) {
        sendMessage(Response.GetSignatureHelp(requestId, signatureHelp(lineNo, col)))
    }

    private fun signatureHelp(lineNo: Int, col: Int): SignatureHelp? {
        // Hacky way of providing parameter hints.
        // We just tokenize the current line and look for the first identifier and check whether
        // it's a valid opcode.
        var signature: Signature? = null
        var activeParam = -1

        getLine(lineNo)?.let { text ->
            tokenizer.tokenize(text)

            while (tokenizer.nextToken()) {
                if (tokenizer.type === Token.Ident) {
                    mnemonicToOpcode(tokenizer.strValue)?.let { opcode ->
                        signature = getSignature(opcode)
                    }
                }

                if (tokenizer.col + tokenizer.len > col) {
                    break
                } else if (tokenizer.type === Token.Ident && activeParam == -1) {
                    activeParam = 0
                } else if (tokenizer.type === Token.ArgSeparator) {
                    activeParam++
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

    private fun getHover(requestId: Int, lineNo: Int, col: Int) {
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

        sendMessage(Response.GetHover(requestId, hover))
    }

    private fun getDefinition(requestId: Int, lineNo: Int, col: Int) {
        var result = emptyList<AsmRange>()

        getInstruction(lineNo, col)?.let { inst ->
            loop@
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
                                result = getLabelDefinitions(label)
                                break@loop
                            }
                        }
                    } else {
                        // Stack arguments.
                        val argSrcLocs = inst.getStackArgSrcLocs(paramIdx)

                        for ((i, argSrcLoc) in argSrcLocs.withIndex()) {
                            if (positionInside(lineNo, col, argSrcLoc)) {
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

        sendMessage(Response.GetDefinition(requestId, result))
    }

    private fun getInstruction(lineNo: Int, col: Int): Instruction? {
        for (segment in bytecodeIr.segments) {
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
        bytecodeIr.segments.asSequence()
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

    companion object {
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
            (OPCODES.asSequence() + OPCODES_F8.asSequence() + OPCODES_F9.asSequence())
                .filterNotNull()
                .map { opcode ->
                    CompletionItem(
                        label = opcode.mnemonic,
                        // TODO: Add signature?
                        type = CompletionItemType.Opcode,
                        insertText = opcode.mnemonic,
                    )
                }
                .sortedBy { it.label }
                .toList()
    }
}

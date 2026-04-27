package world.phantasmal.web.questEditor.controllers

import world.phantasmal.cell.Cell
import world.phantasmal.cell.cell
import world.phantasmal.cell.flatMap
import world.phantasmal.cell.map
import world.phantasmal.cell.map as cellMap
import world.phantasmal.cell.mutableCell
import world.phantasmal.psolib.asm.*
import world.phantasmal.psolib.buffer.Buffer
import world.phantasmal.web.externals.monacoEditor.ICursorStateComputer
import world.phantasmal.web.externals.monacoEditor.IIdentifiedSingleEditOperation
import world.phantasmal.web.externals.monacoEditor.ITextModel
import world.phantasmal.web.externals.monacoEditor.Range
import world.phantasmal.web.questEditor.asm.DataLabelType
import world.phantasmal.web.questEditor.asm.SymbolChatTriggerInfo
import world.phantasmal.web.questEditor.asm.analyzeDataLabels
import world.phantasmal.web.questEditor.asm.analyzeSymbolChatTriggers
import world.phantasmal.web.questEditor.loading.BattleParamRepository
import world.phantasmal.web.questEditor.stores.AsmStore
import world.phantasmal.web.questEditor.stores.QuestEditorStore
import world.phantasmal.webui.controllers.Controller
import world.phantasmal.webui.obj

class DataLabelEntry(
    val labelId: Int,
    val type: DataLabelType,
)

class DataEditorController(
    private val questEditorStore: QuestEditorStore,
    private val asmStore: AsmStore,
    val battleParamRepository: BattleParamRepository,
) : Controller() {
    val enabled: Cell<Boolean> = asmStore.editingEnabled

    /**
     * Manual label-type overrides set via the editor's "Mark label as..." menu.
     * Used for label types that have no opcode-based detection (Float,
     * Symbol Chat) and to force-override auto-detected types when the user
     * knows better.
     *
     * Cleared whenever the current quest changes.
     */
    private val labelOverrides = mutableCell<Map<Int, DataLabelType>>(emptyMap())

    /**
     * All typed data labels in the current quest's bytecodeIr, with manual
     * [labelOverrides] applied on top of opcode-based auto-detection.
     */
    val dataLabels: Cell<List<DataLabelEntry>> =
        questEditorStore.currentQuest.flatMap { quest ->
            if (quest == null) {
                cell(emptyList())
            } else {
                // Depend on both [labelOverrides] and the quest's bytecode
                // revision so re-assembled bytecode refreshes the list.
                cellMap(quest.bytecodeRevision, labelOverrides) { _, overrides ->
                    val labelTypes = analyzeDataLabels(quest.bytecodeIr).toMutableMap()
                    // Only honor overrides for labels that actually point at a DataSegment.
                    val dataLabelIds = quest.bytecodeIr.segments
                        .filterIsInstance<DataSegment>()
                        .flatMap { it.labels }
                        .toSet()
                    for ((labelId, type) in overrides) {
                        if (labelId in dataLabelIds) labelTypes[labelId] = type
                    }
                    labelTypes.map { (labelId, type) -> DataLabelEntry(labelId, type) }
                        .sortedBy { it.labelId }
                }
            }
        }

    /**
     * All `set_symbol_chat_collision` triggers in the current quest's
     * bytecodeIr, with world-space position / radius / per-slot spec + dlabel
     * info resolved via back-trace. Re-derives when the quest's
     * [QuestModel.bytecodeRevision] ticks (i.e. whenever the ASM editor
     * settles a reassembly), so the inline editor previews and 3D trigger
     * rings refresh without requiring a quest reload.
     */
    val symbolChatTriggers: Cell<List<SymbolChatTriggerInfo>> =
        questEditorStore.currentQuest.flatMap { quest ->
            if (quest == null) {
                cell(emptyList())
            } else {
                quest.bytecodeRevision.map { _ ->
                    analyzeSymbolChatTriggers(quest.bytecodeIr)
                }
            }
        }

    init {
        // Reset overrides when the quest changes — they're keyed by label id
        // which is only meaningful within a single quest.
        observe(questEditorStore.currentQuest) { labelOverrides.value = emptyMap() }
    }

    /**
     * Sets or clears a manual label type override. Pass [type] = null to
     * revert to auto-detection.
     */
    fun setLabelTypeOverride(labelId: Int, type: DataLabelType?) {
        val current = labelOverrides.value
        labelOverrides.value = current.toMutableMap().also {
            if (type == null) it.remove(labelId) else it[labelId] = type
        }
    }

    /**
     * Returns data labels filtered by [type].
     */
    fun labelsOfType(type: DataLabelType): Cell<List<DataLabelEntry>> =
        dataLabels.map { labels -> labels.filter { it.type == type } }

    /**
     * Reads the raw [Buffer] for a data segment identified by [labelId].
     * Returns null if the quest or segment is not found.
     */
    fun readSegmentData(labelId: Int): Buffer? {
        val quest = questEditorStore.currentQuest.value ?: return null
        val segment = quest.bytecodeIr.segments
            .filterIsInstance<DataSegment>()
            .find { labelId in it.labels }
        return segment?.data
    }

    /**
     * Writes modified [buffer] back to the Monaco text model by replacing the HEX lines
     * for the given [labelId]. This triggers the normal reassembly pipeline.
     */
    fun writeSegmentData(labelId: Int, buffer: Buffer): Boolean =
        writeSegmentDataAndPatchImageSize(labelId, buffer, patchSize = null)

    /**
     * Replaces the data segment for [labelId] AND patches every matching
     * `call_image_data <size>, <labelId>` instruction's size argument to
     * [patchSize] in a single Monaco edit, so the operation is one undo step.
     *
     * If [patchSize] is null, behaves exactly like [writeSegmentData].
     * If no `call_image_data` instruction references [labelId], only the
     * segment replacement is performed.
     */
    fun writeSegmentDataAndPatchImageSize(
        labelId: Int,
        buffer: Buffer,
        patchSize: Int?,
    ): Boolean {
        val textModel = asmStore.textModel.value ?: return false
        val lineRange = findDataLineRange(textModel, labelId) ?: return false

        val newHexText = formatBufferAsHex(buffer)
        val edits = mutableListOf<IIdentifiedSingleEditOperation>()

        edits.add(obj {
            range = obj {
                startLineNumber = lineRange.first
                startColumn = 1
                endLineNumber = lineRange.second
                endColumn = textModel.getLineMaxColumn(lineRange.second).toInt()
            }
            text = newHexText
        })

        if (patchSize != null) {
            edits.addAll(buildCallImageDataSizeEdits(textModel, labelId, patchSize))
        }

        textModel.pushEditOperations(
            beforeCursorState = null,
            editOperations = edits.toTypedArray(),
            cursorStateComputer = js("function() { return null }").unsafeCast<ICursorStateComputer>(),
        )
        return true
    }

    /**
     * Deletes the entire data segment for [labelId] from the text model —
     * the label declaration line plus all of its indented hex data lines.
     * Operates at segment granularity (not per-line) so the assembler stays
     * consistent. Triggers reassembly via the normal text-edit pipeline.
     */
    fun deleteSegment(labelId: Int): Boolean {
        val textModel = asmStore.textModel.value ?: return false
        val lineCount = textModel.getLineCount().toInt()
        val labelPrefix = "$labelId:"

        var labelLine = -1
        for (lineNo in 1..lineCount) {
            val content = textModel.getLineContent(lineNo).trimStart()
            if (content == labelPrefix || content.startsWith("$labelPrefix ")) {
                labelLine = lineNo
                break
            }
        }
        if (labelLine == -1) return false

        var lastLine = labelLine
        for (lineNo in (labelLine + 1)..lineCount) {
            val content = textModel.getLineContent(lineNo)
            if (content.startsWith("    0x") || content.startsWith("\t0x")) {
                lastLine = lineNo
            } else {
                break
            }
        }

        // Delete labelLine..lastLine inclusive — extend to start of next line
        // (or end of preceding line if at EOF) to swallow the trailing newline.
        val edit = obj<IIdentifiedSingleEditOperation> {
            range = if (lastLine < lineCount) {
                obj {
                    startLineNumber = labelLine
                    startColumn = 1
                    endLineNumber = lastLine + 1
                    endColumn = 1
                }
            } else if (labelLine > 1) {
                obj {
                    startLineNumber = labelLine - 1
                    startColumn = textModel.getLineMaxColumn(labelLine - 1).toInt()
                    endLineNumber = lastLine
                    endColumn = textModel.getLineMaxColumn(lastLine).toInt()
                }
            } else {
                obj {
                    startLineNumber = labelLine
                    startColumn = 1
                    endLineNumber = lastLine
                    endColumn = textModel.getLineMaxColumn(lastLine).toInt()
                }
            }
            text = ""
        }

        textModel.pushEditOperations(
            beforeCursorState = null,
            editOperations = arrayOf(edit),
            cursorStateComputer = js("function() { return null }").unsafeCast<ICursorStateComputer>(),
        )
        return true
    }

    /**
     * Builds Monaco edit operations that patch the size argument of every
     * `call_image_data <size>, <labelId>` instruction so it matches the
     * uncompressed size of a freshly-replaced image segment.
     *
     * Preserves the existing literal format (hex `0x...` or decimal).
     * Returns an empty list if no matching instruction is found.
     */
    private fun buildCallImageDataSizeEdits(
        textModel: ITextModel,
        labelId: Int,
        uncompressedSize: Int,
    ): List<IIdentifiedSingleEditOperation> {
        val lineCount = textModel.getLineCount().toInt()

        // Match: leading ws, "call_image_data", ws, <size token>, optional ws,
        // ",", optional ws, "<labelId>", end of line (no trailing args).
        val pattern = Regex(
            """^(\s*call_image_data\s+)(0x[0-9A-Fa-f]+|-?\d+)(\s*,\s*)""" + labelId + """\s*$"""
        )

        val edits = mutableListOf<IIdentifiedSingleEditOperation>()
        for (lineNo in 1..lineCount) {
            val content = textModel.getLineContent(lineNo)
            val m = pattern.matchEntire(content) ?: continue
            val sizeToken = m.groupValues[2]
            val newToken = if (sizeToken.startsWith("0x") || sizeToken.startsWith("0X")) {
                "0x" + uncompressedSize.toString(16).uppercase().padStart(8, '0')
            } else {
                uncompressedSize.toString()
            }
            val tokenStartCol = 1 + m.groupValues[1].length
            val tokenEndCol = tokenStartCol + sizeToken.length

            edits.add(obj {
                range = obj {
                    startLineNumber = lineNo
                    startColumn = tokenStartCol
                    endLineNumber = lineNo
                    endColumn = tokenEndCol
                }
                text = newToken
            })
        }
        return edits
    }

    /**
     * Returns the label id of the [DataSegment] under the cursor at [lineNo],
     * regardless of whether the segment has a known [DataLabelType] (NPC,
     * Physical, etc.). Used by the generic Change/Save image actions which
     * operate on raw bytes.
     */
    fun dataSegmentLabelAtLine(lineNo: Int): Int? {
        val labelId = labelIdAtLine(lineNo) ?: return null
        val quest = questEditorStore.currentQuest.value ?: return null
        val isDataSegment = quest.bytecodeIr.segments
            .filterIsInstance<DataSegment>()
            .any { labelId in it.labels }
        return if (isDataSegment) labelId else null
    }

    /**
     * Determines which data label the cursor is on (label line or its data lines).
     * Returns the [DataLabelEntry] or null if the cursor is not on a known data segment.
     */
    fun dataLabelAtLine(lineNo: Int): DataLabelEntry? {
        val labelId = labelIdAtLine(lineNo) ?: return null
        return dataLabels.value.find { it.labelId == labelId }
    }

    /**
     * Returns the label id of whatever label the cursor is currently inside,
     * regardless of segment type. Walks backwards from a hex/data line until
     * a label declaration is found.
     */
    private fun labelIdAtLine(lineNo: Int): Int? {
        val model = asmStore.textModel.value ?: return null
        val lineCount = model.getLineCount().toInt()
        if (lineNo < 1 || lineNo > lineCount) return null

        val content = model.getLineContent(lineNo).trimStart()

        // Cursor IS on a label line (e.g. "444:")
        parseLabelId(content)?.let { return it }

        // Cursor is on a hex data line — walk backwards to find the label.
        if (content.startsWith("0x")) {
            for (scanLine in (lineNo - 1) downTo 1) {
                val scanContent = model.getLineContent(scanLine).trimStart()
                if (scanContent.startsWith("0x")) continue // another data line
                parseLabelId(scanContent)?.let { return it }
                break // hit something else (section marker, code, etc.)
            }
        }
        return null
    }

    private fun parseLabelId(trimmedLine: String): Int? {
        if (!trimmedLine.endsWith(":")) return null
        return trimmedLine.dropLast(1).toIntOrNull()
    }

    /**
     * Finds the line range (startLine..endLine, inclusive) of the data bytes for [labelId]
     * in the text model. The data lines are the indented hex lines immediately following the
     * label declaration.
     */
    private fun findDataLineRange(model: ITextModel, labelId: Int): Pair<Int, Int>? {
        val lineCount = model.getLineCount().toInt()
        val labelPrefix = "$labelId:"

        // Find the label line.
        var labelLine = -1
        for (lineNo in 1..lineCount) {
            val content = model.getLineContent(lineNo).trimStart()
            if (content == labelPrefix || content.startsWith("$labelPrefix ")) {
                labelLine = lineNo
                break
            }
        }
        if (labelLine == -1) return null

        // Data lines start right after the label and are indented (start with spaces + 0x).
        var startLine = -1
        var endLine = -1

        for (lineNo in (labelLine + 1)..lineCount) {
            val content = model.getLineContent(lineNo)
            if (content.startsWith("    0x") || content.startsWith("\t0x")) {
                if (startLine == -1) startLine = lineNo
                endLine = lineNo
            } else {
                // Also handle indented-only empty line for zero-size segments.
                if (startLine == -1 && content.isBlank() && content.length >= 4) {
                    startLine = lineNo
                    endLine = lineNo
                }
                break
            }
        }

        return if (startLine != -1) Pair(startLine, endLine) else null
    }

    companion object {
        private const val INDENT = "    "

        /**
         * Formats a [Buffer] as HEX text matching the disassembly format:
         * 4-space indent, `0x` prefix per byte, uppercase, 16 bytes per line.
         */
        fun formatBufferAsHex(buffer: Buffer): String {
            if (buffer.size == 0) return INDENT

            val sb = StringBuilder()

            for (i in 0 until buffer.size) {
                if (i % 16 == 0) {
                    if (i > 0) sb.append("\n")
                    sb.append(INDENT)
                } else {
                    sb.append(" ")
                }

                sb.append("0x")
                sb.append(buffer.getUByte(i).toString(16).uppercase().padStart(2, '0'))
            }

            return sb.toString()
        }
    }
}

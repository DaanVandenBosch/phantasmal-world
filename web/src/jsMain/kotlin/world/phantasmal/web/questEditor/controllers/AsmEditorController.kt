package world.phantasmal.web.questEditor.controllers

import world.phantasmal.cell.Cell
import world.phantasmal.cell.isNull
import world.phantasmal.cell.list.ListCell
import world.phantasmal.cell.list.listMap
import world.phantasmal.cell.map
import world.phantasmal.cell.not
import world.phantasmal.cell.or
import world.phantasmal.cell.orElse
import world.phantasmal.psolib.asm.*
import world.phantasmal.web.core.observable.Observable
import world.phantasmal.web.externals.monacoEditor.FindMatch
import world.phantasmal.web.externals.monacoEditor.ITextModel
import world.phantasmal.web.externals.monacoEditor.createModel
import world.phantasmal.web.questEditor.stores.AsmStore
import world.phantasmal.web.shared.messages.AsmRange
import world.phantasmal.web.shared.messages.Label
import world.phantasmal.web.shared.messages.RegisterInfo
import world.phantasmal.web.shared.messages.SegmentInfo
import world.phantasmal.webui.controllers.Controller

class AsmEditorController(private val store: AsmStore) : Controller() {
    val enabled: Cell<Boolean> = store.editingEnabled
    val readOnly: Cell<Boolean> = !enabled or store.textModel.isNull()

    val textModel: Cell<ITextModel> = store.textModel.orElse { EMPTY_MODEL }

    val didUndo: Observable<Unit> = store.didUndo
    val didRedo: Observable<Unit> = store.didRedo
    val goToLabel: Observable<AsmRange> = store.goToLabelEvent

    val hexFormat: Cell<Boolean> = store.hexFormat
    val hexFormatEnabled: Cell<Boolean> = store.problems.map { it.isEmpty() }

    val hideNops: Cell<Boolean> = store.hideNops
    val hideNopsEnabled: Cell<Boolean> = store.problems.map { it.isEmpty() }

    val labels: ListCell<Label> = store.labels
    val registers: ListCell<RegisterInfo> = store.registers
    val segments: ListCell<SegmentInfo> = store.segments

    /** Opcodes actually used in the current script, derived from labels (triggers on reparse). */
    val usedOpcodes: Cell<List<Opcode>> = map(labels, textModel) { _, model ->
        val text = model.getValue()
        val mnemonics = mutableSetOf<String>()
        for (line in text.split('\n')) {
            val trimmed = line.trimStart()
            if (trimmed.isEmpty() || trimmed.startsWith('.') || trimmed.startsWith("//")) continue
            val firstToken = trimmed.substringBefore(' ').substringBefore('\t')
            if (firstToken.endsWith(':') || firstToken.isEmpty()) continue
            mnemonics.add(firstToken)
        }
        mnemonics.mapNotNull { mnemonicToOpcode(it) }.sortedBy { it.mnemonic }
    }

    fun makeUndoCurrent() {
        store.makeUndoCurrent()
    }

    fun setHexFormat(hex: Boolean) {
        store.setHexFormat(hex)
    }

    fun setHideNops(hide: Boolean) {
        store.setHideNops(hide)
    }

    fun goToLabelRange(range: AsmRange) {
        store.goToLabelRange(range)
    }

    /** Scrolls the editor to whichever label carries [labelId]. No-op if unknown. */
    fun navigateToLabel(labelId: Int) {
        store.goToLabel(labelId)
    }

    fun findRegisterMatches(regId: Int): Array<FindMatch> {
        val model = store.textModel.value ?: return emptyArray()
        // Match register reference like r0, r1, etc. as a word boundary token
        return model.findMatches(
            searchString = "\\br$regId\\b",
            searchOnlyEditableRange = false,
            isRegex = true,
            matchCase = true,
            wordSeparators = null,
            captureMatches = false,
        )
    }

    fun findOpcodeMatches(mnemonic: String): Array<FindMatch> {
        val model = store.textModel.value ?: return emptyArray()
        // Match the mnemonic at the start of an instruction (after indentation)
        return model.findMatches(
            searchString = "^\\s+$mnemonic\\b",
            searchOnlyEditableRange = false,
            isRegex = true,
            matchCase = true,
            wordSeparators = null,
            captureMatches = false,
        )
    }

    fun goToMatch(match: FindMatch) {
        val r = match.range.asDynamic()
        store.goToLabelRange(AsmRange(
            startLineNo = (r.startLineNumber as Number).toInt(),
            startCol = (r.startColumn as Number).toInt(),
            endLineNo = (r.endLineNumber as Number).toInt(),
            endCol = (r.endColumn as Number).toInt(),
        ))
    }

    companion object {
        private val EMPTY_MODEL = createModel("", AsmStore.ASM_LANG_ID)
    }
}

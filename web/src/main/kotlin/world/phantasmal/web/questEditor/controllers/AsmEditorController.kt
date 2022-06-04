package world.phantasmal.web.questEditor.controllers

import world.phantasmal.cell.Cell
import world.phantasmal.cell.isNull
import world.phantasmal.cell.map
import world.phantasmal.cell.not
import world.phantasmal.cell.or
import world.phantasmal.cell.orElse
import world.phantasmal.web.core.observable.Observable
import world.phantasmal.web.externals.monacoEditor.ITextModel
import world.phantasmal.web.externals.monacoEditor.createModel
import world.phantasmal.web.questEditor.stores.AsmStore
import world.phantasmal.webui.controllers.Controller

class AsmEditorController(private val store: AsmStore) : Controller() {
    val enabled: Cell<Boolean> = store.editingEnabled
    val readOnly: Cell<Boolean> = !enabled or store.textModel.isNull()

    val textModel: Cell<ITextModel> = store.textModel.orElse { EMPTY_MODEL }

    val didUndo: Observable<Unit> = store.didUndo
    val didRedo: Observable<Unit> = store.didRedo

    val inlineStackArgs: Cell<Boolean> = store.inlineStackArgs
    val inlineStackArgsEnabled: Cell<Boolean> = store.problems.map { it.isEmpty() }
    val inlineStackArgsTooltip: Cell<String> =
        inlineStackArgsEnabled.map { enabled ->
            buildString {
                append("Transform arg_push* opcodes to be inline with the opcode the arguments are given to.")

                if (!enabled) {
                    append("\nThis mode cannot be toggled because there are issues in the script.")
                }
            }
        }

    fun makeUndoCurrent() {
        store.makeUndoCurrent()
    }

    fun setInlineStackArgs(inline: Boolean) {
        store.setInlineStackArgs(inline)
    }

    companion object {
        private val EMPTY_MODEL = createModel("", AsmStore.ASM_LANG_ID)
    }
}

package world.phantasmal.web.questEditor.controllers

import world.phantasmal.observable.Observable
import world.phantasmal.observable.value.Val
import world.phantasmal.observable.value.not
import world.phantasmal.observable.value.or
import world.phantasmal.observable.value.orElse
import world.phantasmal.web.externals.monacoEditor.ITextModel
import world.phantasmal.web.externals.monacoEditor.createModel
import world.phantasmal.web.questEditor.stores.AsmStore
import world.phantasmal.webui.controllers.Controller

class AsmController(private val store: AsmStore) : Controller() {
    val enabled: Val<Boolean> = store.editingEnabled
    val readOnly: Val<Boolean> = !enabled or store.textModel.isNull()

    val textModel: Val<ITextModel> = store.textModel.orElse { EMPTY_MODEL }

    val didUndo: Observable<Unit> = store.didUndo
    val didRedo: Observable<Unit> = store.didRedo

    val inlineStackArgs: Val<Boolean> = store.inlineStackArgs
    val inlineStackArgsEnabled: Val<Boolean> = store.problems.map { it.isEmpty() }
    val inlineStackArgsTooltip: Val<String> =
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

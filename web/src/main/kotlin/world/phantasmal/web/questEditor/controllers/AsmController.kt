package world.phantasmal.web.questEditor.controllers

import world.phantasmal.observable.Observable
import world.phantasmal.observable.value.*
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
    val inlineStackArgsEnabled: Val<Boolean> = falseVal() // TODO

    // TODO: Notify user when disabled because of issues with the ASM.
    val inlineStackArgsTooltip: Val<String> = value(
        "Transform arg_push* opcodes to be inline with the opcode the arguments are given to."
    )

    fun makeUndoCurrent() {
        store.makeUndoCurrent()
    }

    fun setInlineStackArgs(value: Boolean) {
        TODO()
    }

    companion object {
        private val EMPTY_MODEL = createModel("", AsmStore.ASM_LANG_ID)
    }
}

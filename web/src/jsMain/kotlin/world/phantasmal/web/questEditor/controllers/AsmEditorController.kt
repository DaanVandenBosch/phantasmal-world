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

    fun makeUndoCurrent() {
        store.makeUndoCurrent()
    }

    companion object {
        private val EMPTY_MODEL = createModel("", AsmStore.ASM_LANG_ID)
    }
}

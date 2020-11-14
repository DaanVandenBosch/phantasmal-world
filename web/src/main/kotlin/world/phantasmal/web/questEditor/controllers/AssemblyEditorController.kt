package world.phantasmal.web.questEditor.controllers

import world.phantasmal.observable.value.*
import world.phantasmal.web.externals.monacoEditor.ITextModel
import world.phantasmal.web.externals.monacoEditor.createModel
import world.phantasmal.web.questEditor.stores.AssemblyEditorStore
import world.phantasmal.webui.controllers.Controller

class AssemblyEditorController(assemblyEditorStore: AssemblyEditorStore) : Controller() {
    val textModel: Val<ITextModel> = assemblyEditorStore.textModel.orElse { EMPTY_MODEL }
    val enabled: Val<Boolean> = assemblyEditorStore.editingEnabled
    val readOnly: Val<Boolean> = enabled.not() or assemblyEditorStore.textModel.isNull()

    companion object {
        private val EMPTY_MODEL = createModel("", AssemblyEditorStore.ASM_LANG_ID)
    }
}

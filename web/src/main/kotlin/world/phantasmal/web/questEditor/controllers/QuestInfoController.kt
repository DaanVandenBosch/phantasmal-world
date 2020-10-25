package world.phantasmal.web.questEditor.controllers

import kotlinx.coroutines.CoroutineScope
import world.phantasmal.observable.value.Val
import world.phantasmal.observable.value.value
import world.phantasmal.web.questEditor.stores.QuestEditorStore
import world.phantasmal.webui.controllers.Controller

class QuestInfoController(scope: CoroutineScope, store: QuestEditorStore) : Controller(scope) {
    val id: Val<Int> = store.currentQuest.flatTransform { it?.id ?: value(0) }
}

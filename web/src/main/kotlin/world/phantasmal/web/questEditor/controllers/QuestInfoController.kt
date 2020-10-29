package world.phantasmal.web.questEditor.controllers

import kotlinx.coroutines.CoroutineScope
import world.phantasmal.observable.value.Val
import world.phantasmal.observable.value.value
import world.phantasmal.web.questEditor.stores.QuestEditorStore
import world.phantasmal.webui.controllers.Controller

class QuestInfoController(scope: CoroutineScope, store: QuestEditorStore) : Controller(scope) {
    val unavailable = store.currentQuest.transform { it == null }
    val episode: Val<String> = store.currentQuest.transform { it?.episode?.name ?: "" }
    val id: Val<Int> = store.currentQuest.flatTransform { it?.id ?: value(0) }
    val name: Val<String> = store.currentQuest.flatTransform { it?.name ?: value("") }
}

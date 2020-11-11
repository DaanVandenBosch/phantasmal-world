package world.phantasmal.web.questEditor.controllers

import world.phantasmal.observable.value.Val
import world.phantasmal.observable.value.isNull
import world.phantasmal.observable.value.value
import world.phantasmal.web.questEditor.stores.QuestEditorStore
import world.phantasmal.webui.controllers.Controller

class QuestInfoController(store: QuestEditorStore) : Controller() {
    val unavailable: Val<Boolean> = store.currentQuest.isNull()
    val enabled: Val<Boolean> = store.questEditingEnabled

    val episode: Val<String> = store.currentQuest.map { it?.episode?.name ?: "" }
    val id: Val<Int> = store.currentQuest.flatMap { it?.id ?: value(0) }
    val name: Val<String> = store.currentQuest.flatMap { it?.name ?: value("") }
    val shortDescription: Val<String> =
        store.currentQuest.flatMap { it?.shortDescription ?: value("") }
    val longDescription: Val<String> =
        store.currentQuest.flatMap { it?.longDescription ?: value("") }
}

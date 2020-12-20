package world.phantasmal.web.questEditor.controllers

import world.phantasmal.observable.value.Val
import world.phantasmal.observable.value.emptyStringVal
import world.phantasmal.observable.value.value
import world.phantasmal.web.questEditor.actions.EditPropertyAction
import world.phantasmal.web.questEditor.stores.QuestEditorStore
import world.phantasmal.webui.controllers.Controller

class QuestInfoController(private val store: QuestEditorStore) : Controller() {
    val unavailable: Val<Boolean> = store.currentQuest.isNull()
    val enabled: Val<Boolean> = store.questEditingEnabled

    val episode: Val<String> = store.currentQuest.map { it?.episode?.name ?: "" }
    val id: Val<Int> = store.currentQuest.flatMap { it?.id ?: value(0) }
    val name: Val<String> = store.currentQuest.flatMap { it?.name ?: emptyStringVal() }
    val shortDescription: Val<String> =
        store.currentQuest.flatMap { it?.shortDescription ?: emptyStringVal() }
    val longDescription: Val<String> =
        store.currentQuest.flatMap { it?.longDescription ?: emptyStringVal() }

    fun focused() {
        store.makeMainUndoCurrent()
    }

    fun setId(id: Int) {
        if (!enabled.value) return

        store.currentQuest.value?.let { quest ->
            store.executeAction(EditPropertyAction("Edit ID", quest::setId, id, quest.id.value))
        }
    }

    fun setName(name: String) {
        if (!enabled.value) return

        store.currentQuest.value?.let { quest ->
            store.executeAction(
                EditPropertyAction("Edit name", quest::setName, name, quest.name.value)
            )
        }
    }

    fun setShortDescription(shortDescription: String) {
        if (!enabled.value) return

        store.currentQuest.value?.let { quest ->
            store.executeAction(
                EditPropertyAction(
                    "Edit short description",
                    quest::setShortDescription,
                    shortDescription,
                    quest.shortDescription.value,
                )
            )
        }
    }

    fun setLongDescription(longDescription: String) {
        if (!enabled.value) return

        store.currentQuest.value?.let { quest ->
            store.executeAction(
                EditPropertyAction(
                    "Edit long description",
                    quest::setLongDescription,
                    longDescription,
                    quest.longDescription.value,
                )
            )
        }
    }
}

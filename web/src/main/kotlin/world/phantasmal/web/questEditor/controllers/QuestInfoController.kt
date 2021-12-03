package world.phantasmal.web.questEditor.controllers

import world.phantasmal.observable.cell.*
import world.phantasmal.web.questEditor.actions.EditPropertyAction
import world.phantasmal.web.questEditor.stores.QuestEditorStore
import world.phantasmal.webui.controllers.Controller

class QuestInfoController(private val store: QuestEditorStore) : Controller() {
    val unavailable: Cell<Boolean> = store.currentQuest.isNull()
    val enabled: Cell<Boolean> = store.questEditingEnabled

    val episode: Cell<String> = store.currentQuest.map { it?.episode?.name ?: "" }
    val id: Cell<Int> = store.currentQuest.flatMap { it?.id ?: cell(0) }
    val name: Cell<String> = store.currentQuest.flatMap { it?.name ?: emptyStringCell() }
    val shortDescription: Cell<String> =
        store.currentQuest.flatMap { it?.shortDescription ?: emptyStringCell() }
    val longDescription: Cell<String> =
        store.currentQuest.flatMap { it?.longDescription ?: emptyStringCell() }

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

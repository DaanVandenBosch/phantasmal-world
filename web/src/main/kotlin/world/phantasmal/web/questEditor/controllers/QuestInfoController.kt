package world.phantasmal.web.questEditor.controllers

import world.phantasmal.cell.Cell
import world.phantasmal.cell.cell
import world.phantasmal.cell.emptyStringCell
import world.phantasmal.cell.flatMap
import world.phantasmal.cell.isNull
import world.phantasmal.cell.map
import world.phantasmal.web.questEditor.commands.EditQuestPropertyCommand
import world.phantasmal.web.questEditor.models.QuestModel
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
            store.executeAction(EditQuestPropertyCommand(
                store,
                "Edit ID",
                quest,
                QuestModel::setId,
                id,
                quest.id.value,
            ))
        }
    }

    fun setName(name: String) {
        if (!enabled.value) return

        store.currentQuest.value?.let { quest ->
            store.executeAction(
                EditQuestPropertyCommand(
                    store,
                    "Edit name",
                    quest,
                    QuestModel::setName,
                    name,
                    quest.name.value,
                )
            )
        }
    }

    fun setShortDescription(shortDescription: String) {
        if (!enabled.value) return

        store.currentQuest.value?.let { quest ->
            store.executeAction(
                EditQuestPropertyCommand(
                    store,
                    "Edit short description",
                    quest,
                    QuestModel::setShortDescription,
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
                EditQuestPropertyCommand(
                    store,
                    "Edit long description",
                    quest,
                    QuestModel::setLongDescription,
                    longDescription,
                    quest.longDescription.value,
                )
            )
        }
    }
}

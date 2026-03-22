package world.phantasmal.web.questEditor.controllers

import world.phantasmal.cell.Cell
import world.phantasmal.cell.isNull
import world.phantasmal.cell.list.ListCell
import world.phantasmal.cell.map
import world.phantasmal.web.questEditor.models.QuestNpcModel
import world.phantasmal.web.questEditor.stores.QuestEditorStore
import world.phantasmal.web.questEditor.stores.ViewportStore
import world.phantasmal.webui.controllers.Controller

class AreaNpcListController(private val store: QuestEditorStore, private val viewportStore: ViewportStore) : Controller() {
    val unavailable: Cell<Boolean> = store.currentQuest.isNull()
    val npcs: ListCell<QuestNpcModel> = store.currentAreaNpcs

    val currentAreaIdentifier: Cell<Pair<Int?, Int?>> =
        map(store.currentArea, store.currentAreaVariant) { area, variant ->
            Pair(area?.id, variant?.id)
        }

    fun isSelected(npc: QuestNpcModel): Cell<Boolean> =
        store.selectedEntity.map { it === npc }

    fun selectNpc(npc: QuestNpcModel) {
        store.setSelectedEntity(npc)
        viewportStore.setTargetCameraPosition(npc.worldPosition.value)
    }

    fun clicked() {
        store.setSelectedEntity(null)
    }

    fun focused() {
        store.makeMainUndoCurrent()
    }
}

package world.phantasmal.web.questEditor.controllers

import world.phantasmal.cell.Cell
import world.phantasmal.cell.isNull
import world.phantasmal.cell.list.ListCell
import world.phantasmal.cell.map
import world.phantasmal.web.questEditor.models.QuestObjectModel
import world.phantasmal.web.questEditor.stores.QuestEditorStore
import world.phantasmal.web.questEditor.stores.ViewportStore
import world.phantasmal.webui.controllers.Controller

class AreaObjectListController(private val store: QuestEditorStore, private val viewportStore: ViewportStore) : Controller() {
    val unavailable: Cell<Boolean> = store.currentQuest.isNull()
    val objects: ListCell<QuestObjectModel> = store.currentAreaObjects
    val currentQuest get() = store.currentQuest
    val currentArea get() = store.currentArea

    val currentAreaIdentifier: Cell<Pair<Int?, Int?>> =
        map(store.currentArea, store.currentAreaVariant) { area, variant ->
            Pair(area?.id, variant?.id)
        }

    /** Returns the global index of this object in quest.objects, or -1 if not found. */
    fun globalIndex(obj: QuestObjectModel): Int =
        store.currentQuest.value?.objects?.value?.indexOf(obj) ?: -1

    fun isSelected(obj: QuestObjectModel): Cell<Boolean> =
        store.selectedEntity.map { it === obj }

    fun selectObject(obj: QuestObjectModel) {
        store.setSelectedEntity(obj)
        viewportStore.setTargetCameraPosition(obj.worldPosition.value)
    }

    fun clicked() {
        store.setSelectedEntity(null)
    }

    fun focused() {
        store.makeMainUndoCurrent()
    }
}

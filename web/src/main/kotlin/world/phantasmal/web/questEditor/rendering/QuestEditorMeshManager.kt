package world.phantasmal.web.questEditor.rendering

import world.phantasmal.cell.and
import world.phantasmal.cell.eq
import world.phantasmal.cell.flatMapNull
import world.phantasmal.cell.list.emptyListCell
import world.phantasmal.cell.list.filteredCell
import world.phantasmal.cell.or
import world.phantasmal.web.questEditor.loading.AreaAssetLoader
import world.phantasmal.web.questEditor.loading.EntityAssetLoader
import world.phantasmal.web.questEditor.stores.QuestEditorStore

class QuestEditorMeshManager(
    areaAssetLoader: AreaAssetLoader,
    entityAssetLoader: EntityAssetLoader,
    questEditorStore: QuestEditorStore,
    renderContext: QuestRenderContext,
) : QuestMeshManager(areaAssetLoader, entityAssetLoader, questEditorStore, renderContext) {
    init {
        observeNow(
            questEditorStore.currentQuest,
            questEditorStore.currentAreaVariant,
        ) { quest, areaVariant ->
            loadAreaMeshes(quest?.episode, areaVariant)
        }

        observeNow(
            questEditorStore.currentQuest,
            questEditorStore.currentArea,
            questEditorStore.selectedEvent.flatMapNull { it?.wave },
        ) { quest, area, wave ->
            loadNpcMeshes(
                if (quest != null && area != null) {
                    quest.npcs.filteredCell {
                        it.sectionInitialized and
                                (it.areaId == area.id) and
                                ((wave == null) or (it.wave eq wave))
                    }
                } else {
                    emptyListCell()
                }
            )
        }

        observeNow(
            questEditorStore.currentQuest,
            questEditorStore.currentArea,
        ) { quest, area ->
            loadObjectMeshes(
                if (quest != null && area != null) {
                    quest.objects.filteredCell {
                        it.sectionInitialized and (it.areaId == area.id)
                    }
                } else {
                    emptyListCell()
                }
            )
        }

        observeNow(questEditorStore.showCollisionGeometry) {
            renderContext.collisionGeometryVisible = it
            renderContext.renderGeometryVisible = !it
        }
    }
}

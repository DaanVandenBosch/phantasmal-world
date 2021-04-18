package world.phantasmal.web.questEditor.rendering

import world.phantasmal.observable.value.list.emptyListVal
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
        observe(
            questEditorStore.currentQuest,
            questEditorStore.currentAreaVariant,
        ) { quest, areaVariant ->
            loadAreaMeshes(quest?.episode, areaVariant)
        }

        observe(
            questEditorStore.currentQuest,
            questEditorStore.currentArea,
            questEditorStore.selectedEvent.flatMapNull { it?.wave },
        ) { quest, area, wave ->
            loadNpcMeshes(
                if (quest != null && area != null) {
                    quest.npcs.filtered {
                        it.sectionInitialized.value &&
                                it.areaId == area.id &&
                                (wave == null || it.wave.value == wave)
                    }
                } else {
                    emptyListVal()
                }
            )
        }

        observe(
            questEditorStore.currentQuest,
            questEditorStore.currentArea,
        ) { quest, area ->
            loadObjectMeshes(
                if (quest != null && area != null) {
                    quest.objects.filtered {
                        it.sectionInitialized.value && it.areaId == area.id
                    }
                } else {
                    emptyListVal()
                }
            )
        }

        observe(questEditorStore.showCollisionGeometry) {
            renderContext.collisionGeometryVisible = it
            renderContext.renderGeometryVisible = !it
        }
    }
}

package world.phantasmal.web.questEditor.rendering

import world.phantasmal.lib.Episode
import world.phantasmal.observable.value.list.ListVal
import world.phantasmal.observable.value.list.listVal
import world.phantasmal.observable.value.map
import world.phantasmal.web.questEditor.loading.AreaAssetLoader
import world.phantasmal.web.questEditor.loading.EntityAssetLoader
import world.phantasmal.web.questEditor.models.*
import world.phantasmal.web.questEditor.stores.QuestEditorStore

class QuestEditorMeshManager(
    areaAssetLoader: AreaAssetLoader,
    entityAssetLoader: EntityAssetLoader,
    questEditorStore: QuestEditorStore,
    renderContext: QuestRenderContext,
) : QuestMeshManager(areaAssetLoader, entityAssetLoader, questEditorStore, renderContext) {
    init {
        addDisposables(
            map(
                questEditorStore.currentQuest,
                questEditorStore.currentArea,
                ::getAreaVariantDetails
            ).observe { (details) ->
                loadMeshes(details.episode, details.areaVariant, details.npcs, details.objects)
            },
        )
    }

    private fun getAreaVariantDetails(quest: QuestModel?, area: AreaModel?): AreaVariantDetails {
        quest?.let {
            val areaVariant = area?.let {
                quest.areaVariants.value.find { it.area.id == area.id } ?: area.areaVariants.first()
            }

            areaVariant?.let {
                val npcs = quest.npcs.filtered { it.areaId == area.id }
                val objects = quest.objects.filtered { it.areaId == area.id }
                return AreaVariantDetails(quest.episode, areaVariant, npcs, objects)
            }
        }

        return AreaVariantDetails(null, null, listVal(), listVal())
    }

    private class AreaVariantDetails(
        val episode: Episode?,
        val areaVariant: AreaVariantModel?,
        val npcs: ListVal<QuestNpcModel>,
        val objects: ListVal<QuestObjectModel>,
    )
}

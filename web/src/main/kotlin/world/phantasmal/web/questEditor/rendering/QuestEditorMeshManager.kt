package world.phantasmal.web.questEditor.rendering

import kotlinx.coroutines.CoroutineScope
import world.phantasmal.observable.value.Val
import world.phantasmal.observable.value.list.ListVal
import world.phantasmal.observable.value.list.listVal
import world.phantasmal.web.questEditor.loading.EntityAssetLoader
import world.phantasmal.web.questEditor.models.*

class QuestEditorMeshManager(
    scope: CoroutineScope,
    private val currentQuest: Val<QuestModel?>,
    private val currentArea: Val<AreaModel?>,
    selectedWave: Val<WaveModel?>,
    renderer: QuestRenderer,
    entityAssetLoader: EntityAssetLoader,
) : QuestMeshManager(scope, selectedWave, renderer, entityAssetLoader) {
    init {
        disposer.addAll(
            currentQuest.observe { areaVariantChanged() },
            currentArea.observe { areaVariantChanged() },
        )
    }

    override fun getAreaVariantDetails(): AreaVariantDetails {
        val quest = currentQuest.value
        val area = currentArea.value

        val areaVariant: AreaVariantModel?
        val npcs: ListVal<QuestNpcModel>
        val objects: ListVal<QuestObjectModel>

        if (quest != null /*&& area != null*/) {
            // TODO: Set areaVariant.
            areaVariant = null
            npcs = quest.npcs // TODO: Filter NPCs.
            objects = listVal() // TODO: Filter objects.
        } else {
            areaVariant = null
            npcs = listVal()
            objects = listVal()
        }

        return AreaVariantDetails(quest?.episode, areaVariant, npcs, objects)
    }
}

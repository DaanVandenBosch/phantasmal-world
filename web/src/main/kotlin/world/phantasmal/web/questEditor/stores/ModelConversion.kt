package world.phantasmal.web.questEditor.stores

import world.phantasmal.lib.Episode
import world.phantasmal.lib.fileFormats.quest.DatEventAction
import world.phantasmal.lib.fileFormats.quest.Quest
import world.phantasmal.web.questEditor.models.*

fun convertQuestToModel(
    quest: Quest,
    getVariant: (Episode, areaId: Int, variantId: Int) -> AreaVariantModel?,
): QuestModel {
    return QuestModel(
        quest.id,
        quest.language,
        quest.name,
        quest.shortDescription,
        quest.longDescription,
        quest.episode,
        quest.mapDesignations,
        quest.npcs.mapTo(mutableListOf()) {
            QuestNpcModel(
                it,
                WaveModel(it.wave.toInt(), it.areaId, it.sectionId.toInt()),
            )
        },
        quest.objects.mapTo(mutableListOf()) { QuestObjectModel(it) },
        quest.events.mapTo(mutableListOf()) { event ->
            QuestEventModel(
                event.id,
                event.areaId,
                event.sectionId.toInt(),
                WaveModel(event.wave.toInt(), event.areaId, event.sectionId.toInt()),
                event.delay.toInt(),
                event.unknown.toInt(),
                event.actions.mapTo(mutableListOf()) {
                    when (it) {
                        is DatEventAction.SpawnNpcs ->
                            QuestEventActionModel.SpawnNpcs(
                                it.sectionId.toInt(),
                                it.appearFlag.toInt()
                            )

                        is DatEventAction.Unlock ->
                            QuestEventActionModel.Unlock(it.doorId.toInt())

                        is DatEventAction.Lock ->
                            QuestEventActionModel.Lock(it.doorId.toInt())

                        is DatEventAction.TriggerEvent ->
                            QuestEventActionModel.TriggerEvent(it.eventId)
                    }
                }
            )
        },
        quest.datUnknowns,
        quest.bytecodeIr,
        quest.shopItems,
        getVariant,
    )
}

package world.phantasmal.web.questEditor.stores

import world.phantasmal.lib.Episode
import world.phantasmal.lib.fileFormats.quest.DatEvent
import world.phantasmal.lib.fileFormats.quest.DatEventAction
import world.phantasmal.lib.fileFormats.quest.Quest
import world.phantasmal.web.questEditor.models.*

fun convertQuestToModel(
    quest: Quest,
    getVariant: (Episode, areaId: Int, variantId: Int) -> AreaVariantModel?,
): QuestModel =
    QuestModel(
        quest.id,
        quest.language,
        quest.name,
        quest.shortDescription,
        quest.longDescription,
        quest.episode,
        quest.mapDesignations,
        quest.npcs.mapTo(mutableListOf()) { QuestNpcModel(it, it.wave.toInt()) },
        quest.objects.mapTo(mutableListOf()) { QuestObjectModel(it) },
        quest.events.mapTo(mutableListOf()) { event ->
            QuestEventModel(
                event.id,
                event.areaId,
                event.sectionId.toInt(),
                event.wave.toInt(),
                event.delay.toInt(),
                event.unknown.toInt(),
                event.actions.mapTo(mutableListOf()) { action ->
                    when (action) {
                        is DatEventAction.SpawnNpcs ->
                            QuestEventActionModel.SpawnNpcs(
                                action.sectionId.toInt(),
                                action.appearFlag.toInt()
                            )

                        is DatEventAction.Unlock ->
                            QuestEventActionModel.Door.Unlock(action.doorId.toInt())

                        is DatEventAction.Lock ->
                            QuestEventActionModel.Door.Lock(action.doorId.toInt())

                        is DatEventAction.TriggerEvent ->
                            QuestEventActionModel.TriggerEvent(action.eventId)
                    }
                }
            )
        },
        quest.datUnknowns,
        quest.bytecodeIr,
        quest.shopItems,
        getVariant,
    )

/**
 * The returned [Quest] object will reference parts of [quest], so some changes to [quest] will be
 * reflected in the returned object.
 */
fun convertQuestFromModel(quest: QuestModel): Quest =
    Quest(
        quest.id.value,
        quest.language.value,
        quest.name.value,
        quest.shortDescription.value,
        quest.longDescription.value,
        quest.episode,
        quest.objects.value.map { it.entity },
        quest.npcs.value.map { it.entity },
        quest.events.value.map { event ->
            DatEvent(
                event.id.value,
                event.sectionId.value.toShort(),
                event.wave.value.id.toShort(),
                event.delay.value.toShort(),
                event.actions.value.map { action ->
                    when (action) {
                        is QuestEventActionModel.SpawnNpcs ->
                            DatEventAction.SpawnNpcs(
                                action.sectionId.value.toShort(),
                                action.appearFlag.value.toShort(),
                            )

                        is QuestEventActionModel.Door.Unlock ->
                            DatEventAction.Unlock(action.doorId.value.toShort())

                        is QuestEventActionModel.Door.Lock ->
                            DatEventAction.Lock(action.doorId.value.toShort())

                        is QuestEventActionModel.TriggerEvent ->
                            DatEventAction.TriggerEvent(action.eventId.value)
                    }
                },
                event.areaId,
                event.unknown.toShort(),
            )
        },
        quest.datUnknowns,
        quest.bytecodeIr,
        quest.shopItems,
        quest.mapDesignations.value,
    )

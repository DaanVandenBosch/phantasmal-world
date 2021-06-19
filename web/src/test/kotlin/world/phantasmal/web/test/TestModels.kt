package world.phantasmal.web.test

import world.phantasmal.lib.Episode
import world.phantasmal.lib.asm.BytecodeIr
import world.phantasmal.lib.fileFormats.quest.NpcType
import world.phantasmal.lib.fileFormats.quest.ObjectType
import world.phantasmal.lib.fileFormats.quest.QuestNpc
import world.phantasmal.lib.fileFormats.quest.QuestObject
import world.phantasmal.web.questEditor.models.QuestEventModel
import world.phantasmal.web.questEditor.models.QuestModel
import world.phantasmal.web.questEditor.models.QuestNpcModel
import world.phantasmal.web.questEditor.models.QuestObjectModel

fun WebTestContext.createQuestModel(
    id: Int = 1,
    name: String = "Test",
    shortDescription: String = name,
    longDescription: String = name,
    episode: Episode = Episode.I,
    mapDesignations: Map<Int, Int> = emptyMap(),
    npcs: List<QuestNpcModel> = emptyList(),
    objects: List<QuestObjectModel> = emptyList(),
    events: List<QuestEventModel> = emptyList(),
    bytecodeIr: BytecodeIr = BytecodeIr(emptyList()),
): QuestModel =
    QuestModel(
        id,
        language = 1,
        name,
        shortDescription,
        longDescription,
        episode,
        mapDesignations,
        npcs.toMutableList(),
        objects.toMutableList(),
        events.toMutableList(),
        datUnknowns = emptyList(),
        bytecodeIr,
        UIntArray(0),
        components.areaStore::getVariant,
    )

fun createQuestNpcModel(type: NpcType, episode: Episode): QuestNpcModel =
    QuestNpcModel(
        QuestNpc(type, episode, areaId = 0, wave = 0),
        waveId = 0,
    )

fun createQuestObjectModel(type: ObjectType): QuestObjectModel =
    QuestObjectModel(QuestObject(type, areaId = 0))

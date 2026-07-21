package world.phantasmal.web.test

import world.phantasmal.psolib.Episode
import world.phantasmal.psolib.asm.BytecodeIr
import world.phantasmal.psolib.asm.dataFlowAnalysis.FloorMapping
import world.phantasmal.psolib.fileFormats.quest.NpcType
import world.phantasmal.psolib.fileFormats.quest.ObjectType
import world.phantasmal.psolib.fileFormats.quest.QuestNpc
import world.phantasmal.psolib.fileFormats.quest.QuestObject
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
    floorMappings: List<FloorMapping> = emptyList(),
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
        npcs.toMutableList(),
        objects.toMutableList(),
        events.toMutableList(),
        datUnknowns = emptyList(),
        cmRandomSpawns = mutableListOf(),
        cmMonsterMappings = mutableListOf(),
        cmConfigPool = mutableListOf(),
        bytecodeIr,
        UIntArray(0),
        floorMappings,
        components.areaStore::getVariant,
    )

fun createQuestNpcModel(
    type: NpcType,
    episode: Episode,
    areaId: Int = 0,
): QuestNpcModel =
    QuestNpcModel(
        QuestNpc(type, episode, areaId = areaId, wave = 0),
        waveId = 0,
    )

fun createQuestObjectModel(type: ObjectType, areaId: Int = 0): QuestObjectModel =
    QuestObjectModel(QuestObject(type, areaId = areaId))

package world.phantasmal.web.test

import world.phantasmal.lib.Episode
import world.phantasmal.lib.asm.BytecodeIr
import world.phantasmal.lib.fileFormats.quest.NpcType
import world.phantasmal.lib.fileFormats.quest.QuestNpc
import world.phantasmal.web.questEditor.models.QuestModel
import world.phantasmal.web.questEditor.models.QuestNpcModel
import world.phantasmal.web.questEditor.models.QuestObjectModel
import world.phantasmal.web.questEditor.models.WaveModel

fun createQuestModel(
    id: Int = 1,
    name: String = "Test",
    shortDescription: String = name,
    longDescription: String = name,
    episode: Episode = Episode.I,
    npcs: List<QuestNpcModel> = emptyList(),
    objects: List<QuestObjectModel> = emptyList(),
    bytecodeIr: BytecodeIr = BytecodeIr(emptyList()),
): QuestModel =
    QuestModel(
        id,
        language = 1,
        name,
        shortDescription,
        longDescription,
        episode,
        emptyMap(),
        npcs.toMutableList(),
        objects.toMutableList(),
        events = mutableListOf(),
        datUnknowns = emptyList(),
        bytecodeIr,
        UIntArray(0),
    ) { _, _, _ -> null }

fun createQuestNpcModel(type: NpcType, episode: Episode): QuestNpcModel =
    QuestNpcModel(
        QuestNpc(type, episode, areaId = 0, wave = 0),
        WaveModel(id = 0, areaId = 0, sectionId = 0),
    )

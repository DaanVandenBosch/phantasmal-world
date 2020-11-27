package world.phantasmal.web.test

import world.phantasmal.lib.assembly.Segment
import world.phantasmal.lib.fileFormats.quest.Episode
import world.phantasmal.lib.fileFormats.quest.NpcType
import world.phantasmal.lib.fileFormats.quest.QuestNpc
import world.phantasmal.web.questEditor.models.QuestModel
import world.phantasmal.web.questEditor.models.QuestNpcModel
import world.phantasmal.web.questEditor.models.QuestObjectModel

fun createQuestModel(
    id: Int = 1,
    name: String = "Test",
    shortDescription: String = name,
    longDescription: String = name,
    episode: Episode = Episode.I,
    npcs: List<QuestNpcModel> = emptyList(),
    objects: List<QuestObjectModel> = emptyList(),
    bytecodeIr: List<Segment> = emptyList(),
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
        bytecodeIr,
    ) { _, _, _ -> null }

fun createQuestNpcModel(type: NpcType, episode: Episode): QuestNpcModel =
    QuestNpcModel(QuestNpc(type, episode, areaId = 0, wave = 0), wave = null)

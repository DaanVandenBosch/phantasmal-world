package world.phantasmal.web.questEditor.stores

import world.phantasmal.lib.fileFormats.quest.Quest
import world.phantasmal.web.questEditor.models.QuestModel
import world.phantasmal.web.questEditor.models.QuestNpcModel
import world.phantasmal.web.questEditor.models.QuestObjectModel

fun convertQuestToModel(quest: Quest): QuestModel {
    return QuestModel(
        quest.id,
        quest.language,
        quest.name,
        quest.shortDescription,
        quest.longDescription,
        quest.episode,
        // TODO: Add WaveModel to QuestNpcModel
        quest.npcs.mapTo(mutableListOf()) { QuestNpcModel(it, null) },
        quest.objects.mapTo(mutableListOf()) { QuestObjectModel(it) }
    )
}

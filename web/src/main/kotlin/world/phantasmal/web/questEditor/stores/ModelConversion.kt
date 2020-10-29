package world.phantasmal.web.questEditor.stores

import world.phantasmal.lib.fileFormats.quest.Quest
import world.phantasmal.web.questEditor.models.QuestModel

fun convertQuestToModel(quest: Quest): QuestModel {
    return QuestModel(
        quest.id,
        quest.language,
        quest.name,
        quest.shortDescription,
        quest.longDescription,
        quest.episode,
    )
}

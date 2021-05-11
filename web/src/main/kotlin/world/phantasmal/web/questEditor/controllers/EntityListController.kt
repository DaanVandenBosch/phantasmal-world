package world.phantasmal.web.questEditor.controllers

import world.phantasmal.lib.Episode
import world.phantasmal.lib.fileFormats.quest.EntityType
import world.phantasmal.lib.fileFormats.quest.NpcType
import world.phantasmal.lib.fileFormats.quest.ObjectType
import world.phantasmal.observable.cell.Cell
import world.phantasmal.observable.cell.map
import world.phantasmal.web.questEditor.stores.QuestEditorStore
import world.phantasmal.webui.controllers.Controller

class EntityListController(store: QuestEditorStore, private val npcs: Boolean) : Controller() {
    @Suppress("UNCHECKED_CAST")
    private val entityTypes = (if (npcs) NpcType.VALUES else ObjectType.VALUES) as Array<EntityType>

    val enabled: Cell<Boolean> = store.questEditingEnabled

    val entities: Cell<List<EntityType>> =
        map(store.currentQuest, store.currentArea) { quest, area ->
            val episode = quest?.episode ?: Episode.I
            val areaId = area?.id ?: 0

            entityTypes.filter { entityType ->
                filter(entityType, episode, areaId)
            }
        }

    private fun filter(entityType: EntityType, episode: Episode, areaId: Int): Boolean =
        if (npcs) {
            entityType as NpcType

            (entityType.episode == null || entityType.episode == episode) &&
                    areaId in entityType.areaIds
        } else {
            entityType as ObjectType

            entityType.areaIds[episode]?.contains(areaId) == true
        }
}

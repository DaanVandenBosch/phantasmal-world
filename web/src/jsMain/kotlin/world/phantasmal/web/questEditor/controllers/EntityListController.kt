package world.phantasmal.web.questEditor.controllers

import world.phantasmal.cell.Cell
import world.phantasmal.cell.map
import world.phantasmal.psolib.Episode
import world.phantasmal.psolib.fileFormats.quest.EntityType
import world.phantasmal.psolib.fileFormats.quest.NpcType
import world.phantasmal.psolib.fileFormats.quest.ObjectType
import world.phantasmal.psolib.fileFormats.quest.isBossArea
import world.phantasmal.web.questEditor.stores.QuestEditorStore
import world.phantasmal.webui.controllers.Controller

class EntityListController(store: QuestEditorStore, private val npcs: Boolean) : Controller() {
    @Suppress("UNCHECKED_CAST")
    private val entityTypes = (if (npcs) NpcType.VALUES else ObjectType.VALUES) as Array<EntityType>

    val enabled: Cell<Boolean> = store.questEditingEnabled

    val entities: Cell<List<EntityType>> =
        map(store.currentQuest, store.currentArea, store.omnispawn) { quest, area, omnispawn ->
            val episode = quest?.episode ?: Episode.I
            val areaId = area?.id ?: 0

            entityTypes.filter { entityType ->
                filter(entityType, episode, areaId, omnispawn)
            }
        }

    private fun filter(entityType: EntityType, episode: Episode, areaId: Int, omnispawn: Boolean): Boolean {
        if (npcs) {
            entityType as NpcType

            // Always filter out specific enemy NPCs that should be removed
            if (entityType.minion) {
                return false
            }

            // Pioneer2/Lab areas: only show friendly NPCs (original logic)
            if (areaId == 0) {
                return !entityType.enemy &&
                        (entityType.episode == null || entityType.episode == episode) &&
                        areaId in entityType.areaIds
            }

            // Boss areas: only show current floor's boss NPCs (original logic)
            if (isBossArea(episode, areaId)) {
                return (entityType.episode == null || entityType.episode == episode) &&
                        areaId in entityType.areaIds
            }

            // Regular areas (not Pioneer2/Lab, not boss areas)
            if (omnispawn) {
                // In omnispawn mode: show enemy NPCs from all episodes
                // Exclude boss NPCs from regular areas
                if (entityType.boss) {
                    return false
                }

                // Show enemy NPCs from all episodes (I, II, IV)
                return entityType.enemy && (
                        entityType.episode == null ||
                                entityType.episode == Episode.I ||
                                entityType.episode == Episode.II ||
                                entityType.episode == Episode.IV
                        )
            } else {
                // Original logic: show only NPCs for current area
                return (entityType.episode == null || entityType.episode == episode) &&
                        areaId in entityType.areaIds
            }
        } else {
            entityType as ObjectType

            if (omnispawn) {
                // For objects, show all objects from any area in all episodes
                return entityType.areaIds[Episode.I]?.isNotEmpty() == true ||
                        entityType.areaIds[Episode.II]?.isNotEmpty() == true ||
                        entityType.areaIds[Episode.IV]?.isNotEmpty() == true
            } else {
                // Original logic: show only objects for current area
                return entityType.areaIds[episode]?.contains(areaId) == true
            }
        }
    }
}

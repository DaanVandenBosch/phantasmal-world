package world.phantasmal.web.questEditor.controllers

import world.phantasmal.cell.Cell
import world.phantasmal.cell.map
import world.phantasmal.psolib.Episode
import world.phantasmal.psolib.fileFormats.quest.EntityType
import world.phantasmal.psolib.fileFormats.quest.NpcType
import world.phantasmal.psolib.fileFormats.quest.ObjectType
import world.phantasmal.web.questEditor.models.AreaModel
import world.phantasmal.web.questEditor.models.QuestModel
import world.phantasmal.web.questEditor.stores.QuestEditorStore
import world.phantasmal.web.questEditor.stores.QuestEditorUiStore
import world.phantasmal.webui.controllers.Controller

class EntityListController(store: QuestEditorStore, uiStore: QuestEditorUiStore, private val npcs: Boolean) : Controller() {
    @Suppress("UNCHECKED_CAST")
    private val entityTypes = (if (npcs) NpcType.VALUES else ObjectType.VALUES) as Array<EntityType>

    val enabled: Cell<Boolean> = store.questEditingEnabled
    val currentQuest: Cell<QuestModel?> = store.currentQuest
    val currentArea: Cell<AreaModel?> = store.currentArea

    val entities: Cell<List<EntityType>> =
        map(store.currentQuest, store.currentArea, uiStore.omnispawn) { quest, area, omnispawn ->
            val episode = quest?.episode ?: Episode.I

            entityTypes.filter { entityType ->
                filter(entityType, episode, area, omnispawn)
            }
        }

    private fun filter(entityType: EntityType, episode: Episode, area: AreaModel?, omnispawn: Boolean): Boolean {
        val areaId = area?.id ?: 0

        if (npcs) {
            entityType as NpcType

            if (entityType.minion) {
                return false
            }

            if (omnispawn && areaId != 0 && area?.bossArea != true) {
                // In omnispawn mode for regular (non-Pioneer2, non-boss) areas:
                // show enemy NPCs from all episodes, excluding bosses and minions
                return entityType.enemy && !entityType.boss && !entityType.minion
            } else {
                // Regular logic for Pioneer2, boss areas, and non-omnispawn mode:
                // show NPCs matching current episode and area
                return (entityType.episode == null || entityType.episode == episode) &&
                        areaId in entityType.areaIds
            }
        } else {
            entityType as ObjectType

            if (omnispawn) {
                // For objects, show all objects from any area in all episodes
                return entityType.areaIds.isNotEmpty()
            } else {
                // Original logic: show only objects for current area
                return entityType.areaIds[episode]?.contains(areaId) == true
            }
        }
    }
}

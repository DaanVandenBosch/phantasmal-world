package world.phantasmal.web.questEditor.controllers

import world.phantasmal.web.core.controllers.*
import world.phantasmal.web.questEditor.persistence.QuestEditorUiPersister

class QuestEditorController(
    private val questEditorUiPersister: QuestEditorUiPersister,
) : DockController() {
    override suspend fun initialConfig(): DockedItem =
        questEditorUiPersister.loadLayoutConfig(ALL_WIDGET_IDS) ?: DEFAULT_CONFIG

    override suspend fun configChanged(config: DockedItem) {
        questEditorUiPersister.persistLayoutConfig(config)
    }

    companion object {
        // These IDs are persisted, don't change them.
        const val QUEST_INFO_WIDGET_ID = "quest-info"
        const val NPC_COUNTS_WIDGET_ID = "npc-counts"
        const val ENTITY_INFO_WIDGET_ID = "entity-info"
        const val QUEST_RENDERER_WIDGET_ID = "quest-renderer"
        const val ASM_WIDGET_ID = "asm"
        const val NPC_LIST_WIDGET_ID = "npc-list"
        const val OBJECT_LIST_WIDGET_ID = "object-list"
        const val EVENTS_WIDGET_ID = "events"

        private val ALL_WIDGET_IDS: Set<String> = setOf(
            QUEST_INFO_WIDGET_ID,
            NPC_COUNTS_WIDGET_ID,
            ENTITY_INFO_WIDGET_ID,
            QUEST_RENDERER_WIDGET_ID,
            ASM_WIDGET_ID,
            NPC_LIST_WIDGET_ID,
            OBJECT_LIST_WIDGET_ID,
            EVENTS_WIDGET_ID,
        )

        private val DEFAULT_CONFIG = DockedRow(
            items = listOf(
                DockedColumn(
                    flex = 2.0,
                    items = listOf(
                        DockedStack(
                            items = listOf(
                                DockedWidget(
                                    title = "Info",
                                    id = QUEST_INFO_WIDGET_ID,
                                ),
                                DockedWidget(
                                    title = "NPC Counts",
                                    id = NPC_COUNTS_WIDGET_ID,
                                ),
                            )
                        ),
                        DockedWidget(
                            title = "Entity",
                            id = ENTITY_INFO_WIDGET_ID,
                        ),
                    )
                ),
                DockedStack(
                    flex = 9.0,
                    items = listOf(
                        DockedWidget(
                            title = "3D View",
                            id = QUEST_RENDERER_WIDGET_ID,
                        ),
                        DockedWidget(
                            title = "Script",
                            id = ASM_WIDGET_ID,
                        ),
                    )
                ),
                DockedStack(
                    flex = 2.0,
                    items = listOf(
                        DockedWidget(
                            title = "NPCs",
                            id = NPC_LIST_WIDGET_ID,
                        ),
                        DockedWidget(
                            title = "Objects",
                            id = OBJECT_LIST_WIDGET_ID,
                        ),
                        DockedWidget(
                            title = "Events",
                            id = EVENTS_WIDGET_ID,
                        ),
                    )
                ),
            )
        )
    }
}

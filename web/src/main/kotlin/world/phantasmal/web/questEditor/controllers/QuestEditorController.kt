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
        const val QUEST_INFO_WIDGET_ID = "info"
        const val NPC_COUNTS_WIDGET_ID = "npc_counts"
        const val ENTITY_INFO_WIDGET_ID = "entity_info"
        const val QUEST_RENDERER_WIDGET_ID = "quest_renderer"
        const val ASSEMBLY_EDITOR_WIDGET_ID = "asm_editor"
        const val NPC_LIST_WIDGET_ID = "npc_list_view"
        const val OBJECT_LIST_WIDGET_ID = "object_list_view"
        const val EVENTS_WIDGET_ID = "events_view"

        private val ALL_WIDGET_IDS: Set<String> = setOf(
            "info",
            "npc_counts",
            "entity_info",
            "quest_renderer",
            "asm_editor",
            "npc_list_view",
            "object_list_view",
            "events_view",
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
                            id = ASSEMBLY_EDITOR_WIDGET_ID,
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

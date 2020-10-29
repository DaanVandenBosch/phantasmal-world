package world.phantasmal.web.questEditor.widgets

import kotlinx.coroutines.CoroutineScope
import org.w3c.dom.Node
import world.phantasmal.web.core.widgets.*
import world.phantasmal.webui.dom.div
import world.phantasmal.webui.widgets.Widget

// TODO: Remove TestWidget.
private class TestWidget(scope: CoroutineScope) : Widget(scope) {
    override fun Node.createElement() = div {
        textContent = "Test ${++count}"
    }

    companion object {
        private var count = 0
    }
}

open class QuestEditorWidget(
    scope: CoroutineScope,
    private val toolbar: Widget,
    private val createQuestInfoWidget: (CoroutineScope) -> Widget,
    private val createQuestRendererWidget: (CoroutineScope) -> Widget,
) : Widget(scope) {
    override fun Node.createElement() =
        div {
            className = "pw-quest-editor-quest-editor"

            addChild(toolbar)
            addChild(DockWidget(
                scope,
                item = DockedRow(
                    items = listOf(
                        DockedColumn(
                            flex = 2,
                            items = listOf(
                                DockedStack(
                                    items = listOf(
                                        DockedWidget(
                                            title = "Info",
                                            id = "info",
                                            createWidget = createQuestInfoWidget
                                        ),
                                        DockedWidget(
                                            title = "NPC Counts",
                                            id = "npc_counts",
                                            createWidget = ::TestWidget
                                        ),
                                    )
                                ),
                                DockedWidget(
                                    title = "Entity",
                                    id = "entity_info",
                                    createWidget = ::TestWidget
                                ),
                            )
                        ),
                        DockedStack(
                            flex = 9,
                            items = listOf(
                                DockedWidget(
                                    title = "3D View",
                                    id = "quest_renderer",
                                    createWidget = createQuestRendererWidget
                                ),
                                DockedWidget(
                                    title = "Script",
                                    id = "asm_editor",
                                    createWidget = ::TestWidget
                                ),
                            )
                        ),
                        DockedStack(
                            flex = 2,
                            items = listOf(
                                DockedWidget(
                                    title = "NPCs",
                                    id = "npc_list_view",
                                    createWidget = ::TestWidget
                                ),
                                DockedWidget(
                                    title = "Objects",
                                    id = "object_list_view",
                                    createWidget = ::TestWidget
                                ),
                                DockedWidget(
                                    title = "Events",
                                    id = "events_view",
                                    createWidget = ::TestWidget
                                ),
                            )
                        ),
                    )
                )
            ))
        }

    companion object {
        init {
            @Suppress("CssUnusedSymbol")
            // language=css
            style("""
                .pw-quest-editor-quest-editor {
                    display: flex;
                    flex-direction: column;
                    overflow: hidden;
                }
                .pw-quest-editor-quest-editor > * {
                    flex-grow: 1;
                }
            """.trimIndent())
        }
    }
}

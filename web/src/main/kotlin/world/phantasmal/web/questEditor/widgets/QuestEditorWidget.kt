package world.phantasmal.web.questEditor.widgets

import org.w3c.dom.Node
import world.phantasmal.core.disposable.Scope
import world.phantasmal.web.core.widgets.*
import world.phantasmal.webui.dom.div
import world.phantasmal.webui.widgets.Widget

// TODO: Remove TestWidget.
private class TestWidget(scope: Scope) : Widget(scope) {
    override fun Node.createElement() = div {
        textContent = "Test ${++count}"
    }

    companion object {
        private var count = 0
    }
}

open class QuestEditorWidget(
    scope: Scope,
    private val toolbar: QuestEditorToolbar,
    private val createQuestRendererWidget: (Scope) -> Widget,
) : Widget(scope, ::style) {
    override fun Node.createElement() =
        div(className = "pw-quest-editor-quest-editor") {
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
                                        DocketWidget(
                                            title = "Info",
                                            id = "info",
                                            createWidget = ::TestWidget
                                        ),
                                        DocketWidget(
                                            title = "NPC Counts",
                                            id = "npc_counts",
                                            createWidget = ::TestWidget
                                        ),
                                    )
                                ),
                                DocketWidget(
                                    title = "Entity",
                                    id = "entity_info",
                                    createWidget = ::TestWidget
                                ),
                            )
                        ),
                        DockedStack(
                            flex = 9,
                            items = listOf(
                                DocketWidget(
                                    title = "3D View",
                                    id = "quest_renderer",
                                    createWidget = createQuestRendererWidget
                                ),
                                DocketWidget(
                                    title = "Script",
                                    id = "asm_editor",
                                    createWidget = ::TestWidget
                                ),
                            )
                        ),
                        DockedStack(
                            flex = 2,
                            items = listOf(
                                DocketWidget(
                                    title = "NPCs",
                                    id = "npc_list_view",
                                    createWidget = ::TestWidget
                                ),
                                DocketWidget(
                                    title = "Objects",
                                    id = "object_list_view",
                                    createWidget = ::TestWidget
                                ),
                                DocketWidget(
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
}

@Suppress("CssUnusedSymbol")
// language=css
private fun style() = """
.pw-quest-editor-quest-editor {
    display: flex;
    flex-direction: column;
    overflow: hidden;
}
.pw-quest-editor-quest-editor > * {
    flex-grow: 1;
}
"""

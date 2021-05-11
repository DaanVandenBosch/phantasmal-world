package world.phantasmal.web.questEditor.widgets

import org.w3c.dom.Node
import world.phantasmal.web.core.widgets.UnavailableWidget
import world.phantasmal.web.questEditor.controllers.NpcCountsController
import world.phantasmal.webui.dom.*
import world.phantasmal.webui.widgets.Widget

class NpcCountsWidget(
    private val ctrl: NpcCountsController,
) : Widget() {
    override fun Node.createElement() =
        div {
            className = "pw-quest-editor-npc-counts"
            tabIndex = -1

            addEventListener("focus", { ctrl.focused() }, true)

            table {
                hidden(ctrl.unavailable)

                bindChildrenTo(ctrl.npcCounts) { (name, count), _ ->
                    tr {
                        th { textContent = "$name:" }
                        td { textContent = count }
                    }
                }
            }
            addChild(UnavailableWidget(
                visible = ctrl.unavailable,
                message = "No quest loaded."
            ))
        }

    companion object {
        init {
            @Suppress("CssUnusedSymbol")
            // language=css
            style("""
                .pw-quest-editor-npc-counts {
                    box-sizing: border-box;
                    padding: 3px;
                    overflow: auto;
                    outline: none;
                }

                .pw-quest-editor-npc-counts table {
                    user-select: text;
                    width: 100%;
                    max-width: 300px;
                    margin: 0 auto;
                }

                .pw-quest-editor-npc-counts th {
                    cursor: text;
                    text-align: left;
                }

                .pw-quest-editor-npc-counts td {
                    cursor: text;
                }
            """.trimIndent())
        }
    }
}

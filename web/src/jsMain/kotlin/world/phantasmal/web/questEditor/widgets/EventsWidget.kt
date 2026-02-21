package world.phantasmal.web.questEditor.widgets

import org.w3c.dom.Node
import world.phantasmal.cell.cell
import world.phantasmal.web.core.widgets.UnavailableWidget
import world.phantasmal.web.questEditor.controllers.EventsController
import world.phantasmal.webui.dom.Icon
import world.phantasmal.webui.dom.div
import world.phantasmal.webui.widgets.Button
import world.phantasmal.webui.widgets.Toolbar
import world.phantasmal.webui.widgets.Widget

class EventsWidget(private val ctrl: EventsController) : Widget() {
    override fun Node.createElement() =
        div {
            className = "pw-quest-editor-events"
            tabIndex = -1

            onclick = { ctrl.clicked() }
            addEventListener("focus", { ctrl.focused() }, true)

            div {
                className = "pw-quest-editor-events-inner"
                hidden(ctrl.unavailable)

                addChild(Toolbar(
                    children = listOf(
                        Button(
                            enabled = ctrl.enabled,
                            text = "Add",
                            iconLeft = Icon.Plus,
                            tooltip = cell("Add a new event"),
                            onClick = { ctrl.addEvent() },
                        ),
                        Button(
                            enabled = ctrl.removeEventEnabled,
                            text = "Remove",
                            iconLeft = Icon.Remove,
                            tooltip = cell("Remove the selected event"),
                            onClick = { ctrl.removeSelectedEvent() },
                        ),
                    )
                ))
                div {
                    className = "pw-quest-editor-events-container"

                    bindChildWidgetsTo(ctrl.events) { event, _ ->
                        EventWidget(ctrl, event)
                    }

                    // Reset scroll position when floor/variant changes
                    observe(ctrl.currentAreaIdentifier) {
                        scrollTop = 0.0
                    }
                }
            }
            addChild(UnavailableWidget(
                visible = ctrl.unavailable,
                message = "No quest loaded.",
            ))
        }

    companion object {
        init {
            @Suppress("CssUnusedSymbol")
            // language=css
            style("""
                .pw-quest-editor-events {
                    overflow: hidden;
                    outline: none;
                }

                .pw-quest-editor-events-inner {
                    display: flex;
                    flex-direction: column;
                    align-items: stretch;
                    overflow: hidden;
                    width: 100%;
                    height: 100%;
                }

                .pw-quest-editor-events-container {
                    flex: 1;
                    box-sizing: border-box;
                    overflow-y: auto;
                    display: flex;
                    flex-direction: row;
                    flex-wrap: wrap;
                    align-items: start;
                    justify-content: center;
                    padding: 4px;
                }
            """.trimIndent())
        }
    }
}

package world.phantasmal.web.questEditor.widgets

import org.w3c.dom.Node
import world.phantasmal.cell.cell
import world.phantasmal.cell.map
import world.phantasmal.web.core.widgets.UnavailableWidget
import world.phantasmal.web.questEditor.controllers.EventsController
import world.phantasmal.web.questEditor.controllers.PlaybackState
import world.phantasmal.webui.dom.Icon
import world.phantasmal.webui.dom.div
import world.phantasmal.webui.dom.span
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
                addChild(Toolbar(
                    children = listOf(
                        Button(
                            enabled = ctrl.stepBackwardEnabled,
                            iconLeft = Icon.StepBackward,
                            tooltip = cell("Step backward"),
                            onClick = { ctrl.stepBackward() },
                        ),
                        Button(
                            enabled = ctrl.playEnabled,
                            textCell = ctrl.playbackState.map { state ->
                                if (state == PlaybackState.Paused) "Resume" else "Play"
                            },
                            iconLeft = Icon.Play,
                            tooltip = cell("Play through events"),
                            onClick = { ctrl.play() },
                        ),
                        Button(
                            enabled = ctrl.pauseEnabled,
                            iconLeft = Icon.Pause,
                            tooltip = cell("Pause playback"),
                            onClick = { ctrl.pause() },
                        ),
                        Button(
                            enabled = ctrl.stopEnabled,
                            iconLeft = Icon.Stop,
                            tooltip = cell("Stop playback"),
                            onClick = { ctrl.stopPlayback() },
                        ),
                        Button(
                            enabled = ctrl.stepForwardEnabled,
                            iconLeft = Icon.StepForward,
                            tooltip = cell("Step forward"),
                            onClick = { ctrl.stepForward() },
                        ),
                    )
                ))
                div {
                    className = "pw-quest-editor-events-status"
                    hidden(ctrl.isStopped)
                    toggleClass("pw-playing", ctrl.isPlaying)

                    span {
                        observeNow(ctrl.playbackStatusText) {
                            textContent = it
                        }
                    }
                }
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

                .pw-quest-editor-events-status {
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    padding: 4px 8px;
                    font-size: 12px;
                    color: hsl(0, 0%, 85%);
                    background-color: hsl(0, 0%, 20%);
                    border-bottom: 1px solid hsl(0, 0%, 25%);
                }

                .pw-quest-editor-events-status.pw-playing {
                    background-color: hsl(130, 40%, 20%);
                    color: hsl(130, 60%, 80%);
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

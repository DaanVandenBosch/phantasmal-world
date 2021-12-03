package world.phantasmal.web.questEditor.widgets

import org.w3c.dom.*
import world.phantasmal.observable.cell.map
import world.phantasmal.web.questEditor.controllers.EventsController
import world.phantasmal.web.questEditor.models.QuestEventModel
import world.phantasmal.webui.dom.*
import world.phantasmal.webui.obj
import world.phantasmal.webui.widgets.Dropdown
import world.phantasmal.webui.widgets.IntInput
import world.phantasmal.webui.widgets.Widget

class EventWidget(
    private val ctrl: EventsController,
    private val event: QuestEventModel,
) : Widget() {
    private val isSelected = ctrl.isSelected(event)

    override fun Node.createElement() =
        div {
            className = "pw-quest-editor-event"
            toggleClass("pw-selected", isSelected)
            tabIndex = 0

            onclick = { e ->
                e.stopPropagation()
                ctrl.selectEvent(event)
            }

            onkeyup = { e ->
                if ((e.target as? Element)?.nodeName != "INPUT") {
                    when (e.key) {
                        "Enter" -> ctrl.selectEvent(event)
                        "Delete" -> ctrl.removeEvent(event)
                    }
                }
            }

            observe(isSelected) {
                if (it) {
                    scrollIntoView(obj<ScrollIntoViewOptions> {
                        behavior = ScrollBehavior.SMOOTH
                        inline = ScrollLogicalPosition.NEAREST
                        block = ScrollLogicalPosition.NEAREST
                    })
                }
            }

            div {
                className = "pw-quest-editor-event-props"

                table {
                    tr {
                        val idInput = IntInput(
                            enabled = ctrl.enabled,
                            value = event.id,
                            onChange = { ctrl.setId(event, it) },
                            label = "ID:",
                            min = 0,
                            step = 1,
                        )
                        th { addChild(idInput.label!!) }
                        td { addChild(idInput) }
                    }
                    tr {
                        val sectionIdInput = IntInput(
                            enabled = ctrl.enabled,
                            value = event.sectionId,
                            onChange = { ctrl.setSectionId(event, it) },
                            label = "Section:",
                            min = 0,
                            step = 1,
                        )
                        th { addChild(sectionIdInput.label!!) }
                        td { addChild(sectionIdInput) }
                    }
                    tr {
                        val waveInput = IntInput(
                            enabled = ctrl.enabled,
                            value = event.wave.map { it.id },
                            onChange = { ctrl.setWaveId(event, it) },
                            label = "Wave:",
                            min = 1,
                            step = 1,
                        )
                        th { addChild(waveInput.label!!) }
                        td { addChild(waveInput) }
                    }
                    tr {
                        val delayInput = IntInput(
                            enabled = ctrl.enabled,
                            value = event.delay,
                            onChange = { ctrl.setDelay(event, it) },
                            label = "Delay:",
                            min = 0,
                            step = 1,
                        )
                        th { addChild(delayInput.label!!) }
                        td { addChild(delayInput) }
                    }

                }
            }
            div {
                className = "pw-quest-editor-event-actions"

                table {
                    thead {
                        tr {
                            th {
                                colSpan = 3
                                textContent = "Actions:"
                            }
                        }
                    }
                    tbody {
                        bindChildWidgetsTo(event.actions) { action, _ ->
                            EventActionWidget(ctrl, event, action)
                        }
                    }
                    tfoot {
                        tr {
                            td {
                                colSpan = 3
                                addWidget(Dropdown(
                                    enabled = ctrl.enabled,
                                    text = "Add action",
                                    items = ctrl.eventActionTypes,
                                    onSelect = { ctrl.addAction(event, it) }
                                ))
                            }
                        }
                    }
                }
            }
        }

    companion object {
        init {
            @Suppress("CssUnusedSymbol", "CssUnresolvedCustomProperty")
            // language=css
            style(
                """
                .pw-quest-editor-event {
                    display: flex;
                    flex-wrap: wrap;
                    border: var(--pw-border);
                    margin: 2px;
                    background-color: hsl(0, 0%, 17%);
                    outline: none;
                }

                .pw-quest-editor-event:hover, .pw-quest-editor-event:focus {
                    border-color: hsl(0, 0%, 30%);
                    background-color: hsl(0, 0%, 20%);
                    color: hsl(0, 0%, 85%);
                }

                .pw-quest-editor-event.pw-selected {
                    border-color: hsl(0, 0%, 35%);
                    background-color: hsl(0, 0%, 25%);
                    color: hsl(0, 0%, 90%);
                }
                
                .pw-quest-editor-event-props, .pw-quest-editor-event-actions {
                    padding: 2px 6px;
                }
                
                .pw-quest-editor-event-props {
                    width: 115px;
                }
                
                .pw-quest-editor-event-actions {
                    width: 165px;
                }
                
                .pw-quest-editor-event > div > table {
                    width: 100%;
                    border-collapse: collapse;
                }

                .pw-quest-editor-event th {
                    text-align: left;
                }
                """.trimIndent()
            )
        }
    }
}

package world.phantasmal.web.questEditor.widgets

import org.w3c.dom.Node
import world.phantasmal.core.disposable.Disposable
import world.phantasmal.core.disposable.Disposer
import world.phantasmal.observable.value.falseVal
import world.phantasmal.observable.value.value
import world.phantasmal.web.questEditor.controllers.EventsController
import world.phantasmal.web.questEditor.models.QuestEventActionModel
import world.phantasmal.web.questEditor.models.QuestEventModel
import world.phantasmal.webui.dom.*
import world.phantasmal.webui.widgets.Button
import world.phantasmal.webui.widgets.IntInput
import world.phantasmal.webui.widgets.Widget

class EventWidget(
    private val ctrl: EventsController,
    private val event: QuestEventModel,
) : Widget() {
    override fun Node.createElement() =
        div {
            className = "pw-quest-editor-event"
            toggleClass("pw-selected", ctrl.isSelected(event))

            onclick = { e ->
                e.stopPropagation()
                ctrl.eventClicked(event)
            }

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
                    th { textContent = "Section:" }
                    td { textContent = event.sectionId.toString() }
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
                tr {
                    th {
                        colSpan = 2
                        textContent = "Actions:"
                    }
                }
                tr {
                    td {
                        colSpan = 2

                        table {
                            className = "pw-quest-editor-event-actions"

                            bindDisposableChildrenTo(event.actions) { action, _ ->
                                createActionElement(action)
                            }
                        }
                    }
                }
            }
        }

    private fun Node.createActionElement(action: QuestEventActionModel): Pair<Node, Disposable> {
        val disposer = Disposer()

        val node = tr {
            when (action) {
                is QuestEventActionModel.SpawnNpcs -> {
                    th { textContent = "Spawn:" }
                    td {
                        addWidget(
                            disposer.add(IntInput(
                                enabled = falseVal(),
                                tooltip = value("Section ID"),
                                value = action.sectionId,
                                min = 0,
                                step = 1,
                            )),
                            addToDisposer = false,
                        )
                        addWidget(
                            disposer.add(IntInput(
                                enabled = falseVal(),
                                tooltip = value("Appear flag"),
                                value = action.appearFlag,
                                min = 0,
                                step = 1,
                            )),
                            addToDisposer = false,
                        )
                    }
                }
                is QuestEventActionModel.Unlock -> {
                    th { textContent = "Unlock:" }
                    td {
                        addWidget(
                            disposer.add(IntInput(
                                enabled = falseVal(),
                                value = action.doorId,
                                min = 0,
                                step = 1,
                            )),
                            addToDisposer = false,
                        )
                    }
                }
                is QuestEventActionModel.Lock -> {
                    th { textContent = "Lock:" }
                    td {
                        addWidget(
                            disposer.add(IntInput(
                                enabled = falseVal(),
                                value = action.doorId,
                                min = 0,
                                step = 1,
                            )),
                            addToDisposer = false,
                        )
                    }
                }
                is QuestEventActionModel.TriggerEvent -> {
                    th { textContent = "Event:" }
                    td {
                        addWidget(
                            disposer.add(IntInput(
                                enabled = falseVal(),
                                value = action.eventId,
                                min = 0,
                                step = 1,
                            )),
                            addToDisposer = false,
                        )
                    }
                }
            }

            td {
                addWidget(
                    disposer.add(Button(
                        enabled = falseVal(),
                        tooltip = value("Remove this action"),
                        iconLeft = Icon.Remove,
                    )),
                    addToDisposer = false,
                )
            }
        }

        return Pair(node, disposer)
    }

    companion object {
        init {
            @Suppress("CssUnusedSymbol", "CssUnresolvedCustomProperty")
            // language=css
            style("""
                .pw-quest-editor-event {
                    display: flex;
                    flex-direction: column;
                    align-items: center;
                    padding: 4px 8px;
                    border: var(--pw-border);
                    margin: 4px;
                    background-color: hsl(0, 0%, 17%);
                    outline: none;
                }

                .pw-quest-editor-event:hover {
                    border-color: hsl(0, 0%, 30%);
                    background-color: hsl(0, 0%, 20%);
                    color: hsl(0, 0%, 85%);
                }

                .pw-quest-editor-event.pw-selected {
                    border-color: hsl(0, 0%, 35%);
                    background-color: hsl(0, 0%, 25%);
                    color: hsl(0, 0%, 90%);
                }

                .pw-quest-editor-event > table {
                    min-width: 170px;
                }

                .pw-quest-editor-event th {
                    text-align: left;
                }

                .pw-quest-editor-event-actions {
                    margin-left: 4px;
                }
            """.trimIndent())
        }
    }
}

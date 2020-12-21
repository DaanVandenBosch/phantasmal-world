package world.phantasmal.web.questEditor.widgets

import org.w3c.dom.*
import world.phantasmal.core.disposable.Disposable
import world.phantasmal.core.disposable.Disposer
import world.phantasmal.observable.value.value
import world.phantasmal.web.questEditor.controllers.EventsController
import world.phantasmal.web.questEditor.models.QuestEventActionModel
import world.phantasmal.web.questEditor.models.QuestEventModel
import world.phantasmal.webui.dom.*
import world.phantasmal.webui.obj
import world.phantasmal.webui.widgets.Button
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
                        bindDisposableChildrenTo(event.actions) { action, _ ->
                            createActionElement(action)
                        }
                    }
                    tfoot {
                        tr {
                            th {
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

    private fun Node.createActionElement(action: QuestEventActionModel): Pair<Node, Disposable> {
        val disposer = Disposer()

        val node = tr {
            th { textContent = "${action.shortName}:" }

            when (action) {
                is QuestEventActionModel.SpawnNpcs -> {
                    td {
                        addWidget(
                            disposer.add(IntInput(
                                enabled = ctrl.enabled,
                                tooltip = value("Section"),
                                value = action.sectionId,
                                onChange = { ctrl.setActionSectionId(event, action, it) },
                                min = 0,
                                step = 1,
                            )),
                            addToDisposer = false,
                        )
                        addWidget(
                            disposer.add(IntInput(
                                enabled = ctrl.enabled,
                                tooltip = value("Appear flag"),
                                value = action.appearFlag,
                                onChange = { ctrl.setActionAppearFlag(event, action, it) },
                                min = 0,
                                step = 1,
                            )),
                            addToDisposer = false,
                        )
                    }
                }
                is QuestEventActionModel.Door -> {
                    td {
                        addWidget(
                            disposer.add(IntInput(
                                enabled = ctrl.enabled,
                                tooltip = value("Door"),
                                value = action.doorId,
                                onChange = { ctrl.setActionDoorId(event, action, it) },
                                min = 0,
                                step = 1,
                            )),
                            addToDisposer = false,
                        )
                    }
                }
                is QuestEventActionModel.TriggerEvent -> {
                    td {
                        addWidget(
                            disposer.add(IntInput(
                                enabled = ctrl.enabled,
                                value = action.eventId,
                                onChange = { ctrl.setActionEventId(event, action, it) },
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
                        enabled = ctrl.enabled,
                        tooltip = value("Remove this action from the event"),
                        iconLeft = Icon.Remove,
                        onClick = { ctrl.removeAction(event, action) }
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
                    flex-wrap: wrap;
                    border: var(--pw-border);
                    margin: 4px;
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
                    padding: 3px 6px;
                }
                
                .pw-quest-editor-event-props {
                    width: 130px;
                }
                
                .pw-quest-editor-event-actions {
                    width: 160px;
                }
                
                .pw-quest-editor-event > div > table {
                    width: 100%;
                    border-collapse: collapse;
                }

                .pw-quest-editor-event th {
                    text-align: left;
                }
            """.trimIndent())
        }
    }
}

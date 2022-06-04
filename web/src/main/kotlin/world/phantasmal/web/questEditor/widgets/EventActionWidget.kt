package world.phantasmal.web.questEditor.widgets

import org.w3c.dom.Node
import world.phantasmal.cell.cell
import world.phantasmal.web.questEditor.controllers.EventsController
import world.phantasmal.web.questEditor.models.QuestEventActionModel
import world.phantasmal.web.questEditor.models.QuestEventModel
import world.phantasmal.webui.dom.Icon
import world.phantasmal.webui.dom.td
import world.phantasmal.webui.dom.th
import world.phantasmal.webui.dom.tr
import world.phantasmal.webui.widgets.Button
import world.phantasmal.webui.widgets.IntInput
import world.phantasmal.webui.widgets.Widget

class EventActionWidget(
    private val ctrl: EventsController,
    private val event: QuestEventModel,
    private val action: QuestEventActionModel,
) : Widget() {
    override fun Node.createElement() =
        tr {
            className = "pw-quest-editor-event-action"

            th { textContent = "${action.shortName}:" }

            when (action) {
                is QuestEventActionModel.SpawnNpcs -> {
                    td {
                        addChild(
                            IntInput(
                                enabled = ctrl.enabled,
                                tooltip = cell("Section"),
                                value = action.sectionId,
                                onChange = { ctrl.setActionSectionId(event, action, it) },
                                min = 0,
                                step = 1,
                            )
                        )
                        addChild(
                            IntInput(
                                enabled = ctrl.enabled,
                                tooltip = cell("Appear flag"),
                                value = action.appearFlag,
                                onChange = { ctrl.setActionAppearFlag(event, action, it) },
                                min = 0,
                                step = 1,
                            )
                        )
                    }
                }
                is QuestEventActionModel.Door -> {
                    td {
                        addChild(
                            IntInput(
                                enabled = ctrl.enabled,
                                tooltip = cell("Door"),
                                value = action.doorId,
                                onChange = { ctrl.setActionDoorId(event, action, it) },
                                min = 0,
                                step = 1,
                            )
                        )
                    }
                }
                is QuestEventActionModel.TriggerEvent -> {
                    td {
                        addChild(
                            IntInput(
                                enabled = ctrl.enabled,
                                value = action.eventId,
                                onChange = { ctrl.setActionEventId(event, action, it) },
                                min = 0,
                                step = 1,
                            )
                        )
                    }
                }
            }

            td {
                className = "pw-quest-editor-event-action-buttons"

                addChild(
                    Button(
                        enabled = ctrl.enabled,
                        tooltip = cell("Remove this action from the event"),
                        iconLeft = Icon.Remove,
                        onClick = { ctrl.removeAction(event, action) }
                    )
                )

                if (action is QuestEventActionModel.TriggerEvent) {
                    addChild(
                        Button(
                            enabled = ctrl.canGoToEvent(action.eventId),
                            tooltip = cell("Go to event"),
                            iconLeft = Icon.ArrowRight,
                            onClick = { e ->
                                e.stopPropagation()
                                ctrl.goToEvent(action.eventId.value)
                            }
                        )
                    )
                }
            }
        }

    companion object {
        init {
            @Suppress("CssUnusedSymbol", "CssUnresolvedCustomProperty")
            // language=css
            style(
                """
                .pw-quest-editor-event-action-buttons {
                    display: flex;
                    flex-direction: row;
                }
                """.trimIndent()
            )
        }
    }
}

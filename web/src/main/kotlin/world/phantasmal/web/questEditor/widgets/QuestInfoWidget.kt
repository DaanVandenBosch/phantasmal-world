package world.phantasmal.web.questEditor.widgets

import org.w3c.dom.Node
import world.phantasmal.web.core.widgets.UnavailableWidget
import world.phantasmal.web.questEditor.controllers.QuestInfoController
import world.phantasmal.webui.dom.*
import world.phantasmal.webui.widgets.IntInput
import world.phantasmal.webui.widgets.TextArea
import world.phantasmal.webui.widgets.TextInput
import world.phantasmal.webui.widgets.Widget

class QuestInfoWidget(private val ctrl: QuestInfoController) : Widget(enabled = ctrl.enabled) {
    override fun Node.createElement() =
        div {
            className = "pw-quest-editor-quest-info"
            tabIndex = -1

            addEventListener("focus", { ctrl.focused() }, true)

            table {
                hidden(ctrl.unavailable)

                tr {
                    th { textContent = "Episode:" }
                    td { text(ctrl.episode) }
                }
                tr {
                    val idInput = IntInput(
                        label = "ID:",
                        enabled = ctrl.enabled,
                        value = ctrl.id,
                        onChange = ctrl::setId,
                        min = 0,
                        step = 1,
                    )
                    th { addChild(idInput.label!!) }
                    td { addChild(idInput) }
                }
                tr {
                    val nameInput = TextInput(
                        label = "Name:",
                        enabled = ctrl.enabled,
                        value = ctrl.name,
                        onChange = ctrl::setName,
                        maxLength = 32,
                    )
                    th { addChild(nameInput.label!!) }
                    td { addChild(nameInput) }
                }
                val shortDescriptionTextArea = TextArea(
                    label = "Short description:",
                    enabled = ctrl.enabled,
                    value = ctrl.shortDescription,
                    onChange = ctrl::setShortDescription,
                    maxLength = 128,
                    fontFamily = "\"Courier New\", monospace",
                    cols = 25,
                    rows = 5,
                )
                tr {
                    th {
                        colSpan = 2
                        addChild(shortDescriptionTextArea.label!!)
                    }
                }
                tr {
                    td {
                        colSpan = 2
                        addChild(shortDescriptionTextArea)
                    }
                }
                val longDescriptionTextArea = TextArea(
                    label = "Long description:",
                    enabled = ctrl.enabled,
                    value = ctrl.longDescription,
                    onChange = ctrl::setLongDescription,
                    maxLength = 288,
                    fontFamily = "\"Courier New\", monospace",
                    cols = 25,
                    rows = 10,
                )
                tr {
                    th {
                        colSpan = 2
                        addChild(longDescriptionTextArea.label!!)
                    }
                }
                tr {
                    td {
                        colSpan = 2
                        addChild(longDescriptionTextArea)
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
                .pw-quest-editor-quest-info {
                    box-sizing: border-box;
                    padding: 3px;
                    overflow: auto;
                    outline: none;
                }
                
                .pw-quest-editor-quest-info table {
                    width: 100%;
                }
                
                .pw-quest-editor-quest-info th {
                    text-align: left;
                }
                
                .pw-quest-editor-quest-info .pw-text-input {
                    width: 100%;
                }
                
                .pw-quest-editor-quest-info .pw-text-area {
                    width: 100%;
                }
                
                .pw-quest-editor-quest-info textarea {
                    width: 100%;
                }
            """.trimIndent())
        }
    }
}

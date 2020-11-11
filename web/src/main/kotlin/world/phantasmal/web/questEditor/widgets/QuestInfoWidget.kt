package world.phantasmal.web.questEditor.widgets

import kotlinx.coroutines.CoroutineScope
import org.w3c.dom.Node
import world.phantasmal.web.core.widgets.UnavailableWidget
import world.phantasmal.web.questEditor.controllers.QuestInfoController
import world.phantasmal.webui.dom.*
import world.phantasmal.webui.widgets.IntInput
import world.phantasmal.webui.widgets.TextArea
import world.phantasmal.webui.widgets.TextInput
import world.phantasmal.webui.widgets.Widget

class QuestInfoWidget(
    scope: CoroutineScope,
    private val ctrl: QuestInfoController,
) : Widget(scope, enabled = ctrl.enabled) {
    override fun Node.createElement() =
        div {
            className = "pw-quest-editor-quest-info"
            tabIndex = -1

            table {
                hidden(ctrl.unavailable)

                tr {
                    th { textContent = "Episode:" }
                    td { text(ctrl.episode) }
                }
                tr {
                    th { textContent = "ID:" }
                    td {
                        addChild(IntInput(
                            this@QuestInfoWidget.scope,
                            enabled = ctrl.enabled,
                            valueVal = ctrl.id,
                            min = 0,
                            step = 1,
                        ))
                    }
                }
                tr {
                    th { textContent = "Name:" }
                    td {
                        addChild(TextInput(
                            this@QuestInfoWidget.scope,
                            enabled = ctrl.enabled,
                            valueVal = ctrl.name,
                            maxLength = 32,
                        ))
                    }
                }
                tr {
                    th {
                        colSpan = 2
                        textContent = "Short description:"
                    }
                }
                tr {
                    td {
                        colSpan = 2
                        addChild(TextArea(
                            this@QuestInfoWidget.scope,
                            enabled = ctrl.enabled,
                            valueVal = ctrl.shortDescription,
                            maxLength = 128,
                            fontFamily = "\"Courier New\", monospace",
                            cols = 25,
                            rows = 5,
                        ))
                    }
                }
                tr {
                    th {
                        colSpan = 2
                        textContent = "Long description:"
                    }
                }
                tr {
                    td {
                        colSpan = 2
                        addChild(TextArea(
                            this@QuestInfoWidget.scope,
                            enabled = ctrl.enabled,
                            valueVal = ctrl.longDescription,
                            maxLength = 288,
                            fontFamily = "\"Courier New\", monospace",
                            cols = 25,
                            rows = 10,
                        ))
                    }
                }
            }
            addChild(UnavailableWidget(
                scope,
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

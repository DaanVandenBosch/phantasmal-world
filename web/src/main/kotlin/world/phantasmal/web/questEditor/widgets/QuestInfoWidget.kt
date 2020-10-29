package world.phantasmal.web.questEditor.widgets

import kotlinx.coroutines.CoroutineScope
import org.w3c.dom.Node
import world.phantasmal.observable.value.not
import world.phantasmal.web.core.widgets.UnavailableWidget
import world.phantasmal.web.questEditor.controllers.QuestInfoController
import world.phantasmal.webui.dom.*
import world.phantasmal.webui.widgets.IntInput
import world.phantasmal.webui.widgets.TextInput
import world.phantasmal.webui.widgets.Widget

class QuestInfoWidget(
    scope: CoroutineScope,
    private val ctrl: QuestInfoController,
) : Widget(scope) {
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
                            valueVal = ctrl.id,
                            min = 0,
                            step = 1
                        ))
                    }
                }
                tr {
                    th { textContent = "Name:" }
                    td {
                        addChild(TextInput(
                            this@QuestInfoWidget.scope,
                            valueVal = ctrl.name,
                            maxLength = 32
                        ))
                    }
                }
            }
            addChild(UnavailableWidget(
                scope,
                hidden = !ctrl.unavailable,
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

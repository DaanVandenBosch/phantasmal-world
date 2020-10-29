package world.phantasmal.web.application.widgets

import kotlinx.coroutines.CoroutineScope
import org.w3c.dom.Node
import world.phantasmal.observable.Observable
import world.phantasmal.web.core.stores.PwTool
import world.phantasmal.webui.dom.input
import world.phantasmal.webui.dom.label
import world.phantasmal.webui.dom.span
import world.phantasmal.webui.widgets.Control

class PwToolButton(
    scope: CoroutineScope,
    private val tool: PwTool,
    private val toggled: Observable<Boolean>,
    private val mouseDown: () -> Unit,
) : Control(scope) {
    private val inputId = "pw-application-pw-tool-button-${tool.name.toLowerCase()}"

    override fun Node.createElement() =
        span {
            className = "pw-application-pw-tool-button"

            input {
                type = "radio"
                id = inputId
                name = "pw-application-pw-tool-button"
                observe(toggled) { checked = it }
            }
            label {
                htmlFor = inputId
                textContent = tool.uiName
                onmousedown = { mouseDown() }
            }
        }

    companion object {
        init {
            @Suppress("CssUnresolvedCustomProperty")
            // language=css
            style("""
                .pw-application-pw-tool-button input {
                    display: none;
                }
                
                .pw-application-pw-tool-button label {
                    box-sizing: border-box;
                    display: inline-flex;
                    flex-direction: row;
                    align-items: center;
                    font-size: 13px;
                    height: 100%;
                    padding: 0 20px;
                    color: hsl(0, 0%, 65%);
                }
                
                .pw-application-pw-tool-button label:hover {
                    color: hsl(0, 0%, 85%);
                    background-color: hsl(0, 0%, 12%);
                }
                
                .pw-application-pw-tool-button input:checked + label {
                    color: hsl(0, 0%, 85%);
                    background-color: var(--pw-bg-color);
                }
            """.trimIndent())
        }
    }
}

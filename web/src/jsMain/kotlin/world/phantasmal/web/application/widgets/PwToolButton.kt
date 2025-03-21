package world.phantasmal.web.application.widgets

import org.w3c.dom.Node
import world.phantasmal.cell.Cell
import world.phantasmal.cell.nullCell
import world.phantasmal.cell.trueCell
import world.phantasmal.web.core.PwToolType
import world.phantasmal.webui.dom.input
import world.phantasmal.webui.dom.label
import world.phantasmal.webui.dom.span
import world.phantasmal.webui.widgets.Control

class PwToolButton(
    private val tool: PwToolType,
    private val toggled: Cell<Boolean>,
    private val onMouseDown: () -> Unit,
) : Control(visible = trueCell(), enabled = trueCell(), tooltip = nullCell()) {
    private val inputId = "pw-application-pw-tool-button-${tool.name.lowercase()}"

    override fun Node.createElement() =
        span {
            className = "pw-application-pw-tool-button"

            input {
                type = "radio"
                id = inputId
                name = "pw-application-pw-tool-button"
                observeNow(toggled) { checked = it }
            }
            label {
                htmlFor = inputId
                textContent = tool.uiName
                onmousedown = { onMouseDown() }
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

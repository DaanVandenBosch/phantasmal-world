package world.phantasmal.webui.widgets

import kotlinx.coroutines.CoroutineScope
import org.w3c.dom.Node
import org.w3c.dom.events.KeyboardEvent
import org.w3c.dom.events.MouseEvent
import world.phantasmal.observable.value.Val
import world.phantasmal.observable.value.falseVal
import world.phantasmal.webui.dom.button
import world.phantasmal.webui.dom.span

open class Button(
    scope: CoroutineScope,
    hidden: Val<Boolean> = falseVal(),
    disabled: Val<Boolean> = falseVal(),
    private val text: String? = null,
    private val textVal: Val<String>? = null,
    private val onMouseDown: ((MouseEvent) -> Unit)? = null,
    private val onMouseUp: ((MouseEvent) -> Unit)? = null,
    private val onClick: ((MouseEvent) -> Unit)? = null,
    private val onKeyDown: ((KeyboardEvent) -> Unit)? = null,
    private val onKeyUp: ((KeyboardEvent) -> Unit)? = null,
    private val onKeyPress: ((KeyboardEvent) -> Unit)? = null,
) : Control(scope, hidden, disabled) {
    override fun Node.createElement() =
        button {
            className = "pw-button"
            onmousedown = onMouseDown
            onmouseup = onMouseUp
            onclick = onClick
            onkeydown = onKeyDown
            onkeyup = onKeyUp
            onkeypress = onKeyPress

            span {
                className = "pw-button-inner"

                span {
                    className = "pw-button-center"

                    if (textVal != null) {
                        observe(textVal) {
                            textContent = it
                            hidden = it.isEmpty()
                        }
                    } else if (!text.isNullOrEmpty()) {
                        textContent = text
                    } else {
                        hidden = true
                    }
                }
            }
        }

    companion object {
        init {
            @Suppress("CssUnusedSymbol", "CssUnresolvedCustomProperty")
// language=css
            style("""
                .pw-button {
                    display: inline-flex;
                    flex-direction: row;
                    align-items: stretch;
                    align-content: stretch;
                    box-sizing: border-box;
                    height: 26px;
                    padding: 0;
                    border: var(--pw-control-border);
                    color: var(--pw-control-text-color);
                    outline: none;
                    font-size: 13px;
                    font-family: var(--pw-font-family), sans-serif;
                    overflow: hidden;
                }

                .pw-button .pw-button-inner {
                    flex-grow: 1;
                    display: inline-flex;
                    flex-direction: row;
                    align-items: center;
                    box-sizing: border-box;
                    background-color: var(--pw-control-bg-color);
                    height: 24px;
                    padding: 3px 5px;
                    border: var(--pw-control-inner-border);
                    overflow: hidden;
                }

                .pw-button:hover .pw-button-inner {
                    background-color: var(--pw-control-bg-color-hover);
                    border-color: hsl(0, 0%, 40%);
                    color: var(--pw-control-text-color-hover);
                }

                .pw-button:active .pw-button-inner {
                    background-color: hsl(0, 0%, 20%);
                    border-color: hsl(0, 0%, 30%);
                    color: hsl(0, 0%, 75%);
                }

                .pw-button:focus-within .pw-button-inner {
                    border: var(--pw-control-inner-border-focus);
                }

                .pw-button:disabled .pw-button-inner {
                    background-color: hsl(0, 0%, 15%);
                    border-color: hsl(0, 0%, 25%);
                    color: hsl(0, 0%, 55%);
                }

                .pw-button-inner > * {
                    display: inline-block;
                    margin: 0 3px;
                }

                .pw-button-center {
                    flex-grow: 1;
                    text-align: left;
                    overflow: hidden;
                    text-overflow: ellipsis;
                    white-space: nowrap;
                }

                .pw-button-left,
                .pw-button-right {
                    display: inline-flex;
                    align-content: center;
                    font-size: 11px;
                }
            """.trimIndent())
        }
    }
}

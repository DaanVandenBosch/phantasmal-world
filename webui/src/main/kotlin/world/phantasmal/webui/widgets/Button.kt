package world.phantasmal.webui.widgets

import org.w3c.dom.Node
import org.w3c.dom.events.MouseEvent
import world.phantasmal.core.disposable.Scope
import world.phantasmal.observable.value.Val
import world.phantasmal.observable.value.falseVal
import world.phantasmal.webui.dom.button
import world.phantasmal.webui.dom.span

open class Button(
    scope: Scope,
    hidden: Val<Boolean> = falseVal(),
    disabled: Val<Boolean> = falseVal(),
    private val text: String? = null,
    private val textVal: Val<String>? = null,
    private val onclick: ((MouseEvent) -> Unit)? = null,
) : Control(scope, ::style, hidden, disabled) {
    override fun Node.createElement() =
        button(className = "pw-button") {
            onclick = this@Button.onclick

            span(className = "pw-button-inner") {
                span(className = "pw-button-center") {
                    if (textVal != null) {
                        textVal.observe {
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
}

@Suppress("CssUnusedSymbol", "CssUnresolvedCustomProperty")
// language=css
private fun style() = """
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
"""

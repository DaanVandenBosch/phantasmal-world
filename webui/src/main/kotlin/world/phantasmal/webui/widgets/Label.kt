package world.phantasmal.webui.widgets

import org.w3c.dom.Node
import world.phantasmal.core.disposable.Scope
import world.phantasmal.observable.value.Val
import world.phantasmal.observable.value.falseVal
import world.phantasmal.webui.dom.label

class Label(
    scope: Scope,
    hidden: Val<Boolean> = falseVal(),
    disabled: Val<Boolean> = falseVal(),
    private val text: String? = null,
    private val textVal: Val<String>? = null,
    private val htmlFor: String?,
) : Widget(scope, ::style, hidden, disabled) {
    override fun Node.createElement() =
        label(htmlFor) {
            if (textVal != null) {
                textVal.observe { textContent = it }
            } else if (text != null) {
                textContent = text
            }
        }
}

@Suppress("CssUnusedSymbol", "CssUnresolvedCustomProperty")
// language=css
private fun style() = """
.pw-label.disabled {
    color: var(--pw-text-color-disabled);
}
"""

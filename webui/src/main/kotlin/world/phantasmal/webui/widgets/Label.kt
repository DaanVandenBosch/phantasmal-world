package world.phantasmal.webui.widgets

import kotlinx.coroutines.CoroutineScope
import org.w3c.dom.Node
import world.phantasmal.observable.value.Val
import world.phantasmal.observable.value.falseVal
import world.phantasmal.webui.dom.label

class Label(
    scope: CoroutineScope,
    hidden: Val<Boolean> = falseVal(),
    disabled: Val<Boolean> = falseVal(),
    private val text: String? = null,
    private val textVal: Val<String>? = null,
    private val htmlFor: String?,
) : Widget(scope, listOf(::style), hidden, disabled) {
    override fun Node.createElement() =
        label(htmlFor) {
            if (textVal != null) {
                observe(textVal) { textContent = it }
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

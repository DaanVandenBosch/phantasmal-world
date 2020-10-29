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
    private val htmlFor: String? = null,
) : Widget(scope, hidden, disabled) {
    override fun Node.createElement() =
        label {
            className = "pw-label"
            this@Label.htmlFor?.let { htmlFor = it }

            if (textVal != null) {
                text(textVal)
            } else if (text != null) {
                textContent = text
            }
        }

    companion object{
        init {
            @Suppress("CssUnusedSymbol", "CssUnresolvedCustomProperty")
            // language=css
            style("""
                .pw-label.pw-disabled {
                    color: var(--pw-text-color-disabled);
                }
            """.trimIndent())
        }
    }
}

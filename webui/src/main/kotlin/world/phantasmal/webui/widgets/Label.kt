package world.phantasmal.webui.widgets

import org.w3c.dom.Node
import world.phantasmal.observable.value.Val
import world.phantasmal.observable.value.trueVal
import world.phantasmal.webui.dom.label

class Label(
    visible: Val<Boolean> = trueVal(),
    enabled: Val<Boolean> = trueVal(),
    private val text: String? = null,
    private val textVal: Val<String>? = null,
    private val htmlFor: String? = null,
) : Widget(visible, enabled) {
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

    companion object {
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

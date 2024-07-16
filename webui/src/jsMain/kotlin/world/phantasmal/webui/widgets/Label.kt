package world.phantasmal.webui.widgets

import org.w3c.dom.Node
import world.phantasmal.cell.Cell
import world.phantasmal.cell.trueCell
import world.phantasmal.webui.dom.label

class Label(
    visible: Cell<Boolean> = trueCell(),
    enabled: Cell<Boolean> = trueCell(),
    private val text: String? = null,
    private val textCell: Cell<String>? = null,
    private val htmlFor: String? = null,
) : Widget(visible, enabled) {
    override fun Node.createElement() =
        label {
            className = "pw-label"
            this@Label.htmlFor?.let { htmlFor = it }

            if (textCell != null) {
                text(textCell)
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

package world.phantasmal.web.core.widgets

import org.w3c.dom.Node
import world.phantasmal.cell.Cell
import world.phantasmal.cell.falseCell
import world.phantasmal.cell.trueCell
import world.phantasmal.webui.dom.div
import world.phantasmal.webui.widgets.Label
import world.phantasmal.webui.widgets.Widget

class UnavailableWidget(
    visible: Cell<Boolean> = trueCell(),
    private val message: String,
) : Widget(visible) {
    override fun Node.createElement() =
        div {
            className = "pw-core-unavailable"

            addWidget(Label(enabled = falseCell(), text = message))
        }

    companion object {
        init {
            @Suppress("CssUnusedSymbol")
            // language=css
            style("""
                .pw-core-unavailable {
                    box-sizing: border-box;
                    display: grid;
                    grid-template: 100% / 100%;
                    place-items: center;
                    overflow: auto;
                    width: 100%;
                    height: 100%;
                    padding: 10%;
                    text-align: center;
                }
            """.trimIndent())
        }
    }
}

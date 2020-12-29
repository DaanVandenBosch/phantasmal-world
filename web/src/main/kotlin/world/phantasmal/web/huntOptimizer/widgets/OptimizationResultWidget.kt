package world.phantasmal.web.huntOptimizer.widgets

import org.w3c.dom.Node
import world.phantasmal.webui.dom.div
import world.phantasmal.webui.dom.h2
import world.phantasmal.webui.widgets.Widget

class OptimizationResultWidget : Widget() {
    override fun Node.createElement() =
        div {
            className = "pw-hunt-optimizer-optimization-result"

            h2 { textContent = "Ideal Combination of Methods" }
        }

    companion object {
        init {
            @Suppress("CssUnusedSymbol")
            // language=css
            style("""
                .pw-hunt-optimizer-optimization-result {
                    flex-grow: 1;
                }
            """.trimIndent())
        }
    }
}

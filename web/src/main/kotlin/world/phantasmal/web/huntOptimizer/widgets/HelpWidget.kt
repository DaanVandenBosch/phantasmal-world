package world.phantasmal.web.huntOptimizer.widgets

import kotlinx.coroutines.CoroutineScope
import org.w3c.dom.Node
import world.phantasmal.webui.dom.div
import world.phantasmal.webui.dom.p
import world.phantasmal.webui.widgets.Widget

class HelpWidget(scope: CoroutineScope) : Widget(scope) {
    override fun Node.createElement() =
        div {
            className = "pw-hunt-optimizer-help"

            p {
                textContent =
                    "Add some items with the combo box on the left to see the optimal combination of hunt methods on the right."
            }
            p {
                textContent =
                    "At the moment a hunt method is simply a quest run-through. Partial quest run-throughs are coming. View the list of methods on the \"Methods\" tab. Each method takes a certain amount of time, which affects the optimization result. Make sure the times are correct for you."
            }
            p {
                textContent = "Only enemy drops are considered. Box drops are coming."
            }
            p {
                textContent =
                    "The optimal result is calculated using linear optimization. The optimizer takes into account rare enemies and the fact that pan arms can be split in two."
            }
        }

    companion object {
        init {
            @Suppress("CssUnusedSymbol")
            // language=css
            style("""
                .pw-hunt-optimizer-help {
                    cursor: initial;
                    user-select: text;
                }
                
                .pw-hunt-optimizer-help p {
                    margin: 1em;
                    max-width: 600px;
                }
            """.trimIndent())
        }
    }
}

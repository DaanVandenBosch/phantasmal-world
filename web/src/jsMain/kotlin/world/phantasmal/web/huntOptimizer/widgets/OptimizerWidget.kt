package world.phantasmal.web.huntOptimizer.widgets

import org.w3c.dom.Node
import world.phantasmal.webui.dom.div
import world.phantasmal.webui.widgets.Widget

class OptimizerWidget(
    private val createWantedItemsWidget: () -> WantedItemsWidget,
    private val createOptimizationResultWidget: () -> OptimizationResultWidget,
) : Widget() {
    override fun Node.createElement() =
        div {
            className = "pw-hunt-optimizer-optimizer"

            addChild(createWantedItemsWidget())
            addChild(createOptimizationResultWidget())
        }

    companion object {
        init {
            @Suppress("CssUnusedSymbol")
            // language=css
            style("""
                .pw-hunt-optimizer-optimizer {
                    display: flex;
                    align-items: stretch;
                    overflow: hidden;
                }
            """.trimIndent())
        }
    }
}

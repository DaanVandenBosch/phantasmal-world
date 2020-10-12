package world.phantasmal.web.huntOptimizer.widgets

import org.w3c.dom.Node
import world.phantasmal.webui.widgets.TabContainer
import world.phantasmal.web.huntOptimizer.HuntOptimizerUrls
import world.phantasmal.web.huntOptimizer.controllers.HuntOptimizerController
import world.phantasmal.webui.dom.div
import world.phantasmal.webui.widgets.Widget

class HuntOptimizerWidget(
    private val ctrl: HuntOptimizerController,
    private val createMethodsWidget: () -> MethodsWidget,
) : Widget(::style) {
    override fun Node.createElement() = div(className = "pw-hunt-optimizer-hunt-optimizer") {
        addChild(TabContainer(
            ctrl = ctrl,
            createWidget = { tab ->
                when (tab.path) {
                    HuntOptimizerUrls.optimize -> object : Widget() {
                        override fun Node.createElement() = div {
                            textContent = "TODO"
                        }
                    }
                    HuntOptimizerUrls.methods -> createMethodsWidget()
                    HuntOptimizerUrls.help -> HelpWidget()
                    else -> error("""Unknown tab "${tab.title}".""")
                }
            }
        ))
    }
}

@Suppress("CssUnusedSymbol")
// language=css
private fun style() = """
.pw-hunt-optimizer-hunt-optimizer {
    display: flex;
    flex-direction: column;
}

.pw-hunt-optimizer-hunt-optimizer > * {
    flex-grow: 1;
    overflow: hidden;
}
"""

package world.phantasmal.web.huntoptimizer.widgets

import org.w3c.dom.Node
import world.phantasmal.web.core.widgets.TabContainer
import world.phantasmal.web.huntoptimizer.HuntOptimizerUrls
import world.phantasmal.web.huntoptimizer.controllers.HuntOptimizerController
import world.phantasmal.webui.dom.div
import world.phantasmal.webui.widgets.Widget

class HuntOptimizerWidget(
    private val ctrl: HuntOptimizerController,
    private val createMethodsWidget: () -> MethodsWidget,
) : Widget() {
    override fun Node.createElement() = div(className = "pw-huntoptimizer-hunt-optimizer") {
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

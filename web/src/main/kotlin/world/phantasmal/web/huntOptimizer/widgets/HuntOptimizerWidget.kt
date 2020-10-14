package world.phantasmal.web.huntOptimizer.widgets

import org.w3c.dom.Node
import world.phantasmal.core.disposable.Scope
import world.phantasmal.web.huntOptimizer.HuntOptimizerUrls
import world.phantasmal.web.huntOptimizer.controllers.HuntOptimizerController
import world.phantasmal.webui.dom.div
import world.phantasmal.webui.widgets.TabContainer
import world.phantasmal.webui.widgets.Widget

class HuntOptimizerWidget(
    scope: Scope,
    private val ctrl: HuntOptimizerController,
    private val createMethodsWidget: (Scope) -> MethodsWidget,
) : Widget(scope, ::style) {
    override fun Node.createElement() = div(className = "pw-hunt-optimizer-hunt-optimizer") {
        addChild(TabContainer(
            scope,
            ctrl = ctrl,
            createWidget = { scope, tab ->
                when (tab.path) {
                    HuntOptimizerUrls.optimize -> object : Widget(scope) {
                        override fun Node.createElement() = div {
                            textContent = "TODO"
                        }
                    }
                    HuntOptimizerUrls.methods -> createMethodsWidget(scope)
                    HuntOptimizerUrls.help -> HelpWidget(scope)
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

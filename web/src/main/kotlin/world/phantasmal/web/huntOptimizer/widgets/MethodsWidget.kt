package world.phantasmal.web.huntOptimizer.widgets

import kotlinx.coroutines.CoroutineScope
import org.w3c.dom.Node
import world.phantasmal.web.huntOptimizer.controllers.MethodsController
import world.phantasmal.webui.dom.div
import world.phantasmal.webui.widgets.TabContainer
import world.phantasmal.webui.widgets.Widget

class MethodsWidget(
    scope: CoroutineScope,
    private val ctrl: MethodsController,
) : Widget(scope, listOf(::style)) {
    override fun Node.createElement() =
        div(className = "pw-hunt-optimizer-methods") {
            addChild(TabContainer(scope, ctrl = ctrl, createWidget = { scope, tab ->
                MethodsForEpisodeWidget(scope, ctrl, tab.episode)
            }))
        }
}

@Suppress("CssUnusedSymbol")
// language=css
private fun style() = """
.pw-hunt-optimizer-methods {
    display: flex;
    flex-direction: column;
}

.pw-hunt-optimizer-methods > * {
    flex-grow: 1;
    overflow: hidden;
}
"""

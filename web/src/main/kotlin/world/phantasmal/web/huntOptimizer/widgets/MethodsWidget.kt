package world.phantasmal.web.huntOptimizer.widgets

import org.w3c.dom.Node
import world.phantasmal.webui.widgets.TabContainer
import world.phantasmal.web.huntOptimizer.controllers.MethodsController
import world.phantasmal.webui.dom.div
import world.phantasmal.webui.widgets.Widget

class MethodsWidget(private val ctrl: MethodsController) : Widget(::style) {
    override fun Node.createElement() = div(className = "pw-hunt-optimizer-methods") {
        addChild(TabContainer(ctrl = ctrl, createWidget = { tab ->
            MethodsForEpisodeWidget(ctrl, tab.episode)
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

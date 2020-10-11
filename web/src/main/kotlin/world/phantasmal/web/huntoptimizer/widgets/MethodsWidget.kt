package world.phantasmal.web.huntoptimizer.widgets

import org.w3c.dom.Node
import world.phantasmal.webui.widgets.TabContainer
import world.phantasmal.web.huntoptimizer.controllers.MethodsController
import world.phantasmal.webui.dom.div
import world.phantasmal.webui.widgets.Widget

class MethodsWidget(private val ctrl: MethodsController) : Widget() {
    override fun Node.createElement() = div(className = "pw-huntoptimizer-methods") {
        addChild(TabContainer(ctrl = ctrl, createWidget = { tab ->
            MethodsForEpisodeWidget(ctrl, tab.episode)
        }))
    }
}

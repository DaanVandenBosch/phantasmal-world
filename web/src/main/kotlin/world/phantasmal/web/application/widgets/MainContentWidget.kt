package world.phantasmal.web.application.widgets

import org.w3c.dom.Node
import world.phantasmal.observable.value.not
import world.phantasmal.web.application.controllers.MainContentController
import world.phantasmal.web.core.stores.PwTool
import world.phantasmal.webui.widgets.LazyLoader
import world.phantasmal.webui.dom.div
import world.phantasmal.webui.widgets.Widget

class MainContentWidget(
    private val ctrl: MainContentController,
    private val toolViews: Map<PwTool, () -> Widget>,
) : Widget(::style) {
    override fun Node.createElement() = div(className = "pw-application-main-content") {
        ctrl.tools.forEach { (tool, active) ->
            toolViews[tool]?.let { createWidget ->
                addChild(LazyLoader(hidden = !active, createWidget))
            }
        }
    }
}

@Suppress("CssUnusedSymbol")
// language=css
private fun style() = """
.pw-application-main-content {
    display: flex;
    flex-direction: column;
}

.pw-application-main-content > * {
    flex: 1;
}
"""

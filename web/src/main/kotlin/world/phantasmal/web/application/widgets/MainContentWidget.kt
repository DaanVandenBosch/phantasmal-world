package world.phantasmal.web.application.widgets

import org.w3c.dom.Node
import world.phantasmal.core.disposable.Scope
import world.phantasmal.observable.value.not
import world.phantasmal.web.application.controllers.MainContentController
import world.phantasmal.web.core.stores.PwTool
import world.phantasmal.webui.dom.div
import world.phantasmal.webui.widgets.LazyLoader
import world.phantasmal.webui.widgets.Widget

class MainContentWidget(
    scope: Scope,
    private val ctrl: MainContentController,
    private val toolViews: Map<PwTool, (Scope) -> Widget>,
) : Widget(scope, ::style) {
    override fun Node.createElement() = div(className = "pw-application-main-content") {
        ctrl.tools.forEach { (tool, active) ->
            toolViews[tool]?.let { createWidget ->
                addChild(LazyLoader(scope, hidden = !active, createWidget))
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
    flex-grow: 1;
    overflow: hidden;
}
"""

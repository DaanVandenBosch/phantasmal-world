package world.phantasmal.web.application.widgets

import org.w3c.dom.Node
import world.phantasmal.web.application.controllers.NavigationController
import world.phantasmal.webui.dom.div
import world.phantasmal.webui.widgets.Widget

class NavigationWidget(private val ctrl: NavigationController) : Widget(::style) {
    override fun Node.createElement() =
        div(className = "pw-application-navigation") {
            ctrl.tools.forEach { (tool, active) ->
                addChild(PwToolButton(tool, active) { ctrl.setCurrentTool(tool) })
            }
        }
}

@Suppress("CssUnusedSymbol", "CssUnresolvedCustomProperty")
// language=css
private fun style() = """
.pw-application-navigation {
    box-sizing: border-box;
    display: flex;
    flex-direction: row;
    align-items: stretch;
    background-color: hsl(0, 0%, 10%);
    border-bottom: solid 2px var(--pw-bg-color);
}

.pw-application-navigation-spacer {
    flex: 1;
}

.pw-application-navigation-server {
    display: flex;
    align-items: center;
}

.pw-application-navigation-server > * {
    margin: 0 2px;
}

.pw-application-navigation-time {
    display: flex;
    align-items: center;
}

.pw-application-navigation-github {
    display: flex;
    flex-direction: row;
    align-items: center;
    justify-content: center;
    width: 30px;
    font-size: 16px;
    color: var(--pw-control-text-color);
}

.pw-application-navigation-github:hover {
    color: var(--pw-control-text-color-hover);
}
"""

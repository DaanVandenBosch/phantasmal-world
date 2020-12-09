package world.phantasmal.web.application.widgets

import org.w3c.dom.Node
import world.phantasmal.observable.value.falseVal
import world.phantasmal.observable.value.list.listVal
import world.phantasmal.observable.value.value
import world.phantasmal.web.application.controllers.NavigationController
import world.phantasmal.web.core.dom.externalLink
import world.phantasmal.webui.dom.Icon
import world.phantasmal.webui.dom.div
import world.phantasmal.webui.dom.icon
import world.phantasmal.webui.dom.span
import world.phantasmal.webui.widgets.Select
import world.phantasmal.webui.widgets.Widget

class NavigationWidget(private val ctrl: NavigationController) : Widget() {
    override fun Node.createElement() =
        div {
            className = "pw-application-navigation"

            ctrl.tools.forEach { (tool, active) ->
                addChild(PwToolButton(tool, active) { ctrl.setCurrentTool(tool) })
            }

            div {
                className = "pw-application-navigation-spacer"
            }
            div {
                className = "pw-application-navigation-right"

                val serverSelect = Select(
                    enabled = falseVal(),
                    label = "Server:",
                    items = listVal("Ephinea"),
                    selected = value("Ephinea"),
                    tooltip = value("Only Ephinea is supported at the moment"),
                )
                addChild(serverSelect.label!!)
                addChild(serverSelect)

                span {
                    title = "Internet time in beats"
                    text(ctrl.internetTime)
                }

                externalLink("https://github.com/DaanVandenBosch/phantasmal-world") {
                    className = "pw-application-navigation-github"
                    title = "Phantasmal World is open source, code available on GitHub"

                    icon(Icon.GitHub)
                }
            }
        }

    companion object {
        init {
            @Suppress("CssUnusedSymbol", "CssUnresolvedCustomProperty")
            // language=css
            style("""
                .pw-application-navigation {
                    box-sizing: border-box;
                    display: flex;
                    flex-direction: row;
                    align-items: stretch;
                    background-color: hsl(0, 0%, 10%);
                    border-bottom: solid 2px var(--pw-bg-color);
                }
                
                .pw-application-navigation-spacer {
                    flex-grow: 1;
                }
                
                .pw-application-navigation-right {
                    display: flex;
                    align-items: center;
                }
                
                .pw-application-navigation-right > * {
                    margin: 1px 2px;
                }
                
                .pw-application-navigation-github {
                    margin: 0 6px 0 4px;
                    font-size: 16px;
                    color: var(--pw-control-text-color);
                }
                
                .pw-application-navigation-github:hover {
                    color: var(--pw-control-text-color-hover);
                }
            """.trimIndent())
        }
    }
}

package world.phantasmal.web.huntOptimizer.widgets

import org.w3c.dom.Node
import world.phantasmal.web.huntOptimizer.controllers.MethodsController
import world.phantasmal.webui.dom.div
import world.phantasmal.webui.widgets.TabContainer
import world.phantasmal.webui.widgets.Widget

class MethodsWidget(
    private val ctrl: MethodsController,
) : Widget() {
    override fun Node.createElement() =
        div {
            className = "pw-hunt-optimizer-methods"

            addChild(TabContainer(ctrl = ctrl, createWidget = { tab ->
                MethodsForEpisodeWidget(ctrl, tab.episode)
            }))
        }

    companion object {
        init {
            @Suppress("CssUnusedSymbol")
            // language=css
            style("""
                .pw-hunt-optimizer-methods {
                    display: flex;
                    flex-direction: column;
                }
                
                .pw-hunt-optimizer-methods > * {
                    flex-grow: 1;
                    overflow: hidden;
                }
            """.trimIndent())
        }
    }
}

package world.phantasmal.web.huntOptimizer.widgets

import org.w3c.dom.Node
import world.phantasmal.lib.Episode
import world.phantasmal.web.huntOptimizer.controllers.MethodsController
import world.phantasmal.webui.dom.div
import world.phantasmal.webui.widgets.TabContainer
import world.phantasmal.webui.widgets.Widget

class MethodsWidget(
    private val ctrl: MethodsController,
    private val createMethodsForEpisodeWidget: (Episode) -> MethodsForEpisodeWidget,
) : Widget() {
    override fun Node.createElement() =
        div {
            className = "pw-hunt-optimizer-methods"

            addChild(TabContainer(ctrl = ctrl, createWidget = { tab ->
                createMethodsForEpisodeWidget(tab.episode)
            }))
        }

    companion object {
        init {
            @Suppress("CssUnusedSymbol")
            // language=css
            style("""
                .pw-hunt-optimizer-methods {
                    display: grid;
                    grid-template-rows: 100%;
                    grid-template-columns: 100%;
                    overflow: hidden;
                }
            """.trimIndent())
        }
    }
}

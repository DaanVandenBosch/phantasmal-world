package world.phantasmal.web.application.widgets

import org.w3c.dom.Node
import world.phantasmal.web.application.controllers.MainContentController
import world.phantasmal.web.core.PwToolType
import world.phantasmal.webui.dom.div
import world.phantasmal.webui.widgets.LazyLoader
import world.phantasmal.webui.widgets.Widget

class MainContentWidget(
    private val ctrl: MainContentController,
    private val toolViews: Map<PwToolType, () -> Widget>,
) : Widget() {
    override fun Node.createElement() =
        div {
            className = "pw-application-main-content"

            ctrl.tools.forEach { (tool, active) ->
                toolViews[tool]?.let { createWidget ->
                    addChild(LazyLoader(visible = active, createWidget = createWidget))
                }
            }
        }

    companion object {
        init {
            @Suppress("CssUnusedSymbol")
            // language=css
            style("""
                .pw-application-main-content {
                    display: grid;
                    grid-template: 100% / 100%;
                    overflow: hidden;
                }
            """.trimIndent())
        }
    }
}

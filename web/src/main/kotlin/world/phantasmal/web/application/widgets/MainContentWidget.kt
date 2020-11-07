package world.phantasmal.web.application.widgets

import kotlinx.coroutines.CoroutineScope
import org.w3c.dom.Node
import world.phantasmal.observable.value.not
import world.phantasmal.web.application.controllers.MainContentController
import world.phantasmal.web.core.PwTool
import world.phantasmal.web.core.PwToolType
import world.phantasmal.webui.dom.div
import world.phantasmal.webui.widgets.LazyLoader
import world.phantasmal.webui.widgets.Widget

class MainContentWidget(
    scope: CoroutineScope,
    private val ctrl: MainContentController,
    private val toolViews: Map<PwToolType, (CoroutineScope) -> Widget>,
) : Widget(scope) {
    override fun Node.createElement() =
        div {
            className = "pw-application-main-content"

            ctrl.tools.forEach { (tool, active) ->
                toolViews[tool]?.let { createWidget ->
                    addChild(LazyLoader(scope, hidden = !active, createWidget = createWidget))
                }
            }
        }

    companion object {
        init {
            @Suppress("CssUnusedSymbol")
            // language=css
            style("""
                .pw-application-main-content {
                    display: flex;
                    flex-direction: column;
                }

                .pw-application-main-content > * {
                    flex-grow: 1;
                    overflow: hidden;
                }
            """.trimIndent())
        }
    }
}

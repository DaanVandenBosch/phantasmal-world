package world.phantasmal.web.viewer.widgets

import kotlinx.coroutines.CoroutineScope
import org.w3c.dom.Node
import world.phantasmal.web.viewer.controller.ViewerController
import world.phantasmal.web.viewer.controller.ViewerTab
import world.phantasmal.webui.dom.div
import world.phantasmal.webui.widgets.TabContainer
import world.phantasmal.webui.widgets.Widget

/**
 * Takes ownership of the widget returned by [createToolbar].
 */
class ViewerWidget(
    scope: CoroutineScope,
    private val ctrl: ViewerController,
    private val createToolbar: (CoroutineScope) -> Widget,
    private val createMeshWidget: (CoroutineScope) -> Widget,
    private val createTextureWidget: (CoroutineScope) -> Widget,
) : Widget(scope) {
    override fun Node.createElement() =
        div {
            className = "pw-viewer-viewer"

            addChild(createToolbar(scope))
            addChild(TabContainer(scope, ctrl = ctrl, createWidget = { scope, tab ->
                when (tab) {
                    ViewerTab.Mesh -> createMeshWidget(scope)
                    ViewerTab.Texture -> createTextureWidget(scope)
                }
            }))
        }

    companion object {
        init {
            @Suppress("CssUnusedSymbol")
            // language=css
            style("""
                .pw-viewer-viewer {
                    display: flex;
                    flex-direction: column;
                }
                .pw-viewer-viewer > .pw-tab-container {
                    flex-grow: 1;
                    overflow: hidden;
                }
            """.trimIndent())
        }
    }
}

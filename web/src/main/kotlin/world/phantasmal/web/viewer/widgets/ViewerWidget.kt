package world.phantasmal.web.viewer.widgets

import kotlinx.coroutines.CoroutineScope
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.Node
import world.phantasmal.web.core.rendering.Renderer
import world.phantasmal.web.core.widgets.RendererWidget
import world.phantasmal.webui.dom.div
import world.phantasmal.webui.widgets.Widget

/**
 * Takes ownership of the widget returned by [createToolbar].
 */
class ViewerWidget(
    scope: CoroutineScope,
    private val createToolbar: (CoroutineScope) -> Widget,
    private val canvas: HTMLCanvasElement,
    private val renderer: Renderer,
) : Widget(scope) {
    override fun Node.createElement() =
        div {
            className = "pw-viewer-viewer"

            addChild(createToolbar(scope))
            div {
                className = "pw-viewer-viewer-container"

                addChild(RendererWidget(scope, canvas, renderer))
            }
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
                .pw-viewer-viewer-container {
                    flex-grow: 1;
                    display: flex;
                    flex-direction: row;
                    overflow: hidden;
                }
            """.trimIndent())
        }
    }
}

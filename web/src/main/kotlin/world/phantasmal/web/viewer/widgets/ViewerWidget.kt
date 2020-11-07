package world.phantasmal.web.viewer.widgets

import kotlinx.coroutines.CoroutineScope
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.Node
import world.phantasmal.web.core.rendering.Renderer
import world.phantasmal.web.core.widgets.RendererWidget
import world.phantasmal.webui.dom.div
import world.phantasmal.webui.widgets.Widget

class ViewerWidget(
    scope: CoroutineScope,
    private val toolbar: Widget,
    private val canvas: HTMLCanvasElement,
    private val renderer: Renderer,
) : Widget(scope) {
    override fun Node.createElement() =
        div {
            className = "pw-viewer-viewer"

            addChild(toolbar)
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

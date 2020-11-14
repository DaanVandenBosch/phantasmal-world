package world.phantasmal.web.core.widgets

import kotlinx.coroutines.CoroutineScope
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.Node
import world.phantasmal.web.core.rendering.Renderer
import world.phantasmal.webui.dom.div
import world.phantasmal.webui.widgets.Widget
import kotlin.math.floor

class RendererWidget(
    scope: CoroutineScope,
    private val canvas: HTMLCanvasElement,
    private val renderer: Renderer,
) : Widget(scope) {

    override fun Node.createElement() =
        div {
            className = "pw-core-renderer"
            tabIndex = -1

            observe(selfOrAncestorVisible) { visible ->
                if (visible) {
                    renderer.startRendering()
                } else {
                    renderer.stopRendering()
                }
            }

            addDisposable(size.observe { (size) ->
                canvas.width = floor(size.width).toInt()
                canvas.height = floor(size.height).toInt()
            })

            append(canvas)
        }

    companion object {
        init {
            @Suppress("CssUnusedSymbol")
            // language=css
            style("""
                .pw-core-renderer {
                    width: 100%;
                    height: 100%;
                    outline: none;
                }
            """.trimIndent())
        }
    }
}

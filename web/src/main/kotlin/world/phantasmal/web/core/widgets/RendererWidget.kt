package world.phantasmal.web.core.widgets

import kotlinx.coroutines.CoroutineScope
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.Node
import world.phantasmal.web.externals.Engine
import world.phantasmal.web.questEditor.rendering.QuestRenderer
import world.phantasmal.webui.dom.canvas
import world.phantasmal.webui.widgets.Widget
import kotlin.math.floor

class RendererWidget(
    scope: CoroutineScope,
    private val createEngine: (HTMLCanvasElement) -> Engine,
) : Widget(scope) {
    override fun Node.createElement() =
        canvas {
            className = "pw-core-renderer"

            observeResize()
            addDisposable(QuestRenderer(this, createEngine))
        }

    override fun resized(width: Double, height: Double) {
        val canvas = (element as HTMLCanvasElement)
        canvas.width = floor(width).toInt()
        canvas.height = floor(height).toInt()
    }

    companion object {
        init {
            @Suppress("CssUnusedSymbol")
            // language=css
            style("""
                .pw-core-renderer {
                    width: 100%;
                    height: 100%;
                }
            """.trimIndent())
        }
    }
}

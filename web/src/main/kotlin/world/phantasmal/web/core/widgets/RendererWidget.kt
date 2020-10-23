package world.phantasmal.web.core.widgets

import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.Node
import world.phantasmal.core.disposable.Scope
import world.phantasmal.web.externals.Engine
import world.phantasmal.web.questEditor.rendering.QuestRenderer
import world.phantasmal.webui.dom.canvas
import world.phantasmal.webui.widgets.Widget
import kotlin.math.floor

class RendererWidget(
    scope: Scope,
    private val createEngine: (HTMLCanvasElement) -> Engine,
) : Widget(scope, ::style) {
    override fun Node.createElement() = canvas(className = "pw-core-renderer") {
        observeResize()
        QuestRenderer(scope, this, createEngine)
    }

    override fun resized(width: Double, height: Double) {
        val canvas = (element as HTMLCanvasElement)
        canvas.width = floor(width).toInt()
        canvas.height = floor(height).toInt()
    }
}

@Suppress("CssUnusedSymbol")
// language=css
private fun style() = """
.pw-core-renderer {
    width: 100%;
    height: 100%;
}
"""
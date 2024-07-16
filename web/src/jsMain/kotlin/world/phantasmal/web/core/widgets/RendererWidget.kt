package world.phantasmal.web.core.widgets

import org.w3c.dom.Node
import world.phantasmal.web.core.rendering.Renderer
import world.phantasmal.webui.dom.div
import world.phantasmal.webui.widgets.Widget

class RendererWidget(
    private val renderer: Renderer,
) : Widget() {
    override fun Node.createElement() =
        div {
            className = "pw-core-renderer"

            addDisposable(size.observeChange { (size) ->
                renderer.setSize(size.width.toInt(), size.height.toInt())
            })

            append(renderer.canvas)
        }

    override fun selfAndAncestorsVisibleChanged(visible: Boolean) {
        if (visible) {
            renderer.startRendering()
        } else {
            renderer.stopRendering()
        }
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
                    background-color: #181818;
                }
            """.trimIndent())
        }
    }
}

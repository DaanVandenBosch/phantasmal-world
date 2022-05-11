package world.phantasmal.webui.widgets

import org.w3c.dom.Node
import world.phantasmal.observable.cell.Cell
import world.phantasmal.observable.cell.trueCell
import world.phantasmal.observable.mutateDeferred
import world.phantasmal.webui.dom.div

class LazyLoader(
    visible: Cell<Boolean> = trueCell(),
    enabled: Cell<Boolean> = trueCell(),
    private val createWidget: () -> Widget,
) : Widget(visible, enabled) {
    private var initialized = false

    override fun Node.createElement() =
        div {
            className = "pw-lazy-loader"

            observeNow(this@LazyLoader.visible) { v ->
                if (v && !initialized) {
                    initialized = true

                    mutateDeferred {
                        if (!disposed) {
                            addChild(createWidget())
                        }
                    }
                }
            }
        }

    companion object {
        init {
            @Suppress("CssUnusedSymbol")
            // language=css
            style("""
                .pw-lazy-loader {
                    display: grid;
                    grid-template: 100% / 100%;
                    overflow: hidden;
                }
            """.trimIndent())
        }
    }
}

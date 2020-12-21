package world.phantasmal.webui.widgets

import org.w3c.dom.Node
import world.phantasmal.observable.value.Val
import world.phantasmal.observable.value.trueVal
import world.phantasmal.webui.dom.div

class LazyLoader(
    visible: Val<Boolean> = trueVal(),
    enabled: Val<Boolean> = trueVal(),
    private val createWidget: () -> Widget,
) : Widget(visible, enabled) {
    private var initialized = false

    override fun Node.createElement() =
        div {
            className = "pw-lazy-loader"

            observe(this@LazyLoader.visible) { v ->
                if (v && !initialized) {
                    initialized = true
                    addChild(createWidget())
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

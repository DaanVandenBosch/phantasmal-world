package world.phantasmal.webui.widgets

import kotlinx.coroutines.CoroutineScope
import org.w3c.dom.Node
import world.phantasmal.observable.value.Val
import world.phantasmal.observable.value.trueVal
import world.phantasmal.webui.dom.div

class LazyLoader(
    scope: CoroutineScope,
    visible: Val<Boolean> = trueVal(),
    enabled: Val<Boolean> = trueVal(),
    private val createWidget: (CoroutineScope) -> Widget,
) : Widget(scope, visible, enabled) {
    private var initialized = false

    override fun Node.createElement() =
        div {
            className = "pw-lazy-loader"

            observe(this@LazyLoader.visible) { v ->
                if (v && !initialized) {
                    initialized = true
                    addChild(createWidget(scope))
                }
            }
        }

    companion object {
        init {
            @Suppress("CssUnusedSymbol")
            // language=css
            style("""
                .pw-lazy-loader {
                    display: flex;
                    flex-direction: column;
                    align-items: stretch;
                }

                .pw-lazy-loader > * {
                    flex-grow: 1;
                    overflow: hidden;
                }
            """.trimIndent())
        }
    }
}

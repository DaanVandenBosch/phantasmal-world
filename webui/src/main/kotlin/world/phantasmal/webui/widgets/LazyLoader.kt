package world.phantasmal.webui.widgets

import kotlinx.coroutines.CoroutineScope
import org.w3c.dom.Node
import world.phantasmal.observable.value.Val
import world.phantasmal.observable.value.falseVal
import world.phantasmal.webui.dom.div

class LazyLoader(
    scope: CoroutineScope,
    hidden: Val<Boolean> = falseVal(),
    disabled: Val<Boolean> = falseVal(),
    private val createWidget: (CoroutineScope) -> Widget,
) : Widget(scope, hidden, disabled) {
    private var initialized = false

    override fun Node.createElement() =
        div {
            className = "pw-lazy-loader"

            observe(this@LazyLoader.hidden) { h ->
                if (!h && !initialized) {
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

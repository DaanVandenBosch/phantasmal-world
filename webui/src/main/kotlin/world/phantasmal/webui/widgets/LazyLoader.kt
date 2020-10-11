package world.phantasmal.webui.widgets

import org.w3c.dom.Node
import world.phantasmal.observable.value.Val
import world.phantasmal.observable.value.falseVal
import world.phantasmal.webui.dom.div

class LazyLoader(
    hidden: Val<Boolean> = falseVal(),
    private val createWidget: () -> Widget,
) : Widget(::style, hidden) {
    private var initialized = false

    override fun Node.createElement() = div(className = "pw-lazy-loader") {
        observe(this@LazyLoader.hidden) { h ->
            if (!h && !initialized) {
                initialized = true
                addChild(createWidget())
            }
        }
    }
}

@Suppress("CssUnusedSymbol")
// language=css
private fun style() = """
.pw-lazy-loader {
    display: flex;
    flex-direction: column;
}

.pw-lazy-loader > * {
    flex: 1;
}
"""

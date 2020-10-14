package world.phantasmal.webui.widgets

import org.w3c.dom.Node
import world.phantasmal.core.disposable.Scope
import world.phantasmal.observable.value.Val
import world.phantasmal.observable.value.falseVal
import world.phantasmal.webui.dom.div

class LazyLoader(
    scope: Scope,
    hidden: Val<Boolean> = falseVal(),
    private val createWidget: (Scope) -> Widget,
) : Widget(scope, ::style, hidden) {
    private var initialized = false

    override fun Node.createElement() = div(className = "pw-lazy-loader") {
        this@LazyLoader.hidden.observe { h ->
            if (!h && !initialized) {
                initialized = true
                addChild(createWidget(scope))
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
    align-items: stretch;
}

.pw-lazy-loader > * {
    flex-grow: 1;
    overflow: hidden;
}
"""

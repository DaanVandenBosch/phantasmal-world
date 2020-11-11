package world.phantasmal.web.core.widgets

import kotlinx.coroutines.CoroutineScope
import org.w3c.dom.Node
import world.phantasmal.observable.value.Val
import world.phantasmal.observable.value.falseVal
import world.phantasmal.webui.dom.div
import world.phantasmal.webui.widgets.Label
import world.phantasmal.webui.widgets.Widget

class UnavailableWidget(
    scope: CoroutineScope,
    visible: Val<Boolean>,
    private val message: String,
) : Widget(scope, visible) {
    override fun Node.createElement() =
        div {
            className = "pw-core-unavailable"

            addWidget(Label(scope, enabled = falseVal(), text = message))
        }

    companion object {
        init {
            @Suppress("CssUnusedSymbol")
            // language=css
            style("""
                .pw-core-unavailable {
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    width: 100%;
                    height: 100%;
                    text-align: center;
                }
            """.trimIndent())
        }
    }
}

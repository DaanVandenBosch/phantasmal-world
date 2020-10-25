package world.phantasmal.web.questEditor.widgets

import kotlinx.coroutines.CoroutineScope
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.Node
import world.phantasmal.web.core.widgets.RendererWidget
import world.phantasmal.web.externals.Engine
import world.phantasmal.webui.dom.div
import world.phantasmal.webui.widgets.Widget

abstract class QuestRendererWidget(
    scope: CoroutineScope,
    private val createEngine: (HTMLCanvasElement) -> Engine,
) : Widget(scope, listOf(::style)) {
    override fun Node.createElement() = div(className = "pw-quest-editor-quest-renderer") {
        addChild(RendererWidget(scope, createEngine))
    }
}

@Suppress("CssUnusedSymbol")
// language=css
private fun style() = """
.pw-quest-editor-quest-renderer {
    display: flex;
    overflow: hidden;
}
.pw-quest-editor-quest-renderer > * {
    flex-grow: 1;
}
"""

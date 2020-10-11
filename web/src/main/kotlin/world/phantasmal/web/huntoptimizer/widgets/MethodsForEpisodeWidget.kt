package world.phantasmal.web.huntoptimizer.widgets

import org.w3c.dom.Node
import world.phantasmal.lib.fileformats.quest.Episode
import world.phantasmal.web.huntoptimizer.controllers.MethodsController
import world.phantasmal.webui.dom.bindChildrenTo
import world.phantasmal.webui.dom.div
import world.phantasmal.webui.widgets.Widget

class MethodsForEpisodeWidget(
    private val ctrl: MethodsController,
    private val episode: Episode,
) : Widget(::style) {
    override fun Node.createElement() = div(className = "pw-huntoptimizer-methods-for-episode") {
        bindChildrenTo(ctrl.episodeToMethods.getValue(episode)) { method, _ ->
            div { textContent = method.name }
        }
    }
}

@Suppress("CssUnusedSymbol")
// language=css
private fun style() = """
.pw-huntoptimizer-methods-for-episode {
    overflow: auto;
}
"""

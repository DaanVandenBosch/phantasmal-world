package world.phantasmal.web.huntOptimizer.widgets

import org.w3c.dom.Node
import world.phantasmal.core.disposable.Scope
import world.phantasmal.lib.fileFormats.quest.Episode
import world.phantasmal.web.huntOptimizer.controllers.MethodsController
import world.phantasmal.webui.dom.bindChildrenTo
import world.phantasmal.webui.dom.div
import world.phantasmal.webui.widgets.Widget

class MethodsForEpisodeWidget(
    scope: Scope,
    private val ctrl: MethodsController,
    private val episode: Episode,
) : Widget(scope, ::style) {
    override fun Node.createElement() =
        div(className = "pw-hunt-optimizer-methods-for-episode") {
            bindChildrenTo(scope, ctrl.episodeToMethods.getValue(episode)) { method, _ ->
                div { textContent = method.name }
            }
        }
}

@Suppress("CssUnusedSymbol")
// language=css
private fun style() = """
.pw-hunt-optimizer-methods-for-episode {
    overflow: auto;
}
"""

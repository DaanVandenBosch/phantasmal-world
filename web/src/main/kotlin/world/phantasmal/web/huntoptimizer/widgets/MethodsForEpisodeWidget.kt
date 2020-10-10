package world.phantasmal.web.huntoptimizer.widgets

import org.w3c.dom.Node
import world.phantasmal.lib.fileformats.quest.Episode
import world.phantasmal.web.huntoptimizer.controllers.MethodsController
import world.phantasmal.webui.dom.div
import world.phantasmal.webui.widgets.Widget

class MethodsForEpisodeWidget(
    private val ctrl: MethodsController,
    private val episode: Episode,
) : Widget() {
    override fun Node.createElement() = div {
        textContent = "TODO"
    }
}

package world.phantasmal.web.huntoptimizer.widgets

import org.w3c.dom.Node
import world.phantasmal.webui.dom.div
import world.phantasmal.webui.dom.p
import world.phantasmal.webui.widgets.Widget

class HelpWidget : Widget(::style) {
    override fun Node.createElement() = div(className = "pw-huntoptimizer-help") {
        p {
            textContent =
                "Add some items with the combo box on the left to see the optimal combination of hunt methods on the right."
        }
        p {
            textContent =
                "At the moment a hunt method is simply a quest run-through. Partial quest run-throughs are coming. View the list of methods on the \"Methods\" tab. Each method takes a certain amount of time, which affects the optimization result. Make sure the times are correct for you."
        }
        p {
            textContent = "Only enemy drops are considered. Box drops are coming."
        }
        p {
            textContent =
                "The optimal result is calculated using linear optimization. The optimizer takes into account rare enemies and the fact that pan arms can be split in two."
        }
    }
}

// language=css
private fun style() = """
.pw-huntoptimizer-help p {
    margin: 1em;
    max-width: 600px;
}
"""

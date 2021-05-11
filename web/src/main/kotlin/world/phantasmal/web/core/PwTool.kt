package world.phantasmal.web.core

import world.phantasmal.webui.widgets.Widget

/**
 * Phantasmal World consists of several tools.
 * An instance of PwTool should do as little work as possible in its constructor and defer any
 * initialization work until [initialize] is called.
 */
interface PwTool {
    val toolType: PwToolType

    /**
     * The caller of this method takes ownership of the returned widget.
     */
    fun initialize(): Widget
}

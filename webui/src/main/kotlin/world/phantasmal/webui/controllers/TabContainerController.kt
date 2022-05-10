package world.phantasmal.webui.controllers

import world.phantasmal.observable.cell.Cell

interface Tab {
    val title: String
}

abstract class TabContainerController<T : Tab> : Controller() {
    abstract val tabs: List<T>

    abstract val activeTab: Cell<T?>

    abstract fun setActiveTab(tab: T?)

    open fun visibleChanged(visible: Boolean) {}
}

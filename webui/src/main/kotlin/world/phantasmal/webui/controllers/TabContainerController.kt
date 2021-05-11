package world.phantasmal.webui.controllers

import world.phantasmal.observable.cell.Cell
import world.phantasmal.observable.cell.MutableCell
import world.phantasmal.observable.cell.mutableCell

interface Tab {
    val title: String
}

open class TabContainerController<T : Tab>(val tabs: List<T>) : Controller() {
    private val _activeTab: MutableCell<T?> = mutableCell(tabs.firstOrNull())

    val activeTab: Cell<T?> = _activeTab

    open fun setActiveTab(tab: T?, replaceUrl: Boolean = false) {
        _activeTab.value = tab
    }

    open fun visibleChanged(visible: Boolean) {}
}

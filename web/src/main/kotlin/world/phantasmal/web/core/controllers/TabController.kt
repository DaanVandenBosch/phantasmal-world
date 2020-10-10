package world.phantasmal.web.core.controllers

import world.phantasmal.observable.value.MutableVal
import world.phantasmal.observable.value.Val
import world.phantasmal.observable.value.mutableVal
import world.phantasmal.webui.controllers.Controller

interface Tab {
    val title: String
}

open class TabController<T : Tab>(val tabs: List<T>) : Controller() {
    private val _activeTab: MutableVal<T?> = mutableVal(tabs.firstOrNull())

    val activeTab: Val<T?> = _activeTab

    open fun setActiveTab(tab: T?, replaceUrl: Boolean = false) {
        _activeTab.value = tab
    }

    open fun hiddenChanged(hidden: Boolean) {}
}

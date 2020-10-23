package world.phantasmal.webui.controllers

import world.phantasmal.core.disposable.Scope
import world.phantasmal.observable.value.MutableVal
import world.phantasmal.observable.value.Val
import world.phantasmal.observable.value.mutableVal

interface Tab {
    val title: String
}

open class TabController<T : Tab>(scope: Scope, val tabs: List<T>) : Controller(scope) {
    private val _activeTab: MutableVal<T?> = mutableVal(tabs.firstOrNull())

    val activeTab: Val<T?> = _activeTab

    open fun setActiveTab(tab: T?, replaceUrl: Boolean = false) {
        _activeTab.value = tab
    }

    open fun hiddenChanged(hidden: Boolean) {}
}
package world.phantasmal.web.core.controllers

import world.phantasmal.web.core.stores.PwTool
import world.phantasmal.web.core.stores.UiStore
import world.phantasmal.webui.controllers.Tab
import world.phantasmal.webui.controllers.TabController

open class PathAwareTab(override val title: String, val path: String) : Tab

open class PathAwareTabController<T : PathAwareTab>(
    private val uiStore: UiStore,
    private val tool: PwTool,
    tabs: List<T>,
) : TabController<T>(tabs) {
    init {
        addDisposable(uiStore.path.observe(callNow = true) { (path) ->
            if (uiStore.currentTool.value == tool) {
                tabs.find { path.startsWith(it.path) }?.let {
                    setActiveTab(it, replaceUrl = true)
                }
            }
        })
    }

    override fun setActiveTab(tab: T?, replaceUrl: Boolean) {
        if (tab != null && uiStore.currentTool.value == tool) {
            uiStore.setPathPrefix(tab.path, replaceUrl)
        }

        super.setActiveTab(tab)
    }

    override fun hiddenChanged(hidden: Boolean) {
        super.hiddenChanged(hidden)

        if (!hidden && uiStore.currentTool.value == tool) {
            activeTab.value?.let {
                uiStore.setPathPrefix(it.path, replace = true)
            }
        }
    }
}

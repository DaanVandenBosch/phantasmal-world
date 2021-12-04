package world.phantasmal.web.core.controllers

import world.phantasmal.web.core.PwToolType
import world.phantasmal.web.core.stores.UiStore
import world.phantasmal.webui.controllers.Tab
import world.phantasmal.webui.controllers.TabContainerController

interface PathAwareTab : Tab {
    val path: String
}

open class PathAwareTabContainerController<T : PathAwareTab>(
    private val uiStore: UiStore,
    private val tool: PwToolType,
    tabs: List<T>,
) : TabContainerController<T>(tabs) {
    init {
        observeNow(uiStore.path) { path ->
            if (uiStore.currentTool.value == tool) {
                tabs.find { path.startsWith(it.path) }?.let {
                    setActiveTab(it, replaceUrl = true)
                }
            }
        }
    }

    override fun setActiveTab(tab: T?, replaceUrl: Boolean) {
        if (tab != null && uiStore.currentTool.value == tool) {
            uiStore.setPathPrefix(tab.path, replaceUrl)
        }

        super.setActiveTab(tab)
    }

    override fun visibleChanged(visible: Boolean) {
        super.visibleChanged(visible)

        if (visible && uiStore.currentTool.value == tool) {
            activeTab.value?.let {
                uiStore.setPathPrefix(it.path, replace = true)
            }
        }
    }
}

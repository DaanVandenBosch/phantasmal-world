package world.phantasmal.web.core.controllers

import world.phantasmal.cell.Cell
import world.phantasmal.cell.mutableCell
import world.phantasmal.cell.mutateDeferred
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
    final override val tabs: List<T>,
) : TabContainerController<T>() {
    private val _activeTab = mutableCell(tabs.firstOrNull())
    final override val activeTab: Cell<T?> = _activeTab

    init {
        observeNow(uiStore.currentTool, uiStore.path) { currentTool, path ->
            if (currentTool == tool) {
                val aTab = _activeTab.value

                if (aTab == null || !path.startsWith(aTab.path)) {
                    val newActiveTab = tabs.find { path.startsWith(it.path) }

                    if (newActiveTab != null) {
                        mutateDeferred {
                            _activeTab.value = newActiveTab
                        }
                    }
                }
            }
        }

        setPathPrefix(_activeTab.value, replace = true)
    }

    final override fun setActiveTab(tab: T?) {
        setPathPrefix(tab, replace = false)
    }

    final override fun visibleChanged(visible: Boolean) {
        super.visibleChanged(visible)

        if (visible) {
            mutateDeferred {
                setPathPrefix(_activeTab.value, replace = true)
            }
        }
    }

    private fun setPathPrefix(tab: T?, replace: Boolean) {
        if (tab != null && uiStore.currentTool.value == tool) {
            uiStore.setPathPrefix(tab.path, replace)
        }
    }
}

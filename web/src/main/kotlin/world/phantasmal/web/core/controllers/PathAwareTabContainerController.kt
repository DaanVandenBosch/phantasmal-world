package world.phantasmal.web.core.controllers

import world.phantasmal.observable.cell.Cell
import world.phantasmal.observable.cell.map
import world.phantasmal.observable.mutateDeferred
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
    final override val activeTab: Cell<T?> =
        map(uiStore.currentTool, uiStore.path) { currentTool, path ->
            if (currentTool == tool) {
                tabs.find { path.startsWith(it.path) } ?: tabs.firstOrNull()
            } else {
                null
            }
        }

    init {
        setPathPrefix(activeTab.value, replace = true)
    }

    final override fun setActiveTab(tab: T?) {
        setPathPrefix(tab, replace = false)
    }

    final override fun visibleChanged(visible: Boolean) {
        super.visibleChanged(visible)

        if (visible) {
            mutateDeferred {
                if (!disposed) {
                    setPathPrefix(activeTab.value, replace = true)
                }
            }
        }
    }

    private fun setPathPrefix(tab: T?, replace: Boolean) {
        if (tab != null && uiStore.currentTool.value == tool) {
            uiStore.setPathPrefix(tab.path, replace)
        }
    }
}

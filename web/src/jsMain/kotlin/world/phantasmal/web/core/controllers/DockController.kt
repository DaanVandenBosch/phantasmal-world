package world.phantasmal.web.core.controllers

import world.phantasmal.web.core.observable.Emitter
import world.phantasmal.web.core.observable.Observable
import world.phantasmal.webui.controllers.Controller

sealed class DockedItem {
    abstract val flex: Double?
}

sealed class DockedContainer : DockedItem() {
    abstract val items: List<DockedItem>
}

class DockedRow(
    override val flex: Double? = null,
    override val items: List<DockedItem> = emptyList(),
) : DockedContainer()

class DockedColumn(
    override val flex: Double? = null,
    override val items: List<DockedItem> = emptyList(),
) : DockedContainer()

class DockedStack(
    val activeItemIndex: Int? = null,
    override val flex: Double? = null,
    override val items: List<DockedItem> = emptyList(),
) : DockedContainer()

class DockedWidget(
    val id: String,
    val title: String,
    override val flex: Double? = null,
) : DockedItem()

abstract class DockController : Controller() {
    private val _activateWidgetEvent = Emitter<String>()
    val activateWidgetEvent: Observable<String> = _activateWidgetEvent

    abstract suspend fun initialConfig(): DockedItem

    abstract suspend fun configChanged(config: DockedItem)

    fun requestActivateWidget(widgetId: String) {
        _activateWidgetEvent.emit(widgetId)
    }
}

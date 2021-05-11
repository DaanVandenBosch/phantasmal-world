package world.phantasmal.web.huntOptimizer.controllers

import world.phantasmal.observable.cell.Cell
import world.phantasmal.observable.cell.MutableCell
import world.phantasmal.observable.cell.list.ListCell
import world.phantasmal.observable.cell.mutableCell
import world.phantasmal.web.huntOptimizer.models.WantedItemModel
import world.phantasmal.web.huntOptimizer.stores.HuntOptimizerStore
import world.phantasmal.web.shared.dto.ItemType
import world.phantasmal.webui.controllers.Controller

class WantedItemsController(
    private val huntOptimizerStore: HuntOptimizerStore,
) : Controller() {
    private val selectableItemsFilter: MutableCell<(ItemType) -> Boolean> = mutableCell { true }

    // TODO: Use ListCell.filtered with a Cell when this is supported.
    val selectableItems: Cell<List<ItemType>> = selectableItemsFilter.flatMap { filter ->
        huntOptimizerStore.huntableItems.filtered(filter)
    }

    val wantedItems: ListCell<WantedItemModel> = huntOptimizerStore.wantedItems

    fun filterSelectableItems(text: String) {
        val sanitized = text.trim()
        selectableItemsFilter.value = { it.name.contains(sanitized, ignoreCase = true) }
    }

    fun setAmount(wanted: WantedItemModel, amount: Int) {
        wanted.setAmount(amount)
    }

    fun addWantedItem(itemType: ItemType) {
        huntOptimizerStore.addWantedItem(itemType)
    }

    fun removeWantedItem(wanted: WantedItemModel) {
        huntOptimizerStore.removeWantedItem(wanted)
    }
}

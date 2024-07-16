package world.phantasmal.web.huntOptimizer.controllers

import world.phantasmal.cell.Cell
import world.phantasmal.cell.MutableCell
import world.phantasmal.cell.list.ListCell
import world.phantasmal.cell.list.filtered
import world.phantasmal.cell.mutableCell
import world.phantasmal.web.huntOptimizer.models.WantedItemModel
import world.phantasmal.web.huntOptimizer.stores.HuntOptimizerStore
import world.phantasmal.web.shared.dto.ItemType
import world.phantasmal.webui.controllers.Controller

class WantedItemsController(
    private val huntOptimizerStore: HuntOptimizerStore,
) : Controller() {
    private val selectableItemsFilter: MutableCell<(ItemType) -> Boolean> = mutableCell { true }

    val selectableItems: Cell<List<ItemType>> =
        huntOptimizerStore.huntableItems.filtered(selectableItemsFilter)

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

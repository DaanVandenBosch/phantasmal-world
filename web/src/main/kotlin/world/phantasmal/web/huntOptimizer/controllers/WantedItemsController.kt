package world.phantasmal.web.huntOptimizer.controllers

import world.phantasmal.observable.value.MutableVal
import world.phantasmal.observable.value.Val
import world.phantasmal.observable.value.list.ListVal
import world.phantasmal.observable.value.mutableVal
import world.phantasmal.web.huntOptimizer.models.WantedItemModel
import world.phantasmal.web.huntOptimizer.stores.HuntOptimizerStore
import world.phantasmal.web.shared.dto.ItemType
import world.phantasmal.webui.controllers.Controller

class WantedItemsController(
    private val huntOptimizerStore: HuntOptimizerStore,
) : Controller() {
    private val selectableItemsFilter: MutableVal<(ItemType) -> Boolean> = mutableVal { true }

    // TODO: Use ListVal.filtered with a Val when this is supported.
    val selectableItems: Val<List<ItemType>> = selectableItemsFilter.flatMap { filter ->
        huntOptimizerStore.huntableItems.filtered(filter)
    }

    val wantedItems: ListVal<WantedItemModel> = huntOptimizerStore.wantedItems

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

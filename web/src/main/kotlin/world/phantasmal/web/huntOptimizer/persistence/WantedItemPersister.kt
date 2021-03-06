package world.phantasmal.web.huntOptimizer.persistence

import world.phantasmal.web.core.models.Server
import world.phantasmal.web.core.persistence.Persister
import world.phantasmal.web.core.stores.ItemTypeStore
import world.phantasmal.web.shared.dto.WantedItemDto
import world.phantasmal.web.huntOptimizer.models.WantedItemModel

class WantedItemPersister(private val itemTypeStore: ItemTypeStore) : Persister() {
    suspend fun persistWantedItems(wantedItems: List<WantedItemModel>, server: Server) {
        persistForServer(server, WANTED_ITEMS_KEY, wantedItems.map {
            WantedItemDto(it.itemType.id, it.amount.value)
        })
    }

    suspend fun loadWantedItems(server: Server): List<WantedItemModel> =
        loadForServer<List<WantedItemDto>>(server, WANTED_ITEMS_KEY)?.mapNotNull { wantedItem ->
            itemTypeStore.getById(server, wantedItem.itemTypeId)?.let { itemType ->
                WantedItemModel(itemType, wantedItem.amount)
            }
        } ?: emptyList()

    companion object {
        private const val WANTED_ITEMS_KEY = "HuntOptimizerStore.wantedItems"
    }
}

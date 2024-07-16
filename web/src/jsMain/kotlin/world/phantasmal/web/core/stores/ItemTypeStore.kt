package world.phantasmal.web.core.stores

import world.phantasmal.web.core.loading.AssetLoader
import world.phantasmal.web.core.models.Server
import world.phantasmal.web.core.loading.LoadingCache
import world.phantasmal.web.shared.dto.ItemType
import world.phantasmal.webui.stores.Store

class ItemTypeStore(
    private val assetLoader: AssetLoader,
) : Store() {
    private val cache: LoadingCache<Server, ServerData> = addDisposable(
        LoadingCache(::loadItemTypes) {}
    )

    suspend fun getItemTypes(server: Server): List<ItemType> =
        cache.get(server).itemTypes

    suspend fun getById(server: Server, id: Int): ItemType? =
        cache.get(server).idToItemType[id]

    private suspend fun loadItemTypes(server: Server): ServerData {
        val itemTypes = assetLoader.load<List<ItemType>>("/item_types.${server.slug}.json")
        val idToItemType = itemTypes.associateBy { it.id }
        return ServerData(itemTypes, idToItemType)
    }

    private class ServerData(
        val itemTypes: List<ItemType>,
        val idToItemType: Map<Int, ItemType>,
    )
}

package world.phantasmal.web.core.stores

import kotlinx.coroutines.launch
import world.phantasmal.observable.value.list.ListVal
import world.phantasmal.observable.value.list.mutableListVal
import world.phantasmal.web.core.loading.AssetLoader
import world.phantasmal.web.core.models.Server
import world.phantasmal.web.questEditor.loading.LoadingCache
import world.phantasmal.web.shared.dto.*
import world.phantasmal.webui.stores.Store

class ItemTypeStore(
    private val uiStore: UiStore,
    private val assetLoader: AssetLoader,
) : Store() {
    private val cache: LoadingCache<Server, ServerData> = addDisposable(
        LoadingCache(::loadItemTypes) {}
    )
    private val _itemTypes = mutableListVal<ItemType>()

    val itemTypes: ListVal<ItemType> by lazy {
        observe(uiStore.server) {
            scope.launch {
                _itemTypes.value = cache.get(it).itemTypes
            }
        }

        _itemTypes
    }

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

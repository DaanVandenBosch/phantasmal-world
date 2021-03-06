package world.phantasmal.web.huntOptimizer.stores

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import world.phantasmal.observable.value.list.ListVal
import world.phantasmal.observable.value.list.mutableListVal
import world.phantasmal.web.core.models.Server
import world.phantasmal.web.core.stores.UiStore
import world.phantasmal.web.huntOptimizer.models.WantedItemModel
import world.phantasmal.web.huntOptimizer.persistence.WantedItemPersister
import world.phantasmal.web.shared.dto.ItemType
import world.phantasmal.webui.stores.Store

// TODO: take into account mothmants spawned from mothverts.
// TODO: take into account split slimes.
// TODO: Prefer methods that don't split pan arms over methods that do.
//       For some reason this doesn't actually seem to be a problem, should probably investigate.
// TODO: Show expected value or probability per item per method.
//       Can be useful when deciding which item to hunt first.
// TODO: boxes.
class HuntOptimizerStore(
    private val wantedItemPersister: WantedItemPersister,
    private val uiStore: UiStore,
    private val huntMethodStore: HuntMethodStore,
) : Store() {
    private val _wantedItems = mutableListVal<WantedItemModel> { arrayOf(it.amount) }

    val wantedItems: ListVal<WantedItemModel> by lazy {
        observe(uiStore.server) { loadWantedItems(it) }
        _wantedItems
    }

    init {
        observe(wantedItems) {
            scope.launch(Dispatchers.Default) {
                wantedItemPersister.persistWantedItems(it, uiStore.server.value)
            }
        }
    }

    fun addWantedItem(itemType: ItemType) {
        if (wantedItems.value.none { it.itemType == itemType }) {
            _wantedItems.add(WantedItemModel(itemType, 1))
        }
    }

    fun removeWantedItem(wanted: WantedItemModel) {
        _wantedItems.remove(wanted)
    }

    private fun loadWantedItems(server: Server) {
        scope.launch(Dispatchers.Default) {
            val wantedItems = wantedItemPersister.loadWantedItems(server)

            withContext(Dispatchers.Main) {
                _wantedItems.value = wantedItems
            }
        }
    }
}

package world.phantasmal.web.core.stores

import world.phantasmal.core.unsafe.UnsafeMap
import world.phantasmal.lib.fileFormats.quest.NpcType
import world.phantasmal.web.core.loading.AssetLoader
import world.phantasmal.web.core.models.Server
import world.phantasmal.web.questEditor.loading.LoadingCache
import world.phantasmal.web.shared.dto.Difficulty
import world.phantasmal.web.shared.dto.EnemyDrop
import world.phantasmal.web.shared.dto.ItemType
import world.phantasmal.web.shared.dto.SectionId
import world.phantasmal.webui.stores.Store

class ItemDropStore(
    private val assetLoader: AssetLoader,
) : Store() {
    private val cache: LoadingCache<Server, EnemyDropTable> = addDisposable(
        LoadingCache(::loadEnemyDropTable) {}
    )

    suspend fun getEnemyDropTable(server: Server): EnemyDropTable =
        cache.get(server)

    private suspend fun loadEnemyDropTable(server: Server): EnemyDropTable {
        val drops = assetLoader.load<List<EnemyDrop>>("/enemy_drops.${server.slug}.json")

        val table = UnsafeMap<Difficulty, UnsafeMap<SectionId, UnsafeMap<NpcType, EnemyDrop>>>()
        val itemTypeToDrops = UnsafeMap<Int, MutableList<EnemyDrop>>()

        for (drop in drops) {
            var diffTable = table.get(drop.difficulty)

            if (diffTable == null) {
                diffTable = UnsafeMap()
                table.set(drop.difficulty, diffTable)
            }

            var sectionIdTable = diffTable.get(drop.sectionId)

            if (sectionIdTable == null) {
                sectionIdTable = UnsafeMap()
                diffTable.set(drop.sectionId, sectionIdTable)
            }

            sectionIdTable.set(drop.enemy, drop)

            var itemTypeDrops = itemTypeToDrops.get(drop.itemTypeId)

            if (itemTypeDrops == null) {
                itemTypeDrops = mutableListOf()
                itemTypeToDrops.set(drop.itemTypeId, itemTypeDrops)
            }

            itemTypeDrops.add(drop)
        }

        return EnemyDropTable(table, itemTypeToDrops)
    }
}

class EnemyDropTable(
    private val table: UnsafeMap<Difficulty, UnsafeMap<SectionId, UnsafeMap<NpcType, EnemyDrop>>>,
    /**
     * Mapping of [ItemType] ids to [EnemyDrop]s.
     */
    private val itemTypeToDrops: UnsafeMap<Int, MutableList<EnemyDrop>>,
) {
    fun getDrop(difficulty: Difficulty, sectionId: SectionId, npcType: NpcType): EnemyDrop? =
        table.get(difficulty)?.get(sectionId)?.get(npcType)

    fun getDropsForItemType(itemType: ItemType): List<EnemyDrop> =
        itemTypeToDrops.get(itemType.id) ?: emptyList()
}

package world.phantasmal.web.core.stores

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

        val table = mutableMapOf<Triple<Difficulty, SectionId, NpcType>, EnemyDrop>()
        val itemTypeToDrops = mutableMapOf<Int, MutableList<EnemyDrop>>()

        for (drop in drops) {
            table[Triple(drop.difficulty, drop.sectionId, drop.enemy)] = drop
            itemTypeToDrops.getOrPut(drop.itemTypeId) { mutableListOf() }.add(drop)
        }

        return EnemyDropTable(table, itemTypeToDrops)
    }
}

class EnemyDropTable(
    private val table: Map<Triple<Difficulty, SectionId, NpcType>, EnemyDrop>,
    /**
     * Mapping of [ItemType] ids to [EnemyDrop]s.
     */
    private val itemTypeToDrops: Map<Int, List<EnemyDrop>>,
) {
    fun getDrop(difficulty: Difficulty, sectionId: SectionId, npcType: NpcType): EnemyDrop? =
        table[Triple(difficulty, sectionId, npcType)]

    fun getDropsForItemType(itemType: ItemType): List<EnemyDrop> =
        itemTypeToDrops[itemType.id] ?: emptyList()
}

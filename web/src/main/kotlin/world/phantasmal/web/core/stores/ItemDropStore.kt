package world.phantasmal.web.core.stores

import world.phantasmal.core.JsMap
import world.phantasmal.core.emptyJsMap
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

        val table = emptyJsMap<Difficulty, JsMap<SectionId, JsMap<NpcType, EnemyDrop>>>()
        val itemTypeToDrops = emptyJsMap<Int, MutableList<EnemyDrop>>()

        for (drop in drops) {
            var diffTable = table.get(drop.difficulty)

            if (diffTable == null) {
                diffTable = emptyJsMap()
                table.set(drop.difficulty, diffTable)
            }

            var sectionIdTable = diffTable.get(drop.sectionId)

            if (sectionIdTable == null) {
                sectionIdTable = emptyJsMap()
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
    private val table: JsMap<Difficulty, JsMap<SectionId, JsMap<NpcType, EnemyDrop>>>,
    /**
     * Mapping of [ItemType] ids to [EnemyDrop]s.
     */
    private val itemTypeToDrops: JsMap<Int, MutableList<EnemyDrop>>,
) {
    fun getDrop(difficulty: Difficulty, sectionId: SectionId, npcType: NpcType): EnemyDrop? =
        table.get(difficulty)?.get(sectionId)?.get(npcType)

    fun getDropsForItemType(itemType: ItemType): List<EnemyDrop> =
        itemTypeToDrops.get(itemType.id) ?: emptyList()
}

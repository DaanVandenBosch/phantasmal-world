package world.phantasmal.web.huntOptimizer.stores

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import world.phantasmal.core.JsObject
import world.phantasmal.core.component1
import world.phantasmal.core.component2
import world.phantasmal.core.disposable.Disposable
import world.phantasmal.core.unsafe.UnsafeMap
import world.phantasmal.core.unsafe.UnsafeSet
import world.phantasmal.observable.cell.Cell
import world.phantasmal.observable.cell.list.ListCell
import world.phantasmal.observable.cell.list.dependingOnElements
import world.phantasmal.observable.cell.list.mutableListCell
import world.phantasmal.observable.cell.mutableCell
import world.phantasmal.observable.observe
import world.phantasmal.psolib.fileFormats.quest.NpcType
import world.phantasmal.web.core.models.Server
import world.phantasmal.web.core.stores.EnemyDropTable
import world.phantasmal.web.core.stores.ItemDropStore
import world.phantasmal.web.core.stores.ItemTypeStore
import world.phantasmal.web.core.stores.UiStore
import world.phantasmal.web.externals.javascriptLpSolver.Solver
import world.phantasmal.web.huntOptimizer.models.HuntMethodModel
import world.phantasmal.web.huntOptimizer.models.OptimalMethodModel
import world.phantasmal.web.huntOptimizer.models.OptimizationResultModel
import world.phantasmal.web.huntOptimizer.models.WantedItemModel
import world.phantasmal.web.huntOptimizer.persistence.WantedItemPersister
import world.phantasmal.web.shared.dto.Difficulty
import world.phantasmal.web.shared.dto.ItemType
import world.phantasmal.web.shared.dto.SectionId
import world.phantasmal.webui.obj
import world.phantasmal.webui.stores.Store
import kotlin.time.DurationUnit.HOURS

private val logger = KotlinLogging.logger {}

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
    huntMethodStore: HuntMethodStore,
    private val itemTypeStore: ItemTypeStore,
    private val itemDropStore: ItemDropStore,
) : Store() {
    private val _huntableItems = mutableListCell<ItemType>()
    private val _wantedItems = mutableListCell<WantedItemModel>()
    private val _optimizationResult = mutableCell(OptimizationResultModel(emptyList(), emptyList()))
    private var wantedItemsPersistenceObserver: Disposable? = null

    val huntableItems: ListCell<ItemType> by lazy {
        observeNow(uiStore.server) { server ->
            _huntableItems.clear()

            // There's a race condition here.
            scope.launch {
                val dropTable = itemDropStore.getEnemyDropTable(server)

                _huntableItems.value = itemTypeStore.getItemTypes(server).filter {
                    dropTable.getDropsForItemType(it).isNotEmpty()
                }
            }
        }

        _huntableItems
    }

    val wantedItems: ListCell<WantedItemModel> by lazy {
        observeNow(uiStore.server) { loadWantedItems(it) }
        _wantedItems
    }

    val optimizationResult: Cell<OptimizationResultModel> by lazy {
        observeNow(
            _wantedItems.dependingOnElements { arrayOf(it.amount) },
            huntMethodStore.methods.dependingOnElements { arrayOf(it.time) },
        ) { wantedItems, huntMethods ->
            // There's a race condition here.
            scope.launch(Dispatchers.Default) {
                val dropTable = itemDropStore.getEnemyDropTable(uiStore.server.value)
                val result = optimize(wantedItems, huntMethods, dropTable)

                withContext(Dispatchers.Main) {
                    _optimizationResult.value = result
                }
            }
        }

        _optimizationResult
    }

    override fun dispose() {
        wantedItemsPersistenceObserver?.dispose()
        super.dispose()
    }

    fun addWantedItem(itemType: ItemType) {
        if (_wantedItems.value.none { it.itemType == itemType }) {
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
                // Clear the previous wanted items observer, because we don't want it to persist
                // changes using the previous server as key. This way we also avoid an unnecessary
                // persist call right after the loading and replacing the wanted items.
                wantedItemsPersistenceObserver?.dispose()
                wantedItemsPersistenceObserver = null

                _wantedItems.replaceAll(wantedItems)

                // Wanted items are loaded, start observing them and persist whenever they change.
                wantedItemsPersistenceObserver =
                    _wantedItems.dependingOnElements { arrayOf(it.amount) }.observe { items ->
                        scope.launch(Dispatchers.Main) {
                            wantedItemPersister.persistWantedItems(items, server)
                        }
                    }
            }
        }
    }

    private fun optimize(
        wantedItems: List<WantedItemModel>,
        methods: List<HuntMethodModel>,
        dropTable: EnemyDropTable,
    ): OptimizationResultModel {
        logger.debug { "Optimization start." }

        val filteredWantedItems = wantedItems.filter { it.amount.value > 0 }

        if (filteredWantedItems.isEmpty()) {
            logger.debug { "Optimization end, no wanted items to optimize for." }

            return OptimizationResultModel(emptyList(), emptyList())
        }

        // Add a constraint per wanted item.
        val constraints: dynamic = obj {}

        for (wanted in filteredWantedItems) {
            constraints[wanted.itemType.id] = obj { min = wanted.amount.value }
        }

        // Add a variable to the LP model per method per difficulty per section ID.
        // When a method with pan arms is encountered, two variables are added. One for the method
        // with migiums and hidooms and one with pan arms.
        // Each variable has a time property to minimize and a property per item with the number of
        // enemies that drop the item multiplied by the corresponding drop rate as its value.
        val variables: dynamic = obj {}
        // Each variable has a matching FullMethod.
        val fullMethods = UnsafeMap<String, FullMethod>()

        val wantedItemTypeIds = UnsafeSet<Int>()

        for (wanted in filteredWantedItems) {
            wantedItemTypeIds.add(wanted.itemType.id)
        }

        // TODO: Optimize this by not looping over every method, difficulty, section ID and enemy.
        //       Instead, loop over wanted items, look up drops for the given item type and go from
        //       there.
        for (method in methods) {
            // Calculate enemy counts including rare enemies
            // Counts include rare enemies, so they are fractional.
            val counts = UnsafeMap<NpcType, Double>()

            for ((enemyType, count) in method.enemyCounts) {
                val rareEnemyType = enemyType.rareType
                val oldCount = counts.get(enemyType) ?: .0

                if (rareEnemyType == null) {
                    counts.set(enemyType, oldCount + count)
                } else {
                    val rareRate: Double =
                        if (rareEnemyType == NpcType.Kondrieu) KONDRIEU_PROB
                        else RARE_ENEMY_PROB

                    counts.set(enemyType, oldCount + count * (1.0 - rareRate))
                    counts.set(rareEnemyType, (counts.get(rareEnemyType) ?: .0) + count * rareRate)
                }
            }

            // Create fully specified hunt methods and a variable for each of them.
            for (splitPanArms in arrayOf(false, true)) {
                createFullMethods(
                    dropTable,
                    wantedItemTypeIds,
                    method,
                    counts,
                    splitPanArms,
                    variables,
                    fullMethods,
                )
            }
        }

        val optimalMethods = solve(wantedItemTypeIds, constraints, variables, fullMethods)

        logger.debug { "Optimization end." }

        return OptimizationResultModel(filteredWantedItems.map { it.itemType }, optimalMethods)
    }

    private fun createFullMethods(
        dropTable: EnemyDropTable,
        wantedItemTypeIds: UnsafeSet<Int>,
        method: HuntMethodModel,
        defaultCounts: UnsafeMap<NpcType, Double>,
        splitPanArms: Boolean,
        variables: dynamic,
        fullMethods: UnsafeMap<String, FullMethod>,
    ) {
        val counts: UnsafeMap<NpcType, Double>?

        if (splitPanArms) {
            var splitPanArmsCounts: UnsafeMap<NpcType, Double>? = null

            // Create a secondary counts map if there are any pan arms that can be split
            // into migiums and hidooms.
            val panArmsCount = defaultCounts.get(NpcType.PanArms)
            val panArms2Count = defaultCounts.get(NpcType.PanArms2)

            if (panArmsCount != null || panArms2Count != null) {
                splitPanArmsCounts = UnsafeMap()

                if (panArmsCount != null) {
                    splitPanArmsCounts.delete(NpcType.PanArms)
                    splitPanArmsCounts.set(NpcType.Migium, panArmsCount)
                    splitPanArmsCounts.set(NpcType.Hidoom, panArmsCount)
                }

                if (panArms2Count != null) {
                    splitPanArmsCounts.delete(NpcType.PanArms2)
                    splitPanArmsCounts.set(NpcType.Migium2, panArms2Count)
                    splitPanArmsCounts.set(NpcType.Hidoom2, panArms2Count)
                }
            }

            counts = splitPanArmsCounts
        } else {
            counts = defaultCounts
        }

        if (counts != null) {
            for (difficulty in Difficulty.VALUES) {
                for (sectionId in SectionId.VALUES) {
                    // Will contain an entry per wanted item dropped by enemies in this method/
                    // difficulty/section ID combo.
                    val time = method.time.value.toDouble(HOURS)
                    val variable: dynamic = obj { this.time = time }
                    // Only add the variable if the method provides at least 1 item we want.
                    var addVariable = false

                    counts.forEach { count, npcType ->
                        dropTable.getDrop(difficulty, sectionId, npcType)?.let { drop ->
                            if (wantedItemTypeIds.has(drop.itemTypeId)) {
                                val oldValue = variable[drop.itemTypeId] ?: .0
                                variable[drop.itemTypeId] = oldValue + count * drop.dropRate
                                addVariable = true
                            }
                        }
                    }

                    if (addVariable) {
                        val fullMethod = FullMethod(method, difficulty, sectionId, splitPanArms)
                        variables[fullMethod.variableName] = variable
                        fullMethods.set(fullMethod.variableName, fullMethod)
                    }
                }
            }
        }
    }

    private fun solve(
        wantedItemTypeIds: UnsafeSet<Int>,
        constraints: dynamic,
        variables: dynamic,
        fullMethods: UnsafeMap<String, FullMethod>,
    ): List<OptimalMethodModel> {
        val result = Solver.Solve(obj {
            optimize = "time"
            opType = "min"
            this.constraints = constraints
            this.variables = variables
        })

        if (!result.feasible.unsafeCast<Boolean>()) {
            return emptyList()
        }

        // Loop over the entries in result, ignore standard properties that aren't variables.
        return JsObject.entries(result).mapNotNull { (variableName, runsOrOther) ->
            fullMethods.get(variableName)?.let { fullMethod ->
                val runs = runsOrOther as Double
                val variable = variables[variableName]

                val itemTypeIdToCount: Map<Int, Double> =
                    JsObject.entries(variable)
                        .mapNotNull { (itemTypeIdStr, expectedAmount) ->
                            itemTypeIdStr.toIntOrNull()?.let { itemTypeId ->
                                if (wantedItemTypeIds.has(itemTypeId)) {
                                    Pair(itemTypeId, runs * (expectedAmount as Double))
                                } else {
                                    null
                                }
                            }
                        }
                        .toMap()

                check(itemTypeIdToCount.isNotEmpty()) {
                    """Item counts map for variable "$variableName" was empty."""
                }

                // Find all section IDs that provide the same items with the same expected amount.
                // E.g. if you need a spread needle and a bringer's right arm, using either
                // purplenum or yellowboze will give you the exact same probabilities.
                val sectionIds = mutableListOf<SectionId>()

                for (sectionId in SectionId.VALUES) {
                    var matchFound = true

                    if (sectionId != fullMethod.sectionId) {
                        val v = variables[getVariableName(
                            fullMethod.difficulty,
                            sectionId,
                            fullMethod.method,
                            fullMethod.splitPanArms,
                        )]

                        if (v == null) {
                            matchFound = false
                        } else {
                            for ((itemName, expectedAmount) in JsObject.entries(variable)) {
                                if (v[itemName] != expectedAmount.unsafeCast<Double>()) {
                                    matchFound = false
                                    break
                                }
                            }
                        }
                    }

                    if (matchFound) {
                        sectionIds.add(sectionId)
                    }
                }

                val method = fullMethod.method

                val methodName = buildString {
                    append(method.name)

                    if (fullMethod.splitPanArms) {
                        append(" (Split Pan Arms)")
                    }
                }

                OptimalMethodModel(
                    fullMethod.difficulty,
                    sectionIds,
                    methodName,
                    method.episode,
                    method.time.value,
                    runs,
                    itemTypeIdToCount,
                )
            }
        }
    }

    /**
     * Describes a fully specified hunt method.
     */
    private data class FullMethod(
        val method: HuntMethodModel,
        val difficulty: Difficulty,
        val sectionId: SectionId,
        val splitPanArms: Boolean,
    ) {
        val variableName: String =
            getVariableName(difficulty, sectionId, method, splitPanArms)
    }

    companion object {
        private const val RARE_ENEMY_PROB = 1.0 / 512.0
        private const val KONDRIEU_PROB = 1.0 / 10.0

        private fun getVariableName(
            difficulty: Difficulty,
            sectionId: SectionId,
            method: HuntMethodModel,
            splitPanArms: Boolean,
        ): String =
            "$difficulty\t$sectionId\t${method.id}\t$splitPanArms"
    }
}

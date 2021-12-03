package world.phantasmal.web.huntOptimizer.controllers

import world.phantasmal.observable.cell.list.ListCell
import world.phantasmal.observable.cell.list.filtered
import world.phantasmal.observable.cell.list.listCell
import world.phantasmal.observable.cell.list.sortedWith
import world.phantasmal.observable.cell.mutableCell
import world.phantasmal.psolib.Episode
import world.phantasmal.psolib.fileFormats.quest.NpcType
import world.phantasmal.web.huntOptimizer.models.HuntMethodModel
import world.phantasmal.web.huntOptimizer.stores.HuntMethodStore
import world.phantasmal.webui.LoadingStatusCell
import world.phantasmal.webui.controllers.Column
import world.phantasmal.webui.controllers.SortColumn
import world.phantasmal.webui.controllers.SortDirection
import world.phantasmal.webui.controllers.TableController
import kotlin.time.Duration

class MethodsForEpisodeController(
    private val huntMethodStore: HuntMethodStore,
    episode: Episode,
) : TableController<HuntMethodModel>() {
    private val enemies: List<NpcType> = NpcType.VALUES.filter { it.enemy && it.episode == episode }

    private val comparator = mutableCell(Comparator<HuntMethodModel> { _, _ -> 0 })

    override val fixedColumns = 2

    override val values: ListCell<HuntMethodModel> by lazy {
        huntMethodStore.methods
            .filtered { it.episode == episode }
            .sortedWith(comparator)
    }

    override val loadingStatus: LoadingStatusCell = huntMethodStore.methodsStatus

    override val columns: ListCell<Column<HuntMethodModel>> = listCell(
        Column(
            key = METHOD_COL_KEY,
            title = "Method",
            width = 250,
            sortable = true,
        ),
        Column(
            key = TIME_COL_KEY,
            title = "Time",
            width = 50,
            input = true,
            sortable = true,
        ),
        *enemies.map { enemy ->
            // Word-wrap long names.
            val title = when (enemy) {
                NpcType.Gigobooma -> "Gigo-\nbooma"
                NpcType.Shambertin -> "Shamber-\ntin"
                else -> enemy.simpleName
            }
            Column<HuntMethodModel>(
                key = enemy.name,
                title = title,
                width = 70,
                headerClassName = "pw-hunt-optimizer-methods-for-episode-header-cell",
                className = "pw-hunt-optimizer-methods-for-episode-cell",
                sortable = true,
                textAlign = "right",
            )
        }.toTypedArray()
    )

    override fun sort(sortColumns: List<SortColumn<HuntMethodModel>>) {
        comparator.value = Comparator { a, b ->
            for (sortColumn in sortColumns) {
                val cmp = when (sortColumn.column.key) {
                    METHOD_COL_KEY ->
                        a.name.asDynamic().localeCompare(b.name).unsafeCast<Int>()

                    TIME_COL_KEY -> a.time.value.compareTo(b.time.value)

                    else -> {
                        val type = NpcType.valueOf(sortColumn.column.key)
                        (a.enemyCounts[type] ?: 0) - (b.enemyCounts[type] ?: 0)
                    }
                }

                if (cmp != 0) {
                    return@Comparator if (sortColumn.direction == SortDirection.Asc) cmp else -cmp
                }
            }

            0
        }
    }

    suspend fun setMethodTime(method: HuntMethodModel, time: Duration) {
        huntMethodStore.setMethodTime(method, time)
    }

    companion object {
        const val METHOD_COL_KEY = "method"
        const val TIME_COL_KEY = "time"
    }
}

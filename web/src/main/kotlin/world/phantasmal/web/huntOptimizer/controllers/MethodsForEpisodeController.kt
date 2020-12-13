package world.phantasmal.web.huntOptimizer.controllers

import world.phantasmal.lib.Episode
import world.phantasmal.lib.fileFormats.quest.NpcType
import world.phantasmal.observable.value.list.ListVal
import world.phantasmal.observable.value.list.mutableListVal
import world.phantasmal.web.huntOptimizer.models.HuntMethodModel
import world.phantasmal.web.huntOptimizer.stores.HuntMethodStore
import world.phantasmal.webui.controllers.Column
import world.phantasmal.webui.controllers.SortColumn
import world.phantasmal.webui.controllers.SortDirection
import world.phantasmal.webui.controllers.TableController
import kotlin.time.Duration

class MethodsForEpisodeController(
    private val huntMethodStore: HuntMethodStore,
    episode: Episode,
) : TableController<HuntMethodModel>() {
    private val methods = mutableListVal<HuntMethodModel>()
    private val enemies: List<NpcType> = NpcType.VALUES.filter { it.enemy && it.episode == episode }

    override val values: ListVal<HuntMethodModel> = methods

    override val columns: List<Column<HuntMethodModel>> = listOf(
        Column(
            key = METHOD_COL_KEY,
            title = "Method",
            fixed = true,
            width = 250,
            sortable = true,
        ),
        Column(
            key = TIME_COL_KEY,
            title = "Time",
            fixed = true,
            width = 60,
            input = true,
            sortable = true,
        ),
        *enemies.map { enemy ->
            Column<HuntMethodModel>(
                key = enemy.name,
                title = enemy.simpleName,
                width = 90,
                textAlign = "right",
                sortable = true,
            )
        }.toTypedArray()
    )

    init {
        observe(huntMethodStore.methods) { allMethods ->
            methods.value = allMethods.filter { it.episode == episode }
        }
    }

    override fun sort(sortColumns: List<SortColumn<HuntMethodModel>>) {
        methods.sortWith { a, b ->
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
                    return@sortWith if (sortColumn.direction == SortDirection.Asc) cmp else -cmp
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

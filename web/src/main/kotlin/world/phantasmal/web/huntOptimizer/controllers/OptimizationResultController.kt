package world.phantasmal.web.huntOptimizer.controllers

import world.phantasmal.observable.value.list.ListVal
import world.phantasmal.observable.value.toListVal
import world.phantasmal.observable.value.value
import world.phantasmal.web.huntOptimizer.models.OptimalMethodModel
import world.phantasmal.web.huntOptimizer.stores.HuntOptimizerStore
import world.phantasmal.webui.controllers.Column
import world.phantasmal.webui.controllers.TableController
import world.phantasmal.webui.toRoundedString
import kotlin.time.Duration

class OptimizationResultController(
    huntOptimizerStore: HuntOptimizerStore,
) : TableController<OptimalMethodModel>() {
    override val fixedColumns = 4
    override val hasFooter = true

    override val values: ListVal<OptimalMethodModel> =
        huntOptimizerStore.optimizationResult.map { it.optimalMethods }.toListVal()

    override val columns: ListVal<Column<OptimalMethodModel>> =
        huntOptimizerStore.optimizationResult.map { result ->
            var totalRuns = .0
            var totalTime = Duration.ZERO

            for (optimalMethod in result.optimalMethods) {
                totalRuns += optimalMethod.runs
                totalTime += optimalMethod.totalTime
            }

            listOf<Column<OptimalMethodModel>>(
                Column(
                    key = DIFF_COL,
                    title = "Difficulty",
                    width = 80,
                    footer = value("Totals:"),
                ),
                Column(
                    key = METHOD_COL,
                    title = "Method",
                    width = 250,
                ),
                Column(
                    key = EPISODE_COL,
                    title = "Ep.",
                    width = 40,
                ),
                Column(
                    key = SECTION_ID_COL,
                    title = "Section ID",
                    width = 90,
                ),
                Column(
                    key = TIME_PER_RUN_COL,
                    title = "Time/Run",
                    width = 90,
                    textAlign = "center",
                ),
                Column(
                    key = RUNS_COL,
                    title = "Runs",
                    width = 60,
                    textAlign = "right",
                    tooltip = { it.runs.toString() },
                    footer = value(totalRuns.toRoundedString(1)),
                    footerTooltip = value(totalRuns.toString()),
                ),
                Column(
                    key = TOTAL_TIME_COL,
                    title = "Total Hours",
                    width = 60,
                    textAlign = "right",
                    tooltip = { it.totalTime.inHours.toString() },
                    footer = value(totalTime.inHours.toRoundedString(1)),
                    footerTooltip = value(totalTime.inHours.toString()),
                ),
                *Array(result.wantedItems.size) { index ->
                    val wanted = result.wantedItems[index]
                    val totalCount = result.optimalMethods.sumByDouble {
                        it.itemTypeIdToCount[wanted.id] ?: .0
                    }

                    Column(
                        key = wanted.id.toString(),
                        title = wanted.name,
                        width = 80,
                        textAlign = "right",
                        tooltip = { it.itemTypeIdToCount[wanted.id]?.toString() },
                        footer = value(totalCount.toRoundedString(2)),
                        footerTooltip = value(totalCount.toString()),
                    )
                },
            )
        }.toListVal()

    companion object {
        const val DIFF_COL = "diff"
        const val METHOD_COL = "method"
        const val EPISODE_COL = "episode"
        const val SECTION_ID_COL = "section_id"
        const val TIME_PER_RUN_COL = "time_per_run"
        const val RUNS_COL = "runs"
        const val TOTAL_TIME_COL = "total_time"
    }
}

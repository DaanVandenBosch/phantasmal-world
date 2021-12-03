package world.phantasmal.web.huntOptimizer.controllers

import world.phantasmal.observable.cell.cell
import world.phantasmal.observable.cell.list.ListCell
import world.phantasmal.observable.cell.list.mapToList
import world.phantasmal.web.huntOptimizer.models.OptimalMethodModel
import world.phantasmal.web.huntOptimizer.stores.HuntOptimizerStore
import world.phantasmal.webui.controllers.Column
import world.phantasmal.webui.controllers.TableController
import world.phantasmal.webui.toRoundedString
import kotlin.time.Duration
import kotlin.time.DurationUnit.HOURS

class OptimizationResultController(
    huntOptimizerStore: HuntOptimizerStore,
) : TableController<OptimalMethodModel>() {
    override val fixedColumns = 4
    override val hasFooter = true

    override val values: ListCell<OptimalMethodModel> =
        huntOptimizerStore.optimizationResult.mapToList { it.optimalMethods }

    override val columns: ListCell<Column<OptimalMethodModel>> =
        huntOptimizerStore.optimizationResult.mapToList { result ->
            var totalRuns = .0
            var totalTime = Duration.ZERO

            for (optimalMethod in result.optimalMethods) {
                totalRuns += optimalMethod.runs
                totalTime += optimalMethod.totalTime
            }

            listOf(
                Column(
                    key = DIFF_COL,
                    title = "Difficulty",
                    width = 80,
                    footer = cell("Totals:"),
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
                    footer = cell(totalRuns.toRoundedString(1)),
                    footerTooltip = cell(totalRuns.toString()),
                ),
                Column(
                    key = TOTAL_TIME_COL,
                    title = "Total Hours",
                    width = 60,
                    textAlign = "right",
                    tooltip = { it.totalTime.toDouble(HOURS).toString() },
                    footer = cell(totalTime.toDouble(HOURS).toRoundedString(1)),
                    footerTooltip = cell(totalTime.toDouble(HOURS).toString()),
                ),
                *Array(result.wantedItems.size) { index ->
                    val wanted = result.wantedItems[index]
                    val totalCount = result.optimalMethods.sumOf {
                        it.itemTypeIdToCount[wanted.id] ?: .0
                    }

                    Column(
                        key = wanted.id.toString(),
                        title = wanted.name,
                        width = 80,
                        textAlign = "right",
                        tooltip = { it.itemTypeIdToCount[wanted.id]?.toString() },
                        footer = cell(totalCount.toRoundedString(2)),
                        footerTooltip = cell(totalCount.toString()),
                    )
                },
            )
        }

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

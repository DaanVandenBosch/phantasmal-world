package world.phantasmal.web.huntOptimizer.controllers

import world.phantasmal.observable.value.list.ListVal
import world.phantasmal.observable.value.toListVal
import world.phantasmal.web.huntOptimizer.models.OptimalMethodModel
import world.phantasmal.web.huntOptimizer.stores.HuntOptimizerStore
import world.phantasmal.webui.controllers.Column
import world.phantasmal.webui.controllers.TableController

class OptimizationResultController(
    huntOptimizerStore: HuntOptimizerStore,
) : TableController<OptimalMethodModel>() {
    override val fixedColumns: Int = 4

    override val values: ListVal<OptimalMethodModel> =
        huntOptimizerStore.optimizationResult.map { it.optimalMethods }.toListVal()

    override val columns: ListVal<Column<OptimalMethodModel>> =
        huntOptimizerStore.optimizationResult.map {
            listOf<Column<OptimalMethodModel>>(
                Column(
                    key = DIFF_COL,
                    title = "Difficulty",
                    width = 80,
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
                    tooltip = { it.runs.toString() }
                ),
                Column(
                    key = TOTAL_TIME_COL,
                    title = "Total Hours",
                    width = 60,
                    textAlign = "right",
                    tooltip = { it.totalTime.inHours.toString() }
                ),
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

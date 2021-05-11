package world.phantasmal.web.huntOptimizer.widgets

import org.w3c.dom.Node
import world.phantasmal.web.core.dom.sectionIdIcon
import world.phantasmal.web.huntOptimizer.controllers.OptimizationResultController
import world.phantasmal.web.huntOptimizer.controllers.OptimizationResultController.Companion.DIFF_COL
import world.phantasmal.web.huntOptimizer.controllers.OptimizationResultController.Companion.EPISODE_COL
import world.phantasmal.web.huntOptimizer.controllers.OptimizationResultController.Companion.METHOD_COL
import world.phantasmal.web.huntOptimizer.controllers.OptimizationResultController.Companion.RUNS_COL
import world.phantasmal.web.huntOptimizer.controllers.OptimizationResultController.Companion.SECTION_ID_COL
import world.phantasmal.web.huntOptimizer.controllers.OptimizationResultController.Companion.TIME_PER_RUN_COL
import world.phantasmal.web.huntOptimizer.controllers.OptimizationResultController.Companion.TOTAL_TIME_COL
import world.phantasmal.webui.dom.div
import world.phantasmal.webui.dom.dom
import world.phantasmal.webui.dom.h2
import world.phantasmal.webui.dom.span
import world.phantasmal.webui.formatAsHoursAndMinutes
import world.phantasmal.webui.toRoundedString
import world.phantasmal.webui.widgets.Table
import world.phantasmal.webui.widgets.Widget

class OptimizationResultWidget(private val ctrl: OptimizationResultController) : Widget() {
    override fun Node.createElement() =
        div {
            className = "pw-hunt-optimizer-optimization-result"

            h2 { textContent = "Ideal Combination of Methods" }

            addWidget(Table(
                ctrl = ctrl,
                className = "pw-hunt-optimizer-optimization-result-table",
                renderCell = { optimalMethod, column ->
                    when (column.key) {
                        DIFF_COL -> optimalMethod.difficulty
                        METHOD_COL -> optimalMethod.name
                        EPISODE_COL -> optimalMethod.episode
                        SECTION_ID_COL -> dom {
                            span {
                                style.display = "flex"

                                for (sectionId in optimalMethod.sectionIds) {
                                    sectionIdIcon(sectionId, size = 17)
                                }
                            }
                        }
                        TIME_PER_RUN_COL -> optimalMethod.methodTime.formatAsHoursAndMinutes()
                        RUNS_COL -> optimalMethod.runs.toRoundedString(1)
                        TOTAL_TIME_COL -> optimalMethod.totalTime.inHours.toRoundedString(1)
                        else -> {
                            optimalMethod.itemTypeIdToCount[column.key.toInt()]
                                ?.toRoundedString(2)
                                ?: ""
                        }
                    }
                },
            ))
        }

    companion object {
        init {
            @Suppress("CssUnusedSymbol", "CssUnresolvedCustomProperty")
            // language=css
            style("""
                .pw-hunt-optimizer-optimization-result {
                    flex-grow: 1;
                    display: flex;
                    flex-direction: column;
                    align-items: stretch;
                    overflow: hidden;
                }
                
                .pw-hunt-optimizer-optimization-result-table {
                    flex-grow: 1;
                    border-top: var(--pw-border);
                    border-left: var(--pw-border);
                }
            """.trimIndent())
        }
    }
}

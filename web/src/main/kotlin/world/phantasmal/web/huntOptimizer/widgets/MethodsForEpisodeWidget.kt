package world.phantasmal.web.huntOptimizer.widgets

import kotlinx.coroutines.launch
import org.w3c.dom.Node
import world.phantasmal.lib.fileFormats.quest.NpcType
import world.phantasmal.web.huntOptimizer.controllers.MethodsForEpisodeController
import world.phantasmal.web.huntOptimizer.controllers.MethodsForEpisodeController.Companion.METHOD_COL_KEY
import world.phantasmal.web.huntOptimizer.controllers.MethodsForEpisodeController.Companion.TIME_COL_KEY
import world.phantasmal.webui.dom.div
import world.phantasmal.webui.widgets.DurationInput
import world.phantasmal.webui.widgets.Table
import world.phantasmal.webui.widgets.Widget

class MethodsForEpisodeWidget(private val ctrl: MethodsForEpisodeController) : Widget() {
    override fun Node.createElement() =
        div {
            className = "pw-hunt-optimizer-methods-for-episode"

            addChild(Table(
                ctrl = ctrl,
                renderCell = { method, column ->
                    when (column.key) {
                        METHOD_COL_KEY -> method.name
                        TIME_COL_KEY -> DurationInput(
                            value = method.time,
                            onChange = { scope.launch { ctrl.setMethodTime(method, it) } }
                        )
                        else -> method.enemyCounts[NpcType.valueOf(column.key)]?.toString() ?: ""
                    }
                }
            ))
        }

    companion object {
        init {
            @Suppress("CssUnusedSymbol")
            // language=css
            style("""
                .pw-hunt-optimizer-methods-for-episode {
                    display: grid;
                    grid-template: 100% / 100%;
                    overflow: hidden;
                }
                
                .pw-hunt-optimizer-methods-for-episode-header-cell {
                    font-size: 10px;
                    padding: 2px;
                }
                
                .pw-hunt-optimizer-methods-for-episode-cell {
                    text-align: right;
                }
            """.trimIndent())
        }
    }
}

package world.phantasmal.web.huntOptimizer.widgets

import org.w3c.dom.Node
import world.phantasmal.web.huntOptimizer.controllers.MethodsForEpisodeController
import world.phantasmal.web.huntOptimizer.models.HuntMethodModel
import world.phantasmal.webui.dom.div
import world.phantasmal.webui.widgets.Column
import world.phantasmal.webui.widgets.Table
import world.phantasmal.webui.widgets.Widget

class MethodsForEpisodeWidget(private val ctrl: MethodsForEpisodeController) : Widget() {
    override fun Node.createElement() =
        div {
            className = "pw-hunt-optimizer-methods-for-episode"

            addChild(
                Table(
                    values = ctrl.methods,
                    columns = listOf(
                        Column(
                            title = "Method",
                            fixed = true,
                            width = 250,
                            renderCell = { it.name },
                        ),
                        Column(
                            title = "Time",
                            fixed = true,
                            width = 60,
                            renderCell = { it.time.value.toIsoString() },
                        ),
                        *ctrl.enemies.map { enemy ->
                            Column<HuntMethodModel>(
                                title = enemy.simpleName,
                                width = 90,
                                renderCell = { 69 }
                            )
                        }.toTypedArray()
                    ),
                )
            )
        }

    companion object {
        init {
            @Suppress("CssUnusedSymbol")
            // language=css
            style("""
                .pw-hunt-optimizer-methods-for-episode {
                    display: grid;
                    grid-template-rows: 100%;
                    grid-template-columns: 100%;
                    overflow: hidden;
                }
            """.trimIndent())
        }
    }
}

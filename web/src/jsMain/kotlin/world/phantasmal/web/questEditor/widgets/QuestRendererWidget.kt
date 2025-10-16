package world.phantasmal.web.questEditor.widgets

import org.w3c.dom.Node
import world.phantasmal.cell.Cell
import world.phantasmal.cell.map
import world.phantasmal.web.core.widgets.RendererWidget
import world.phantasmal.web.externals.three.Vector3
import world.phantasmal.web.questEditor.rendering.QuestRenderer
import world.phantasmal.webui.dom.div
import world.phantasmal.webui.widgets.Label
import world.phantasmal.webui.widgets.Widget

abstract class QuestRendererWidget(
    private val renderer: QuestRenderer,
    private val mouseWorldPosition: Cell<Vector3?>,
) : Widget() {
    override fun Node.createElement() =
        div {
            className = "pw-quest-editor-quest-renderer"

            addChild(RendererWidget(renderer))

            addChild(
                Label(
                textCell = mouseWorldPosition.map { position ->
                    if (position != null) {
                        "World Position: (${position.x.asDynamic().toFixed(1)}, ${
                            position.y.asDynamic().toFixed(1)
                        }, ${position.z.asDynamic().toFixed(1)})"
                    } else {
                        "World Position: (--, --, --)"
                    }
                }
            ).apply {
                element.className += " pw-quest-editor-world-coordinates"
            })
        }

    companion object {
        init {
            @Suppress("CssUnusedSymbol")
            // language=css
            style("""
                .pw-quest-editor-quest-renderer {
                    position: relative;
                    display: flex;
                    overflow: hidden;
                }
                .pw-quest-editor-quest-renderer > * {
                    flex-grow: 1;
                }
                .pw-quest-editor-world-coordinates {
                    position: absolute;
                    top: 8px;
                    left: 8px;
                    font-family: monospace;
                    font-size: 12px;
                    color: #fff;
                    background: rgba(0, 0, 0, 0.7);
                    padding: 4px 8px;
                    border-radius: 4px;
                    pointer-events: none;
                    z-index: 1000;
                    white-space: nowrap;
                }
            """.trimIndent())
        }
    }
}

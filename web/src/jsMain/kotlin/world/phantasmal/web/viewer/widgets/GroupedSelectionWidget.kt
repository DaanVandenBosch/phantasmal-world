package world.phantasmal.web.viewer.widgets

import org.w3c.dom.Node
import world.phantasmal.cell.Cell
import world.phantasmal.cell.eq
import world.phantasmal.web.viewer.models.ViewerModel
import world.phantasmal.webui.dom.li
import world.phantasmal.webui.dom.ul
import world.phantasmal.webui.widgets.Widget

class GroupedSelectionWidget(
    private val groups: List<ViewerModel.Group>,
    private val selected: Cell<ViewerModel?>,
    private val onSelect: (ViewerModel) -> Unit,
) : Widget() {
    override fun Node.createElement() =
        ul {
            className = "pw-viewer-selection"

            for (group in groups) {
                li {
                    className = "pw-viewer-selection-group-header"
                    textContent = group.label
                }

                for (item in group.items) {
                    li {
                        className = "pw-viewer-selection-item"
                        textContent = item.uiName

                        toggleClass("pw-active", selected eq item)

                        onclick = { onSelect(item) }
                    }
                }
            }
        }

    companion object {
        init {
            @Suppress("CssUnusedSymbol")
            // language=css
            style("""
                .pw-viewer-selection-group-header {
                    padding: 6px 8px 2px;
                    white-space: nowrap;
                    font-size: 11px;
                    font-weight: bold;
                    text-transform: uppercase;
                    color: hsl(0, 0%, 55%);
                    user-select: none;
                }
            """.trimIndent())
        }
    }
}

package world.phantasmal.web.viewer.widgets

import org.w3c.dom.Node
import world.phantasmal.cell.Cell
import world.phantasmal.cell.eq
import world.phantasmal.webui.dom.li
import world.phantasmal.webui.dom.ul
import world.phantasmal.webui.widgets.Widget

class SelectionWidget<T>(
    private val items: List<T>,
    private val selected: Cell<T?>,
    private val onSelect: (T) -> Unit,
    private val itemToString: (T) -> String,
    private val borderLeft: Boolean = false,
) : Widget() {
    override fun Node.createElement() =
        ul {
            className = "pw-viewer-selection"

            if (borderLeft) {
                style.borderLeft = "var(--pw-border)"
            }

            for (item in items) {
                li {
                    className = "pw-viewer-selection-item"
                    textContent = itemToString(item)

                    toggleClass("pw-active", selected eq item)

                    onclick = { onSelect(item) }
                }
            }
        }

    companion object {
        init {
            @Suppress("CssUnusedSymbol")
            // language=css
            style("""
                .pw-viewer-selection {
                    box-sizing: border-box;
                    list-style: none;
                    padding: 0;
                    margin: 0;
                    overflow: auto;
                }

                .pw-viewer-selection-item {
                    padding: 4px 8px;
                    white-space: nowrap; /* Necessary for Firefox. */
                }

                /* Firefox-specific hack to ensure the scrollbar doesn't cover the items. */
                @-moz-document url-prefix() {
                    .pw-viewer-selection-item {
                        padding-right: 24px;
                    }
                }

                .pw-viewer-selection-item:hover {
                    color: hsl(0, 0%, 90%);
                    background-color: hsl(0, 0%, 18%);
                }

                .pw-viewer-selection-item.pw-active {
                    color: hsl(0, 0%, 90%);
                    background-color: hsl(0, 0%, 21%);
                }
            """.trimIndent())
        }
    }
}

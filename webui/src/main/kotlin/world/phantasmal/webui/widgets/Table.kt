package world.phantasmal.webui.widgets

import org.w3c.dom.Node
import world.phantasmal.observable.value.Val
import world.phantasmal.observable.value.list.ListVal
import world.phantasmal.observable.value.trueVal
import world.phantasmal.webui.dom.*

class Column<T>(
    val title: String,
    val fixed: Boolean = false,
    val width: Int,
    val renderCell: (T) -> Any,
)

class Table<T>(
    visible: Val<Boolean> = trueVal(),
    enabled: Val<Boolean> = trueVal(),
    private val values: ListVal<T>,
    private val columns: List<Column<T>>,
) : Widget(visible, enabled) {
    override fun Node.createElement() =
        table {
            className = "pw-table"

            thead {
                tr {
                    var runningWidth = 0

                    for ((index, column) in columns.withIndex()) {
                        th {
                            span { textContent = column.title }

                            if (column.fixed) {
                                style.position = "sticky"
                                style.left = "${runningWidth}px"
                                runningWidth += column.width
                            }

                            style.width = "${column.width}px"
                        }
                    }
                }
            }
            tbody {
                bindChildrenTo(values) { value, index ->
                    tr {
                        var runningWidth = 0

                        for ((index, column) in columns.withIndex()) {
                            (if (column.fixed) ::th else ::td) {
                                append(column.renderCell(value))

                                if (column.fixed) {
                                    classList.add("pw-fixed")
                                    style.left = "${runningWidth}px"
                                    runningWidth += column.width
                                }

                                style.width = "${column.width}px"
                            }
                        }
                    }
                }
            }
        }

    companion object {
        init {
            @Suppress("CssUnresolvedCustomProperty", "CssUnusedSymbol")
            // language=css
            style("""
                .pw-table {
                    position: relative;
                    display: block;
                    box-sizing: border-box;
                    overflow: auto;
                    background-color: var(--pw-bg-color);
                    border-collapse: collapse;
                }

                .pw-table tr {
                    display: flex;
                    align-items: stretch;
                }

                .pw-table thead {
                    position: sticky;
                    display: inline-block;
                    top: 0;
                    z-index: 2;
                }

                .pw-table thead tr {
                    position: sticky;
                    top: 0;
                }

                .pw-table thead th {
                    display: flex;
                    flex-direction: column;
                    justify-content: center;
                    overflow: hidden;
                }

                .pw-table th,
                .pw-table td {
                    box-sizing: border-box;
                    overflow: hidden;
                    text-overflow: ellipsis;
                    padding: 3px 6px;
                    border-right: var(--pw-border);
                    border-bottom: var(--pw-border);
                    background-color: var(--pw-bg-color);
                }

                .pw-table tbody {
                    user-select: text;
                    cursor: text;
                }

                .pw-table tbody th,
                .pw-table tbody td {
                    white-space: nowrap;
                }

                .pw-table tbody th,
                .pw-table tfoot th {
                    text-align: left;
                }

                .pw-table th.pw-fixed {
                    position: sticky;
                    text-align: left;
                }

                .pw-table th.input {
                    padding: 0;
                    overflow: visible;
                }

                .pw-table th.input .pw-duration-input {
                    z-index: 0;
                    height: 100%;
                    width: 100%;
                    border: none;
                }

                .pw-table th.input .pw-duration-input:hover,
                .pw-table th.input .pw-duration-input:focus-within {
                    margin: -1px;
                    height: calc(100% + 2px);
                    width: calc(100% + 2px);
                }

                .pw-table th.input .pw-duration-input:hover {
                    z-index: 4;
                    border: var(--pw-input-border-hover);
                }

                .pw-table th.input .pw-duration-input:focus-within {
                    z-index: 6;
                    border: var(--pw-input-border-focus);
                }
            """.trimIndent())
        }
    }
}

package world.phantasmal.webui.widgets

import org.w3c.dom.Node
import world.phantasmal.core.disposable.Disposer
import world.phantasmal.observable.value.Val
import world.phantasmal.observable.value.trueVal
import world.phantasmal.webui.controllers.Column
import world.phantasmal.webui.controllers.TableController
import world.phantasmal.webui.dom.*

class Table<T>(
    visible: Val<Boolean> = trueVal(),
    enabled: Val<Boolean> = trueVal(),
    private val ctrl: TableController<T>,
    /**
     * Can return a [Widget].
     */
    private val renderCell: (T, Column<T>) -> Any,
) : Widget(visible, enabled) {
    override fun Node.createElement() =
        table {
            className = "pw-table"

            thead {
                tr {
                    className = "pw-table-row pw-table-header-row"

                    var runningWidth = 0

                    for (column in ctrl.columns) {
                        th {
                            className = "pw-table-cell"

                            span { textContent = column.title }

                            if (column.fixed) {
                                style.position = "sticky"
                                style.left = "${runningWidth}px"
                                runningWidth += column.width
                            }

                            style.width = "${column.width}px"

                            if (column.sortable) {
                                onmousedown = { e ->
                                    if (e.buttons.toInt() == 1) {
                                        ctrl.sortByColumn(column)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            tbody {
                bindDisposableChildrenTo(ctrl.values) { value, _ ->
                    val rowDisposer = Disposer()

                    val row = tr {
                        className = "pw-table-row"

                        var runningWidth = 0

                        for (column in ctrl.columns) {
                            (if (column.fixed) ::th else ::td) {
                                className = "pw-table-cell pw-table-body-cell"

                                val child = renderCell(value, column)

                                if (child is Widget) {
                                    rowDisposer.add(child)
                                    append(child.element)
                                } else {
                                    append(child)
                                }

                                if (column.input) {
                                    classList.add("pw-table-cell-input")
                                }

                                if (column.fixed) {
                                    classList.add("pw-table-cell-fixed")
                                    style.left = "${runningWidth}px"
                                    runningWidth += column.width
                                }

                                style.width = "${column.width}px"

                                column.textAlign?.let { style.textAlign = it }
                                column.tooltip?.let { title = it(value) }
                            }
                        }
                    }

                    Pair(row, rowDisposer)
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

                .pw-table > thead {
                    position: sticky;
                    display: inline-block;
                    top: 0;
                    z-index: 2;
                }

                .pw-table > tbody {
                    user-select: text;
                    cursor: text;
                }

                .pw-table-row {
                    display: flex;
                    align-items: stretch;
                }

                .pw-table-header-row {
                    position: sticky;
                    top: 0;
                }

                .pw-table-header-row > th {
                    display: flex;
                    flex-direction: column;
                    justify-content: center;
                    overflow: hidden;
                }

                .pw-table-cell {
                    box-sizing: border-box;
                    overflow: hidden;
                    text-overflow: ellipsis;
                    padding: 3px 6px;
                    border-right: var(--pw-border);
                    border-bottom: var(--pw-border);
                    background-color: var(--pw-bg-color);
                }

                .pw-table-body-cell {
                    white-space: nowrap;
                }

                .pw-table-body-cell,
                .pw-table-footer-cell {
                    text-align: left;
                }

                .pw-table-cell-fixed {
                    position: sticky;
                    text-align: left;
                }

                .pw-table-cell-input {
                    padding: 0;
                    overflow: visible;
                }

                .pw-table-cell-input > .pw-input {
                    z-index: 0;
                    height: 100%;
                    width: 100%;
                    border: none;
                }

                .pw-table-cell-input > .pw-input:hover,
                .pw-table-cell-input > .pw-input:focus-within {
                    margin: -1px;
                    height: calc(100% + 2px);
                    width: calc(100% + 2px);
                }

                .pw-table-cell-input > .pw-input:hover {
                    z-index: 4;
                    border: var(--pw-input-border-hover);
                }

                .pw-table-cell-input > .pw-input:focus-within {
                    z-index: 6;
                    border: var(--pw-input-border-focus);
                }
            """.trimIndent())
        }
    }
}

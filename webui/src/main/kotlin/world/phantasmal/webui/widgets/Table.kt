package world.phantasmal.webui.widgets

import org.w3c.dom.*
import world.phantasmal.core.disposable.Disposable
import world.phantasmal.core.disposable.Disposer
import world.phantasmal.observable.cell.Cell
import world.phantasmal.observable.cell.observeNow
import world.phantasmal.observable.cell.trueCell
import world.phantasmal.webui.LoadingStatus
import world.phantasmal.webui.controllers.Column
import world.phantasmal.webui.controllers.TableController
import world.phantasmal.webui.dom.*

class Table<T>(
    visible: Cell<Boolean> = trueCell(),
    enabled: Cell<Boolean> = trueCell(),
    private val ctrl: TableController<T>,
    private val className: String? = null,
    /**
     * Can return a [Widget].
     */
    private val renderCell: (T, Column<T>) -> Any,
) : Widget(visible, enabled) {
    override fun Node.createElement() =
        table {
            className = "pw-table"

            this@Table.className?.let { classList.add(it) }

            ctrl.loadingStatus?.let { loadingStatus ->
                div {
                    className = "pw-table-notification"

                    observeNow(loadingStatus) { status ->
                        when (status) {
                            LoadingStatus.Uninitialized,
                            LoadingStatus.InitialLoad,
                            -> {
                                hidden = false
                                innerText = "Loading..."
                            }
                            LoadingStatus.Error -> {
                                hidden = false
                                innerText = "An error occurred while loading this table."
                            }
                            else -> {
                                hidden = true
                            }
                        }
                    }
                }
            }

            thead {
                tr {
                    className = "pw-table-row pw-table-header-row"

                    addDisposable(bindChildrenTo(
                        this,
                        ctrl.columns,
                        createChild = { column, _ ->
                            createHeaderRowCell(column)
                        },
                        after = {
                            positionFixedColumns(row = this, headerRow = true)
                        },
                    ))
                }
            }
            tbody {
                bindDisposableChildrenTo(ctrl.values) { value, _ ->
                    val rowDisposer = Disposer()

                    val row = tr {
                        className = "pw-table-row"

                        addDisposable(bindChildrenTo(
                            this,
                            ctrl.columns,
                            createChild = { column, _ ->
                                createRowCell(column, value, rowDisposer)
                            },
                            after = {
                                positionFixedColumns(row = this, headerRow = false)
                            },
                        ))
                    }

                    Pair(row, rowDisposer)
                }
            }

            if (ctrl.hasFooter) {
                tfoot {
                    tr {
                        className = "pw-table-row pw-table-footer-row"

                        addDisposable(bindDisposableChildrenTo(
                            this,
                            ctrl.columns,
                            createChild = { column, _ ->
                                createFooterRowCell(column)
                            },
                            after = {
                                positionFixedColumns(row = this, headerRow = false)
                            },
                        ))
                    }
                }
            }
        }

    private fun Node.createHeaderRowCell(column: Column<T>): HTMLTableCellElement =
        th {
            className = "pw-table-cell pw-table-header-cell"

            column.headerClassName?.let { classList.add(it) }

            textContent = column.title

            style.width = "${column.width}px"

            if (column.sortable) {
                onmousedown = { e ->
                    if (e.buttons.toInt() == 1) {
                        ctrl.sortByColumn(column)
                    }
                }
            }
        }

    private fun Node.createRowCell(
        column: Column<T>,
        value: T,
        rowDisposer: Disposer,
    ): HTMLTableCellElement =
        td {
            className = "pw-table-cell pw-table-body-cell"

            column.className?.let { classList.add(it) }

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

            style.width = "${column.width}px"
            column.tooltip(value)?.let { title = it }
            column.textAlign?.let { style.textAlign = it }
        }

    private fun Node.createFooterRowCell(
        column: Column<T>,
    ): Pair<HTMLTableCellElement, Disposable> {
        val disposer = Disposer()

        val cell = th {
            className = "pw-table-cell pw-table-footer-cell"

            style.width = "${column.width}px"
            column.textAlign?.let { style.textAlign = it }

            disposer.add(column.footer.observeNow { textContent = it ?: "" })
            disposer.add(column.footerTooltip.observeNow { title = it ?: "" })
        }

        return Pair(cell, disposer)
    }

    private fun positionFixedColumns(row: HTMLTableRowElement, headerRow: Boolean) {
        val columns = ctrl.columns.value
        var left = 0

        for (index in columns.indices) {
            val el = row.children[index].unsafeCast<HTMLElement>()

            if (index < ctrl.fixedColumns) {
                el.style.position = "sticky"
                el.style.left = "${left}px"

                if (!headerRow) {
                    el.classList.add("pw-table-cell-fixed")
                }
            } else {
                el.style.position = ""
            }

            left += columns[index].width
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

                .pw-table-notification {
                    position: absolute;
                    top: 0;
                    bottom: 0;
                    left: 0;
                    right: 0;
                    display: grid;
                    grid-template: 100% / 100%;
                    place-items: center;
                    text-align: center;
                    color: var(--pw-text-color-disabled);
                    font-size: 20px;
                }

                .pw-table > thead {
                    position: sticky;
                    display: inline-block;
                    top: 0;
                    z-index: 2;
                }

                .pw-table > tbody, .pw-table > tfoot {
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

                .pw-table-header-cell {
                    font-weight: bold;
                    display: flex;
                    flex-direction: column;
                    justify-content: center;
                    overflow: hidden;
                }

                .pw-table-footer-cell {
                    font-weight: bold;
                    text-align: left;
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

                .pw-table-body-cell {
                    text-align: left;
                }

                .pw-table-cell-fixed {
                    font-weight: bold;
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

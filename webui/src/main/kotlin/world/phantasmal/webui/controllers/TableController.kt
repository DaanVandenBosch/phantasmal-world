package world.phantasmal.webui.controllers

import world.phantasmal.observable.cell.Cell
import world.phantasmal.observable.cell.list.ListCell
import world.phantasmal.observable.cell.nullCell

class Column<T>(
    val key: String,
    val title: String,
    val width: Int,
    /**
     * Whether cells in this column contain an input widget.
     */
    val input: Boolean = false,
    val tooltip: (T) -> String? = { null },
    val sortable: Boolean = false,
    val headerClassName: String? = null,
    val className: String? = null,
    val textAlign: String? = null,
    val footer: Cell<String?> = nullCell(),
    val footerTooltip: Cell<String?> = nullCell(),
)

enum class SortDirection {
    Asc,
    Desc,
}

interface SortColumn<T> {
    val column: Column<T>
    val direction: SortDirection
}

abstract class TableController<T> : Controller() {
    private val sortColumns: MutableList<SortColumnImpl> = mutableListOf()

    /**
     * How many columns stay in place on the left side while scrolling.
     */
    open val fixedColumns: Int = 0
    open val hasFooter: Boolean = false

    abstract val values: ListCell<T>
    abstract val columns: ListCell<Column<T>>

    open fun sort(sortColumns: List<SortColumn<T>>) {
        error("Not sortable.")
    }

    fun sortByColumn(column: Column<T>) {
        require(column.sortable) { "Column ${column.key} should be sortable." }

        val index = sortColumns.indexOfFirst { it.column == column }

        if (index == 0) {
            val sc = sortColumns[index]
            sc.direction =
                if (sc.direction == SortDirection.Asc) SortDirection.Desc else SortDirection.Asc
        } else {
            if (index != -1) {
                sortColumns.removeAt(index)
            }

            sortColumns.add(0, SortColumnImpl(column, SortDirection.Asc))
        }

        sort(sortColumns)
    }

    private inner class SortColumnImpl(
        override val column: Column<T>,
        override var direction: SortDirection,
    ) : SortColumn<T>
}

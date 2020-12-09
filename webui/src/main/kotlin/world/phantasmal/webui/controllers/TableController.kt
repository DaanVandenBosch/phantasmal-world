package world.phantasmal.webui.controllers

import world.phantasmal.observable.value.list.ListVal

class Column<T>(
    val key: String,
    val title: String,
    /**
     * Whether the column stays in place while scrolling.
     */
    val fixed: Boolean = false,
    val width: Int,
    /**
     * Whether cells in this column contain an input widget.
     */
    val input: Boolean = false,
    val textAlign: String? = null,
    val tooltip: ((T) -> String)? = null,
    val sortable: Boolean = false,
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

    abstract val values: ListVal<T>
    abstract val columns: List<Column<T>>

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

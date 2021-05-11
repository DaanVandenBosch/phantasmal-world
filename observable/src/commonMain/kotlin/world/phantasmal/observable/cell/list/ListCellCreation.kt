package world.phantasmal.observable.cell.list

import world.phantasmal.observable.cell.Cell

private val EMPTY_LIST_CELL = StaticListCell<Nothing>(emptyList())

fun <E> listCell(vararg elements: E): ListCell<E> = StaticListCell(elements.toList())

fun <E> emptyListCell(): ListCell<E> = EMPTY_LIST_CELL

fun <E> mutableListCell(
    vararg elements: E,
    extractObservables: ObservablesExtractor<E>? = null,
): MutableListCell<E> = SimpleListCell(mutableListOf(*elements), extractObservables)

fun <T1, T2, R> flatMapToList(
    c1: Cell<T1>,
    c2: Cell<T2>,
    transform: (T1, T2) -> ListCell<R>,
): ListCell<R> =
    FlatteningDependentListCell(c1, c2) { transform(c1.value, c2.value) }

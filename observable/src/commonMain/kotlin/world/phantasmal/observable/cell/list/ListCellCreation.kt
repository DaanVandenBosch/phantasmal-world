package world.phantasmal.observable.cell.list

import world.phantasmal.observable.cell.Cell

private val EMPTY_LIST_CELL = ImmutableListCell<Nothing>(emptyList())

/** Returns an immutable list cell containing [elements]. */
fun <E> listCell(vararg elements: E): ListCell<E> = ImmutableListCell(elements.toList())

/** Returns a singleton empty immutable cell. */
fun <E> emptyListCell(): ListCell<E> = EMPTY_LIST_CELL

/** Returns a mutable list cell containing [elements]. */
fun <E> mutableListCell(
    vararg elements: E,
    extractDependencies: DependenciesExtractor<E>? = null,
): MutableListCell<E> =
    SimpleListCell(mutableListOf(*elements), extractDependencies)

fun <T, R> Cell<T>.flatMapToList(
    transform: (T) -> ListCell<R>,
): ListCell<R> =
    FlatteningDependentListCell(this) { transform(value) }

fun <T1, T2, R> flatMapToList(
    c1: Cell<T1>,
    c2: Cell<T2>,
    transform: (T1, T2) -> ListCell<R>,
): ListCell<R> =
    FlatteningDependentListCell(c1, c2) { transform(c1.value, c2.value) }

package world.phantasmal.observable.cell.list

import world.phantasmal.observable.cell.Cell
import world.phantasmal.observable.cell.DependentCell

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

fun <E, R> ListCell<E>.listMap(transform: (E) -> R): ListCell<R> =
    DependentListCell(this) { value.map(transform) }

fun <E, R> ListCell<E>.fold(initialValue: R, operation: (R, E) -> R): Cell<R> =
    DependentCell(this) { value.fold(initialValue, operation) }

fun <E> ListCell<E>.all(predicate: (E) -> Boolean): Cell<Boolean> =
    DependentCell(this) { value.all(predicate) }

fun <E> ListCell<E>.sumOf(selector: (E) -> Int): Cell<Int> =
    DependentCell(this) { value.sumOf(selector) }

fun <E> ListCell<E>.filtered(predicate: (E) -> Boolean): ListCell<E> =
    FilteredListCell(this, predicate)

fun <E> ListCell<E>.filtered(predicate: Cell<(E) -> Boolean>): ListCell<E> =
    DependentListCell(this, predicate) { value.filter(predicate.value) }

fun <E> ListCell<E>.sortedWith(comparator: Cell<Comparator<E>>): ListCell<E> =
    DependentListCell(this, comparator) { value.sortedWith(comparator.value) }

fun <E> ListCell<E>.firstOrNull(): Cell<E?> =
    DependentCell(this) { value.firstOrNull() }

fun <T, R> Cell<T>.mapToList(
    transform: (T) -> List<R>,
): ListCell<R> =
    DependentListCell(this) { transform(value) }

fun <T1, T2, R> mapToList(
    c1: Cell<T1>,
    c2: Cell<T2>,
    transform: (T1, T2) -> List<R>,
): ListCell<R> =
    DependentListCell(c1, c2) { transform(c1.value, c2.value) }

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

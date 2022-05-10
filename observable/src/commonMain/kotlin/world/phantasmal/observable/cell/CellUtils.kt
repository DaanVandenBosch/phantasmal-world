package world.phantasmal.observable.cell

import world.phantasmal.core.disposable.Disposable
import world.phantasmal.observable.CallbackObserver

private val TRUE_CELL: Cell<Boolean> = ImmutableCell(true)
private val FALSE_CELL: Cell<Boolean> = ImmutableCell(false)
private val NULL_CELL: Cell<Nothing?> = ImmutableCell(null)
private val ZERO_INT_CELL: Cell<Int> = ImmutableCell(0)
private val EMPTY_STRING_CELL: Cell<String> = ImmutableCell("")

/** Returns an immutable cell containing [value]. */
fun <T> cell(value: T): Cell<T> = ImmutableCell(value)

/** Returns a singleton immutable cell containing the value `true`. */
fun trueCell(): Cell<Boolean> = TRUE_CELL

/** Returns a singleton immutable cell containing the value `false`. */
fun falseCell(): Cell<Boolean> = FALSE_CELL

/** Returns a singleton immutable cell containing the value `null`. */
fun nullCell(): Cell<Nothing?> = NULL_CELL

/** Returns a singleton immutable cell containing the integer value `0`. */
fun zeroIntCell(): Cell<Int> = ZERO_INT_CELL

/** Returns a singleton immutable cell containing the empty string (""). */
fun emptyStringCell(): Cell<String> = EMPTY_STRING_CELL

/**
 * Creates a [MutableCell] with initial value [value].
 */
fun <T> mutableCell(value: T): MutableCell<T> = SimpleCell(value)

/**
 * Creates a [MutableCell] which calls [getter] or [setter] when its value is being read or written
 * to, respectively.
 */
fun <T> mutableCell(getter: () -> T, setter: (T) -> Unit): MutableCell<T> =
    DelegatingCell(getter, setter)

fun <T> Cell<T>.observeNow(
    observer: (T) -> Unit,
): Disposable {
    val disposable = observeChange { observer(it.value) }
    // Call observer after observeChange to avoid double recomputation in most observables.
    observer(value)
    return disposable
}

fun <T1, T2> observeNow(
    c1: Cell<T1>,
    c2: Cell<T2>,
    observer: (T1, T2) -> Unit,
): Disposable {
    val disposable = CallbackObserver(c1, c2) { observer(c1.value, c2.value) }
    // Call observer after observeChange to avoid double recomputation in most observables.
    observer(c1.value, c2.value)
    return disposable
}

fun <T1, T2, T3> observeNow(
    c1: Cell<T1>,
    c2: Cell<T2>,
    c3: Cell<T3>,
    observer: (T1, T2, T3) -> Unit,
): Disposable {
    val disposable = CallbackObserver(c1, c2, c3) { observer(c1.value, c2.value, c3.value) }
    // Call observer after observeChange to avoid double recomputation in most observables.
    observer(c1.value, c2.value, c3.value)
    return disposable
}

fun <T1, T2, T3, T4> observeNow(
    c1: Cell<T1>,
    c2: Cell<T2>,
    c3: Cell<T3>,
    c4: Cell<T4>,
    observer: (T1, T2, T3, T4) -> Unit,
): Disposable {
    val disposable =
        CallbackObserver(c1, c2, c3, c4) { observer(c1.value, c2.value, c3.value, c4.value) }
    // Call observer after observeChange to avoid double recomputation in most observables.
    observer(c1.value, c2.value, c3.value, c4.value)
    return disposable
}

fun <T1, T2, T3, T4, T5> observeNow(
    c1: Cell<T1>,
    c2: Cell<T2>,
    c3: Cell<T3>,
    c4: Cell<T4>,
    c5: Cell<T5>,
    observer: (T1, T2, T3, T4, T5) -> Unit,
): Disposable {
    val disposable = CallbackObserver(c1, c2, c3, c4, c5) {
        observer(c1.value, c2.value, c3.value, c4.value, c5.value)
    }
    // Call observer after observeChange to avoid double recomputation in most observables.
    observer(c1.value, c2.value, c3.value, c4.value, c5.value)
    return disposable
}

/**
 * Map a transformation function over this cell.
 *
 * @param transform called whenever this cell changes
 */
fun <T, R> Cell<T>.map(
    transform: (T) -> R,
): Cell<R> =
    DependentCell(this) { transform(value) }

/**
 * Map a transformation function over 2 cells.
 *
 * @param transform called whenever [c1] or [c2] changes
 */
fun <T1, T2, R> map(
    c1: Cell<T1>,
    c2: Cell<T2>,
    transform: (T1, T2) -> R,
): Cell<R> =
    DependentCell(c1, c2) { transform(c1.value, c2.value) }

/**
 * Map a transformation function over 3 cells.
 *
 * @param transform called whenever [c1], [c2] or [c3] changes
 */
fun <T1, T2, T3, R> map(
    c1: Cell<T1>,
    c2: Cell<T2>,
    c3: Cell<T3>,
    transform: (T1, T2, T3) -> R,
): Cell<R> =
    DependentCell(c1, c2, c3) { transform(c1.value, c2.value, c3.value) }

fun <T> Cell<Cell<T>>.flatten(): Cell<T> =
    FlatteningDependentCell(this) { this.value }

/**
 * Map a transformation function that returns a cell over this cell. The resulting cell will
 * change when this cell changes and when the cell returned by [transform] changes.
 *
 * @param transform called whenever this cell changes
 */
fun <T, R> Cell<T>.flatMap(
    transform: (T) -> Cell<R>,
): Cell<R> =
    FlatteningDependentCell(this) { transform(value) }

/**
 * Map a transformation function that returns a cell over 2 cells. The resulting cell will change
 * when either cell changes and also when the cell returned by [transform] changes.
 *
 * @param transform called whenever this cell changes
 */
fun <T1, T2, R> flatMap(
    c1: Cell<T1>,
    c2: Cell<T2>,
    transform: (T1, T2) -> Cell<R>,
): Cell<R> =
    FlatteningDependentCell(c1, c2) { transform(c1.value, c2.value) }

fun <T, R> Cell<T>.flatMapNull(transform: (T) -> Cell<R>?): Cell<R?> =
    FlatteningDependentCell(this) { transform(value) ?: nullCell() }

fun Cell<*>.isNull(): Cell<Boolean> =
    map { it == null }

fun Cell<*>.isNotNull(): Cell<Boolean> =
    map { it != null }

infix fun <T> Cell<T>.eq(value: T): Cell<Boolean> =
    map { it == value }

infix fun <T> Cell<T>.eq(other: Cell<T>): Cell<Boolean> =
    map(this, other) { a, b -> a == b }

infix fun <T> Cell<T>.ne(value: T): Cell<Boolean> =
    map { it != value }

infix fun <T> Cell<T>.ne(other: Cell<T>): Cell<Boolean> =
    map(this, other) { a, b -> a != b }

fun <T> Cell<T?>.orElse(defaultValue: () -> T): Cell<T> =
    map { it ?: defaultValue() }

infix fun <T : Comparable<T>> Cell<T>.gt(value: T): Cell<Boolean> =
    map { it > value }

infix fun <T : Comparable<T>> Cell<T>.gt(other: Cell<T>): Cell<Boolean> =
    map(this, other) { a, b -> a > b }

infix fun <T : Comparable<T>> Cell<T>.lt(value: T): Cell<Boolean> =
    map { it < value }

infix fun <T : Comparable<T>> Cell<T>.lt(other: Cell<T>): Cell<Boolean> =
    map(this, other) { a, b -> a < b }

infix fun Cell<Boolean>.and(other: Cell<Boolean>): Cell<Boolean> =
    map(this, other) { a, b -> a && b }

infix fun Cell<Boolean>.and(other: Boolean): Cell<Boolean> =
    if (other) this else falseCell()

infix fun Boolean.and(other: Cell<Boolean>): Cell<Boolean> =
    if (this) other else falseCell()

infix fun Cell<Boolean>.or(other: Cell<Boolean>): Cell<Boolean> =
    map(this, other) { a, b -> a || b }

infix fun Cell<Boolean>.or(other: Boolean): Cell<Boolean> =
    if (other) trueCell() else this

infix fun Boolean.or(other: Cell<Boolean>): Cell<Boolean> =
    if (this) trueCell() else other

infix fun Cell<Boolean>.xor(other: Cell<Boolean>): Cell<Boolean> =
    // Use != because of https://youtrack.jetbrains.com/issue/KT-31277.
    map(this, other) { a, b -> a != b }

operator fun Cell<Boolean>.not(): Cell<Boolean> = map { !it }

operator fun Cell<Int>.plus(value: Int): Cell<Int> =
    map { it + value }

operator fun Cell<Int>.minus(value: Int): Cell<Int> =
    map { it - value }

fun Cell<String>.isEmpty(): Cell<Boolean> =
    map { it.isEmpty() }

fun Cell<String>.isNotEmpty(): Cell<Boolean> =
    map { it.isNotEmpty() }

fun Cell<String>.isBlank(): Cell<Boolean> =
    map { it.isBlank() }

fun Cell<String>.isNotBlank(): Cell<Boolean> =
    map { it.isNotBlank() }

fun cellToString(cell: Cell<*>): String {
    val className = cell::class.simpleName
    val value = cell.value
    return "$className{$value}"
}

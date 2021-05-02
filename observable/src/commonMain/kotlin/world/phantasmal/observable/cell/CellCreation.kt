package world.phantasmal.observable.cell

private val TRUE_CELL: Cell<Boolean> = StaticCell(true)
private val FALSE_CELL: Cell<Boolean> = StaticCell(false)
private val NULL_CELL: Cell<Nothing?> = StaticCell(null)
private val ZERO_INT_CELL: Cell<Int> = StaticCell(0)
private val EMPTY_STRING_CELL: Cell<String> = StaticCell("")

fun <T> cell(value: T): Cell<T> = StaticCell(value)

fun trueCell(): Cell<Boolean> = TRUE_CELL

fun falseCell(): Cell<Boolean> = FALSE_CELL

fun nullCell(): Cell<Nothing?> = NULL_CELL

fun zeroIntCell(): Cell<Int> = ZERO_INT_CELL

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

fun and(vararg cells: Cell<Boolean>): Cell<Boolean> =
    DependentCell(*cells) { cells.all { it.value } }

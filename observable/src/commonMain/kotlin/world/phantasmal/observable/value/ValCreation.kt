package world.phantasmal.observable.value

private val TRUE_VAL: Val<Boolean> = StaticVal(true)
private val FALSE_VAL: Val<Boolean> = StaticVal(false)
private val NULL_VAL: Val<Nothing?> = StaticVal(null)
private val ZERO_INT_VAL: Val<Int> = StaticVal(0)
private val EMPTY_STRING_VAL: Val<String> = StaticVal("")

fun <T> value(value: T): Val<T> = StaticVal(value)

fun trueVal(): Val<Boolean> = TRUE_VAL

fun falseVal(): Val<Boolean> = FALSE_VAL

fun nullVal(): Val<Nothing?> = NULL_VAL

fun zeroIntVal(): Val<Int> = ZERO_INT_VAL

fun emptyStringVal(): Val<String> = EMPTY_STRING_VAL

/**
 * Creates a [MutableVal] with initial value [value].
 */
fun <T> mutableVal(value: T): MutableVal<T> = SimpleVal(value)

/**
 * Creates a [MutableVal] which calls [getter] or [setter] when its value is being read or written
 * to, respectively.
 */
fun <T> mutableVal(getter: () -> T, setter: (T) -> Unit): MutableVal<T> =
    DelegatingVal(getter, setter)

/**
 * Map a transformation function over 2 vals.
 *
 * @param transform called whenever [v1] or [v2] changes
 */
fun <T1, T2, R> map(
    v1: Val<T1>,
    v2: Val<T2>,
    transform: (T1, T2) -> R,
): Val<R> =
    DependentVal(listOf(v1, v2)) { transform(v1.value, v2.value) }

/**
 * Map a transformation function over 3 vals.
 *
 * @param transform called whenever [v1], [v2] or [v3] changes
 */
fun <T1, T2, T3, R> map(
    v1: Val<T1>,
    v2: Val<T2>,
    v3: Val<T3>,
    transform: (T1, T2, T3) -> R,
): Val<R> =
    DependentVal(listOf(v1, v2, v3)) { transform(v1.value, v2.value, v3.value) }

/**
 * Map a transformation function that returns a val over 2 vals. The resulting val will change when
 * either val changes and when the val returned by [transform] changes.
 *
 * @param transform called whenever this val changes
 */
fun <T1, T2, R> flatMap(
    v1: Val<T1>,
    v2: Val<T2>,
    transform: (T1, T2) -> Val<R>,
): Val<R> =
    FlatMappedVal(listOf(v1, v2)) { transform(v1.value, v2.value) }

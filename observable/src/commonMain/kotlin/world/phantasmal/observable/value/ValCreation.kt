package world.phantasmal.observable.value

private val TRUE_VAL: Val<Boolean> = StaticVal(true)
private val FALSE_VAL: Val<Boolean> = StaticVal(false)
private val NULL_VALL: Val<Nothing?> = StaticVal(null)
private val EMPTY_STRING_VAL: Val<String> = StaticVal("")

fun <T> value(value: T): Val<T> = StaticVal(value)

fun trueVal(): Val<Boolean> = TRUE_VAL

fun falseVal(): Val<Boolean> = FALSE_VAL

fun nullVal(): Val<Nothing?> = NULL_VALL

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

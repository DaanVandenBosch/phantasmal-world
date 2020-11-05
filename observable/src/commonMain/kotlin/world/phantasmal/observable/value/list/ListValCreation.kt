package world.phantasmal.observable.value.list

private val EMPTY_LIST_VAL = StaticListVal<Nothing>(emptyList())

fun <E> listVal(vararg elements: E): ListVal<E> = StaticListVal(elements.toList())

@Suppress("UNCHECKED_CAST")
fun <E> emptyListVal(): ListVal<E> = EMPTY_LIST_VAL as ListVal<E>

fun <E> mutableListVal(
    elements: MutableList<E> = mutableListOf(),
    extractObservables: ObservablesExtractor<E>? = null,
): MutableListVal<E> = SimpleListVal(elements, extractObservables)

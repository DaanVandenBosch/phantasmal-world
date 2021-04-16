package world.phantasmal.observable.value.list

import world.phantasmal.observable.value.Val

private val EMPTY_LIST_VAL = StaticListVal<Nothing>(emptyList())

fun <E> listVal(vararg elements: E): ListVal<E> = StaticListVal(elements.toList())

fun <E> emptyListVal(): ListVal<E> = EMPTY_LIST_VAL

fun <E> mutableListVal(
    vararg elements: E,
    extractObservables: ObservablesExtractor<E>? = null,
): MutableListVal<E> = SimpleListVal(mutableListOf(*elements), extractObservables)

fun <T1, T2, R> flatMapToList(
    v1: Val<T1>,
    v2: Val<T2>,
    transform: (T1, T2) -> ListVal<R>,
): ListVal<R> =
    FlatteningDependentListVal(v1, v2) { transform(v1.value, v2.value) }

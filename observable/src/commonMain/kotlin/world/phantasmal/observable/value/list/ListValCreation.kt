package world.phantasmal.observable.value.list

fun <E> mutableListVal(
    elements: MutableList<E> = mutableListOf(),
    extractObservables: ObservablesExtractor<E>? = null
): MutableListVal<E> = SimpleListVal(elements, extractObservables)

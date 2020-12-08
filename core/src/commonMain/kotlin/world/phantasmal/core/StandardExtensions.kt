package world.phantasmal.core

fun <E> MutableList<E>.replaceAll(elements: Collection<E>): Boolean {
    clear()
    return addAll(elements)
}

fun <E> MutableList<E>.replaceAll(elements: Iterable<E>): Boolean {
    clear()
    return addAll(elements)
}

fun <E> MutableList<E>.replaceAll(elements: Sequence<E>): Boolean {
    clear()
    return addAll(elements)
}

/**
 * Replace [amount] elements at [startIndex] with [elements].
 */
fun <E> MutableList<E>.splice(startIndex: Int, amount: Int, elements: Iterable<E>) {
    repeat(amount) { removeAt(startIndex) }

    var i = startIndex

    for (element in elements) {
        add(i++, element)
    }
}

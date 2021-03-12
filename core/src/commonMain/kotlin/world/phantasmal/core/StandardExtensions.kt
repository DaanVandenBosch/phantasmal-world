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
 * Remove [n] elements at [startIndex].
 */
fun <E> MutableList<E>.removeAt(startIndex: Int, n: Int) {
    repeat(n) { removeAt(startIndex) }
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

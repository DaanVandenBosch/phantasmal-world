package world.phantasmal.observable.value.list

// Test suite for all ListVal implementations.
// These functions are called from type-specific unit tests.

import world.phantasmal.observable.test.withScope
import kotlin.test.assertEquals

typealias ListValAndAdd = Pair<ListVal<*>, () -> Unit>

fun listValTests(create: () -> ListValAndAdd) {
    listValShouldUpdateSizeValCorrectly(create)
}

private fun listValShouldUpdateSizeValCorrectly(create: () -> ListValAndAdd) {
    val (list: List<*>, add) = create()

    assertEquals(0, list.sizeVal.value)

    var observedSize = 0

    withScope { scope ->
        list.sizeVal.observe(scope) { observedSize = it.value }

        for (i in 1..3) {
            add()

            assertEquals(i, list.sizeVal.value)
            assertEquals(i, observedSize)
        }
    }
}

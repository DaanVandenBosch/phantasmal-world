package world.phantasmal.observable.value.list

import world.phantasmal.observable.ObservableAndEmit
import world.phantasmal.observable.value.ValTests
import kotlin.test.Test
import kotlin.test.assertEquals

class ListValAndAdd<T, out O : ListVal<T>>(
    observable: O,
    add: () -> Unit,
) : ObservableAndEmit<List<T>, O>(observable, add)

/**
 * Test suite for all [ListVal] implementations. There is a subclass of this suite for every
 * [ListVal] implementation.
 */
abstract class ListValTests : ValTests() {
    abstract override fun create(): ListValAndAdd<*, ListVal<*>>

    @Test
    fun listVal_updates_size_correctly() = test {
        val (list: ListVal<*>, add) = create()

        assertEquals(0, list.size.value)

        var observedSize = 0

        disposer.add(
            list.size.observe { observedSize = it.value }
        )

        for (i in 1..3) {
            add()

            assertEquals(i, list.size.value)
            assertEquals(i, observedSize)
        }
    }
}

package world.phantasmal.observable.value.list

import kotlin.test.Test
import kotlin.test.assertEquals

class SimpleListValTests : MutableListValTests() {
    override fun create() = object : MutableListValAndAdd {
        override val observable = SimpleListVal(mutableListOf<Int>())

        override fun add() {
            observable.add(7)
        }
    }

    @Test
    fun instantiates_correctly() = test {
        val list = SimpleListVal(mutableListOf(1, 2, 3))

        assertEquals(3, list.size.value)
        assertEquals(3, list.value.size)
        assertEquals(1, list[0])
        assertEquals(2, list[1])
        assertEquals(3, list[2])
    }
}

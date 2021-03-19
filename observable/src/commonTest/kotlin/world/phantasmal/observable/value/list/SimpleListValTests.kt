package world.phantasmal.observable.value.list

import kotlin.test.Test
import kotlin.test.assertEquals

class SimpleListValTests : MutableListValTests<Int> {
    override fun createProvider() = object : MutableListValTests.Provider<Int> {
        private var nextElement = 0

        override val observable = SimpleListVal(mutableListOf<Int>())

        override fun addElement() {
            observable.add(createElement())
        }

        override fun createValue(): List<Int> = listOf(createElement())

        override fun createElement(): Int = nextElement++
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

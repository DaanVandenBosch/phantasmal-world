package world.phantasmal.cell.list

import world.phantasmal.cell.test.assertListCellEquals
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SimpleListCellTests : MutableListCellTests<Int> {
    override fun createProvider() = createListProvider(empty = true)

    override fun createListProvider(empty: Boolean) = object : MutableListCellTests.Provider<Int> {
        private var nextElement = 0

        override val cell = SimpleListCell(if (empty) mutableListOf() else mutableListOf(-13))

        override fun addElement() {
            cell.add(createElement())
        }

        override fun createValue(): List<Int> = listOf(createElement())

        override fun createElement(): Int = nextElement++
    }

    @Test
    fun instantiates_correctly() = test {
        val list = SimpleListCell(mutableListOf(1, 2, 3))

        assertListCellEquals(listOf(1, 2, 3), list)
    }

    @Test
    fun set() = test {
        val list = SimpleListCell(mutableListOf("a", "b", "c"))

        list[1] = "test"
        list[2] = "test2"
        assertFailsWith<IndexOutOfBoundsException> {
            list[-1] = "should not be in list"
        }
        assertFailsWith<IndexOutOfBoundsException> {
            list[3] = "should not be in list"
        }

        assertListCellEquals(listOf("a", "test", "test2"), list)
    }

    @Test
    fun add_with_index() = test {
        val list = SimpleListCell(mutableListOf<String>())

        list.add(0, "b")
        list.add(1, "c")
        list.add(0, "a")

        assertListCellEquals(listOf("a", "b", "c"), list)
    }

    @Test
    fun remove() = test {
        val list = SimpleListCell(mutableListOf("a", "b", "c", "d", "e"))

        assertTrue(list.remove("c"))

        assertListCellEquals(listOf("a", "b", "d", "e"), list)

        assertTrue(list.remove("a"))

        assertListCellEquals(listOf("b", "d", "e"), list)

        assertTrue(list.remove("e"))

        assertListCellEquals(listOf("b", "d"), list)

        // The following values are not in the list (anymore).
        assertFalse(list.remove("x"))
        assertFalse(list.remove("a"))
        assertFalse(list.remove("c"))

        // List should remain unchanged after removal attempts of nonexistent elements.
        assertListCellEquals(listOf("b", "d"), list)
    }

    @Test
    fun removeAt() = test {
        val list = SimpleListCell(mutableListOf("a", "b", "c", "d", "e"))

        list.removeAt(2)

        assertListCellEquals(listOf("a", "b", "d", "e"), list)

        list.removeAt(0)

        assertListCellEquals(listOf("b", "d", "e"), list)

        list.removeAt(2)

        assertListCellEquals(listOf("b", "d"), list)

        assertFailsWith<IndexOutOfBoundsException> {
            list.removeAt(-1)
        }
        assertFailsWith<IndexOutOfBoundsException> {
            list.removeAt(list.size.value)
        }

        // List should remain unchanged after invalid calls.
        assertListCellEquals(listOf("b", "d"), list)
    }

    @Test
    fun splice() = test {
        val list = SimpleListCell((0..9).toMutableList())

        list.splice(fromIndex = 3, removeCount = 5, newElement = 100)

        assertListCellEquals(listOf(0, 1, 2, 100, 8, 9), list)

        list.splice(fromIndex = 0, removeCount = 0, newElement = 101)

        assertListCellEquals(listOf(101, 0, 1, 2, 100, 8, 9), list)

        list.splice(fromIndex = list.size.value, removeCount = 0, newElement = 102)

        assertListCellEquals(listOf(101, 0, 1, 2, 100, 8, 9, 102), list)

        // Negative fromIndex.
        assertFailsWith<IndexOutOfBoundsException> {
            list.splice(fromIndex = -1, removeCount = 0, newElement = 200)
        }
        // fromIndex too large.
        assertFailsWith<IndexOutOfBoundsException> {
            list.splice(fromIndex = list.size.value + 1, removeCount = 0, newElement = 201)
        }
        // removeCount too large.
        assertFailsWith<IndexOutOfBoundsException> {
            list.splice(fromIndex = 0, removeCount = 50, newElement = 202)
        }

        // List should remain unchanged after invalid calls.
        assertListCellEquals(listOf(101, 0, 1, 2, 100, 8, 9, 102), list)
    }
}

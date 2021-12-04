package world.phantasmal.observable.cell.list

import world.phantasmal.observable.cell.SimpleCell
import world.phantasmal.observable.test.assertListCellEquals
import world.phantasmal.testUtils.TestContext
import kotlin.test.*

class SimpleListCellTests : MutableListCellTests<Int> {
    override fun createProvider() = createListProvider(empty = true)

    override fun createListProvider(empty: Boolean) = object : MutableListCellTests.Provider<Int> {
        private var nextElement = 0

        override val observable = SimpleListCell(if (empty) mutableListOf() else mutableListOf(-13))

        override fun addElement() {
            observable.add(createElement())
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
        testSet(SimpleListCell(mutableListOf("a", "b", "c")))
    }

    @Test
    fun set_with_extractDependencies() = test {
        testSet(SimpleListCell(mutableListOf("a", "b", "c")) { arrayOf() })
    }

    private fun testSet(list: SimpleListCell<String>) {
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

    @Test
    fun element_changes_propagate_correctly_after_set() = test {
        testElementChangePropagation {
            val old = it[1]
            it[1] = SimpleCell("new")
            listOf(old)
        }
    }

    @Test
    fun element_changes_propagate_correctly_after_add() = test {
        testElementChangePropagation {
            it.add(SimpleCell("new"))
            emptyList()
        }
    }

    @Test
    fun element_changes_propagate_correctly_after_add_with_index() = test {
        testElementChangePropagation {
            it.add(1, SimpleCell("new"))
            emptyList()
        }
    }

    @Test
    fun element_changes_propagate_correctly_after_remove() = test {
        testElementChangePropagation {
            val removed = it[1]
            it.remove(removed)
            listOf(removed)
        }
    }

    @Test
    fun element_changes_propagate_correctly_after_removeAt() = test {
        testElementChangePropagation {
            listOf(it.removeAt(2))
        }
    }

    @Test
    fun element_changes_propagate_correctly_after_replaceAll() = test {
        testElementChangePropagation {
            val removed = it.value.toList()
            it.replaceAll(listOf(SimpleCell("new a"), SimpleCell("new b")))
            removed
        }
    }

    @Test
    fun element_changes_propagate_correctly_after_replaceAll_with_sequence() = test {
        testElementChangePropagation {
            val removed = it.value.toList()
            it.replaceAll(sequenceOf(SimpleCell("new a"), SimpleCell("new b")))
            removed
        }
    }

    @Test
    fun element_changes_propagate_correctly_after_splice() = test {
        testElementChangePropagation {
            val removed = it.value.toList().drop(1)
            it.splice(1, 2, SimpleCell("new"))
            removed
        }
    }

    @Test
    fun element_changes_propagate_correctly_after_clear() = test {
        testElementChangePropagation {
            val removed = it.value.toList()
            it.clear()
            removed
        }
    }

    /**
     * Creates a list with 3 SimpleCells as elements, calls [updateList] with this list and then
     * checks that changes to old elements don't affect the list and changes to new elements do
     * affect the list.
     *
     * @param updateList Function that changes the list and returns the removed elements if any.
     */
    private fun TestContext.testElementChangePropagation(
        updateList: (SimpleListCell<SimpleCell<String>>) -> List<SimpleCell<String>>
    ) {
        val list = SimpleListCell(
            mutableListOf(
                SimpleCell("a"),
                SimpleCell("b"),
                SimpleCell("c")
            )
        ) { arrayOf(it) }

        var event: ListChangeEvent<SimpleCell<String>>? = null

        disposer.add(list.observeListChange {
            assertNull(event)
            event = it
        })

        val removed = updateList(list)

        event = null

        // The list should not emit events when an old element is changed.
        for (element in removed) {
            element.value += "-1"

            assertNull(event)
        }

        // The list should emit events when any of the current elements are changed.
        for ((index, element) in list.value.withIndex()) {
            event = null

            element.value += "-2"

            val e = event
            assertNotNull(e)
            assertEquals(1, e.changes.size)
            val c = e.changes.first()
            assertTrue(c is ListChange.Element)
            assertEquals(index, c.index)
            assertEquals(element, c.updated)
        }
    }
}

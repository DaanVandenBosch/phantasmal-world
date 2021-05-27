package world.phantasmal.observable.cell.list

import world.phantasmal.observable.cell.SimpleCell
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

        assertEquals(3, list.size.value)
        assertEquals(3, list.value.size)
        assertEquals(1, list[0])
        assertEquals(2, list[1])
        assertEquals(3, list[2])
    }

    @Test
    fun add_with_index() = test {
        val list = SimpleListCell(mutableListOf<String>())

        list.add(0, "b")
        list.add(1, "c")
        list.add(0, "a")

        assertEquals(3, list.size.value)
        assertEquals("a", list[0])
        assertEquals("b", list[1])
        assertEquals("c", list[2])
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

        disposer.add(list.observeList {
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

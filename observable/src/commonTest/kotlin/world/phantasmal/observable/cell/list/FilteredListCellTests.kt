package world.phantasmal.observable.cell.list

import world.phantasmal.observable.cell.SimpleCell
import kotlin.test.*

class FilteredListCellTests : ListCellTests {
    override fun createListProvider(empty: Boolean) = object : ListCellTests.Provider {
        private val dependencyCell =
            SimpleListCell(if (empty) mutableListOf(5) else mutableListOf(5, 10))

        override val observable = FilteredListCell(dependencyCell, predicate = { it % 2 == 0 })

        override fun addElement() {
            dependencyCell.add(4)
        }
    }

    @Test
    fun contains_only_values_that_match_the_predicate() = test {
        val dep = SimpleListCell(mutableListOf("a", "b"))
        val list = FilteredListCell(dep, predicate = { 'a' in it })

        assertEquals(1, list.value.size)
        assertEquals("a", list.value[0])

        dep.add("foo")
        dep.add("bar")

        assertEquals(2, list.value.size)
        assertEquals("a", list.value[0])
        assertEquals("bar", list.value[1])

        dep.add("quux")
        dep.add("qaax")

        assertEquals(3, list.value.size)
        assertEquals("a", list.value[0])
        assertEquals("bar", list.value[1])
        assertEquals("qaax", list.value[2])
    }

    @Test
    fun only_emits_when_necessary() = test {
        val dep = SimpleListCell<Int>(mutableListOf())
        val list = FilteredListCell(dep, predicate = { it % 2 == 0 })
        var changes = 0
        var listChanges = 0

        disposer.add(list.observe {
            changes++
        })
        disposer.add(list.observeList {
            listChanges++
        })

        dep.add(1)
        dep.add(3)
        dep.add(5)

        assertEquals(0, changes)
        assertEquals(0, listChanges)

        dep.add(0)
        dep.add(2)
        dep.add(4)

        assertEquals(3, changes)
        assertEquals(3, listChanges)
    }

    @Test
    fun emits_correct_change_events() = test {
        val dep = SimpleListCell<Int>(mutableListOf())
        val list = FilteredListCell(dep, predicate = { it % 2 == 0 })
        var event: ListChangeEvent<Int>? = null

        disposer.add(list.observeList {
            assertNull(event)
            event = it
        })

        dep.replaceAll(listOf(1, 2, 3, 4, 5))

        run {
            val e = event
            assertNotNull(e)
            assertEquals(1, e.changes.size)

            val c = e.changes.first()
            assertTrue(c is ListChange.Structural)
            assertEquals(0, c.index)
            assertEquals(0, c.removed.size)
            assertEquals(2, c.inserted.size)
            assertEquals(2, c.inserted[0])
            assertEquals(4, c.inserted[1])
        }

        event = null

        dep.splice(2, 2, 10)

        run {
            val e = event
            assertNotNull(e)
            assertEquals(1, e.changes.size)

            val c = e.changes.first()
            assertTrue(c is ListChange.Structural)
            assertEquals(1, c.index)
            assertEquals(1, c.removed.size)
            assertEquals(4, c.removed[0])
            assertEquals(1, c.inserted.size)
            assertEquals(10, c.inserted[0])
        }
    }

    /**
     * When the dependency of a [FilteredListCell] emits [ListChange.Element] changes, the
     * [FilteredListCell] should emit either [ListChange.Structural] or [ListChange.Element]
     * changes, depending on whether the predicate result has changed.
     */
    @Test
    fun emits_correct_events_when_dependency_emits_element_changes() = test {
        val dep = SimpleListCell(
            mutableListOf(SimpleCell(1), SimpleCell(2), SimpleCell(3), SimpleCell(4)),
            extractDependencies = { arrayOf(it) },
        )
        val list = FilteredListCell(dep, predicate = { it.value % 2 == 0 })
        var event: ListChangeEvent<SimpleCell<Int>>? = null

        disposer.add(list.observeList {
            assertNull(event)
            event = it
        })

        for (i in 0 until dep.size.value) {
            event = null

            // Make an even number odd or an odd number even. List should emit a structural change.
            val newValue = dep[i].value + 1
            dep[i].value = newValue

            val e = event
            assertNotNull(e)
            assertEquals(1, e.changes.size)

            val c = e.changes.first()
            assertTrue(c is ListChange.Structural)

            if (newValue % 2 == 0) {
                assertEquals(0, c.removed.size)
                assertEquals(1, c.inserted.size)
                assertEquals(newValue, c.inserted[0].value)
            } else {
                assertEquals(1, c.removed.size)
                assertEquals(0, c.inserted.size)
                assertEquals(newValue, c.removed[0].value)
            }
        }

        for (i in 0 until dep.size.value) {
            event = null

            // Change a value, but keep even numbers even and odd numbers odd. List should emit an
            // ElementChange event.
            val newValue = dep[i].value + 2
            dep[i].value = newValue

            val e = event
            assertNotNull(e)
            assertEquals(1, e.changes.size)

            val c = e.changes.first()
            assertTrue(c is ListChange.Element)

            assertEquals(newValue, c.updated.value)
        }
    }
}

package world.phantasmal.observable.cell.list

import world.phantasmal.observable.cell.Cell
import world.phantasmal.observable.cell.ImmutableCell
import world.phantasmal.observable.cell.SimpleCell
import world.phantasmal.observable.test.ObservableTestSuite
import kotlin.test.*

/**
 * Tests that apply to all filtered list implementations.
 */
interface SuperFilteredListCellTests : ObservableTestSuite {
    fun <E> createFilteredListCell(list: ListCell<E>, predicate: Cell<(E) -> Boolean>): ListCell<E>

    @Test
    fun contains_only_values_that_match_the_predicate() = test {
        val dep = SimpleListCell(mutableListOf("a", "b"))
        val list = createFilteredListCell(dep, predicate = ImmutableCell { 'a' in it })

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
        val list = createFilteredListCell(dep, predicate = ImmutableCell { it % 2 == 0 })
        var changes = 0
        var listChanges = 0

        disposer.add(list.observeChange {
            changes++
        })
        disposer.add(list.observeListChange {
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
        val list = createFilteredListCell(dep, predicate = ImmutableCell { it % 2 == 0 })
        var event: ListChangeEvent<Int>? = null

        disposer.add(list.observeListChange {
            assertNull(event)
            event = it
        })

        run {
            dep.replaceAll(listOf(1, 2, 3, 4, 5))

            val e = event
            assertNotNull(e)
            assertEquals(1, e.changes.size)

            val c = e.changes.first()
            assertEquals(0, c.index)
            assertEquals(0, c.removed.size)
            assertEquals(2, c.inserted.size)
            assertEquals(2, c.inserted[0])
            assertEquals(4, c.inserted[1])
        }

        event = null

        run {
            dep.splice(2, 2, 10)

            val e = event
            assertNotNull(e)
            assertEquals(1, e.changes.size)

            val c = e.changes.first()
            assertEquals(1, c.index)
            assertEquals(1, c.removed.size)
            assertEquals(4, c.removed[0])
            assertEquals(1, c.inserted.size)
            assertEquals(10, c.inserted[0])
        }
    }

    @Test
    fun value_changes_and_emits_when_predicate_changes() = test {
        val predicate: SimpleCell<(Int) -> Boolean> = SimpleCell { it % 2 == 0 }
        val list = createFilteredListCell(ImmutableListCell(listOf(1, 2, 3, 4, 5)), predicate)
        var event: ListChangeEvent<Int>? = null

        disposer.add(list.observeListChange {
            assertNull(event)
            event = it
        })

        run {
            // Change predicate.
            predicate.value = { it % 2 == 1 }

            // Value changes.
            assertEquals(listOf(1, 3, 5), list.value)

            // An event was emitted.
            val e = event
            assertNotNull(e)
            assertEquals(1, e.changes.size)

            val c = e.changes.first()
            assertEquals(0, c.index)
            assertEquals(listOf(2, 4), c.removed)
            assertEquals(listOf(1, 3, 5), c.inserted)
        }

        event = null

        run {
            // Change predicate.
            predicate.value = { it % 2 == 0 }

            // Value changes.
            assertEquals(listOf(2, 4), list.value)

            // An event was emitted.
            val e = event
            assertNotNull(e)
            assertEquals(1, e.changes.size)

            val c = e.changes.first()
            assertEquals(0, c.index)
            assertEquals(listOf(1, 3, 5), c.removed)
            assertEquals(listOf(2, 4), c.inserted)
        }
    }

    @Test
    fun emits_correctly_when_multiple_changes_happen_at_once() = test {
        val dependency = object : AbstractListCell<Int>() {
            private val elements: MutableList<Int> = mutableListOf()
            override val value: List<Int> get() = elements
            override var changeEvent: ListChangeEvent<Int>? = null
                private set

            fun makeChanges(newElements: List<Int>) {
                applyChange {
                    val changes: MutableList<ListChange<Int>> = mutableListOf()

                    for (newElement in newElements) {
                        changes.add(ListChange(
                            index = elements.size,
                            prevSize = elements.size,
                            removed = emptyList(),
                            inserted = listOf(newElement),
                        ))
                        elements.add(newElement)
                    }

                    changeEvent = ListChangeEvent(elements.toList(), changes)
                }
            }
        }

        val list = createFilteredListCell(dependency, ImmutableCell { true })
        var event: ListChangeEvent<Int>? = null

        disposer.add(list.observeListChange {
            assertNull(event)
            event = it
        })

        for (i in 1..3) {
            event = null

            // Make two changes at once everytime.
            val change0 = i * 13
            val change1 = i * 17
            val changes = listOf(change0, change1)
            val oldList = list.value.toList()

            dependency.makeChanges(changes)

            // These checks are very implementation-specific. At some point the filtered list might,
            // for example, emit an event with a single change instead of two changes and then this
            // test will incorrectly fail.
            val e = event
            assertNotNull(e)
            assertEquals(oldList + changes, e.value)
            assertEquals(2, e.changes.size)

            val lc0 = e.changes[0]
            assertEquals(oldList.size, lc0.index)
            assertEquals(oldList.size, lc0.prevSize)
            assertTrue(lc0.removed.isEmpty())
            assertEquals(listOf(change0), lc0.inserted)

            val lc1 = e.changes[1]
            assertEquals(oldList.size + 1, lc1.index)
            assertEquals(oldList.size + 1, lc1.prevSize)
            assertTrue(lc1.removed.isEmpty())
            assertEquals(listOf(change1), lc1.inserted)
        }
    }

    @Test
    fun emits_correctly_when_dependency_contains_same_element_twice() = test {
        val x = "x"
        val y = "y"
        val z = "z"
        val dependency = SimpleListCell(mutableListOf(x, y, z, x, y, z))
        val list = createFilteredListCell(dependency, SimpleCell { it != y })
        var event: ListChangeEvent<String>? = null

        disposer.add(list.observeListChange {
            assertNull(event)
            event = it
        })

        assertEquals(listOf(x, z, x, z), list.value)

        run {
            // Remove second x element.
            dependency.removeAt(3)

            // Value changes.
            assertEquals(listOf(x, z, z), list.value)

            // An event was emitted.
            val e = event
            assertNotNull(e)
            assertEquals(1, e.changes.size)

            val c = e.changes.first()
            assertEquals(2, c.index)
            assertEquals(listOf(x), c.removed)
            assertTrue(c.inserted.isEmpty())
        }

        event = null

        run {
            // Remove first x element.
            dependency.removeAt(0)

            // Value changes.
            assertEquals(listOf(z, z), list.value)

            // An event was emitted.
            val e = event
            assertNotNull(e)
            assertEquals(1, e.changes.size)

            val c = e.changes.first()
            assertEquals(0, c.index)
            assertEquals(listOf(x), c.removed)
            assertTrue(c.inserted.isEmpty())
        }

        event = null

        run {
            // Remove second z element.
            dependency.removeAt(3)

            // Value changes.
            assertEquals(listOf(z), list.value)

            // An event was emitted.
            val e = event
            assertNotNull(e)
            assertEquals(1, e.changes.size)

            val c = e.changes.first()
            assertEquals(1, c.index)
            assertEquals(listOf(z), c.removed)
            assertTrue(c.inserted.isEmpty())
        }
    }
}

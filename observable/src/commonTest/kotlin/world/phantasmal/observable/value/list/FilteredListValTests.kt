package world.phantasmal.observable.value.list

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class FilteredListValTests : ListValTests() {
    @Test
    fun only_emits_when_necessary() = test {
        val dep = SimpleListVal<Int>(mutableListOf())
        val list = FilteredListVal(dep) { it % 2 == 0 }
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
        val dep = SimpleListVal<Int>(mutableListOf())
        val list = FilteredListVal(dep) { it % 2 == 0 }
        var event: ListValChangeEvent<Int>? = null

        disposer.add(list.observeList {
            assertNull(event)
            event = it
        })

        dep.replaceAll(listOf(1, 2, 3, 4, 5))

        (event as ListValChangeEvent.Change).let { e ->
            assertEquals(0, e.index)
            assertEquals(0, e.removed.size)
            assertEquals(2, e.inserted.size)
            assertEquals(2, e.inserted[0])
            assertEquals(4, e.inserted[1])
        }

        event = null

        dep.splice(2, 2, 10)

        (event as ListValChangeEvent.Change).let { e ->
            assertEquals(1, e.index)
            assertEquals(1, e.removed.size)
            assertEquals(4, e.removed[0])
            assertEquals(1, e.inserted.size)
            assertEquals(10, e.inserted[0])
        }
    }

    override fun create(): ListValAndAdd<*, FilteredListVal<*>> {
        val l = SimpleListVal<Int>(mutableListOf())
        val list = FilteredListVal(l) { it % 2 == 0 }
        return ListValAndAdd(list) { l.add(4) }
    }
}

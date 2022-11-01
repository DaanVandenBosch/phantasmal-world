package world.phantasmal.cell

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

interface MutableCellTests<T : Any> : CellTests {
    override fun createProvider(): Provider<T>

    @Test
    fun emits_a_change_event_when_value_is_modified() = test {
        val p = createProvider()

        var observedValue: Any? = null

        disposer.add(p.cell.observeChange {
            assertNull(observedValue)
            observedValue = it.value
        })

        val newValue = p.createValue()
        p.cell.value = newValue

        assertEquals(newValue, p.cell.value)
        assertEquals(newValue, observedValue)
    }

    interface Provider<T : Any> : CellTests.Provider {
        override val cell: MutableCell<T>

        /**
         * Returns a value that can be assigned to [cell] and that's different from
         * [cell]'s current and all previous values.
         */
        fun createValue(): T
    }
}

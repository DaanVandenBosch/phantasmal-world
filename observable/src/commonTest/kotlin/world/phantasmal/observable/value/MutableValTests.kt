package world.phantasmal.observable.value

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

interface MutableValTests<T : Any> : ValTests {
    override fun createProvider(): Provider<T>

    @Test
    fun emits_a_change_event_when_value_is_modified() = test {
        val p = createProvider()

        var observedValue: Any? = null

        disposer.add(p.observable.observe {
            assertNull(observedValue)
            observedValue = it.value
        })

        val newValue = p.createValue()
        p.observable.value = newValue

        assertEquals(newValue, p.observable.value)
        assertEquals(newValue, observedValue)
    }

    interface Provider<T : Any> : ValTests.Provider {
        override val observable: MutableVal<T>

        /**
         * Returns a value that can be assigned to [observable] and that's different from
         * [observable]'s current value.
         */
        fun createValue(): T
    }
}

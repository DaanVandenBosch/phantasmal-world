package world.phantasmal.lib.assembly.dataFlowAnalysis

import world.phantasmal.lib.test.LibTestSuite
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ValueSetTests : LibTestSuite() {
    @Test
    fun empty_set_has_size_0() {
        val vs = ValueSet.empty()

        assertEquals(0L, vs.size)
    }

    @Test
    fun get() {
        val vs = ValueSet.ofInterval(10, 13)
            .union(ValueSet.ofInterval(20, 22))

        assertEquals(7L, vs.size)
        assertEquals(10, vs[0])
        assertEquals(11, vs[1])
        assertEquals(12, vs[2])
        assertEquals(13, vs[3])
        assertEquals(20, vs[4])
        assertEquals(21, vs[5])
        assertEquals(22, vs[6])
    }

    @Test
    fun contains() {
        val vs = ValueSet.ofInterval(-20, 13)
            .union(ValueSet.ofInterval(20, 22))

        assertEquals(37L, vs.size)
        assertFalse(-9001 in vs)
        assertFalse(-21 in vs)
        assertTrue(-20 in vs)
        assertTrue(13 in vs)
        assertFalse(14 in vs)
        assertFalse(19 in vs)
        assertTrue(20 in vs)
        assertTrue(22 in vs)
        assertFalse(23 in vs)
        assertFalse(9001 in vs)
    }

    @Test
    fun setValue() {
        val vs = ValueSet.empty()
        vs.setValue(100)
        vs.setValue(4)
        vs.setValue(24324)

        assertEquals(1L, vs.size)
        assertEquals(24324, vs[0])
    }

    @Test
    fun plusAssign_integer_overflow() {
        // The set of all integers should stay the same after adding any integer.
        for (i in Int.MIN_VALUE..Int.MAX_VALUE step 10_000_000) {
            val vs = ValueSet.all()
            vs += i

            assertEquals(1L shl 32, vs.size)
            assertEquals(Int.MIN_VALUE, vs.minOrNull())
            assertEquals(Int.MAX_VALUE, vs.maxOrNull())
        }

        // Cause two intervals to split into three intervals.
        val vs = ValueSet.ofInterval(5, 7)
        vs.union(ValueSet.ofInterval(Int.MAX_VALUE - 2, Int.MAX_VALUE))
        vs += 1

        assertEquals(6L, vs.size)
        assertEquals(Int.MIN_VALUE, vs[0])
        assertEquals(6, vs[1])
        assertEquals(7, vs[2])
        assertEquals(8, vs[3])
        assertEquals(Int.MAX_VALUE - 1, vs[4])
        assertEquals(Int.MAX_VALUE, vs[5])

        // Cause part of one interval to be joined to another.
        vs.setInterval(Int.MIN_VALUE, Int.MIN_VALUE + 2)
        vs.union(ValueSet.ofInterval(Int.MAX_VALUE - 2, Int.MAX_VALUE))
        vs += 1

        assertEquals(6L, vs.size)
        assertEquals(Int.MIN_VALUE, vs[0])
        assertEquals(Int.MIN_VALUE + 1, vs[1])
        assertEquals(Int.MIN_VALUE + 2, vs[2])
        assertEquals(Int.MIN_VALUE + 3, vs[3])
        assertEquals(Int.MAX_VALUE - 1, vs[4])
        assertEquals(Int.MAX_VALUE, vs[5])
    }

    @Test
    fun minusAssign_integer_underflow() {
        // The set of all integers should stay the same after subtracting any integer.
        for (i in Int.MIN_VALUE..Int.MAX_VALUE step 10_000_000) {
            val vs = ValueSet.all()
            vs -= i

            assertEquals(1L shl 32, vs.size)
            assertEquals(Int.MIN_VALUE, vs.minOrNull())
            assertEquals(Int.MAX_VALUE, vs.maxOrNull())
        }

        // Cause two intervals to split into three intervals.
        val vs = ValueSet.ofInterval(Int.MIN_VALUE, Int.MIN_VALUE + 2)
        vs.union(ValueSet.ofInterval(5, 7))
        vs -= 1

        assertEquals(6L, vs.size)
        assertEquals(Int.MIN_VALUE, vs[0])
        assertEquals(Int.MIN_VALUE + 1, vs[1])
        assertEquals(4, vs[2])
        assertEquals(5, vs[3])
        assertEquals(6, vs[4])
        assertEquals(Int.MAX_VALUE, vs[5])

        // Cause part of one interval to be joined to another.
        vs.setInterval(Int.MIN_VALUE, Int.MIN_VALUE + 2)
        vs.union(ValueSet.ofInterval(Int.MAX_VALUE - 2, Int.MAX_VALUE))
        vs -= 1

        assertEquals(6L, vs.size)
        assertEquals(Int.MIN_VALUE, vs[0])
        assertEquals(Int.MIN_VALUE + 1, vs[1])
        assertEquals(Int.MAX_VALUE - 3, vs[2])
        assertEquals(Int.MAX_VALUE - 2, vs[3])
        assertEquals(Int.MAX_VALUE - 1, vs[4])
        assertEquals(Int.MAX_VALUE, vs[5])
    }

    @Test
    fun union() {
        val vs = ValueSet.empty()
            .union(ValueSet.of(21))
            .union(ValueSet.of(4968))

        assertEquals(2L, vs.size)
        assertEquals(21, vs[0])
        assertEquals(4968, vs[1])
    }

    @Test
    fun union_of_intervals() {
        val vs = ValueSet.empty()
            .union(ValueSet.ofInterval(10, 12))
            .union(ValueSet.ofInterval(14, 16))

        assertEquals(6L, vs.size)
        assertTrue(arrayOf(10, 11, 12, 14, 15, 16).all { it in vs })

        vs.union(ValueSet.ofInterval(13, 13))

        assertEquals(7L, vs.size)
        assertEquals(10, vs[0])
        assertEquals(11, vs[1])
        assertEquals(12, vs[2])
        assertEquals(13, vs[3])
        assertEquals(14, vs[4])
        assertEquals(15, vs[5])
        assertEquals(16, vs[6])

        vs.union(ValueSet.ofInterval(1, 2))

        assertEquals(9L, vs.size)
        assertTrue(arrayOf(1, 2, 10, 11, 12, 13, 14, 15, 16).all { it in vs })

        vs.union(ValueSet.ofInterval(30, 32))

        assertEquals(12L, vs.size)
        assertTrue(arrayOf(1, 2, 10, 11, 12, 13, 14, 15, 16, 30, 31, 32).all { it in vs })

        vs.union(ValueSet.ofInterval(20, 21))

        assertEquals(14L, vs.size)
        assertTrue(arrayOf(1, 2, 10, 11, 12, 13, 14, 15, 16, 20, 21, 30, 31, 32).all { it in vs })
    }

    @Test
    fun iterator() {
        val vs = ValueSet.empty()
            .union(ValueSet.ofInterval(5, 7))
            .union(ValueSet.ofInterval(14, 16))

        val iter = vs.iterator()

        assertTrue(iter.hasNext())
        assertEquals(5, iter.next())
        assertTrue(iter.hasNext())
        assertEquals(6, iter.next())
        assertTrue(iter.hasNext())
        assertEquals(7, iter.next())
        assertTrue(iter.hasNext())
        assertEquals(14, iter.next())
        assertTrue(iter.hasNext())
        assertEquals(15, iter.next())
        assertTrue(iter.hasNext())
        assertEquals(16, iter.next())
        assertFalse(iter.hasNext())
    }
}

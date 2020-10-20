package world.phantasmal.lib.assembly.dataFlowAnalysis

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ValueSetTests {
    @Test
    fun empty_set_has_size_0() {
        val vs = ValueSet()

        assertEquals(0, vs.size)
    }

    @Test
    fun get() {
        val vs = ValueSet().setInterval(10, 13)
            .union(ValueSet().setInterval(20, 22))

        assertEquals(7, vs.size)
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
        val vs = ValueSet().setInterval(-20, 13)
            .union(ValueSet().setInterval(20, 22))

        assertEquals(37, vs.size)
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
        val vs = ValueSet()
        vs.setValue(100)
        vs.setValue(4)
        vs.setValue(24324)

        assertEquals(1, vs.size)
        assertEquals(24324, vs[0])
    }

    @Test
    fun union() {
        val vs = ValueSet()
            .union(ValueSet().setValue(21))
            .union(ValueSet().setValue(4968))

        assertEquals(2, vs.size)
        assertEquals(21, vs[0])
        assertEquals(4968, vs[1])
    }

    @Test
    fun union_of_intervals() {
        val vs = ValueSet()
            .union(ValueSet().setInterval(10, 12))
            .union(ValueSet().setInterval(14, 16))

        assertEquals(6, vs.size)
        assertTrue(arrayOf(10, 11, 12, 14, 15, 16).all { it in vs })

        vs.union(ValueSet().setInterval(13, 13))

        assertEquals(7, vs.size)
        assertEquals(10, vs[0])
        assertEquals(11, vs[1])
        assertEquals(12, vs[2])
        assertEquals(13, vs[3])
        assertEquals(14, vs[4])
        assertEquals(15, vs[5])
        assertEquals(16, vs[6])

        vs.union(ValueSet().setInterval(1, 2))

        assertEquals(9, vs.size)
        assertTrue(arrayOf(1, 2, 10, 11, 12, 13, 14, 15, 16).all { it in vs })

        vs.union(ValueSet().setInterval(30, 32))

        assertEquals(12, vs.size)
        assertTrue(arrayOf(1, 2, 10, 11, 12, 13, 14, 15, 16, 30, 31, 32).all { it in vs })

        vs.union(ValueSet().setInterval(20, 21))

        assertEquals(14, vs.size)
        assertTrue(arrayOf(1, 2, 10, 11, 12, 13, 14, 15, 16, 20, 21, 30, 31, 32).all { it in vs })
    }

    @Test
    fun iterator() {
        val vs = ValueSet()
            .union(ValueSet().setInterval(5, 7))
            .union(ValueSet().setInterval(14, 16))

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

package world.phantasmal.lib.buffer

import world.phantasmal.lib.Endianness
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BufferTests {
    @Test
    fun simple_properties_and_invariants() {
        val capacity = 500u
        val buffer = Buffer.withCapacity(capacity)

        assertEquals(0u, buffer.size)
        assertEquals(capacity, buffer.capacity)
        assertEquals(Endianness.Little, buffer.endianness)
    }

    @Test
    fun reallocates_internal_storage_when_necessary() {
        val buffer = Buffer.withCapacity(100u)

        assertEquals(0u, buffer.size)
        assertEquals(100u, buffer.capacity)

        buffer.size = 101u

        assertEquals(101u, buffer.size)
        assertTrue(buffer.capacity >= 101u)

        buffer.setU8(100u, (0xABu).toUByte())

        assertEquals(0xABu, buffer.getU8(100u).toUInt())
    }
}

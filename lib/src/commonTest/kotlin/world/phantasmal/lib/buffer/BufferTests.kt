package world.phantasmal.lib.buffer

import world.phantasmal.lib.Endianness
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BufferTests {
    @Test
    fun withCapacity() {
        val capacity = 500
        val buffer = Buffer.withCapacity(capacity)

        assertEquals(0, buffer.size)
        assertEquals(capacity, buffer.capacity)
        assertEquals(Endianness.Little, buffer.endianness)
    }

    @Test
    fun withSize() {
        val size = 500
        val buffer = Buffer.withSize(size)

        assertEquals(size, buffer.size)
        assertEquals(size, buffer.capacity)
        assertEquals(Endianness.Little, buffer.endianness)
    }

    @Test
    fun reallocates_internal_storage_when_necessary() {
        val buffer = Buffer.withCapacity(100)

        assertEquals(0, buffer.size)
        assertEquals(100, buffer.capacity)

        buffer.size = 101

        assertEquals(101, buffer.size)
        assertTrue(buffer.capacity >= 101)

        buffer.setU8(100, (0xABu).toUByte())

        assertEquals(0xABu, buffer.getU8(100).toUInt())
    }

    @Test
    fun fill_and_zero() {
        val buffer = Buffer.withSize(100)

        buffer.fill(100)

        for (i in 0 until buffer.size) {
            assertEquals(100u, buffer.getU8(i))
        }

        buffer.zero()

        for (i in 0 until buffer.size) {
            assertEquals(0u, buffer.getU8(i))
        }
    }
}

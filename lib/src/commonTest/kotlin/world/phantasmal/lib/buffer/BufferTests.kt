package world.phantasmal.lib.buffer

import world.phantasmal.lib.Endianness
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BufferTests {
    @Test
    fun withCapacity() {
        withCapacity(Endianness.Little)
        withCapacity(Endianness.Big)
    }

    private fun withCapacity(endianness: Endianness) {
        val capacity = 500
        val buffer = Buffer.withCapacity(capacity, endianness)

        assertEquals(0, buffer.size)
        assertEquals(capacity, buffer.capacity)
        assertEquals(endianness, buffer.endianness)
    }

    @Test
    fun withSize() {
        withSize(Endianness.Little)
        withSize(Endianness.Big)
    }

    private fun withSize(endianness: Endianness) {
        val size = 500
        val buffer = Buffer.withSize(size, endianness)

        assertEquals(size, buffer.size)
        assertEquals(size, buffer.capacity)
        assertEquals(endianness, buffer.endianness)
    }

    @Test
    fun reallocates_internal_storage_when_necessary() {
        reallocates_internal_storage_when_necessary(Endianness.Little)
        reallocates_internal_storage_when_necessary(Endianness.Big)
    }

    private fun reallocates_internal_storage_when_necessary(endianness: Endianness) {
        val buffer = Buffer.withCapacity(100, endianness)

        assertEquals(0, buffer.size)
        assertEquals(100, buffer.capacity)

        buffer.size = 101

        assertEquals(101, buffer.size)
        assertTrue(buffer.capacity >= 101)

        buffer.setUByte(100, (0xABu).toUByte())

        assertEquals(0xABu, buffer.getUByte(100).toUInt())
        assertEquals(endianness, buffer.endianness)
    }

    @Test
    fun fill_and_zero() {
        val buffer = Buffer.withSize(100)

        buffer.fillByte(100)

        for (i in 0 until buffer.size) {
            assertEquals(100, buffer.getByte(i))
        }

        buffer.zero()

        for (i in 0 until buffer.size) {
            assertEquals(0, buffer.getByte(i))
        }
    }
}

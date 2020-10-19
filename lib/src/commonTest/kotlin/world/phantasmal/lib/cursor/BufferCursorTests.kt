package world.phantasmal.lib.cursor

import world.phantasmal.lib.Endianness
import world.phantasmal.lib.buffer.Buffer
import kotlin.test.Test
import kotlin.test.assertEquals

class BufferCursorTests : WritableCursorTests() {
    override fun createCursor(bytes: ByteArray, endianness: Endianness) =
        BufferCursor(Buffer.fromByteArray(bytes, endianness))

    @Test
    fun writeU8_increases_size_correctly() {
        testIntegerWriteSize(1, { writeU8(it.toUByte()) }, Endianness.Little)
        testIntegerWriteSize(1, { writeU8(it.toUByte()) }, Endianness.Big)
    }

    @Test
    fun writeU16_increases_size_correctly() {
        testIntegerWriteSize(2, { writeU16(it.toUShort()) }, Endianness.Little)
        testIntegerWriteSize(2, { writeU16(it.toUShort()) }, Endianness.Big)
    }

    @Test
    fun writeU32_increases_size_correctly() {
        testIntegerWriteSize(4, { writeU32(it.toUInt()) }, Endianness.Little)
        testIntegerWriteSize(4, { writeU32(it.toUInt()) }, Endianness.Big)
    }

    @Test
    fun writeI8_increases_size_correctly() {
        testIntegerWriteSize(1, { writeI8(it.toByte()) }, Endianness.Little)
        testIntegerWriteSize(1, { writeI8(it.toByte()) }, Endianness.Big)
    }

    @Test
    fun writeI16_increases_size_correctly() {
        testIntegerWriteSize(2, { writeI16(it.toShort()) }, Endianness.Little)
        testIntegerWriteSize(2, { writeI16(it.toShort()) }, Endianness.Big)
    }

    @Test
    fun writeI32_increases_size_correctly() {
        testIntegerWriteSize(4, { writeI32(it) }, Endianness.Little)
        testIntegerWriteSize(4, { writeI32(it) }, Endianness.Big)
    }

    private fun testIntegerWriteSize(
        byteCount: Int,
        write: BufferCursor.(Int) -> Unit,
        endianness: Endianness,
    ) {
        val expectedNumber1 = 7891378
        val expectedNumber2 = 893894273

        val buffer = Buffer.withCapacity(8u, endianness)
        val cursor = BufferCursor(buffer)

        assertEquals(0u, buffer.size)
        assertEquals(0u, cursor.size)

        cursor.write(expectedNumber1)

        assertEquals(byteCount.toUInt(), buffer.size)
        assertEquals(byteCount.toUInt(), cursor.size)

        cursor.write(expectedNumber2)

        assertEquals(2u * byteCount.toUInt(), buffer.size)
        assertEquals(2u * byteCount.toUInt(), cursor.size)
    }
}

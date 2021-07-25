package world.phantasmal.psolib.cursor

import world.phantasmal.psolib.Endianness
import world.phantasmal.psolib.buffer.Buffer
import kotlin.test.Test
import kotlin.test.assertEquals

class BufferCursorTests : WritableCursorTests() {
    override fun createCursor(bytes: ByteArray, endianness: Endianness, size: Int) =
        BufferCursor(Buffer.fromByteArray(bytes, endianness), size = size)

    @Test
    fun writeUByte_increases_size_correctly() {
        testIntegerWriteSize(1, { writeUByte(it.toUByte()) }, Endianness.Little)
        testIntegerWriteSize(1, { writeUByte(it.toUByte()) }, Endianness.Big)
    }

    @Test
    fun writeUShort_increases_size_correctly() {
        testIntegerWriteSize(2, { writeUShort(it.toUShort()) }, Endianness.Little)
        testIntegerWriteSize(2, { writeUShort(it.toUShort()) }, Endianness.Big)
    }

    @Test
    fun writeUInt_increases_size_correctly() {
        testIntegerWriteSize(4, { writeUInt(it.toUInt()) }, Endianness.Little)
        testIntegerWriteSize(4, { writeUInt(it.toUInt()) }, Endianness.Big)
    }

    @Test
    fun writeByte_increases_size_correctly() {
        testIntegerWriteSize(1, { writeByte(it.toByte()) }, Endianness.Little)
        testIntegerWriteSize(1, { writeByte(it.toByte()) }, Endianness.Big)
    }

    @Test
    fun writeShort_increases_size_correctly() {
        testIntegerWriteSize(2, { writeShort(it.toShort()) }, Endianness.Little)
        testIntegerWriteSize(2, { writeShort(it.toShort()) }, Endianness.Big)
    }

    @Test
    fun writeInt_increases_size_correctly() {
        testIntegerWriteSize(4, { writeInt(it) }, Endianness.Little)
        testIntegerWriteSize(4, { writeInt(it) }, Endianness.Big)
    }

    private fun testIntegerWriteSize(
        byteCount: Int,
        write: BufferCursor.(Int) -> Unit,
        endianness: Endianness,
    ) {
        val expectedNumber1 = 7891378
        val expectedNumber2 = 893894273

        val buffer = Buffer.withCapacity(8, endianness)
        val cursor = BufferCursor(buffer)

        assertEquals(0, buffer.size)
        assertEquals(0, cursor.size)

        cursor.write(expectedNumber1)

        assertEquals(byteCount, buffer.size)
        assertEquals(byteCount, cursor.size)

        cursor.write(expectedNumber2)

        assertEquals(2 * byteCount, buffer.size)
        assertEquals(2 * byteCount, cursor.size)
    }
}

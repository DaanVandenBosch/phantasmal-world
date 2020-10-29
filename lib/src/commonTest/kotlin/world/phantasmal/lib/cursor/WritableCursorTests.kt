package world.phantasmal.lib.cursor

import world.phantasmal.lib.Endianness
import world.phantasmal.lib.buffer.Buffer
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

abstract class WritableCursorTests : CursorTests() {
    abstract override fun createCursor(bytes: ByteArray, endianness: Endianness): WritableCursor

    @Test
    fun simple_WritableCursor_properties_and_invariants() {
        simple_WritableCursor_properties_and_invariants(Endianness.Little)
        simple_WritableCursor_properties_and_invariants(Endianness.Big)
    }

    private fun simple_WritableCursor_properties_and_invariants(endianness: Endianness) {
        val cursor = createCursor(byteArrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9), endianness)

        assertEquals(0, cursor.position)

        cursor.writeUByte(99u).writeUByte(99u).writeUByte(99u).writeUByte(99u)
        cursor.seek(-1)

        assertEquals(cursor.position + cursor.bytesLeft, cursor.size)
        assertEquals(10, cursor.size)
        assertEquals(3, cursor.position)
        assertEquals(7, cursor.bytesLeft)
        assertEquals(endianness, cursor.endianness)
    }

    @Test
    fun writeUByte() {
        testIntegerWrite(1, { uByte().toInt() }, { writeUByte(it.toUByte()) }, Endianness.Little)
        testIntegerWrite(1, { uByte().toInt() }, { writeUByte(it.toUByte()) }, Endianness.Big)
    }

    @Test
    fun writeUShort() {
        testIntegerWrite(2, { uShort().toInt() }, { writeUShort(it.toUShort()) }, Endianness.Little)
        testIntegerWrite(2, { uShort().toInt() }, { writeUShort(it.toUShort()) }, Endianness.Big)
    }

    @Test
    fun writeUInt() {
        testIntegerWrite(4, { uInt().toInt() }, { writeUInt(it.toUInt()) }, Endianness.Little)
        testIntegerWrite(4, { uInt().toInt() }, { writeUInt(it.toUInt()) }, Endianness.Big)
    }

    @Test
    fun writeByte() {
        testIntegerWrite(1, { byte().toInt() }, { writeByte(it.toByte()) }, Endianness.Little)
        testIntegerWrite(1, { byte().toInt() }, { writeByte(it.toByte()) }, Endianness.Big)
    }

    @Test
    fun writeShort() {
        testIntegerWrite(2, { short().toInt() }, { writeShort(it.toShort()) }, Endianness.Little)
        testIntegerWrite(2, { short().toInt() }, { writeShort(it.toShort()) }, Endianness.Big)
    }

    @Test
    fun writeInt() {
        testIntegerWrite(4, { int() }, { writeInt(it) }, Endianness.Little)
        testIntegerWrite(4, { int() }, { writeInt(it) }, Endianness.Big)
    }

    /**
     * Writes and reads two integers.
     */
    private fun testIntegerWrite(
        byteCount: Int,
        read: Cursor.() -> Int,
        write: WritableCursor.(Int) -> Unit,
        endianness: Endianness,
    ) {
        val expectedNumber1 = 0x01020304 shr (8 * (4 - byteCount))
        val expectedNumber2 = 0x05060708 shr (8 * (4 - byteCount))

        val cursor = createCursor(ByteArray(2 * byteCount), endianness)

        cursor.write(expectedNumber1)
        cursor.write(expectedNumber2)

        assertEquals(2 * byteCount, cursor.position)

        cursor.seekStart(0)

        assertEquals(expectedNumber1, cursor.read())
        assertEquals(expectedNumber2, cursor.read())
    }

    @Test
    fun writeFloat() {
        writeFloat(Endianness.Little)
        writeFloat(Endianness.Big)
    }

    /**
     * Writes and reads two floats.
     */
    private fun writeFloat(endianness: Endianness) {
        val cursor = createCursor(ByteArray(8), endianness)

        cursor.writeFloat(1337.9001f)
        cursor.writeFloat(103.502f)

        assertEquals(8, cursor.position)

        cursor.seekStart(0)

        // The read floats won't be exactly the same as the written floats in Kotlin JS, because
        // they're backed by numbers (64-bit floats).
        assertTrue(abs(1337.9001f - cursor.float()) < 0.001)
        assertTrue(abs(103.502f - cursor.float()) < 0.001)

        assertEquals(8, cursor.position)
    }

    @Test
    fun writeUByteArray() {
        val read: Cursor.(Int) -> IntArray = { n ->
            val arr = uByteArray(n)
            IntArray(n) { arr[it].toInt() }
        }
        val write: WritableCursor.(IntArray) -> Unit = { a ->
            writeUByteArray(UByteArray(a.size) { a[it].toUByte() })
        }

        testIntegerArrayWrite(1, read, write, Endianness.Little)
        testIntegerArrayWrite(1, read, write, Endianness.Big)
    }

    @Test
    fun writeUShortArray() {
        val read: Cursor.(Int) -> IntArray = { n ->
            val arr = uShortArray(n)
            IntArray(n) { arr[it].toInt() }
        }
        val write: WritableCursor.(IntArray) -> Unit = { a ->
            writeUShortArray(UShortArray(a.size) { a[it].toUShort() })
        }

        testIntegerArrayWrite(2, read, write, Endianness.Little)
        testIntegerArrayWrite(2, read, write, Endianness.Big)
    }

    @Test
    fun writeUIntArray() {
        val read: Cursor.(Int) -> IntArray = { n ->
            val arr = uIntArray(n)
            IntArray(n) { arr[it].toInt() }
        }
        val write: WritableCursor.(IntArray) -> Unit = { a ->
            writeUIntArray(UIntArray(a.size) { a[it].toUInt() })
        }

        testIntegerArrayWrite(4, read, write, Endianness.Little)
        testIntegerArrayWrite(4, read, write, Endianness.Big)
    }

    @Test
    fun writeByteArray() {
        val read: Cursor.(Int) -> IntArray = { n ->
            val arr = byteArray(n)
            IntArray(n) { arr[it].toInt() }
        }
        val write: WritableCursor.(IntArray) -> Unit = { a ->
            writeByteArray(ByteArray(a.size) { a[it].toByte() })
        }

        testIntegerArrayWrite(1, read, write, Endianness.Little)
        testIntegerArrayWrite(1, read, write, Endianness.Big)
    }

    @Test
    fun writeIntArray() {
        val read: Cursor.(Int) -> IntArray = { n ->
            intArray(n)
        }
        val write: WritableCursor.(IntArray) -> Unit = { a ->
            writeIntArray(a)
        }

        testIntegerArrayWrite(4, read, write, Endianness.Little)
        testIntegerArrayWrite(4, read, write, Endianness.Big)
    }

    private fun testIntegerArrayWrite(
        byteCount: Int,
        read: Cursor.(Int) -> IntArray,
        write: WritableCursor.(IntArray) -> Unit,
        endianness: Endianness,
    ) {
        val testArray1 = IntArray(10) { it }
        val testArray2 = IntArray(10) { it + 10 }

        val cursor = createCursor(ByteArray(20 * byteCount), endianness)

        cursor.write(testArray1)
        assertEquals(10 * byteCount, cursor.position)

        cursor.write(testArray2)
        assertEquals(20 * byteCount, cursor.position)

        cursor.seekStart(0)

        assertTrue(testArray1.contentEquals(cursor.read(10)))
        assertTrue(testArray2.contentEquals(cursor.read(10)))
        assertEquals(20 * byteCount, cursor.position)
    }

    @Test
    fun writeCursor() {
        testWriteCursor(Endianness.Little)
        testWriteCursor(Endianness.Big)
    }

    private fun testWriteCursor(endianness: Endianness) {
        val cursor = createCursor(ByteArray(8), endianness)

        cursor.seek(2)
        cursor.writeCursor(Buffer.fromByteArray(byteArrayOf(1, 2, 3, 4)).cursor())

        assertEquals(6, cursor.position)

        cursor.seekStart(0)

        assertEquals(0, cursor.byte())
        assertEquals(0, cursor.byte())
        assertEquals(1, cursor.byte())
        assertEquals(2, cursor.byte())
        assertEquals(3, cursor.byte())
        assertEquals(4, cursor.byte())
        assertEquals(0, cursor.byte())
        assertEquals(0, cursor.byte())
    }

    @Test
    fun write_seek_backwards_then_take() {
        write_seek_backwards_then_take(Endianness.Little)
        write_seek_backwards_then_take(Endianness.Big)
    }

    private fun write_seek_backwards_then_take(endianness: Endianness) {
        val cursor = createCursor(ByteArray(16), endianness)

        cursor.writeUInt(1u).writeUInt(2u).writeUInt(3u).writeUInt(4u)
        cursor.seek(-8)
        val newCursor = cursor.take(8)

        assertEquals(16, cursor.position)
        assertEquals(8, newCursor.size)
        assertEquals(0, newCursor.position)
        assertEquals(3u, newCursor.uInt())
        assertEquals(4u, newCursor.uInt())
    }
}

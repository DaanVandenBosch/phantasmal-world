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

        cursor.writeU8(99u).writeU8(99u).writeU8(99u).writeU8(99u)
        cursor.seek(-1)

        assertEquals(cursor.position + cursor.bytesLeft, cursor.size)
        assertEquals(10, cursor.size)
        assertEquals(3, cursor.position)
        assertEquals(7, cursor.bytesLeft)
        assertEquals(endianness, cursor.endianness)
    }

    @Test
    fun writeU8() {
        testIntegerWrite(1, { u8().toInt() }, { writeU8(it.toUByte()) }, Endianness.Little)
        testIntegerWrite(1, { u8().toInt() }, { writeU8(it.toUByte()) }, Endianness.Big)
    }

    @Test
    fun writeU16() {
        testIntegerWrite(2, { u16().toInt() }, { writeU16(it.toUShort()) }, Endianness.Little)
        testIntegerWrite(2, { u16().toInt() }, { writeU16(it.toUShort()) }, Endianness.Big)
    }

    @Test
    fun writeU32() {
        testIntegerWrite(4, { u32().toInt() }, { writeU32(it.toUInt()) }, Endianness.Little)
        testIntegerWrite(4, { u32().toInt() }, { writeU32(it.toUInt()) }, Endianness.Big)
    }

    @Test
    fun writeI8() {
        testIntegerWrite(1, { i8().toInt() }, { writeI8(it.toByte()) }, Endianness.Little)
        testIntegerWrite(1, { i8().toInt() }, { writeI8(it.toByte()) }, Endianness.Big)
    }

    @Test
    fun writeI16() {
        testIntegerWrite(2, { i16().toInt() }, { writeI16(it.toShort()) }, Endianness.Little)
        testIntegerWrite(2, { i16().toInt() }, { writeI16(it.toShort()) }, Endianness.Big)
    }

    @Test
    fun writeI32() {
        testIntegerWrite(4, { i32() }, { writeI32(it) }, Endianness.Little)
        testIntegerWrite(4, { i32() }, { writeI32(it) }, Endianness.Big)
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
    fun writeF32() {
        writeF32(Endianness.Little)
        writeF32(Endianness.Big)
    }

    /**
     * Writes and reads two floats.
     */
    private fun writeF32(endianness: Endianness) {
        val cursor = createCursor(ByteArray(8), endianness)

        cursor.writeF32(1337.9001f)
        cursor.writeF32(103.502f)

        assertEquals(8, cursor.position)

        cursor.seekStart(0)

        // The read floats won't be exactly the same as the written floats in Kotlin JS, because
        // they're backed by numbers (64-bit floats).
        assertTrue(abs(1337.9001f - cursor.f32()) < 0.001)
        assertTrue(abs(103.502f - cursor.f32()) < 0.001)

        assertEquals(8, cursor.position)
    }

    @Test
    fun writeU8Array() {
        val read: Cursor.(Int) -> IntArray = { n ->
            val arr = u8Array(n)
            IntArray(n) { arr[it].toInt() }
        }
        val write: WritableCursor.(IntArray) -> Unit = { a ->
            writeU8Array(UByteArray(a.size) { a[it].toUByte() })
        }

        testIntegerArrayWrite(1, read, write, Endianness.Little)
        testIntegerArrayWrite(1, read, write, Endianness.Big)
    }

    @Test
    fun writeU16Array() {
        val read: Cursor.(Int) -> IntArray = { n ->
            val arr = u16Array(n)
            IntArray(n) { arr[it].toInt() }
        }
        val write: WritableCursor.(IntArray) -> Unit = { a ->
            writeU16Array(UShortArray(a.size) { a[it].toUShort() })
        }

        testIntegerArrayWrite(2, read, write, Endianness.Little)
        testIntegerArrayWrite(2, read, write, Endianness.Big)
    }

    @Test
    fun writeU32Array() {
        val read: Cursor.(Int) -> IntArray = { n ->
            val arr = u32Array(n)
            IntArray(n) { arr[it].toInt() }
        }
        val write: WritableCursor.(IntArray) -> Unit = { a ->
            writeU32Array(UIntArray(a.size) { a[it].toUInt() })
        }

        testIntegerArrayWrite(4, read, write, Endianness.Little)
        testIntegerArrayWrite(4, read, write, Endianness.Big)
    }

    @Test
    fun writeI32Array() {
        val read: Cursor.(Int) -> IntArray = { n ->
            i32Array(n)
        }
        val write: WritableCursor.(IntArray) -> Unit = { a ->
            writeI32Array(a)
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

        assertEquals(0, cursor.i8())
        assertEquals(0, cursor.i8())
        assertEquals(1, cursor.i8())
        assertEquals(2, cursor.i8())
        assertEquals(3, cursor.i8())
        assertEquals(4, cursor.i8())
        assertEquals(0, cursor.i8())
        assertEquals(0, cursor.i8())
    }

    @Test
    fun write_seek_backwards_then_take() {
        write_seek_backwards_then_take(Endianness.Little)
        write_seek_backwards_then_take(Endianness.Big)
    }

    private fun write_seek_backwards_then_take(endianness: Endianness) {
        val cursor = createCursor(ByteArray(16), endianness)

        cursor.writeU32(1u).writeU32(2u).writeU32(3u).writeU32(4u)
        cursor.seek(-8)
        val newCursor = cursor.take(8)

        assertEquals(16, cursor.position)
        assertEquals(8, newCursor.size)
        assertEquals(0, newCursor.position)
        assertEquals(3u, newCursor.u32())
        assertEquals(4u, newCursor.u32())
    }
}

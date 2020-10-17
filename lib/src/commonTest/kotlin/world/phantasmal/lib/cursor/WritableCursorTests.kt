package world.phantasmal.lib.cursor

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

abstract class WritableCursorTests : CursorTests() {
    abstract override fun createCursor(bytes: Array<Byte>, endianness: Endianness): WritableCursor

    @Test
    fun simple_WritableCursor_properties_and_invariants() {
        simple_WritableCursor_properties_and_invariants(Endianness.Little)
        simple_WritableCursor_properties_and_invariants(Endianness.Big)
    }

    private fun simple_WritableCursor_properties_and_invariants(endianness: Endianness) {
        val cursor = createCursor(arrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9), endianness)

        assertEquals(0u, cursor.position)

        cursor.writeU8(99u).writeU8(99u).writeU8(99u).writeU8(99u)
        cursor.seek(-1)

        assertEquals(cursor.position + cursor.bytesLeft, cursor.size)
        assertEquals(10u, cursor.size)
        assertEquals(3u, cursor.position)
        assertEquals(7u, cursor.bytesLeft)
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

        val cursor = createCursor(Array(2 * byteCount) { 0 }, endianness)

        cursor.write(expectedNumber1)
        cursor.write(expectedNumber2)

        assertEquals((2 * byteCount).toUInt(), cursor.position)

        cursor.seekStart(0u)

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
        val cursor = createCursor(Array(8) { 0 }, endianness)

        cursor.writeF32(1337.9001f)
        cursor.writeF32(103.502f)

        assertEquals(8u, cursor.position)

        cursor.seekStart(0u)

        // The read floats won't be exactly the same as the written floats in Kotlin JS, because
        // they're backed by numbers (64-bit floats).
        assertTrue(abs(1337.9001f - cursor.f32()) < 0.001)
        assertTrue(abs(103.502f - cursor.f32()) < 0.001)

        assertEquals(8u, cursor.position)
    }

    @Test
    fun writeU8Array() {
        val read: Cursor.(UInt) -> IntArray = { n ->
            val arr = u8Array(n)
            IntArray(n.toInt()) { arr[it].toInt() }
        }
        val write: WritableCursor.(IntArray) -> Unit = { a ->
            writeU8Array(UByteArray(a.size) { a[it].toUByte() })
        }

        testIntegerArrayWrite(1, read, write, Endianness.Little)
        testIntegerArrayWrite(1, read, write, Endianness.Big)
    }

    @Test
    fun writeU16Array() {
        val read: Cursor.(UInt) -> IntArray = { n ->
            val arr = u16Array(n)
            IntArray(n.toInt()) { arr[it].toInt() }
        }
        val write: WritableCursor.(IntArray) -> Unit = { a ->
            writeU16Array(UShortArray(a.size) { a[it].toUShort() })
        }

        testIntegerArrayWrite(2, read, write, Endianness.Little)
        testIntegerArrayWrite(2, read, write, Endianness.Big)
    }

    @Test
    fun writeU32Array() {
        val read: Cursor.(UInt) -> IntArray = { n ->
            val arr = u32Array(n)
            IntArray(n.toInt()) { arr[it].toInt() }
        }
        val write: WritableCursor.(IntArray) -> Unit = { a ->
            writeU32Array(UIntArray(a.size) { a[it].toUInt() })
        }

        testIntegerArrayWrite(4, read, write, Endianness.Little)
        testIntegerArrayWrite(4, read, write, Endianness.Big)
    }

    @Test
    fun writeI32Array() {
        val read: Cursor.(UInt) -> IntArray = { n ->
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
        read: Cursor.(UInt) -> IntArray,
        write: WritableCursor.(IntArray) -> Unit,
        endianness: Endianness,
    ) {
        val testArray1 = IntArray(10) { it }
        val testArray2 = IntArray(10) { it + 10 }

        val cursor = createCursor(Array(20 * byteCount) { 0 }, endianness)

        cursor.write(testArray1)
        assertEquals(10u * byteCount.toUInt(), cursor.position)

        cursor.write(testArray2)
        assertEquals(20u * byteCount.toUInt(), cursor.position)

        cursor.seekStart(0u)

        assertTrue(testArray1.contentEquals(cursor.read(10u)))
        assertTrue(testArray2.contentEquals(cursor.read(10u)))
        assertEquals(20u * byteCount.toUInt(), cursor.position)
    }

    @Test
    fun write_seek_backwards_then_take() {
        write_seek_backwards_then_take(Endianness.Little)
        write_seek_backwards_then_take(Endianness.Big)
    }

    private fun write_seek_backwards_then_take(endianness: Endianness) {
        val cursor = createCursor(Array(16) { 0 }, endianness)

        cursor.writeU32(1u).writeU32(2u).writeU32(3u).writeU32(4u)
        cursor.seek(-8)
        val newCursor = cursor.take(8u)

        assertEquals(8u, newCursor.size)
        assertEquals(0u, newCursor.position)
        assertEquals(3u, newCursor.u32())
        assertEquals(4u, newCursor.u32())
    }
}

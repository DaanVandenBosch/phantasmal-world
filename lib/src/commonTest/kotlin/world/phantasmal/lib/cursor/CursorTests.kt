package world.phantasmal.lib.cursor

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Test suite for all [Cursor] implementations. There is a subclass of this suite for every [Cursor]
 * implementation.
 */
abstract class CursorTests {
    abstract fun createCursor(bytes: Array<Byte>, endianness: Endianness): Cursor

    @Test
    fun simple_cursor_properties_and_invariants() {
        simple_cursor_properties_and_invariants(Endianness.Little)
        simple_cursor_properties_and_invariants(Endianness.Big)
    }

    private fun simple_cursor_properties_and_invariants(endianness: Endianness) {
        val cursor = createCursor(arrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9), endianness)

        for ((seek_to, expectedPos) in listOf(
            0 to 0u,
            3 to 3u,
            5 to 8u,
            2 to 10u,
            -10 to 0u,
        )) {
            cursor.seek(seek_to)

            assertEquals(10u, cursor.size)
            assertEquals(expectedPos, cursor.position)
            assertEquals(cursor.position + cursor.bytesLeft, cursor.size)
            assertEquals(endianness, cursor.endianness)
        }
    }

    @Test
    fun cursor_handles_byte_order_correctly() {
        cursor_handles_byte_order_correctly(Endianness.Little)
        cursor_handles_byte_order_correctly(Endianness.Big)
    }

    private fun cursor_handles_byte_order_correctly(endianness: Endianness) {
        val cursor = createCursor(arrayOf(1, 2, 3, 4), endianness)

        if (endianness == Endianness.Little) {
            assertEquals(0x04030201u, cursor.u32())
        } else {
            assertEquals(0x01020304u, cursor.u32())
        }
    }

    @Test
    fun u8() {
        testIntegerRead(1, { u8().toInt() }, Endianness.Little)
        testIntegerRead(1, { u8().toInt() }, Endianness.Big)
    }

    @Test
    fun u16() {
        testIntegerRead(2, { u16().toInt() }, Endianness.Little)
        testIntegerRead(2, { u16().toInt() }, Endianness.Big)
    }

    @Test
    fun u32() {
        testIntegerRead(4, { u32().toInt() }, Endianness.Little)
        testIntegerRead(4, { u32().toInt() }, Endianness.Big)
    }

    @Test
    fun i8() {
        testIntegerRead(1, { i8().toInt() }, Endianness.Little)
        testIntegerRead(1, { i8().toInt() }, Endianness.Big)
    }

    @Test
    fun i16() {
        testIntegerRead(2, { i16().toInt() }, Endianness.Little)
        testIntegerRead(2, { i16().toInt() }, Endianness.Big)
    }

    @Test
    fun i32() {
        testIntegerRead(4, { i32() }, Endianness.Little)
        testIntegerRead(4, { i32() }, Endianness.Big)
    }

    /**
     * Reads two integers.
     */
    private fun testIntegerRead(byteCount: Int, read: Cursor.() -> Int, endianness: Endianness) {
        // Generate two numbers of the form 0x010203...
        val expectedNumber1 = 0x01020304 shr (8 * (4 - byteCount))
        val expectedNumber2 = 0x05060708 shr (8 * (4 - byteCount))

        // Put them in a byte array.
        val bytes = Array<Byte>(2 * byteCount) { 0 }

        for (i in 0 until byteCount) {
            val shift =
                if (endianness == Endianness.Little) {
                    8 * i
                } else {
                    8 * (byteCount - i - 1)
                }

            bytes[i] = (expectedNumber1 shr shift).toByte()
            bytes[byteCount + i] = (expectedNumber2 shr shift).toByte()
        }

        // Check that individual bytes are in the correct order when read as part of a larger
        // integer.
        val cursor = createCursor(bytes, endianness)

        assertEquals(expectedNumber1, cursor.read())
        assertEquals(byteCount.toUInt(), cursor.position)

        assertEquals(expectedNumber2, cursor.read())
        assertEquals(2u * byteCount.toUInt(), cursor.position)
    }

    @Test
    fun f32() {
        f32(Endianness.Little)
        f32(Endianness.Big)
    }

    private fun f32(endianness: Endianness) {
        val bytes = arrayOf<Byte>(0x40, 0x20, 0, 0, 0x42, 1, 0, 0)

        if (endianness == Endianness.Little) {
            bytes.reverse(0, 4)
            bytes.reverse(4, 8)
        }

        val cursor = createCursor(bytes, endianness)

        assertEquals(2.5f, cursor.f32())
        assertEquals(4u, cursor.position)

        assertEquals(32.25f, cursor.f32())
        assertEquals(8u, cursor.position)
    }

    @Test
    fun u8Array() {
        val read: Cursor.(UInt) -> IntArray = { n ->
            val arr = u8Array(n)
            IntArray(n.toInt()) { arr[it].toInt() }
        }

        testIntegerArrayRead(1, read, Endianness.Little)
        testIntegerArrayRead(1, read, Endianness.Big)
    }

    @Test
    fun u16Array() {
        val read: Cursor.(UInt) -> IntArray = { n ->
            val arr = u16Array(n)
            IntArray(n.toInt()) { arr[it].toInt() }
        }

        testIntegerArrayRead(2, read, Endianness.Little)
        testIntegerArrayRead(2, read, Endianness.Big)
    }

    @Test
    fun u32Array() {
        val read: Cursor.(UInt) -> IntArray = { n ->
            val arr = u32Array(n)
            IntArray(n.toInt()) { arr[it].toInt() }
        }

        testIntegerArrayRead(4, read, Endianness.Little)
        testIntegerArrayRead(4, read, Endianness.Big)
    }

    @Test
    fun i32Array() {
        val read: Cursor.(UInt) -> IntArray = { n ->
            val arr = i32Array(n)
            IntArray(n.toInt()) { arr[it] }
        }

        testIntegerArrayRead(4, read, Endianness.Little)
        testIntegerArrayRead(4, read, Endianness.Big)
    }

    private fun testIntegerArrayRead(
        byteCount: Int,
        read: Cursor.(UInt) -> IntArray,
        endianness: Endianness,
    ) {
        // Generate array of the form 1, 2, 0xFF, 4, 5, 6, 7, 8.
        val bytes = Array<Byte>(8 * byteCount) { 0 }

        for (i in 0 until 8) {
            if (i == 2) {
                for (j in 0 until byteCount) {
                    bytes[i * byteCount + j] = (0xff).toByte()
                }
            } else {
                if (endianness == Endianness.Little) {
                    bytes[i * byteCount] = (i + 1).toByte()
                } else {
                    bytes[i * byteCount + byteCount - 1] = (i + 1).toByte()
                }
            }
        }

        var allOnes = 0
        repeat(byteCount) { allOnes = ((allOnes shl 8) or 0xff) }

        // Test cursor.
        val cursor = createCursor(bytes, endianness)

        val array1 = cursor.read(3u)
        assertEquals(1, array1[0])
        assertEquals(2, array1[1])
        assertEquals(allOnes, array1[2])
        assertEquals(3u * byteCount.toUInt(), cursor.position)

        cursor.seekStart((2 * byteCount).toUInt())
        val array2 = cursor.read(4u)
        assertEquals(allOnes, array2[0])
        assertEquals(4, array2[1])
        assertEquals(5, array2[2])
        assertEquals(6, array2[3])
        assertEquals(6u * byteCount.toUInt(), cursor.position)

        cursor.seekStart((5 * byteCount).toUInt())
        val array3 = cursor.read(3u)
        assertEquals(6, array3[0])
        assertEquals(7, array3[1])
        assertEquals(8, array3[2])
        assertEquals(8u * byteCount.toUInt(), cursor.position)
    }

    @Test
    fun stringAscii() {
        testStringRead(1, Cursor::stringAscii, Endianness.Little)
        testStringRead(1, Cursor::stringAscii, Endianness.Big)
    }

    @Test
    fun stringUtf16() {
        testStringRead(2, Cursor::stringUtf16, Endianness.Little)
        testStringRead(2, Cursor::stringUtf16, Endianness.Big)
    }

    private fun testStringRead(
        byteCount: Int,
        read: Cursor.(
            maxByteLength: UInt,
            nullTerminated: Boolean,
            dropRemaining: Boolean,
        ) -> String,
        endianness: Endianness,
    ) {
        val chars = byteArrayOf(7, 65, 66, 0, (255).toByte(), 13)
        val bytes = Array<Byte>(chars.size * byteCount) { 0 }

        for (i in 0..chars.size) {
            if (endianness == Endianness.Little) {
                bytes[byteCount * i] = chars[i]
            } else {
                bytes[byteCount * i + byteCount - 1] = chars[i]
            }
        }

        val bc = byteCount.toUInt()
        val cursor = createCursor(bytes, endianness)

        cursor.seekStart(bc)
        assertEquals("AB", cursor.read(4u * bc, true, true))
        assertEquals(5u * bc, cursor.position)
        cursor.seekStart(bc)
        assertEquals("AB", cursor.read(2u * bc, true, true))
        assertEquals(3u * bc, cursor.position)

        cursor.seekStart(bc)
        assertEquals("AB", cursor.read(4u * bc, true, false))
        assertEquals(4u * bc, cursor.position)
        cursor.seekStart(bc)
        assertEquals("AB", cursor.read(2u * bc, true, false))
        assertEquals(3u * bc, cursor.position)

        cursor.seekStart(bc)
        assertEquals("AB\u0000ÿ", cursor.read(4u * bc, false, true))
        assertEquals(5u * bc, cursor.position)

        cursor.seekStart(bc)
        assertEquals("AB\u0000ÿ", cursor.read(4u * bc, false, false))
        assertEquals(5u * bc, cursor.position)
    }
}

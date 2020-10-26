package world.phantasmal.lib.compression.prs

import world.phantasmal.lib.buffer.Buffer
import world.phantasmal.lib.cursor.cursor
import kotlin.random.Random
import kotlin.random.nextUInt
import kotlin.test.Test
import kotlin.test.assertEquals

class PrsDecompressTests {
    @Test
    fun edge_case_0_bytes() {
        testWithBuffer(Buffer.withSize(0))
    }

    @Test
    fun edge_case_1_byte() {
        testWithBuffer(Buffer.withSize(1).fill(111))
    }

    @Test
    fun edge_case_2_bytes() {
        testWithBuffer(Buffer.fromByteArray(byteArrayOf(7, 111)))
    }

    @Test
    fun edge_case_3_bytes() {
        testWithBuffer(Buffer.fromByteArray(byteArrayOf(7, 55, 120)))
    }

    @Test
    fun best_case() {
        testWithBuffer(Buffer.withSize(10_000).fill(127))
    }

    @Test
    fun worst_case() {
        val random = Random(37)
        val buffer = Buffer.withSize(10_000)

        for (i in 0 until buffer.size step 4) {
            buffer.setU32(i, random.nextUInt())
        }

        testWithBuffer(buffer)
    }

    @Test
    fun typical_case() {
        val random = Random(37)
        val pattern = byteArrayOf(0, 0, 2, 0, 3, 0, 5, 0, 0, 0, 7, 9, 11, 13, 0, 0)
        val buffer = Buffer.withSize(1000 * pattern.size)

        for (i in 0 until buffer.size) {
            buffer.setI8(i, (pattern[i % pattern.size] + random.nextInt(10)).toByte())
        }

        testWithBuffer(buffer)
    }

    private fun testWithBuffer(buffer: Buffer) {
        val cursor = buffer.cursor()
        val compressedCursor = prsCompress(cursor)

        val decompressedCursor = prsDecompress(compressedCursor).unwrap()
        cursor.seekStart(0)

        assertEquals(cursor.size, decompressedCursor.size)

        while (cursor.hasBytesLeft()) {
            val expected = cursor.i8()
            val actual = decompressedCursor.i8()

            if (expected != actual) {
                // Assert after check for performance.
                assertEquals(
                    expected,
                    actual,
                    "Got $actual, expected $expected at ${cursor.position - 1}."
                )
            }
        }
    }
}

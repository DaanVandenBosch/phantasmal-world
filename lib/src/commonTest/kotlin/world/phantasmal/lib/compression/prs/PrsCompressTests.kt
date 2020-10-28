package world.phantasmal.lib.compression.prs

import world.phantasmal.lib.buffer.Buffer
import world.phantasmal.lib.cursor.cursor
import kotlin.random.Random
import kotlin.random.nextUInt
import kotlin.test.Test
import kotlin.test.assertEquals

class PrsCompressTests {
    @Test
    fun edge_case_0_bytes() {
        val compressed = prsCompress(Buffer.withSize(0).cursor())

        assertEquals(3, compressed.size)
    }

    @Test
    fun edge_case_1_byte() {
        val compressed = prsCompress(Buffer.withSize(1).fillByte(111).cursor())

        assertEquals(4, compressed.size)
    }

    @Test
    fun edge_case_2_bytes() {
        val compressed = prsCompress(Buffer.fromByteArray(byteArrayOf(7, 111)).cursor())

        assertEquals(5, compressed.size)
    }

    @Test
    fun edge_case_3_bytes() {
        val compressed = prsCompress(Buffer.fromByteArray(byteArrayOf(7, 55, 120)).cursor())

        assertEquals(6, compressed.size)
    }

    @Test
    fun best_case() {
        val compressed = prsCompress(Buffer.withSize(10_000).fillByte(127).cursor())

        assertEquals(475, compressed.size)
    }

    @Test
    fun worst_case() {
        val random = Random(37)
        val buffer = Buffer.withSize(10_000)

        for (i in 0 until buffer.size step 4) {
            buffer.setInt(i, random.nextInt())
        }

        val compressed = prsCompress(buffer.cursor())

        assertEquals(11252, compressed.size)
    }

    @Test
    fun typical_case() {
        val random = Random(37)
        val pattern = byteArrayOf(0, 0, 2, 0, 3, 0, 5, 0, 0, 0, 7, 9, 11, 13, 0, 0)
        val buffer = Buffer.withSize(1000 * pattern.size)

        for (i in 0 until buffer.size) {
            buffer.setByte(i, (pattern[i % pattern.size] + random.nextInt(10)).toByte())
        }

        val compressed = prsCompress(buffer.cursor())

        assertEquals(14549, compressed.size)
    }
}

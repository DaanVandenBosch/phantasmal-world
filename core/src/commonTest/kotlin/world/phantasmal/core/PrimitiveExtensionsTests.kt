package world.phantasmal.core

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PrimitiveExtensionsTests {
    @Test
    fun test_Int_isBitSet() {
        // Test with all zero bits.
        repeat(32) { bit ->
            assertFalse((0).isBitSet(bit))
        }

        // Test with all one bits.
        repeat(32) { bit ->
            assertTrue((-1).isBitSet(bit))
        }

        // Test with leftmost bit set to one.
        assertTrue(Int.MIN_VALUE.isBitSet(31))

        for (bit in 0..30) {
            assertFalse(Int.MIN_VALUE.isBitSet(bit))
        }

        // Test with rightmost bit set to one.
        assertTrue((1).isBitSet(0))

        for (bit in 1..31) {
            assertFalse((1).isBitSet(bit))
        }
    }

    @Test
    fun test_UByte_isBitSet() {
        // Test with all zero bits.
        repeat(8) { bit ->
            assertFalse((0).toUByte().isBitSet(bit))
        }

        // Test with all one bits.
        repeat(8) { bit ->
            assertTrue((-1).toUByte().isBitSet(bit))
        }

        // Test with leftmost bit set to one.
        assertTrue((0x80).toUByte().isBitSet(7))

        for (bit in 0..6) {
            assertFalse((0x80).toUByte().isBitSet(bit))
        }

        // Test with rightmost bit set to one.
        assertTrue((1).toUByte().isBitSet(0))

        for (bit in 1..7) {
            assertFalse((1).toUByte().isBitSet(bit))
        }
    }
}

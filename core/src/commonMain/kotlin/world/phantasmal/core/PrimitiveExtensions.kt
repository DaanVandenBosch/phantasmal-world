package world.phantasmal.core

// Char.isWhitespace is very slow in JS, use this until
// https://youtrack.jetbrains.com/issue/KT-43216 lands.
expect inline fun Char.fastIsWhitespace(): Boolean

expect inline fun Char.isDigit(): Boolean

/**
 * Returns true if the bit at the given position is set. Bits are indexed from lowest-order
 * ("rightmost") to the highest-order ("leftmost") bit.
 *
 * @param bit The bit position, starting from 0.
 */
fun Int.isBitSet(bit: Int): Boolean =
    (this ushr bit) and 1 == 1

/**
 * Returns true if the bit at the given position is set. Bits are indexed from lowest-order
 * ("rightmost") to the highest-order ("leftmost") bit.
 *
 * @param bit The bit position, starting from 0.
 */
fun UByte.isBitSet(bit: Int): Boolean =
    toInt().isBitSet(bit)

fun Int.setBit(bit: Int, value: Boolean): Int =
    if (value) {
        this or (1 shl bit)
    } else {
        this and (1 shl bit).inv()
    }

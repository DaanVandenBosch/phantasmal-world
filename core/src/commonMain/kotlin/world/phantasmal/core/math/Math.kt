package world.phantasmal.core.math

/**
 * Returns the floored modulus of its arguments. The computed value will have the same sign as the
 * [divisor].
 */
fun floorMod(dividend: Double, divisor: Double): Double =
    ((dividend % divisor) + divisor) % divisor

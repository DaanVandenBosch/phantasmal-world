package world.phantasmal.core.math

import kotlin.math.PI

private const val TO_DEG = 180 / PI
private const val TO_RAD = PI / 180

/**
 * Converts radians to degrees.
 */
fun radToDeg(rad: Double): Double = rad * TO_DEG

/**
 * Converts degrees to radians.
 */
fun degToRad(deg: Double): Double = deg * TO_RAD

/**
 * Returns the floored modulus of its arguments. The computed value will have the same sign as the
 * [divisor].
 */
fun floorMod(dividend: Double, divisor: Double): Double =
    ((dividend % divisor) + divisor) % divisor

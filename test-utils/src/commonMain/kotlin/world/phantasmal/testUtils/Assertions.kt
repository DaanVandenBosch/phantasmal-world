package world.phantasmal.testUtils

import kotlin.math.abs
import kotlin.test.assertTrue

fun assertCloseTo(expected: Double, actual: Double, epsilon: Double = 0.001) {
    assertTrue(abs(expected - actual) <= epsilon)
}

fun assertCloseTo(expected: Float, actual: Float, epsilon: Float = 0.001f) {
    assertTrue(abs(expected - actual) <= epsilon)
}

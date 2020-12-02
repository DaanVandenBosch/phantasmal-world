package world.phantasmal.web.core

import world.phantasmal.web.externals.three.Euler
import world.phantasmal.web.externals.three.Quaternion
import world.phantasmal.web.externals.three.Vector3

operator fun Vector3.plus(other: Vector3): Vector3 =
    clone().add(other)

operator fun Vector3.plusAssign(other: Vector3) {
    add(other)
}

operator fun Vector3.minus(other: Vector3): Vector3 =
    clone().sub(other)

operator fun Vector3.minusAssign(other: Vector3) {
    sub(other)
}

operator fun Vector3.times(scalar: Double): Vector3 =
    clone().multiplyScalar(scalar)

operator fun Vector3.timesAssign(scalar: Double) {
    multiplyScalar(scalar)
}

infix fun Vector3.dot(other: Vector3): Double =
    dot(other)

infix fun Vector3.cross(other: Vector3): Vector3 =
    cross(other)

operator fun Quaternion.timesAssign(other: Quaternion) {
    multiply(other)
}

/**
 * Creates an [Euler] object from a [Quaternion] with the correct rotation order.
 */
fun Quaternion.toEuler(): Euler =
    Euler().setFromQuaternion(this, "ZXY")

/**
 * Creates an [Euler] object with the correct rotation order.
 */
fun euler(x: Float, y: Float, z: Float): Euler =
    euler(x.toDouble(), y.toDouble(), z.toDouble())

/**
 * Creates an [Euler] object with the correct rotation order.
 */
fun euler(x: Double, y: Double, z: Double): Euler =
    Euler(x, y, z, "ZXY")

/**
 * Creates an [Euler] object from a [Quaternion] with the correct rotation order.
 */
fun Euler.toQuaternion(): Quaternion =
    Quaternion().setFromEuler(this)

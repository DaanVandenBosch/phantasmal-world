package world.phantasmal.web.core

import world.phantasmal.web.externals.babylon.Matrix
import world.phantasmal.web.externals.babylon.Vector3

operator fun Vector3.minus(other: Vector3): Vector3 =
    subtract(other)

infix fun Vector3.dot(other: Vector3): Double =
    Vector3.Dot(this, other)

infix fun Vector3.cross(other: Vector3): Vector3 =
    cross(other)

operator fun Matrix.timesAssign(other: Matrix) {
    other.preMultiply(this)
}

fun Matrix.preMultiply(other: Matrix) {
    // Multiplies this by other.
    multiplyToRef(other, this)
}

fun Matrix.multiply(v: Vector3) {
    Vector3.TransformCoordinatesToRef(v, this, v)
}

fun Matrix.multiply3x3(v: Vector3) {
    Vector3.TransformNormalToRef(v, this, v)
}

package world.phantasmal.web.core

import world.phantasmal.web.externals.three.*
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

private val tmpSphere = Sphere()

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

@OptIn(ExperimentalContracts::class)
inline fun Object3D.isMesh(): Boolean {
    contract {
        returns(true) implies (this@isMesh is Mesh)
    }

    return unsafeCast<Mesh>().isMesh
}

@OptIn(ExperimentalContracts::class)
inline fun Object3D.isSkinnedMesh(): Boolean {
    contract {
        returns(true) implies (this@isSkinnedMesh is SkinnedMesh)
    }

    return unsafeCast<SkinnedMesh>().isSkinnedMesh
}

fun boundingSphere(object3d: Object3D, bSphere: Sphere = Sphere()): Sphere {
    if (object3d.isMesh()) {
        // Don't use reference to union method to improve performance of emitted JS.
        object3d.geometry.boundingSphere?.let {
            tmpSphere.copy(it)
            tmpSphere.applyMatrix4(object3d.matrixWorld)
            bSphere.union(tmpSphere)
        }
    }

    object3d.children.forEach { boundingSphere(it, bSphere) }

    return bSphere
}

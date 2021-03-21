package world.phantasmal.web.core.rendering.conversion

import world.phantasmal.lib.fileFormats.Vec2
import world.phantasmal.lib.fileFormats.Vec3
import world.phantasmal.web.core.euler
import world.phantasmal.web.externals.three.Euler
import world.phantasmal.web.externals.three.Vector2
import world.phantasmal.web.externals.three.Vector3

fun vec2ToThree(v: Vec2): Vector2 = Vector2(v.x.toDouble(), v.y.toDouble())

fun vec3ToThree(v: Vec3): Vector3 = Vector3(v.x.toDouble(), v.y.toDouble(), v.z.toDouble())

inline fun Vector3.setFromVec3(v: Vec3) {
    set(v.x.toDouble(), v.y.toDouble(), v.z.toDouble())
}

inline fun Euler.setFromVec3(v: Vec3) {
    set(v.x.toDouble(), v.y.toDouble(), v.z.toDouble())
}

fun vec3ToEuler(v: Vec3): Euler = euler(v.x, v.y, v.z)

package world.phantasmal.web.core.rendering.conversion

import world.phantasmal.lib.fileFormats.Vec2
import world.phantasmal.lib.fileFormats.Vec3
import world.phantasmal.web.externals.three.Vector2
import world.phantasmal.web.externals.three.Vector3

fun vec2ToThree(v: Vec2): Vector2 = Vector2(v.x.toDouble(), v.y.toDouble())

fun vec3ToThree(v: Vec3): Vector3 = Vector3(v.x.toDouble(), v.y.toDouble(), v.z.toDouble())

fun threeToVec3(v: Vector3): Vec3 = Vec3(v.x.toFloat(), v.y.toFloat(), v.z.toFloat())

package world.phantasmal.web.core.rendering.conversion

import world.phantasmal.lib.fileFormats.Vec2
import world.phantasmal.lib.fileFormats.Vec3
import world.phantasmal.web.externals.babylon.Vector2
import world.phantasmal.web.externals.babylon.Vector3

fun vec2ToBabylon(v: Vec2): Vector2 = Vector2(v.x.toDouble(), v.y.toDouble())

fun vec3ToBabylon(v: Vec3): Vector3 = Vector3(v.x.toDouble(), v.y.toDouble(), v.z.toDouble())

fun babylonToVec3(v: Vector3): Vec3 = Vec3(v.x.toFloat(), v.y.toFloat(), v.z.toFloat())

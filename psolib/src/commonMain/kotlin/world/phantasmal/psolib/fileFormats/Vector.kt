package world.phantasmal.psolib.fileFormats

import world.phantasmal.psolib.cursor.Cursor

data class Vec2(val x: Float, val y: Float)

data class Vec3(val x: Float, val y: Float, val z: Float)

fun Cursor.vec2Float(): Vec2 = Vec2(float(), float())

fun Cursor.vec3Float(): Vec3 = Vec3(float(), float(), float())

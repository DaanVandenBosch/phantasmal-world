package world.phantasmal.core

fun Char.isDigit(): Boolean = this in '0'..'9'

expect fun Int.reinterpretAsFloat(): Float

expect fun Float.reinterpretAsInt(): Int

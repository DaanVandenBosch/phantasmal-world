@file:JvmName("IntCells")

package world.phantasmal.cell

import kotlin.jvm.JvmName

operator fun Cell<Int>.unaryMinus(): Cell<Int> =
    map { -it }

operator fun Cell<Int>.plus(other: Cell<Int>): Cell<Int> =
    map(this, other) { a, b -> a + b }

operator fun Cell<Int>.plus(other: Int): Cell<Int> =
    map { it + other }

operator fun Int.plus(other: Cell<Int>): Cell<Int> =
    other.map { this + it }

operator fun Cell<Int>.minus(other: Cell<Int>): Cell<Int> =
    map(this, other) { a, b -> a - b }

operator fun Cell<Int>.minus(other: Int): Cell<Int> =
    map { it - other }

operator fun Int.minus(other: Cell<Int>): Cell<Int> =
    other.map { this - it }

operator fun Cell<Int>.times(other: Cell<Int>): Cell<Int> =
    map(this, other) { a, b -> a * b }

operator fun Cell<Int>.times(other: Int): Cell<Int> =
    map { it * other }

operator fun Int.times(other: Cell<Int>): Cell<Int> =
    other.map { this * it }

operator fun Cell<Int>.div(other: Cell<Int>): Cell<Int> =
    map(this, other) { a, b -> a / b }

operator fun Cell<Int>.div(other: Int): Cell<Int> =
    map { it / other }

operator fun Int.div(other: Cell<Int>): Cell<Int> =
    other.map { this / it }

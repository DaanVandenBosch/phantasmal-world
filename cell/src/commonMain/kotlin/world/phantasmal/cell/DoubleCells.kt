@file:JvmName("DoubleCells")

package world.phantasmal.cell

import kotlin.jvm.JvmName

operator fun Cell<Double>.unaryMinus(): Cell<Double> =
    map { -it }

operator fun Cell<Double>.plus(other: Cell<Double>): Cell<Double> =
    map(this, other) { a, b -> a + b }

operator fun Cell<Double>.plus(other: Double): Cell<Double> =
    map { it + other }

operator fun Double.plus(other: Cell<Double>): Cell<Double> =
    other.map { this + it }

operator fun Cell<Double>.minus(other: Cell<Double>): Cell<Double> =
    map(this, other) { a, b -> a - b }

operator fun Cell<Double>.minus(other: Double): Cell<Double> =
    map { it - other }

operator fun Double.minus(other: Cell<Double>): Cell<Double> =
    other.map { this - it }

operator fun Cell<Double>.times(other: Cell<Double>): Cell<Double> =
    map(this, other) { a, b -> a * b }

operator fun Cell<Double>.times(other: Double): Cell<Double> =
    map { it * other }

operator fun Double.times(other: Cell<Double>): Cell<Double> =
    other.map { this * it }

operator fun Cell<Double>.div(other: Cell<Double>): Cell<Double> =
    map(this, other) { a, b -> a / b }

operator fun Cell<Double>.div(other: Double): Cell<Double> =
    map { it / other }

operator fun Double.div(other: Cell<Double>): Cell<Double> =
    other.map { this / it }

package world.phantasmal.observable.cell

infix fun <T> Cell<T>.eq(value: T): Cell<Boolean> =
    map { it == value }

infix fun <T> Cell<T>.eq(other: Cell<T>): Cell<Boolean> =
    map(this, other) { a, b -> a == b }

infix fun <T> Cell<T>.ne(value: T): Cell<Boolean> =
    map { it != value }

infix fun <T> Cell<T>.ne(other: Cell<T>): Cell<Boolean> =
    map(this, other) { a, b -> a != b }

fun <T> Cell<T?>.orElse(defaultValue: () -> T): Cell<T> =
    map { it ?: defaultValue() }

infix fun <T : Comparable<T>> Cell<T>.gt(value: T): Cell<Boolean> =
    map { it > value }

infix fun <T : Comparable<T>> Cell<T>.gt(other: Cell<T>): Cell<Boolean> =
    map(this, other) { a, b -> a > b }

infix fun <T : Comparable<T>> Cell<T>.lt(value: T): Cell<Boolean> =
    map { it < value }

infix fun <T : Comparable<T>> Cell<T>.lt(other: Cell<T>): Cell<Boolean> =
    map(this, other) { a, b -> a < b }

infix fun Cell<Boolean>.and(other: Cell<Boolean>): Cell<Boolean> =
    map(this, other) { a, b -> a && b }

infix fun Cell<Boolean>.and(other: Boolean): Cell<Boolean> =
    if (other) this else falseCell()

infix fun Cell<Boolean>.or(other: Cell<Boolean>): Cell<Boolean> =
    map(this, other) { a, b -> a || b }

infix fun Cell<Boolean>.xor(other: Cell<Boolean>): Cell<Boolean> =
    // Use != because of https://youtrack.jetbrains.com/issue/KT-31277.
    map(this, other) { a, b -> a != b }

operator fun Cell<Boolean>.not(): Cell<Boolean> = map { !it }

operator fun Cell<Int>.plus(value: Int): Cell<Int> =
    map { it + value }

operator fun Cell<Int>.minus(value: Int): Cell<Int> =
    map { it - value }

fun Cell<String>.isEmpty(): Cell<Boolean> =
    map { it.isEmpty() }

fun Cell<String>.isNotEmpty(): Cell<Boolean> =
    map { it.isNotEmpty() }

fun Cell<String>.isBlank(): Cell<Boolean> =
    map { it.isBlank() }

fun Cell<String>.isNotBlank(): Cell<Boolean> =
    map { it.isNotBlank() }

fun <T> Cell<Cell<T>>.flatten(): Cell<T> =
    FlatteningDependentCell(this) { this.value }

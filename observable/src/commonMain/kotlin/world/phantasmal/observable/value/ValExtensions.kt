package world.phantasmal.observable.value

infix fun <T> Val<T>.eq(value: T): Val<Boolean> =
    map { it == value }

infix fun <T> Val<T>.eq(value: Val<T>): Val<Boolean> =
    map(this, value) { a, b -> a == b }

infix fun <T> Val<T>.ne(value: T): Val<Boolean> =
    map { it != value }

infix fun <T> Val<T>.ne(value: Val<T>): Val<Boolean> =
    map(this, value) { a, b -> a != b }

fun <T> Val<T?>.orElse(defaultValue: () -> T): Val<T> =
    map { it ?: defaultValue() }

infix fun <T : Comparable<T>> Val<T>.gt(value: T): Val<Boolean> =
    map { it > value }

infix fun <T : Comparable<T>> Val<T>.gt(value: Val<T>): Val<Boolean> =
    map(this, value) { a, b -> a > b }

infix fun <T : Comparable<T>> Val<T>.lt(value: T): Val<Boolean> =
    map { it < value }

infix fun <T : Comparable<T>> Val<T>.lt(value: Val<T>): Val<Boolean> =
    map(this, value) { a, b -> a < b }

infix fun Val<Boolean>.and(other: Val<Boolean>): Val<Boolean> =
    map(this, other) { a, b -> a && b }

infix fun Val<Boolean>.or(other: Val<Boolean>): Val<Boolean> =
    map(this, other) { a, b -> a || b }

infix fun Val<Boolean>.xor(other: Val<Boolean>): Val<Boolean> =
    // Use != because of https://youtrack.jetbrains.com/issue/KT-31277.
    map(this, other) { a, b -> a != b }

operator fun Val<Boolean>.not(): Val<Boolean> = map { !it }

operator fun Val<Int>.plus(other: Int): Val<Int> =
    map { it + other }

operator fun Val<Int>.minus(other: Int): Val<Int> =
    map { it - other }

fun Val<String>.isEmpty(): Val<Boolean> =
    map { it.isEmpty() }

fun Val<String>.isNotEmpty(): Val<Boolean> =
    map { it.isNotEmpty() }

fun Val<String>.isBlank(): Val<Boolean> =
    map { it.isBlank() }

fun Val<String>.isNotBlank(): Val<Boolean> =
    map { it.isNotBlank() }

package world.phantasmal.observable.value

infix fun <T> Val<T>.eq(value: T): Val<Boolean> =
    map { it == value }

infix fun <T> Val<T>.eq(value: Val<T>): Val<Boolean> =
    map(value) { a, b -> a == b }

infix fun <T> Val<T>.ne(value: T): Val<Boolean> =
    map { it != value }

infix fun <T> Val<T>.ne(value: Val<T>): Val<Boolean> =
    map(value) { a, b -> a != b }

fun <T> Val<T?>.orElse(defaultValue: () -> T): Val<T> =
    map { it ?: defaultValue() }

infix fun <T : Comparable<T>> Val<T>.gt(value: T): Val<Boolean> =
    map { it > value }

infix fun <T : Comparable<T>> Val<T>.gt(value: Val<T>): Val<Boolean> =
    map(value) { a, b -> a > b }

infix fun <T : Comparable<T>> Val<T>.lt(value: T): Val<Boolean> =
    map { it < value }

infix fun <T : Comparable<T>> Val<T>.lt(value: Val<T>): Val<Boolean> =
    map(value) { a, b -> a < b }

infix fun Val<Boolean>.and(other: Val<Boolean>): Val<Boolean> =
    map(other) { a, b -> a && b }

infix fun Val<Boolean>.or(other: Val<Boolean>): Val<Boolean> =
    map(other) { a, b -> a || b }

// Use != because of https://youtrack.jetbrains.com/issue/KT-31277.
infix fun Val<Boolean>.xor(other: Val<Boolean>): Val<Boolean> =
    map(other) { a, b -> a != b }

operator fun Val<Boolean>.not(): Val<Boolean> = map { !it }

fun Val<String>.isEmpty(): Val<Boolean> =
    map { it.isEmpty() }

fun Val<String>.isNotEmpty(): Val<Boolean> =
    map { it.isNotEmpty() }

fun Val<String>.isBlank(): Val<Boolean> =
    map { it.isBlank() }

fun Val<String>.isNotBlank(): Val<Boolean> =
    map { it.isNotBlank() }

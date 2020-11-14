package world.phantasmal.observable.value

infix fun Val<Any?>.eq(value: Any?): Val<Boolean> =
    map { it == value }

infix fun Val<Any?>.eq(value: Val<Any?>): Val<Boolean> =
    map(value) { a, b -> a == b }

infix fun Val<Any?>.ne(value: Any?): Val<Boolean> =
    map { it != value }

infix fun Val<Any?>.ne(value: Val<Any?>): Val<Boolean> =
    map(value) { a, b -> a != b }

fun Val<Any?>.isNull(): Val<Boolean> =
    map { it == null }

fun Val<Any?>.isNotNull(): Val<Boolean> =
    map { it != null }

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

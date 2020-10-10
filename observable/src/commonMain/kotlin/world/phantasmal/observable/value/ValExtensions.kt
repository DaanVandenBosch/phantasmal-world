package world.phantasmal.observable.value

infix fun Val<Boolean>.and(other: Val<Boolean>): Val<Boolean> =
    transform(other) { a, b -> a && b }

infix fun Val<Boolean>.or(other: Val<Boolean>): Val<Boolean> =
    transform(other) { a, b -> a || b }

// Use != because of https://youtrack.jetbrains.com/issue/KT-31277.
infix fun Val<Boolean>.xor(other: Val<Boolean>): Val<Boolean> =
    transform(other) { a, b -> a != b }

operator fun Val<Boolean>.not(): Val<Boolean> = transform { !it }

package world.phantasmal.core

fun requireNonNegative(value: Int, name: String) {
    require(value >= 0) {
        "$name should be non-negative but was $value."
    }
}

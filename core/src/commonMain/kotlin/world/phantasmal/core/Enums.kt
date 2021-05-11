package world.phantasmal.core

inline fun <reified T : Enum<T>> enumValueOfOrNull(value: String): T? =
    try {
        enumValueOf<T>(value)
    } catch (e: IllegalArgumentException) {
        null
    }

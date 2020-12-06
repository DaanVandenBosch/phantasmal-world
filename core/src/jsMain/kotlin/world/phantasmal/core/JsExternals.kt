package world.phantasmal.core

external interface JsArray<T> {
    fun push(vararg elements: T): Int
}

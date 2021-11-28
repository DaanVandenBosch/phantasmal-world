package world.phantasmal.core.unsafe

@JsName("Set")
actual external class UnsafeSet<T> {
    actual constructor()
    actual constructor(values: Array<out T>)

    actual val size: Int

    actual fun add(value: T): UnsafeSet<T>
    actual fun clear()
    actual fun delete(value: T): Boolean
    actual fun has(value: T): Boolean
    actual fun forEach(callback: (value: T) -> Unit)
}

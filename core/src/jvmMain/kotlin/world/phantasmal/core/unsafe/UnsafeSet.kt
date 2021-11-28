package world.phantasmal.core.unsafe

actual class UnsafeSet<T>(private val set: LinkedHashSet<T>) {
    actual constructor() : this(LinkedHashSet())
    actual constructor(values: Array<out T>) : this(LinkedHashSet<T>().apply { addAll(values) })

    actual val size: Int get() = set.size

    actual fun add(value: T): UnsafeSet<T> {
        set.add(value)
        return this
    }

    actual fun clear() {
        set.clear()
    }

    actual fun delete(value: T): Boolean = set.remove(value)

    actual fun has(value: T): Boolean = value in set

    actual fun forEach(callback: (value: T) -> Unit) {
        for (v in set) {
            callback(v)
        }
    }
}

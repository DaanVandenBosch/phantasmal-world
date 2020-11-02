package world.phantasmal.web.questEditor.loading

import kotlinx.coroutines.Deferred
import world.phantasmal.core.disposable.TrackedDisposable

class LoadingCache<K, V> : TrackedDisposable() {
    private val map = mutableMapOf<K, Deferred<V>>()

    operator fun set(key: K, value: Deferred<V>) {
        map[key] = value
    }

    fun getOrPut(key: K, defaultValue: () -> Deferred<V>): Deferred<V> =
        map.getOrPut(key, defaultValue)

    override fun internalDispose() {
        map.values.forEach { it.cancel() }
        super.internalDispose()
    }
}

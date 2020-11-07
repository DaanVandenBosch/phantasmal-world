package world.phantasmal.web.questEditor.loading

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import world.phantasmal.core.disposable.TrackedDisposable

class LoadingCache<K, V>(private val disposeValue: (V) -> Unit) : TrackedDisposable() {
    private val map = mutableMapOf<K, Deferred<V>>()

    operator fun set(key: K, value: Deferred<V>) {
        map[key] = value
    }

    @Suppress("DeferredIsResult")
    fun getOrPut(key: K, defaultValue: () -> Deferred<V>): Deferred<V> =
        map.getOrPut(key, defaultValue)

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun internalDispose() {
        map.values.forEach {
            if (it.isActive) {
                it.cancel()
            } else if (it.isCompleted) {
                disposeValue(it.getCompleted())
            }
        }

        super.internalDispose()
    }
}

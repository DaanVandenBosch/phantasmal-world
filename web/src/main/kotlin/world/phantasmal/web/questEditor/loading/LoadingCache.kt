package world.phantasmal.web.questEditor.loading

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import world.phantasmal.core.disposable.TrackedDisposable

class LoadingCache<K, V>(
    private val scope: CoroutineScope,
    private val loadValue: suspend (K) -> V,
    private val disposeValue: (V) -> Unit,
) : TrackedDisposable() {
    private val map = mutableMapOf<K, Deferred<V>>()

    suspend fun get(key: K): V =
        map.getOrPut(key) { scope.async { loadValue(key) } }.await()

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

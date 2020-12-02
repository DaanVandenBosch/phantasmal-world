package world.phantasmal.web.questEditor.loading

import kotlinx.coroutines.*
import world.phantasmal.core.disposable.TrackedDisposable

@OptIn(ExperimentalCoroutinesApi::class)
class LoadingCache<K, V>(
    private val loadValue: suspend (K) -> V,
    private val disposeValue: (V) -> Unit,
) : TrackedDisposable() {
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
    private val map = mutableMapOf<K, Deferred<V>>()

    val values: Collection<Deferred<V>> = map.values

    suspend fun get(key: K): V =
        map.getOrPut(key) { scope.async { loadValue(key) } }.await()

    fun getIfPresentNow(key: K): V? =
        map[key]?.takeIf { it.isCompleted }?.getCompleted()

    override fun internalDispose() {
        map.values.forEach {
            if (it.isActive) {
                it.cancel()
            } else if (it.isCompleted) {
                disposeValue(it.getCompleted())
            }
        }

        scope.cancel("LoadingCache disposed.")
        super.internalDispose()
    }
}

package world.phantasmal.web.core.observable

import world.phantasmal.core.disposable.Disposable
import world.phantasmal.core.disposable.disposable

class Emitter<T> : Observable<T> {
    private val observers: MutableList<(T) -> Unit> = mutableListOf()

    fun emit(event: T) {
        for (observer in observers) {
            observer(event)
        }
    }

    override fun observe(observer: (T) -> Unit): Disposable {
        observers.add(observer)
        return disposable { observers.remove(observer) }
    }
}

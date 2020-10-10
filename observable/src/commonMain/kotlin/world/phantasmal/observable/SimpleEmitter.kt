package world.phantasmal.observable

import world.phantasmal.core.disposable.Disposable
import world.phantasmal.core.disposable.disposable

class SimpleEmitter<T> : Emitter<T> {
    private val observers = mutableListOf<Observer<T>>()

    override fun observe(observer: Observer<T>): Disposable {
        observers.add(observer)

        return disposable {
            observers.remove(observer)
        }
    }

    override fun emit(event: ChangeEvent<T>) {
        observers.forEach { it(event) }
    }
}

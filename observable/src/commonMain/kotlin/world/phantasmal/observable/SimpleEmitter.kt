package world.phantasmal.observable

import world.phantasmal.core.disposable.Scope
import world.phantasmal.core.disposable.disposable

class SimpleEmitter<T> : Emitter<T> {
    private val observers = mutableListOf<Observer<T>>()

    override fun observe(scope: Scope, observer: Observer<T>) {
        observers.add(observer)

        scope.disposable {
            observers.remove(observer)
        }
    }

    override fun emit(event: ChangeEvent<T>) {
        observers.forEach { it(event) }
    }
}

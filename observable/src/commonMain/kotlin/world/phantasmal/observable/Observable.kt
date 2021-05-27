package world.phantasmal.observable

import world.phantasmal.core.disposable.Disposable

interface Observable<out T> : Dependency {
    fun observe(observer: Observer<T>): Disposable
}

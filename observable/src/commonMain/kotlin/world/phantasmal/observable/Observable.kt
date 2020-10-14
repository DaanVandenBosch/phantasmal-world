package world.phantasmal.observable

import world.phantasmal.core.disposable.Scope

interface Observable<out T> {
    fun observe(scope: Scope, observer: Observer<T>)
}

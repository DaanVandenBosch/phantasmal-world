package world.phantasmal.observable

import world.phantasmal.core.disposable.Disposable

interface Observable<out T> : Dependency<T> {
    /**
     * [observer] will be called whenever this observable changes.
     */
    fun observeChange(observer: ChangeObserver<T>): Disposable
}

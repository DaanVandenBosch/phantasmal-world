package world.phantasmal.web.core.observable

import world.phantasmal.core.disposable.Disposable

interface Observable<out T> {
    /**
     * [observer] will be called whenever this observable changes.
     */
    fun observe(observer: (T) -> Unit): Disposable
}

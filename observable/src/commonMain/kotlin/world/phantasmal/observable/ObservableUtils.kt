package world.phantasmal.observable

import world.phantasmal.core.disposable.Disposable

fun <T> emitter(): Emitter<T> = SimpleEmitter()

fun <T> Observable<T>.observe(observer: (T) -> Unit): Disposable =
    observeChange { observer(it.value) }

package world.phantasmal.webui

import world.phantasmal.core.disposable.Disposable
import world.phantasmal.core.disposable.Disposer
import world.phantasmal.core.disposable.TrackedDisposable
import world.phantasmal.observable.Observable
import world.phantasmal.observable.Observer
import world.phantasmal.observable.value.Val

abstract class DisposableContainer : TrackedDisposable() {
    private val disposer = Disposer()

    override fun internalDispose() {
        disposer.dispose()
        super.internalDispose()
    }

    protected fun <T : Disposable> addDisposable(disposable: T): T =
        disposer.add(disposable)

    protected fun addDisposables(vararg disposables: Disposable) {
        disposer.addAll(*disposables)
    }

    /**
     * Removes and disposes the given [disposable].
     */
    protected fun removeDisposable(disposable: Disposable) {
        disposer.remove(disposable)
    }

    protected fun <V1> observe(observable: Observable<V1>, operation: (V1) -> Unit) {
        addDisposable(
            if (observable is Val<V1>) {
                observable.observe(callNow = true) { operation(it.value) }
            } else {
                observable.observe { operation(it.value) }
            }
        )
    }

    protected fun <V1, V2> observe(
        v1: Val<V1>,
        v2: Val<V2>,
        operation: (V1, V2) -> Unit,
    ) {
        val observer: Observer<*> = {
            operation(v1.value, v2.value)
        }
        addDisposables(
            v1.observe(observer),
            v2.observe(observer),
        )
        operation(v1.value, v2.value)
    }

    protected fun <V1, V2, V3> observe(
        v1: Val<V1>,
        v2: Val<V2>,
        v3: Val<V3>,
        operation: (V1, V2, V3) -> Unit,
    ) {
        val observer: Observer<*> = {
            operation(v1.value, v2.value, v3.value)
        }
        addDisposables(
            v1.observe(observer),
            v2.observe(observer),
            v3.observe(observer),
        )
        operation(v1.value, v2.value, v3.value)
    }

    protected fun <V1, V2, V3, V4> observe(
        v1: Val<V1>,
        v2: Val<V2>,
        v3: Val<V3>,
        v4: Val<V4>,
        operation: (V1, V2, V3, V4) -> Unit,
    ) {
        val observer: Observer<*> = {
            operation(v1.value, v2.value, v3.value, v4.value)
        }
        addDisposables(
            v1.observe(observer),
            v2.observe(observer),
            v3.observe(observer),
            v4.observe(observer),
        )
        operation(v1.value, v2.value, v3.value, v4.value)
    }

    protected fun <V1, V2, V3, V4, V5> observe(
        v1: Val<V1>,
        v2: Val<V2>,
        v3: Val<V3>,
        v4: Val<V4>,
        v5: Val<V5>,
        operation: (V1, V2, V3, V4, V5) -> Unit,
    ) {
        val observer: Observer<*> = {
            operation(v1.value, v2.value, v3.value, v4.value, v5.value)
        }
        addDisposables(
            v1.observe(observer),
            v2.observe(observer),
            v3.observe(observer),
            v4.observe(observer),
            v5.observe(observer),
        )
        operation(v1.value, v2.value, v3.value, v4.value, v5.value)
    }
}

package world.phantasmal.webui

import world.phantasmal.core.disposable.Disposable
import world.phantasmal.core.disposable.Disposer
import world.phantasmal.core.disposable.TrackedDisposable
import world.phantasmal.cell.Cell
import world.phantasmal.cell.observe
import world.phantasmal.cell.observeNow

abstract class DisposableContainer : TrackedDisposable() {
    private val disposer = Disposer()

    override fun dispose() {
        disposer.dispose()
        super.dispose()
    }

    protected fun <T : Disposable> addDisposable(disposable: T): T =
        disposer.add(disposable)

    protected fun addDisposables(vararg disposables: Disposable) {
        disposer.addAll(*disposables)
    }

    /**
     * Removes and by default disposes the given [disposable].
     */
    protected fun removeDisposable(disposable: Disposable, dispose: Boolean = true) {
        disposer.remove(disposable, dispose)
    }

    protected fun <T> observe(
        c1: Cell<T>,
        observer: (T) -> Unit,
    ) {
        addDisposable(c1.observe(observer))
    }

    protected fun <T> observeNow(
        c1: Cell<T>,
        observer: (T) -> Unit,
    ) {
        addDisposable(c1.observeNow(observer))
    }

    protected fun <T1, T2> observeNow(
        c1: Cell<T1>,
        c2: Cell<T2>,
        observer: (T1, T2) -> Unit,
    ) {
        addDisposable(world.phantasmal.cell.observeNow(c1, c2, observer))
    }

    protected fun <T1, T2, T3> observeNow(
        c1: Cell<T1>,
        c2: Cell<T2>,
        c3: Cell<T3>,
        observer: (T1, T2, T3) -> Unit,
    ) {
        addDisposable(world.phantasmal.cell.observeNow(c1, c2, c3, observer))
    }

    protected fun <T1, T2, T3, T4> observeNow(
        c1: Cell<T1>,
        c2: Cell<T2>,
        c3: Cell<T3>,
        c4: Cell<T4>,
        observer: (T1, T2, T3, T4) -> Unit,
    ) {
        addDisposable(world.phantasmal.cell.observeNow(c1, c2, c3, c4, observer))
    }

    protected fun <T1, T2, T3, T4, T5> observeNow(
        c1: Cell<T1>,
        c2: Cell<T2>,
        c3: Cell<T3>,
        c4: Cell<T4>,
        c5: Cell<T5>,
        observer: (T1, T2, T3, T4, T5) -> Unit,
    ) {
        addDisposable(world.phantasmal.cell.observeNow(c1, c2, c3, c4, c5, observer))
    }
}

package world.phantasmal.observable.cell

import world.phantasmal.core.disposable.Disposable
import world.phantasmal.core.disposable.nopDisposable
import world.phantasmal.observable.AbstractDependency
import world.phantasmal.observable.ChangeObserver

class ImmutableCell<T>(override val value: T) : AbstractDependency(), Cell<T> {
    override fun observeChange(observer: ChangeObserver<T>): Disposable = nopDisposable()

    override fun emitDependencyChanged() {
        error("ImmutableCell can't change.")
    }
}

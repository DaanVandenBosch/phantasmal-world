package world.phantasmal.observable.cell

import world.phantasmal.core.disposable.Disposable
import world.phantasmal.observable.AbstractDependency
import world.phantasmal.observable.CallbackChangeObserver
import world.phantasmal.observable.ChangeObserver

abstract class AbstractCell<T> : AbstractDependency<T>(), Cell<T> {
    override fun observeChange(observer: ChangeObserver<T>): Disposable =
        CallbackChangeObserver(this, observer)

    override fun toString(): String = cellToString(this)
}

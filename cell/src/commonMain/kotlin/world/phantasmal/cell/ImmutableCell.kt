package world.phantasmal.cell

import world.phantasmal.core.disposable.Disposable
import world.phantasmal.core.disposable.nopDisposable

internal class ImmutableCell<T>(override val value: T) : Dependency<T>, Cell<T> {
    override val changeEvent: ChangeEvent<T>? get() = null

    override fun addDependent(dependent: Dependent) {
        // We don't remember our dependents because we never need to notify them of changes.
    }

    override fun removeDependent(dependent: Dependent) {
        // Nothing to remove because we don't remember our dependents.
    }

    override fun observeChange(observer: ChangeObserver<T>): Disposable = nopDisposable()

    override fun toString(): String = cellToString(this)
}

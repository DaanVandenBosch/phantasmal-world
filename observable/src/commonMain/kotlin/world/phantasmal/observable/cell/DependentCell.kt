package world.phantasmal.observable.cell

import world.phantasmal.core.unsafe.unsafeCast
import world.phantasmal.observable.ChangeEvent
import world.phantasmal.observable.Dependency
import world.phantasmal.observable.Dependent

/**
 * Cell of which the value depends on 0 or more dependencies.
 */
class DependentCell<T>(
    private vararg val dependencies: Dependency,
    private val compute: () -> T,
) : AbstractDependentCell<T>() {

    private var _value: T? = null
    override val value: T
        get() {
            // Recompute value every time when we have no dependents. At this point we're not yet a
            // dependent of our own dependencies, and thus we won't automatically recompute our
            // value when they change.
            if (dependents.isEmpty()) {
                _value = compute()
            }

            return unsafeCast(_value)
        }

    override fun addDependent(dependent: Dependent) {
        if (dependents.isEmpty()) {
            // Start actually depending on or dependencies when we get our first dependent.
            // Make sure value is up-to-date here, because from now on `compute` will only be called
            // when our dependencies change.
            _value = compute()

            for (dependency in dependencies) {
                dependency.addDependent(this)
            }
        }

        super.addDependent(dependent)
    }

    override fun removeDependent(dependent: Dependent) {
        super.removeDependent(dependent)

        if (dependents.isEmpty()) {
            // Stop actually depending on our dependencies when we no longer have any dependents.
            for (dependency in dependencies) {
                dependency.removeDependent(this)
            }
        }
    }

    override fun dependenciesFinishedChanging() {
        val newValue = compute()
        _value = newValue
        emitDependencyChangedEvent(ChangeEvent(newValue))
    }
}

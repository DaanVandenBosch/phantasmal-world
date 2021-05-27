package world.phantasmal.observable.cell

import world.phantasmal.core.unsafe.unsafeCast
import world.phantasmal.observable.ChangeEvent
import world.phantasmal.observable.Dependency
import world.phantasmal.observable.Dependent

/**
 * Cell of which the value depends on 0 or more other cells.
 */
class DependentCell<T>(
    private vararg val dependencies: Dependency,
    private val compute: () -> T
) : AbstractDependentCell<T>() {

    private var _value: T? = null
    override val value: T
        get() {
            if (dependents.isEmpty()) {
                _value = compute()
            }

            return unsafeCast(_value)
        }

    override fun addDependent(dependent: Dependent) {
        if (dependents.isEmpty()) {
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
            for (dependency in dependencies) {
                dependency.removeDependent(this)
            }
        }
    }

    override fun dependenciesChanged() {
        val newValue = compute()

        if (newValue != _value) {
            _value = newValue
            emitChanged(ChangeEvent(newValue))
        } else {
            emitChanged(null)
        }
    }
}

package world.phantasmal.observable.cell

import world.phantasmal.core.unsafe.unsafeAssertNotNull
import world.phantasmal.core.unsafe.unsafeCast
import world.phantasmal.observable.ChangeEvent
import world.phantasmal.observable.Dependency
import world.phantasmal.observable.Dependent

/**
 * Similar to [DependentCell], except that this cell's [compute] returns a cell.
 */
class FlatteningDependentCell<T>(
    private vararg val dependencies: Dependency,
    private val compute: () -> Cell<T>,
) : AbstractDependentCell<T>() {

    private var computedCell: Cell<T>? = null
    private var computedInDeps = false
    private var shouldRecompute = false

    private var _value: T? = null
    override val value: T
        get() {
            if (dependents.isEmpty()) {
                _value = compute().value
            }

            return unsafeCast(_value)
        }

    override fun addDependent(dependent: Dependent) {
        if (dependents.isEmpty()) {
            for (dependency in dependencies) {
                dependency.addDependent(this)
            }

            computedCell = compute().also { computedCell ->
                computedCell.addDependent(this)
                computedInDeps = dependencies.any { it === computedCell }
                _value = computedCell.value
            }
        }

        super.addDependent(dependent)
    }

    override fun removeDependent(dependent: Dependent) {
        super.removeDependent(dependent)

        if (dependents.isEmpty()) {
            computedCell?.removeDependent(this)
            computedCell = null
            computedInDeps = false

            for (dependency in dependencies) {
                dependency.removeDependent(this)
            }
        }
    }

    override fun dependencyChanged(dependency: Dependency, event: ChangeEvent<*>?) {
        if ((dependency !== computedCell || computedInDeps) && event != null) {
            shouldRecompute = true
        }

        super.dependencyChanged(dependency, event)
    }

    override fun dependenciesFinishedChanging() {
        if (shouldRecompute) {
            computedCell?.removeDependent(this)

            computedCell = compute().also { computedCell ->
                computedCell.addDependent(this)
                computedInDeps = dependencies.any { it === computedCell }
            }

            shouldRecompute = false
        }

        val newValue = unsafeAssertNotNull(computedCell).value
        _value = newValue
        emitDependencyChangedEvent(ChangeEvent(newValue))
    }
}

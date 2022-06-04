package world.phantasmal.cell

import world.phantasmal.core.unsafe.unsafeAssertNotNull

abstract class AbstractFlatteningDependentCell<T, ComputedCell : Cell<T>, Event : ChangeEvent<T>>(
    private val dependencies: Array<out Cell<*>>,
    private val compute: () -> ComputedCell,
) : AbstractDependentCell<T, Event>() {

    private var computedCell: ComputedCell? = null
    private var computedInDeps = false
    private var shouldRecomputeCell = true
    private var valid = false

    override fun computeValueAndEvent() {
        if (!valid) {
            val oldValue = _value
            val hasDependents = dependents.isNotEmpty()

            val computedCell: ComputedCell

            if (shouldRecomputeCell) {
                this.computedCell?.removeDependent(this)
                computedCell = compute()

                if (hasDependents) {
                    // Only hold onto and depend on the computed cell if we have dependents
                    // ourselves.
                    computedCell.addDependent(this)
                    this.computedCell = computedCell
                    computedInDeps = dependencies.any { it === computedCell }
                    shouldRecomputeCell = false
                } else {
                    // Set field to null to allow the cell to be garbage collected.
                    this.computedCell = null
                }
            } else {
                computedCell = unsafeAssertNotNull(this.computedCell)
            }

            val newValue = computedCell.value
            _value = newValue
            changeEvent = createEvent(oldValue, newValue)
            // We stay invalid if we have no dependents to ensure our value is always recomputed.
            valid = hasDependents
        }
    }

    protected abstract fun createEvent(oldValue: T?, newValue: T): Event

    override fun addDependent(dependent: Dependent) {
        super.addDependent(dependent)

        if (dependents.size == 1) {
            for (dependency in dependencies) {
                dependency.addDependent(this)
            }

            // Called to ensure that we depend on the computed cell. This could be optimized by
            // avoiding the value and changeEvent calculation.
            computeValueAndEvent()
        }
    }

    override fun removeDependent(dependent: Dependent) {
        super.removeDependent(dependent)

        if (dependents.isEmpty()) {
            valid = false
            computedCell?.removeDependent(this)
            // Set field to null to allow the cell to be garbage collected.
            computedCell = null
            shouldRecomputeCell = true

            for (dependency in dependencies) {
                dependency.removeDependent(this)
            }
        }
    }

    override fun dependencyInvalidated(dependency: Dependency<*>) {
        valid = false

        // We should recompute the computed cell when any dependency except the computed cell is
        // invalidated. When the computed cell is in our dependency array (i.e. the computed cell
        // itself takes part in determining what the computed cell is) we should also recompute.
        if (dependency !== computedCell || computedInDeps) {
            // We're not allowed to change the dependency graph at this point, so we just set this
            // field to true and remove ourselves as dependency from the computed cell right before
            // we recompute it.
            shouldRecomputeCell = true
        }

        emitDependencyInvalidated()
    }
}

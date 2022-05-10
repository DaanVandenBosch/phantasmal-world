package world.phantasmal.observable.cell

import world.phantasmal.core.unsafe.unsafeAssertNotNull
import world.phantasmal.observable.ChangeEvent
import world.phantasmal.observable.Dependency
import world.phantasmal.observable.Dependent
import world.phantasmal.observable.Observable

/**
 * Similar to [DependentCell], except that this cell's [compute] returns a cell.
 */
// TODO: Shares 99% of its code with FlatteningDependentListCell, should use common super class.
class FlatteningDependentCell<T>(
    private vararg val dependencies: Observable<*>,
    private val compute: () -> Cell<T>,
) : AbstractDependentCell<T>() {

    private var computedCell: Cell<T>? = null
    private var computedInDeps = false
    private var shouldRecomputeCell = true
    private var valid = false

    override fun computeValueAndEvent() {
        if (!valid) {
            val hasDependents = dependents.isNotEmpty()

            val computedCell: Cell<T>

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
            setValueAndEvent(newValue, ChangeEvent(newValue))
            // We stay invalid if we have no dependents to ensure our value is always recomputed.
            valid = hasDependents
        }
    }

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

package world.phantasmal.observable.cell.list

import world.phantasmal.core.unsafe.unsafeAssertNotNull
import world.phantasmal.observable.Dependency
import world.phantasmal.observable.Dependent
import world.phantasmal.observable.Observable

/**
 * Similar to [DependentListCell], except that this cell's [computeElements] returns a [ListCell].
 */
// TODO: Shares 99% of its code with FlatteningDependentCell, should use common super class.
class FlatteningDependentListCell<E>(
    private vararg val dependencies: Observable<*>,
    private val computeElements: () -> ListCell<E>,
) : AbstractListCell<E>(), Dependent {

    private var computedCell: ListCell<E>? = null
    private var computedInDeps = false
    private var shouldRecomputeCell = true
    private var valid = false

    private var _value:List<E> = emptyList()
    override val value: List<E>
        get() {
            computeValueAndEvent()
            return _value
        }

    override var changeEvent: ListChangeEvent<E>? = null
        get() {
            computeValueAndEvent()
            return field
        }
        private set

    private fun computeValueAndEvent() {
        if (!valid) {
            val oldElements = _value
            val hasDependents = dependents.isNotEmpty()

            val computedCell: ListCell<E>

            if (shouldRecomputeCell) {
                this.computedCell?.removeDependent(this)
                computedCell = computeElements()

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

            val newElements = computedCell.value
            _value = newElements
            changeEvent = ListChangeEvent(
                newElements,
                listOf(ListChange(
                    index = 0,
                    prevSize = oldElements.size,
                    removed = oldElements,
                    inserted = newElements,
                )),
            )
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

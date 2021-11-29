package world.phantasmal.observable.cell.list

import world.phantasmal.core.unsafe.unsafeAssertNotNull
import world.phantasmal.observable.ChangeEvent
import world.phantasmal.observable.Dependency
import world.phantasmal.observable.Dependent

/**
 * Similar to [DependentListCell], except that this cell's [computeElements] returns a [ListCell].
 */
class FlatteningDependentListCell<E>(
    private vararg val dependencies: Dependency,
    private val computeElements: () -> ListCell<E>,
) : AbstractDependentListCell<E>() {

    private var computedCell: ListCell<E>? = null
    private var computedInDeps = false
    private var shouldRecompute = false

    override var elements: List<E> = emptyList()
        private set

    override fun addDependent(dependent: Dependent) {
        if (dependents.isEmpty()) {
            for (dependency in dependencies) {
                dependency.addDependent(this)
            }

            computedCell = computeElements.invoke().also { computedCell ->
                computedCell.addDependent(this)
                computedInDeps = dependencies.any { it === computedCell }
                elements = computedCell.value
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

    override fun computeElements() {
        if (shouldRecompute || dependents.isEmpty()) {
            computedCell?.removeDependent(this)

            computedCell = computeElements.invoke().also { computedCell ->
                computedCell.addDependent(this)
                computedInDeps = dependencies.any { it === computedCell }
            }

            shouldRecompute = false
        }

        elements = unsafeAssertNotNull(computedCell).value
    }
}

package world.phantasmal.observable.cell.list

import world.phantasmal.observable.Dependent
import world.phantasmal.observable.cell.Cell

/**
 * ListCell of which the value depends on 0 or more other cells.
 */
class DependentListCell<E>(
    private vararg val dependencies: Cell<*>,
    private val computeElements: () -> List<E>,
) : AbstractDependentListCell<E>() {

    override var elements: List<E> = emptyList()
        private set

    override fun addDependent(dependent: Dependent) {
        if (dependents.isEmpty()) {
            computeElements()

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

    override fun computeElements() {
        elements = computeElements.invoke()
    }
}

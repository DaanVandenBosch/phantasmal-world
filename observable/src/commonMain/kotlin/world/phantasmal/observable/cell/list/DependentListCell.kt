package world.phantasmal.observable.cell.list

import world.phantasmal.observable.Dependency
import world.phantasmal.observable.Dependent
import world.phantasmal.observable.Observable

/**
 * ListCell of which the value depends on 0 or more other observables.
 */
class DependentListCell<E>(
    private vararg val dependencies: Observable<*>,
    private val computeElements: () -> List<E>,
) : AbstractListCell<E>(), Dependent {

    private var valid = false

    private var _value: List<E> = emptyList()
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
            val newElements = computeElements()
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
            valid = dependents.isNotEmpty()
        }
    }

    override fun addDependent(dependent: Dependent) {
        if (dependents.isEmpty()) {
            for (dependency in dependencies) {
                dependency.addDependent(this)
            }
        }

        super.addDependent(dependent)
    }

    override fun removeDependent(dependent: Dependent) {
        super.removeDependent(dependent)

        if (dependents.isEmpty()) {
            valid = false

            for (dependency in dependencies) {
                dependency.removeDependent(this)
            }
        }
    }

    override fun dependencyInvalidated(dependency: Dependency<*>) {
        valid = false
        emitDependencyInvalidated()
    }
}

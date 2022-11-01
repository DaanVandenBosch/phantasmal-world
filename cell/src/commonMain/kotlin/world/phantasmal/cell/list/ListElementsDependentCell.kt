package world.phantasmal.cell.list

import world.phantasmal.core.splice
import world.phantasmal.cell.ChangeEvent
import world.phantasmal.cell.Dependency
import world.phantasmal.cell.Dependent
import world.phantasmal.cell.AbstractCell
import world.phantasmal.cell.Cell

/**
 * Depends on a [ListCell] and zero or more cells per element in the list.
 */
internal class ListElementsDependentCell<E>(
    private val list: ListCell<E>,
    private val extractCells: (element: E) -> Array<out Cell<*>>,
) : AbstractCell<List<E>>(), Dependent {
    /** An array of dependencies per [list] element, extracted by [extractCells]. */
    private val elementDependencies = mutableListOf<Array<out Dependency<*>>>()

    private var valid = false
    private var listInvalidated = false

    override val value: List<E>
        get() {
            updateElementDependenciesAndEvent()
            return list.value
        }

    override var changeEvent: ChangeEvent<List<E>>? = null
        get() {
            updateElementDependenciesAndEvent()
            return field
        }
        private set

    private fun updateElementDependenciesAndEvent() {
        if (!valid) {
            if (listInvalidated) {
                // At this point we can remove this dependent from the removed elements'
                // dependencies and add it to the newly inserted elements' dependencies.
                list.changeEvent?.let { listChangeEvent ->
                    for (change in listChangeEvent.changes) {
                        for (i in change.index until (change.index + change.removed.size)) {
                            for (elementDependency in elementDependencies[i]) {
                                elementDependency.removeDependent(this)
                            }
                        }

                        val inserted = change.inserted.map(extractCells)

                        elementDependencies.splice(
                            startIndex = change.index,
                            amount = change.removed.size,
                            elements = inserted,
                        )

                        for (elementDependencies in inserted) {
                            for (elementDependency in elementDependencies) {
                                elementDependency.addDependent(this)
                            }
                        }
                    }
                }

                // Reset for the next change wave.
                listInvalidated = false
            }

            changeEvent = ChangeEvent(list.value)
            // We stay invalid if we have no dependents to ensure our change event is always
            // recomputed.
            valid = dependents.isNotEmpty()
        }
    }

    override fun addDependent(dependent: Dependent) {
        if (dependents.isEmpty()) {
            // Once we have our first dependent, we start depending on our own dependencies.
            list.addDependent(this)

            for (element in list.value) {
                val dependencies = extractCells(element)

                for (dependency in dependencies) {
                    dependency.addDependent(this)
                }

                elementDependencies.add(dependencies)
            }
        }

        super.addDependent(dependent)
    }

    override fun removeDependent(dependent: Dependent) {
        super.removeDependent(dependent)

        if (dependents.isEmpty()) {
            valid = false
            listInvalidated = false

            // At this point we have no more dependents, so we can stop depending on our own
            // dependencies.
            for (dependencies in elementDependencies) {
                for (dependency in dependencies) {
                    dependency.removeDependent(this)
                }
            }

            elementDependencies.clear()
            list.removeDependent(this)
        }
    }

    override fun dependencyInvalidated(dependency: Dependency<*>) {
        valid = false

        if (dependency === list) {
            listInvalidated = true
        }

        emitDependencyInvalidated()
    }
}

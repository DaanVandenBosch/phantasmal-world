package world.phantasmal.observable.cell.list

import world.phantasmal.core.splice
import world.phantasmal.core.unsafe.unsafeCast
import world.phantasmal.observable.ChangeEvent
import world.phantasmal.observable.Dependency
import world.phantasmal.observable.Dependent
import world.phantasmal.observable.Observable
import world.phantasmal.observable.cell.AbstractCell

/**
 * Depends on a [ListCell] and zero or more [Observable]s per element in the list.
 */
class ListElementsDependentCell<E>(
    private val list: ListCell<E>,
    private val extractObservables: (element: E) -> Array<out Observable<*>>,
) : AbstractCell<List<E>>(), Dependent {
    /** An array of dependencies per [list] element, extracted by [extractObservables]. */
    private val elementDependencies = mutableListOf<Array<out Dependency>>()

    /** Keeps track of how many of our dependencies are about to (maybe) change. */
    private var changingDependencies = 0

    /**
     * Set to true once one of our dependencies has actually changed. Reset to false whenever
     * [changingDependencies] hits 0 again.
     */
    private var dependenciesActuallyChanged = false

    private var listChangeEvent: ListChangeEvent<E>? = null

    override val value: List<E>
        get() = list.value

    override fun addDependent(dependent: Dependent) {
        if (dependents.isEmpty()) {
            // Once we have our first dependent, we start depending on our own dependencies.
            list.addDependent(this)

            for (element in list.value) {
                val dependencies = extractObservables(element)

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

    override fun dependencyMightChange() {
        changingDependencies++
        emitMightChange()
    }

    override fun dependencyChanged(dependency: Dependency, event: ChangeEvent<*>?) {
        if (event != null) {
            dependenciesActuallyChanged = true

            // Simply store all list changes when the changing dependency is our list dependency. We
            // don't update our dependencies yet to avoid receiving dependencyChanged notifications
            // from newly inserted dependencies for which we haven't received any
            // dependencyMightChange notifications and to avoid *NOT* receiving dependencyChanged
            // notifications from removed dependencies for which we *HAVE* received
            // dependencyMightChange notifications.
            if (dependency === list) {
                listChangeEvent = unsafeCast(event)
            }
        }

        changingDependencies--

        if (changingDependencies == 0) {
            // All of our dependencies have finished changing.

            // At this point we can remove this dependent from the removed elements' dependencies
            // and add it to the newly inserted elements' dependencies.
            listChangeEvent?.let { listChangeEvent ->
                for (change in listChangeEvent.changes) {
                    for (i in change.index until (change.index + change.removed.size)) {
                        for (elementDependency in elementDependencies[i]) {
                            elementDependency.removeDependent(this)
                        }
                    }

                    val inserted = change.inserted.map(extractObservables)

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
            listChangeEvent = null

            if (dependenciesActuallyChanged) {
                dependenciesActuallyChanged = false

                emitDependencyChangedEvent(ChangeEvent(list.value))
            } else {
                emitDependencyChangedEvent(null)
            }
        }
    }

    override fun emitDependencyChanged() {
        // Nothing to do because ListElementsDependentCell emits dependencyChanged immediately. We
        // don't defer this operation because ListElementsDependentCell only changes when there is
        // no transaction or the current transaction is being committed.
    }
}

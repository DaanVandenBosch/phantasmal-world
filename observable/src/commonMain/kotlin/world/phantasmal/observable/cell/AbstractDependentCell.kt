package world.phantasmal.observable.cell

import world.phantasmal.observable.ChangeEvent
import world.phantasmal.observable.Dependency
import world.phantasmal.observable.Dependent

abstract class AbstractDependentCell<T> : AbstractCell<T>(), Dependent {
    private var changingDependencies = 0
    private var dependenciesActuallyChanged = false

    override fun dependencyMightChange() {
        changingDependencies++
        emitMightChange()
    }

    override fun dependencyChanged(dependency: Dependency, event: ChangeEvent<*>?) {
        if (event != null) {
            dependenciesActuallyChanged = true
        }

        changingDependencies--

        if (changingDependencies == 0) {
            if (dependenciesActuallyChanged) {
                dependenciesActuallyChanged = false

                dependenciesFinishedChanging()
            } else {
                emitDependencyChangedEvent(null)
            }
        }
    }

    override fun emitDependencyChanged() {
        // Nothing to do because dependent cells emit dependencyChanged immediately. They don't
        // defer this operation because they only change when there is no change set or the current
        // change set is being completed.
    }

    /**
     * Called after a wave of dependencyMightChange notifications followed by an equal amount of
     * dependencyChanged notifications of which at least one signified an actual change.
     */
    protected abstract fun dependenciesFinishedChanging()
}

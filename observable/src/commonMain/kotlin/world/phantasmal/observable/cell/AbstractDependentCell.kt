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

        if (--changingDependencies == 0) {
            if (dependenciesActuallyChanged) {
                dependenciesActuallyChanged = false

                dependenciesChanged()
            } else {
                emitChanged(null)
            }
        }
    }

    abstract fun dependenciesChanged()
}

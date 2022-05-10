package world.phantasmal.observable

import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

abstract class AbstractDependency<T> : Dependency<T> {
    protected val dependents: MutableList<Dependent> = mutableListOf()

    override fun addDependent(dependent: Dependent) {
        dependents.add(dependent)
    }

    override fun removeDependent(dependent: Dependent) {
        dependents.remove(dependent)
    }

    protected fun emitDependencyInvalidated() {
        for (dependent in dependents) {
            dependent.dependencyInvalidated(this)
        }
    }

    protected inline fun applyChange(block: () -> Unit) {
        contract {
            callsInPlace(block, InvocationKind.EXACTLY_ONCE)
        }

        ChangeManager.changeDependency {
            emitDependencyInvalidated()
            block()
        }
    }
}

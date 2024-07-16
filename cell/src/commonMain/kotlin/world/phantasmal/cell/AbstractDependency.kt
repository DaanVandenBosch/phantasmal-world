package world.phantasmal.cell

import kotlin.contracts.InvocationKind.EXACTLY_ONCE
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
            callsInPlace(block, EXACTLY_ONCE)
        }

        MutationManager.changeDependency {
            emitDependencyInvalidated()
            block()
        }
    }
}

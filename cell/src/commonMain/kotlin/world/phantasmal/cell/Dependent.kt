package world.phantasmal.cell

/**
 * This interface is not meant to be implemented by typical application code.
 */
interface Dependent {
    /**
     * This method is not meant to be called from typical application code.
     *
     * Called whenever a dependency of this dependent might change. Sometimes a dependency doesn't
     * know that it will actually change, just that it might change.
     *
     * E.g. C depends on B and B depends on A. A is about to change, so it calls
     * [dependencyInvalidated] on B. At this point B doesn't know whether it will actually change
     * since the new value of A doesn't necessarily result in a new value for B (e.g. B = A % 2 and
     * A changes from 0 to 2). So B then calls [dependencyInvalidated] on C.
     */
    fun dependencyInvalidated(dependency: Dependency<*>)
}

/**
 * This interface is not meant to be implemented by typical application code.
 */
interface LeafDependent : Dependent {
    /**
     * This method is not meant to be called from typical application code. Called by
     * [MutationManager] when invalidation notifications have finished propagating, the current
     * outer mutation is ending and leaves should be updated.
     */
    fun dependenciesChanged()
}

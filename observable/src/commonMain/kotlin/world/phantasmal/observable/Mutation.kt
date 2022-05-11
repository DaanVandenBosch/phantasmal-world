package world.phantasmal.observable

/**
 * Defer propagation of changes to observables until the end of a code block. All changes to
 * observables in a single mutation won't be propagated to their dependencies until the mutation is
 * completed.
 */
inline fun mutate(block: () -> Unit) {
    MutationManager.mutate(block)
}

/**
 * Schedule a mutation to run right after the current mutation finishes. You can use this to change
 * observables in an observer callback. This is usually a bad idea, but sometimes the situation
 * where you have to change observables in response to observables changing is very hard to avoid.
 */
fun mutateDeferred(block: () -> Unit) {
    MutationManager.mutateDeferred(block)
}

package world.phantasmal.observable

/**
 * Defer propagation of changes to observables until the end of a code block. All changes to
 * observables in a single change set won't be propagated to their dependencies until the change set
 * is completed.
 */
fun change(block: () -> Unit) {
    ChangeManager.inChangeSet(block)
}

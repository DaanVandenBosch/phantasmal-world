package world.phantasmal.cell

/**
 * Defer propagation of changes to cells until the end of a code block. All changes to cells in a
 * single mutation won't be propagated to their dependencies until the mutation is completed.
 */
inline fun mutate(block: () -> Unit) {
    MutationManager.mutate(block)
}

/**
 * Schedule a mutation to run right after the current mutation finishes. Can be used to change cells
 * in an observer callback. This is usually a bad idea, but sometimes the situation where you have
 * to change cells in response to other cells changing is very hard to avoid.
 */
fun mutateDeferred(block: () -> Unit) {
    MutationManager.mutateDeferred(block)
}

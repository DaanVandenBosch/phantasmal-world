package world.phantasmal.cell

import kotlin.contracts.InvocationKind.EXACTLY_ONCE
import kotlin.contracts.contract

// TODO: Throw exception by default when triggering early recomputation during change set. Allow to
//       to turn this check off, because partial early recomputation might be useful in rare cases.
//       Dependencies will need to partially apply ListChangeEvents etc. and remember which part of
//       the event they've already applied (i.e. an index into the changes list).
// TODO: Think about nested change sets. Initially don't allow nesting?
object MutationManager {
    private val invalidatedLeaves = HashSet<LeafDependent>()

    /** Whether a dependency's value is changing at the moment. */
    private var dependencyChanging = false

    private val deferredMutations: MutableList<() -> Unit> = mutableListOf()
    private var applyingDeferredMutations = false

    inline fun mutate(block: () -> Unit) {
        contract {
            callsInPlace(block, EXACTLY_ONCE)
        }

        // TODO: Implement mutate correctly.
        block()
    }

    fun mutateDeferred(block: () -> Unit) {
        if (dependencyChanging) {
            deferredMutations.add(block)
        } else {
            block()
        }
    }

    fun invalidated(dependent: LeafDependent) {
        invalidatedLeaves.add(dependent)
    }

    inline fun changeDependency(block: () -> Unit) {
        contract {
            callsInPlace(block, EXACTLY_ONCE)
        }

        dependencyStartedChanging()

        try {
            block()
        } finally {
            dependencyFinishedChanging()
        }
    }

    fun dependencyStartedChanging() {
        check(!dependencyChanging) { "A cell is already changing." }

        dependencyChanging = true
    }

    fun dependencyFinishedChanging() {
        try {
            for (dependent in invalidatedLeaves) {
                dependent.pull()
            }
        } finally {
            dependencyChanging = false
            invalidatedLeaves.clear()

            if (!applyingDeferredMutations) {
                try {
                    applyingDeferredMutations = true
                    var i = 0

                    while (i < deferredMutations.size) {
                        deferredMutations[i]()
                        i++
                    }
                } finally {
                    applyingDeferredMutations = false
                    deferredMutations.clear()
                }
            }
        }
    }
}

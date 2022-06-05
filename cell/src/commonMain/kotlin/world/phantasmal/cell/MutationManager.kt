package world.phantasmal.cell

import world.phantasmal.core.assert
import kotlin.contracts.InvocationKind.EXACTLY_ONCE
import kotlin.contracts.contract

// TODO: Throw exception by default when triggering early recomputation during change set. Allow to
//       to turn this check off, because partial, early recomputation might be useful in rare cases.
//       Dependents will need to partially apply ListChangeEvents etc. and remember which part of
//       the event they've already applied (i.e. an index into the changes list).
object MutationManager {
    private val invalidatedLeaves = HashSet<LeafDependent>()

    /** Non-zero when a mutation is active. */
    private var mutationNestingLevel = 0

    /** Whether a dependency's value is changing at the moment. */
    private var dependencyChanging = false

    private val deferredMutations: MutableList<() -> Unit> = mutableListOf()
    private var applyingDeferredMutations = false

    inline fun mutate(block: () -> Unit) {
        contract {
            callsInPlace(block, EXACTLY_ONCE)
        }

        mutationStart()

        try {
            block()
        } finally {
            mutationEnd()
        }
    }

    fun mutateDeferred(block: () -> Unit) {
        if (dependencyChanging || mutationNestingLevel > 0) {
            deferredMutations.add(block)
        } else {
            block()
        }
    }

    fun mutationStart() {
        mutationNestingLevel++
    }

    fun mutationEnd() {
        assert(mutationNestingLevel > 0) { "No mutation was started." }

        mutationNestingLevel--

        if (mutationNestingLevel == 0) {
            try {
                for (dependent in invalidatedLeaves) {
                    dependent.pull()
                }
            } finally {
                invalidatedLeaves.clear()
                applyDeferredMutations()
            }
        }
    }

    inline fun changeDependency(block: () -> Unit) {
        contract {
            callsInPlace(block, EXACTLY_ONCE)
        }

        dependencyChangeStart()

        try {
            block()
        } finally {
            dependencyChangeEnd()
        }
    }

    fun dependencyChangeStart() {
        check(!dependencyChanging) { "A cell is already changing." }

        dependencyChanging = true
    }

    fun dependencyChangeEnd() {
        assert(dependencyChanging) { "No cell was changing." }

        if (mutationNestingLevel == 0) {
            try {
                for (dependent in invalidatedLeaves) {
                    dependent.pull()
                }
            } finally {
                dependencyChanging = false
                invalidatedLeaves.clear()
                applyDeferredMutations()
            }
        } else {
            dependencyChanging = false
        }
    }

    fun invalidated(dependent: LeafDependent) {
        invalidatedLeaves.add(dependent)
    }

    private fun applyDeferredMutations() {
        if (!applyingDeferredMutations) {
            try {
                applyingDeferredMutations = true
                // Use index instead of iterator because list can grow while applying deferred
                // mutations.
                var idx = 0

                while (idx < deferredMutations.size) {
                    mutate {
                        deferredMutations[idx]()
                    }

                    idx++
                }
            } finally {
                applyingDeferredMutations = false
                deferredMutations.clear()
            }
        }
    }
}

package world.phantasmal.cell

import world.phantasmal.core.assert
import kotlin.contracts.InvocationKind.EXACTLY_ONCE
import kotlin.contracts.contract

object MutationManager {
    private val invalidatedLeaves = HashSet<LeafDependent>()

    /** Non-zero when a mutation is active. */
    private var mutationNestingLevel: Int = 0

    /**
     * ID of the current outermost mutation. Meaningless when not in a mutation. Nested mutations
     * don't have IDs at the moment.
     */
    var currentMutationId: Long = -1
        private set

    /**
     * Set to true when a mutation was automatically started by changing a cell without first
     * manually starting a mutation.
     */
    private var artificialMutation = false

    private var dependencyChanging = false
    private var pulling = false

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
        if (mutationNestingLevel > 0) {
            deferredMutations.add(block)
        } else {
            block()
        }
    }

    fun mutationStart() {
        assert(!dependencyChanging) { "Can't start a mutation while a dependency is changing." }
        assert(!pulling) { "Can't start a mutation while pulling." }

        if (mutationNestingLevel == 0) {
            currentMutationId++
        }

        mutationNestingLevel++
    }

    fun mutationEnd() {
        assert(mutationNestingLevel > 0) { "No mutation was started." }

        if (mutationNestingLevel == 1) {
            assert(!pulling) { "Already pulling." }

            try {
                pulling = true

                for (dependent in invalidatedLeaves) {
                    dependent.pull()
                }
            } finally {
                dependencyChanging = false
                pulling = false
                mutationNestingLevel--
                invalidatedLeaves.clear()
                applyDeferredMutations()
            }
        } else {
            dependencyChanging = false
            mutationNestingLevel--
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
        assert(!pulling) { "Can't change a cell while pulling." }

        if (mutationNestingLevel == 0) {
            mutationStart()
            artificialMutation = true
        }

        dependencyChanging = true
    }

    fun dependencyChangeEnd() {
        if (artificialMutation) {
            artificialMutation = false
            mutationEnd()
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
                    mutate(deferredMutations[idx])

                    idx++
                }
            } finally {
                applyingDeferredMutations = false
                deferredMutations.clear()
            }
        }
    }
}

package world.phantasmal.observable.value.list

import world.phantasmal.observable.value.SimpleVal

/**
 * In these tests the direct dependency of the [FlatteningDependentListVal] changes.
 */
class FlatteningDependentListValDependentValEmitsTests : ListValTests() {
    override fun create() = object : ListValAndAdd {
        // The nested val can't change.
        private val nestedVal = StaticListVal<Int>(emptyList())

        // The direct dependency of the list under test can change.
        private val dependencyVal = SimpleVal<ListVal<Int>>(nestedVal)

        override val observable =
            FlatteningDependentListVal(listOf(dependencyVal)) { dependencyVal.value }

        override fun add() {
            // Update the direct dependency.
            val oldNestedVal: ListVal<Int> = dependencyVal.value
            dependencyVal.value = StaticListVal(oldNestedVal.value + 4)
        }
    }
}

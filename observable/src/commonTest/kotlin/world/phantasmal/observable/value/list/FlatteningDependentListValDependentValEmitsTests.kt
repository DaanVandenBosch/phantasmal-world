package world.phantasmal.observable.value.list

import world.phantasmal.observable.value.SimpleVal

/**
 * In these tests the direct dependency of the [FlatteningDependentListVal] changes.
 */
class FlatteningDependentListValDependentValEmitsTests : ListValTests() {
    override fun create(): ListValAndAdd<*, FlatteningDependentListVal<*>> {
        // The nested val can't change.
        val nestedVal = StaticListVal<Int>(emptyList())
        // The direct dependency of the list under test can change.
        val dependencyVal = SimpleVal<ListVal<Int>>(nestedVal)
        val list = FlatteningDependentListVal(listOf(dependencyVal)) { dependencyVal.value }

        return ListValAndAdd(list) {
            // Update the direct dependency.
            val oldNestedVal: ListVal<Int> = dependencyVal.value
            dependencyVal.value = StaticListVal(oldNestedVal.value + 4)
        }
    }
}

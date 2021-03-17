package world.phantasmal.observable.value.list

import world.phantasmal.observable.value.StaticVal

/**
 * In these tests the dependency of the [FlatteningDependentListVal]'s direct dependency changes.
 */
class FlatteningDependentListValNestedValEmitsTests : ListValTests() {
    override fun create(): ListValAndAdd<*, FlatteningDependentListVal<*>> {
        // The nested val can change.
        val nestedVal = SimpleListVal(mutableListOf<Int>())
        // The direct dependency of the list under test can't change.
        val dependentVal = StaticVal<ListVal<Int>>(nestedVal)
        val list = FlatteningDependentListVal(listOf(dependentVal)) { dependentVal.value }

        return ListValAndAdd(list) {
            // Update the nested dependency.
            nestedVal.add(4)
        }
    }
}

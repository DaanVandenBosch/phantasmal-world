package world.phantasmal.observable.value.list

import world.phantasmal.observable.value.StaticVal

/**
 * In these tests the dependency of the [FlatteningDependentListVal]'s direct dependency changes.
 */
class FlatteningDependentListValNestedValEmitsTests : ListValTests {
    override fun createProvider() = object : ListValTests.Provider {
        // The nested val can change.
        private val nestedVal = SimpleListVal(mutableListOf<Int>())

        // The direct dependency of the list under test can't change.
        private val dependentVal = StaticVal<ListVal<Int>>(nestedVal)

        override val observable =
            FlatteningDependentListVal(dependentVal) { dependentVal.value }

        override fun addElement() {
            // Update the nested dependency.
            nestedVal.add(4)
        }
    }
}

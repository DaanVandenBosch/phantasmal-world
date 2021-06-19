package world.phantasmal.observable.cell

import world.phantasmal.observable.ChangeEvent
import world.phantasmal.observable.Dependency
import world.phantasmal.observable.Dependent
import world.phantasmal.observable.change
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

interface MutableCellTests<T : Any> : CellTests {
    override fun createProvider(): Provider<T>

    @Test
    fun emits_a_change_event_when_value_is_modified() = test {
        val p = createProvider()

        var observedValue: Any? = null

        disposer.add(p.observable.observe {
            assertNull(observedValue)
            observedValue = it.value
        })

        val newValue = p.createValue()
        p.observable.value = newValue

        assertEquals(newValue, p.observable.value)
        assertEquals(newValue, observedValue)
    }

    // TODO: Figure out change set bug and enable change sets again.
    /**
     * Modifying mutable cells in a change set doesn't result in calls to
     * [Dependent.dependencyChanged] of their dependents until the change set is completed.
     */
//    @Test
//    fun cell_changes_in_change_set_dont_immediately_produce_dependencyChanged_calls() = test {
//        val dependencies = (1..5).map { createProvider() }
//
//        var dependencyMightChangeCount = 0
//        var dependencyChangedCount = 0
//
//        val dependent = object : Dependent {
//            override fun dependencyMightChange() {
//                dependencyMightChangeCount++
//            }
//
//            override fun dependencyChanged(dependency: Dependency, event: ChangeEvent<*>?) {
//                dependencyChangedCount++
//            }
//        }
//
//        for (dependency in dependencies) {
//            dependency.observable.addDependent(dependent)
//        }
//
//        change {
//            for (dependency in dependencies) {
//                dependency.observable.value = dependency.createValue()
//            }
//
//            // Calls to dependencyMightChange happen immediately.
//            assertEquals(dependencies.size, dependencyMightChangeCount)
//            // Calls to dependencyChanged happen later.
//            assertEquals(0, dependencyChangedCount)
//        }
//
//        assertEquals(dependencies.size, dependencyMightChangeCount)
//        assertEquals(dependencies.size, dependencyChangedCount)
//    }

    // TODO: Figure out change set bug and enable change sets again.
    /**
     * Modifying a mutable cell multiple times in one change set results in a single call to
     * [Dependent.dependencyMightChange] and [Dependent.dependencyChanged].
     */
//    @Test
//    fun multiple_changes_to_one_cell_in_change_set() = test {
//        val dependency = createProvider()
//
//        var dependencyMightChangeCount = 0
//        var dependencyChangedCount = 0
//
//        val dependent = object : Dependent {
//            override fun dependencyMightChange() {
//                dependencyMightChangeCount++
//            }
//
//            override fun dependencyChanged(dependency: Dependency, event: ChangeEvent<*>?) {
//                dependencyChangedCount++
//            }
//        }
//
//        dependency.observable.addDependent(dependent)
//
//        // Change the dependency multiple times in a transaction.
//        change {
//            repeat(5) {
//                dependency.observable.value = dependency.createValue()
//            }
//
//            // Calls to dependencyMightChange happen immediately.
//            assertEquals(1, dependencyMightChangeCount)
//            // Calls to dependencyChanged happen later.
//            assertEquals(0, dependencyChangedCount)
//        }
//
//        assertEquals(1, dependencyMightChangeCount)
//        assertEquals(1, dependencyChangedCount)
//    }

    // TODO: Figure out change set bug and enable change sets again.
    /**
     * Modifying two mutable cells in a change set results in a single recomputation of their
     * dependent.
     */
//    @Test
//    fun modifying_two_cells_together_results_in_one_recomputation() = test {
//        val dependency1 = createProvider()
//        val dependency2 = createProvider()
//
//        var computeCount = 0
//
//        val dependent = DependentCell(dependency1.observable, dependency2.observable) {
//            computeCount++
//            Unit
//        }
//
//        // Observe dependent to ensure it gets recomputed when its dependencies change.
//        disposer.add(dependent.observe {})
//
//        // DependentCell's compute function is called once when we start observing.
//        assertEquals(1, computeCount)
//
//        change {
//            dependency1.observable.value = dependency1.createValue()
//            dependency2.observable.value = dependency2.createValue()
//        }
//
//        assertEquals(2, computeCount)
//    }

    interface Provider<T : Any> : CellTests.Provider {
        override val observable: MutableCell<T>

        /**
         * Returns a value that can be assigned to [observable] and that's different from
         * [observable]'s current and all previous values.
         */
        fun createValue(): T
    }
}

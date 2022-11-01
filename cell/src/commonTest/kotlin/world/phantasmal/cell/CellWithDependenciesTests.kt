package world.phantasmal.cell

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

interface CellWithDependenciesTests : CellTests {
    fun createWithDependencies(
        dependency1: Cell<Int>,
        dependency2: Cell<Int>,
        dependency3: Cell<Int>,
    ): Cell<Any>

    @Test
    fun emits_at_least_once_when_all_of_its_dependencies_emit() = test {
        val root = SimpleCell(5)
        val branch1 = DependentCell(root) { root.value * 2 }
        val branch2 = DependentCell(root) { root.value * 3 }
        val branch3 = DependentCell(root) { root.value * 4 }
        val leaf = createWithDependencies(branch1, branch2, branch3)
        var dependencyInvalidatedCalled: Boolean

        leaf.addDependent(object : Dependent {
            override fun dependencyInvalidated(dependency: Dependency<*>) {
                dependencyInvalidatedCalled = true
            }
        })

        repeat(5) { index ->
            dependencyInvalidatedCalled = false

            root.value += 1

            assertTrue(dependencyInvalidatedCalled, "repetition $index")
        }
    }

    @Test
    fun is_recomputed_once_even_when_many_dependencies_change() = test {
        val root = SimpleCell(5)
        val branch1 = DependentCell(root) { root.value * 2 }
        val branch2 = DependentCell(root) { root.value * 3 }
        val branch3 = DependentCell(root) { root.value * 4 }
        val leaf = createWithDependencies(branch1, branch2, branch3)

        var observedChanges = 0

        disposer.add(leaf.observeChange { observedChanges++ })

        // Change root, which results in all branches changing and thus three dependencies of leaf
        // changing.
        root.value++

        assertEquals(1, observedChanges)
    }

    @Test
    fun doesnt_register_as_dependent_of_its_dependencies_until_it_has_dependents_itself() = test {
        val dependency1 = TestCell()
        val dependency2 = TestCell()
        val dependency3 = TestCell()

        val cell = createWithDependencies(dependency1, dependency2, dependency3)

        assertEquals(0, dependency1.dependentCount)
        assertEquals(0, dependency2.dependentCount)
        assertEquals(0, dependency3.dependentCount)

        disposer.add(cell.observeChange { })

        assertEquals(1, dependency1.dependentCount)
        assertEquals(1, dependency2.dependentCount)
        assertEquals(1, dependency3.dependentCount)
    }

    private class TestCell : AbstractCell<Int>() {
        val dependentCount: Int get() = dependents.size

        override val value: Int = 5
        override val changeEvent: ChangeEvent<Int> = ChangeEvent(value)
    }
}

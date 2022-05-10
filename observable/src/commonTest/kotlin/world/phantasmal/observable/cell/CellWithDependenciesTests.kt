package world.phantasmal.observable.cell

import world.phantasmal.observable.ChangeEvent
import world.phantasmal.observable.Dependency
import world.phantasmal.observable.Dependent
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

        // Change root, which results in both branches changing and thus two dependencies of leaf
        // changing.
        root.value = 7

        assertEquals(1, observedChanges)
    }

    @Test
    fun doesnt_register_as_dependent_of_its_dependencies_until_it_has_dependents_itself() = test {
        val dependency1 = TestCell()
        val dependency2 = TestCell()
        val dependency3 = TestCell()

        val cell = createWithDependencies(dependency1, dependency2, dependency3)

        assertTrue(dependency1.publicDependents.isEmpty())
        assertTrue(dependency2.publicDependents.isEmpty())
        assertTrue(dependency3.publicDependents.isEmpty())

        disposer.add(cell.observeChange { })

        assertEquals(1, dependency1.publicDependents.size)
        assertEquals(1, dependency2.publicDependents.size)
        assertEquals(1, dependency3.publicDependents.size)
    }

    private class TestCell : AbstractCell<Int>() {
        val publicDependents: List<Dependent> = dependents

        override val value: Int = 5
        override val changeEvent: ChangeEvent<Int> = ChangeEvent(value)
    }
}

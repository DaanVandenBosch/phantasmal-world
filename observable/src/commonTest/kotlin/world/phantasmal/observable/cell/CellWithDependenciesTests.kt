package world.phantasmal.observable.cell

import world.phantasmal.observable.Dependent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

interface CellWithDependenciesTests : CellTests {
    override fun createProvider(): Provider

    @Test
    fun is_recomputed_once_even_when_many_dependencies_change() = test {
        val p = createProvider()

        val root = SimpleCell(5)
        val branch1 = root.map { it * 2 }
        val branch2 = root.map { it * 4 }
        val leaf = p.createWithDependencies(branch1, branch2)

        var observedChanges = 0

        disposer.add(leaf.observeChange { observedChanges++ })

        // Change root, which results in both branches changing and thus two dependencies of leaf
        // changing.
        root.value = 7

        assertEquals(1, observedChanges)
    }

    @Test
    fun doesnt_register_as_dependent_of_its_dependencies_until_it_has_dependents_itself() = test {
        val p = createProvider()

        val dependency = object : AbstractCell<Int>() {
            val publicDependents: List<Dependent> = dependents

            override val value: Int = 5

            override fun emitDependencyChanged() {
                // Not going to change.
                throw NotImplementedError()
            }
        }

        val cell = p.createWithDependencies(dependency)

        assertTrue(dependency.publicDependents.isEmpty())

        disposer.add(cell.observeChange { })

        assertEquals(1, dependency.publicDependents.size)
    }

    interface Provider : CellTests.Provider {
        fun createWithDependencies(vararg dependencies: Cell<Int>): Cell<Any>
    }
}

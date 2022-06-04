package world.phantasmal.cell

import world.phantasmal.cell.test.CellTestSuite
import kotlin.test.*

interface DependencyTests : CellTestSuite {
    fun createProvider(): Provider

    @Test
    fun correctly_emits_invalidation_notifications_to_its_dependents() = test {
        val p = createProvider()
        var dependencyInvalidatedCalled: Boolean

        p.dependency.addDependent(object : Dependent {
            override fun dependencyInvalidated(dependency: Dependency<*>) {
                assertEquals(p.dependency, dependency)
                dependencyInvalidatedCalled = true
            }
        })

        repeat(5) { index ->
            dependencyInvalidatedCalled = false

            p.emit()

            assertTrue(dependencyInvalidatedCalled, "repetition $index")
        }
    }

    interface Provider {
        val dependency: Dependency<*>

        /**
         * Makes [dependency] call [Dependent.dependencyInvalidated] on its dependents.
         */
        fun emit()
    }
}

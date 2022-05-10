package world.phantasmal.observable

import world.phantasmal.observable.test.ObservableTestSuite
import kotlin.test.*

interface DependencyTests : ObservableTestSuite {
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

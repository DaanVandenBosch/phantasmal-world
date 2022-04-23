package world.phantasmal.observable

import world.phantasmal.observable.test.ObservableTestSuite
import kotlin.test.*

interface DependencyTests : ObservableTestSuite {
    fun createProvider(): Provider

    @Test
    fun correctly_emits_changes_to_its_dependents() = test {
        val p = createProvider()
        var dependencyMightChangeCalled = false
        var dependencyChangedCalled = false

        p.dependency.addDependent(object : Dependent {
            override fun dependencyMightChange() {
                assertFalse(dependencyMightChangeCalled)
                assertFalse(dependencyChangedCalled)
                dependencyMightChangeCalled = true
            }

            override fun dependencyChanged(dependency: Dependency, event: ChangeEvent<*>?) {
                assertTrue(dependencyMightChangeCalled)
                assertFalse(dependencyChangedCalled)
                assertEquals(p.dependency, dependency)
                assertNotNull(event)
                dependencyChangedCalled = true
            }
        })

        repeat(5) { index ->
            dependencyMightChangeCalled = false
            dependencyChangedCalled = false

            p.emit()

            assertTrue(dependencyMightChangeCalled, "repetition $index")
            assertTrue(dependencyChangedCalled, "repetition $index")
        }
    }

    interface Provider {
        val dependency: Dependency

        /**
         * Makes [dependency] emit [Dependent.dependencyMightChange] followed by
         * [Dependent.dependencyChanged] with a non-null event.
         */
        fun emit()
    }
}

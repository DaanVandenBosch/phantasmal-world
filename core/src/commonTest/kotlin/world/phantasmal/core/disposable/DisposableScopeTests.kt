package world.phantasmal.core.disposable

import kotlin.test.*

class DisposableScopeTests {
    @Test
    fun calling_add_or_addAll_increases_size_correctly() {
        TrackedDisposable.checkNoLeaks {
            val scope = DisposableScope()
            assertEquals(scope.size, 0)

            scope.add(Dummy())
            assertEquals(scope.size, 1)

            scope.addAll(Dummy(), Dummy())
            assertEquals(scope.size, 3)

            scope.add(Dummy())
            assertEquals(scope.size, 4)

            scope.addAll(Dummy(), Dummy())
            assertEquals(scope.size, 6)

            scope.dispose()
        }
    }

    @Test
    fun disposes_all_its_disposables_when_disposed() {
        TrackedDisposable.checkNoLeaks {
            val scope = DisposableScope()
            var disposablesDisposed = 0

            for (i in 1..5) {
                scope.add(object : Disposable {
                    override fun dispose() {
                        disposablesDisposed++
                    }
                })
            }

            scope.addAll((1..5).map {
                object : Disposable {
                    override fun dispose() {
                        disposablesDisposed++
                    }
                }
            })

            scope.dispose()

            assertEquals(10, disposablesDisposed)
        }
    }

    @Test
    fun disposeAll_disposes_all_disposables() {
        TrackedDisposable.checkNoLeaks {
            val scope = DisposableScope()

            var disposablesDisposed = 0

            for (i in 1..5) {
                scope.add(object : Disposable {
                    override fun dispose() {
                        disposablesDisposed++
                    }
                })
            }

            scope.disposeAll()

            assertEquals(5, disposablesDisposed)

            scope.dispose()
        }
    }

    @Test
    fun size_and_is_empty_should_correctly_reflect_the_contained_disposables() {
        TrackedDisposable.checkNoLeaks {
            val scope = DisposableScope()

            assertEquals(scope.size, 0)
            assertTrue(scope.isEmpty())

            for (i in 1..5) {
                scope.add(Dummy())

                assertEquals(scope.size, i)
                assertFalse(scope.isEmpty())
            }

            scope.dispose()

            assertEquals(scope.size, 0)
            assertTrue(scope.isEmpty())
        }
    }

    @Test
    fun adding_disposables_after_being_disposed_throws() {
        TrackedDisposable.checkNoLeaks {
            val scope = DisposableScope()
            scope.dispose()

            for (i in 1..3) {
                assertFails {
                    scope.add(Dummy())
                }
            }

            assertFails {
                scope.addAll((1..3).map { Dummy() })
            }
        }
    }

    private class Dummy : Disposable {
        override fun dispose() {
            // Do nothing.
        }
    }
}

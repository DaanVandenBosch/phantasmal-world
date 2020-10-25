package world.phantasmal.core.disposable

import kotlin.test.*

class DisposerTests {
    @Test
    fun calling_add_or_addAll_increases_size_correctly() {
        TrackedDisposable.checkNoLeaks {
            val disposer = Disposer()
            assertEquals(disposer.size, 0)

            disposer.add(StubDisposable())
            assertEquals(disposer.size, 1)

            disposer.addAll(StubDisposable(),
                StubDisposable())
            assertEquals(disposer.size, 3)

            disposer.add(StubDisposable())
            assertEquals(disposer.size, 4)

            disposer.addAll(StubDisposable(),
                StubDisposable())
            assertEquals(disposer.size, 6)

            disposer.dispose()
        }
    }

    @Test
    fun disposes_all_its_disposables_when_disposed() {
        TrackedDisposable.checkNoLeaks {
            val disposer = Disposer()
            var disposablesDisposed = 0

            for (i in 1..5) {
                disposer.add(object : Disposable {
                    override fun dispose() {
                        disposablesDisposed++
                    }
                })
            }

            disposer.addAll((1..5).map {
                object : Disposable {
                    override fun dispose() {
                        disposablesDisposed++
                    }
                }
            })

            disposer.dispose()

            assertEquals(10, disposablesDisposed)
        }
    }

    @Test
    fun disposeAll_disposes_all_disposables() {
        TrackedDisposable.checkNoLeaks {
            val disposer = Disposer()

            var disposablesDisposed = 0

            for (i in 1..5) {
                disposer.add(object : Disposable {
                    override fun dispose() {
                        disposablesDisposed++
                    }
                })
            }

            disposer.disposeAll()

            assertEquals(5, disposablesDisposed)

            disposer.dispose()
        }
    }

    @Test
    fun size_and_is_empty_should_correctly_reflect_the_contained_disposables() {
        TrackedDisposable.checkNoLeaks {
            val disposer = Disposer()

            assertEquals(disposer.size, 0)
            assertTrue(disposer.isEmpty())

            for (i in 1..5) {
                disposer.add(StubDisposable())

                assertEquals(disposer.size, i)
                assertFalse(disposer.isEmpty())
            }

            disposer.dispose()

            assertEquals(disposer.size, 0)
            assertTrue(disposer.isEmpty())
        }
    }

    @Test
    fun adding_disposables_after_being_disposed_throws() {
        TrackedDisposable.checkNoLeaks {
            val disposer = Disposer()
            disposer.dispose()

            for (i in 1..3) {
                assertFails {
                    disposer.add(StubDisposable())
                }
            }

            assertFails {
                disposer.addAll((1..3).map { StubDisposable() })
            }
        }
    }

    private class StubDisposable : Disposable {
        override fun dispose() {
            // Do nothing.
        }
    }
}

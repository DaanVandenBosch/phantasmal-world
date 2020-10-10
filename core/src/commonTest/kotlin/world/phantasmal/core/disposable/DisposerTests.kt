package world.phantasmal.core.disposable

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DisposerTests {
    @Test
    fun calling_add_or_add_all_should_increase_size_correctly() {
        TrackedDisposable.checkNoLeaks {
            val disposer = Disposer()
            assertEquals(disposer.size, 0)

            disposer.add(dummy())
            assertEquals(disposer.size, 1)

            disposer.addAll(dummy(), dummy())
            assertEquals(disposer.size, 3)

            disposer.add(dummy())
            assertEquals(disposer.size, 4)

            disposer.addAll(dummy(), dummy())
            assertEquals(disposer.size, 6)

            disposer.dispose()
        }
    }

    @Test
    fun a_disposer_should_dispose_all_its_disposables_when_disposed() {
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
    fun dispose_all_should_dispose_all_disposables() {
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
                disposer.add(dummy())

                assertEquals(disposer.size, i)
                assertFalse(disposer.isEmpty())
            }

            disposer.dispose()

            assertEquals(disposer.size, 0)
            assertTrue(disposer.isEmpty())
        }
    }

    @Test
    fun a_disposer_should_dispose_added_disposables_after_being_disposed() {
        TrackedDisposable.checkNoLeaks {
            val disposer = Disposer()
            disposer.dispose()

            var disposedCount = 0

            for (i in 1..3) {
                disposer.add(object : Disposable {
                    override fun dispose() {
                        disposedCount++
                    }
                })
            }

            disposer.addAll((1..3).map {
                object : Disposable {
                    override fun dispose() {
                        disposedCount++
                    }
                }
            })

            assertEquals(6, disposedCount)
        }
    }

    private fun dummy(): Disposable = disposable {}
}

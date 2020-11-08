package world.phantasmal.testUtils

import world.phantasmal.core.disposable.Disposer
import world.phantasmal.core.disposable.TrackedDisposable

abstract class AbstractTestSuite<Ctx : TestContext> {
    fun test(testBlock: Ctx.() -> Unit) {
        TrackedDisposable.checkNoLeaks(trackPrecise = true) {
            val disposer = Disposer()

            testBlock(createContext(disposer))

            disposer.dispose()
        }
    }

    fun asyncTest(testBlock: suspend Ctx.() -> Unit) = world.phantasmal.testUtils.asyncTest {
        TrackedDisposable.checkNoLeaks(trackPrecise = true) {
            val disposer = Disposer()

            testBlock(createContext(disposer))

            disposer.dispose()
        }
    }

    protected abstract fun createContext(disposer: Disposer): Ctx
}

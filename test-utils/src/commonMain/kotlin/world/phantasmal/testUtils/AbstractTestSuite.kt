package world.phantasmal.testUtils

import world.phantasmal.core.disposable.Disposer
import world.phantasmal.core.disposable.TrackedDisposable

abstract class AbstractTestSuite<Ctx : TestContext> {
    fun test(slow: Boolean = false, testBlock: Ctx.() -> Unit) {
        if (slow && !canExecuteSlowTests()) return

        TrackedDisposable.checkNoLeaks(trackPrecise = true) {
            val disposer = Disposer()

            testBlock(createContext(disposer))

            disposer.dispose()
        }
    }

    fun testAsync(slow: Boolean = false, testBlock: suspend Ctx.() -> Unit) =
        world.phantasmal.testUtils.testAsync lambda@{
            if (slow && !canExecuteSlowTests()) return@lambda

            TrackedDisposable.checkNoLeaks(trackPrecise = true) {
                val disposer = Disposer()

                testBlock(createContext(disposer))

                disposer.dispose()
            }
        }

    protected abstract fun createContext(disposer: Disposer): Ctx
}

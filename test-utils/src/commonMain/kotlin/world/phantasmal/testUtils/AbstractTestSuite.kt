package world.phantasmal.testUtils

import world.phantasmal.core.disposable.Disposer
import world.phantasmal.core.disposable.TrackedDisposable

abstract class AbstractTestSuite<Ctx : TestContext> {
    fun test(slowTest: Boolean = false, testBlock: Ctx.() -> Unit) {
        if (slowTest && !canExecuteSlowTests()) return

        TrackedDisposable.checkNoLeaks(trackPrecise = true) {
            val disposer = Disposer()

            testBlock(createContext(disposer))

            disposer.dispose()
        }
    }

    fun asyncTest(slow: Boolean = false, testBlock: suspend Ctx.() -> Unit) =
        world.phantasmal.testUtils.asyncTest lambda@{
            if (slow && !canExecuteSlowTests()) return@lambda

            TrackedDisposable.checkNoLeaks(trackPrecise = true) {
                val disposer = Disposer()

                testBlock(createContext(disposer))

                disposer.dispose()
            }
        }

    protected abstract fun createContext(disposer: Disposer): Ctx
}

package world.phantasmal.testUtils

import world.phantasmal.core.disposable.Disposer
import world.phantasmal.core.disposable.DisposableTracking

interface AbstractTestSuite<Ctx : TestContext> {
    fun test(slow: Boolean = false, testBlock: Ctx.() -> Unit) {
        if (slow && !canExecuteSlowTests()) return

        DisposableTracking.checkNoLeaks {
            val disposer = Disposer()

            createContext(disposer).testBlock()

            disposer.dispose()
        }
    }

    fun testAsync(slow: Boolean = false, testBlock: suspend Ctx.() -> Unit) =
        world.phantasmal.testUtils.testAsync lambda@{
            if (slow && !canExecuteSlowTests()) return@lambda

            DisposableTracking.checkNoLeaks {
                val disposer = Disposer()

                createContext(disposer).testBlock()

                disposer.dispose()
            }
        }

    fun createContext(disposer: Disposer): Ctx
}

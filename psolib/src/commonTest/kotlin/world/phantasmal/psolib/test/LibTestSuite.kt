package world.phantasmal.psolib.test

import world.phantasmal.core.disposable.Disposer
import world.phantasmal.testUtils.AbstractTestSuite
import world.phantasmal.testUtils.TestContext

interface LibTestSuite : AbstractTestSuite<TestContext> {
    override fun createContext(disposer: Disposer) = TestContext(disposer)
}

package world.phantasmal.web.assemblyWorker.test

import world.phantasmal.core.disposable.Disposer
import world.phantasmal.testUtils.AbstractTestSuite
import world.phantasmal.testUtils.TestContext

abstract class AssemblyWorkerTestSuite : AbstractTestSuite<TestContext> {
    override fun createContext(disposer: Disposer) = TestContext(disposer)
}

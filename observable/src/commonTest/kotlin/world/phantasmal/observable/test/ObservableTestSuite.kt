package world.phantasmal.observable.test

import world.phantasmal.core.disposable.Disposer
import world.phantasmal.testUtils.AbstractTestSuite
import world.phantasmal.testUtils.TestContext

abstract class ObservableTestSuite : AbstractTestSuite<TestContext>() {
    override fun createContext(disposer: Disposer) = TestContext(disposer)
}

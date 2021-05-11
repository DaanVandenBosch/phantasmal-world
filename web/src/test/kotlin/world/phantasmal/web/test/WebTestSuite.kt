package world.phantasmal.web.test

import world.phantasmal.core.disposable.Disposer
import world.phantasmal.testUtils.AbstractTestSuite

interface WebTestSuite : AbstractTestSuite<WebTestContext> {
    override fun createContext(disposer: Disposer) = WebTestContext(disposer)
}

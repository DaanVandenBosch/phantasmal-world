package world.phantasmal.web.test

import world.phantasmal.core.disposable.Disposer
import world.phantasmal.testUtils.TestContext

open class WebTestContext(disposer: Disposer) : TestContext(disposer) {
    @Suppress("LeakingThis")
    val components = TestComponents(this)
}

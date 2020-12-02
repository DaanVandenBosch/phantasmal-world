package world.phantasmal.web.huntOptimizer

import world.phantasmal.web.core.PwToolType
import world.phantasmal.web.core.stores.UiStore
import world.phantasmal.web.test.TestApplicationUrl
import world.phantasmal.web.test.WebTestSuite
import kotlin.test.Test

class HuntOptimizerTests : WebTestSuite() {
    @Test
    fun initialization_and_shutdown_should_succeed_without_throwing() = test {
        val uiStore =
            disposer.add(UiStore(TestApplicationUrl("/${PwToolType.HuntOptimizer}")))

        val huntOptimizer = disposer.add(HuntOptimizer(components.assetLoader, uiStore))
        disposer.add(huntOptimizer.initialize())
    }
}

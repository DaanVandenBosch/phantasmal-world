package world.phantasmal.web.huntOptimizer

import world.phantasmal.web.core.PwToolType
import world.phantasmal.web.test.TestApplicationUrl
import world.phantasmal.web.test.WebTestSuite
import kotlin.test.Test

class HuntOptimizerTests : WebTestSuite() {
    @Test
    fun initialization_and_shutdown_should_succeed_without_throwing() = test {
        components.applicationUrl = TestApplicationUrl("/${PwToolType.HuntOptimizer}")

        val huntOptimizer = disposer.add(HuntOptimizer(components.assetLoader, components.uiStore))
        disposer.add(huntOptimizer.initialize())
    }
}

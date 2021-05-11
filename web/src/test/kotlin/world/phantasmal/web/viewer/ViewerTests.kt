package world.phantasmal.web.viewer

import world.phantasmal.web.core.PwToolType
import world.phantasmal.web.test.TestApplicationUrl
import world.phantasmal.web.test.WebTestSuite
import kotlin.test.Test

class ViewerTests : WebTestSuite {
    @Test
    fun initialization_and_shutdown_should_succeed_without_throwing() = test {
        components.applicationUrl = TestApplicationUrl("/${PwToolType.Viewer}")

        val viewer = disposer.add(
            Viewer(components.assetLoader, components.uiStore, components.createThreeRenderer)
        )
        disposer.add(viewer.initialize())
    }
}

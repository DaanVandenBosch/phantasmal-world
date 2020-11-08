package world.phantasmal.web.viewer

import world.phantasmal.web.externals.babylon.Engine
import world.phantasmal.web.test.WebTestSuite
import kotlin.test.Test

class ViewerTests : WebTestSuite() {
    @Test
    fun initialization_and_shutdown_should_succeed_without_throwing() = test {
        val viewer = disposer.add(
            Viewer(createEngine = { Engine(it) })
        )
        disposer.add(viewer.initialize(scope))
    }
}

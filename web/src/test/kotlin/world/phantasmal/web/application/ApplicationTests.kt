package world.phantasmal.web.application

import kotlinx.browser.document
import world.phantasmal.core.disposable.Disposer
import world.phantasmal.core.disposable.use
import world.phantasmal.web.core.PwToolType
import world.phantasmal.web.test.TestApplicationUrl
import world.phantasmal.web.test.WebTestSuite
import kotlin.test.Test

class ApplicationTests : WebTestSuite() {
    @Test
    fun initialization_and_shutdown_should_succeed_without_throwing() = test {
        (listOf(null) + PwToolType.values().toList()).forEach { tool ->
            Disposer().use { disposer ->
                val appUrl = TestApplicationUrl(if (tool == null) "" else "/${tool.slug}")

                disposer.add(
                    Application(
                        scope,
                        rootElement = document.body!!,
                        assetLoader = components.assetLoader,
                        applicationUrl = appUrl,
                        createThreeRenderer = components.createThreeRenderer,
                    )
                )
            }
        }
    }
}

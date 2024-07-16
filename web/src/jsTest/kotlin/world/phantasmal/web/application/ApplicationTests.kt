package world.phantasmal.web.application

import kotlinx.browser.document
import world.phantasmal.web.core.PwToolType
import world.phantasmal.web.core.persistence.MemoryKeyValueStore
import world.phantasmal.web.test.TestApplicationUrl
import world.phantasmal.web.test.WebTestContext
import world.phantasmal.web.test.WebTestSuite
import kotlin.test.Test

class ApplicationTests : WebTestSuite {
    @Test
    fun initialization_and_shutdown_succeeds_with_empty_url() = test {
        initialization_and_shutdown_succeeds("")
    }

    @Test
    fun initialization_and_shutdown_succeeds_with_hunt_optimizer_url() = test {
        initialization_and_shutdown_succeeds("/" + PwToolType.HuntOptimizer.slug)
    }

    @Test
    fun initialization_and_shutdown_succeeds_with_quest_editor_url() = test {
        initialization_and_shutdown_succeeds("/" + PwToolType.QuestEditor.slug)
    }

    @Test
    fun initialization_and_shutdown_succeeds_with_viewer_url() = test {
        initialization_and_shutdown_succeeds("/" + PwToolType.Viewer.slug)
    }

    private fun WebTestContext.initialization_and_shutdown_succeeds(url: String) {
        components.applicationUrl = TestApplicationUrl(url)

        disposer.add(
            Application(
                keyValueStore = MemoryKeyValueStore(),
                rootElement = document.body!!,
                assetLoader = components.assetLoader,
                applicationUrl = components.applicationUrl,
                createThreeRenderer = components.createThreeRenderer,
                clock = components.clock,
            )
        )
    }
}

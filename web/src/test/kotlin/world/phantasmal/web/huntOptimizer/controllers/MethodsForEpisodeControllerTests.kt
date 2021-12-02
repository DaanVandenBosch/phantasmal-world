package world.phantasmal.web.huntOptimizer.controllers

import world.phantasmal.psolib.Episode
import world.phantasmal.web.huntOptimizer.stores.HuntMethodStore
import world.phantasmal.web.test.WebTestSuite
import world.phantasmal.webui.LoadingStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MethodsForEpisodeControllerTests : WebTestSuite {
    @Test
    fun methods_for_the_given_episode_are_loaded_when_necessary() = testAsync {
        for (episode in Episode.values()) {
            val ctrl = disposer.add(
                MethodsForEpisodeController(
                    // Create our own store each time to ensure methods is uninitialized.
                    disposer.add(HuntMethodStore(
                        components.uiStore,
                        components.assetLoader,
                        components.huntMethodPersister,
                    )),
                    episode,
                )
            )

            assertEquals(LoadingStatus.Uninitialized, ctrl.loadingStatus.value)

            // Start loading methods by accessing values.
            ctrl.values

            ctrl.loadingStatus.await()

            assertEquals(LoadingStatus.Ok, ctrl.loadingStatus.value)

            assertTrue(ctrl.values.value.all { it.episode == episode })
        }
    }
}

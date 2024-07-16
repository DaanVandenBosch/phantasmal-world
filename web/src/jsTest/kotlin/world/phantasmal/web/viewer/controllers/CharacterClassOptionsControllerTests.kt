package world.phantasmal.web.viewer.controllers

import world.phantasmal.web.core.PwToolType
import world.phantasmal.web.shared.dto.SectionId
import world.phantasmal.web.test.TestApplicationUrl
import world.phantasmal.web.test.WebTestSuite
import world.phantasmal.web.viewer.ViewerUrls
import world.phantasmal.web.viewer.stores.ViewerStore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class CharacterClassOptionsControllerTests : WebTestSuite {
    /**
     * When starting at the viewer root URL, we're redirected to the viewer mesh URL.
     * I.e. `/viewer` becomes `/viewer/models`.
     */
    @Test
    fun url_parameters_reflect_initial_options_when_starting_at_viewer_root_url() {
        url_parameters_reflect_initial_options(
            fromPath = "/",
            toPath = null,
        )
    }

    @Test
    fun url_parameters_reflect_initial_options_when_starting_at_mesh_url() {
        url_parameters_reflect_initial_options(
            fromPath = ViewerUrls.mesh,
            toPath = null,
        )
    }

    @Test
    fun url_parameters_reflect_initial_options_after_navigating_to_mesh_url() {
        url_parameters_reflect_initial_options(
            fromPath = ViewerUrls.texture,
            toPath = ViewerUrls.mesh,
        )
    }

    @Test
    fun url_parameters_reflect_initial_options_when_starting_at_texture_url() {
        url_parameters_reflect_initial_options(
            fromPath = ViewerUrls.texture,
            toPath = null,
        )
    }

    @Test
    fun url_parameters_reflect_initial_options_after_navigating_to_texture_url() {
        url_parameters_reflect_initial_options(
            fromPath = ViewerUrls.mesh,
            toPath = ViewerUrls.texture,
        )
    }

    /**
     * When no URL parameters are given, we update the URL to include some default (random) options.
     * I.e. `/viewer/models` becomes `/viewer/models?model=RAcast&section_id=Purplenum&body=10`.
     */
    private fun url_parameters_reflect_initial_options(fromPath: String, toPath: String?) =
        test {
            val applicationUrl = TestApplicationUrl("/${PwToolType.Viewer.slug}$fromPath")
            components.applicationUrl = applicationUrl

            val viewerCtrl =
                disposer.add(ViewerController(components.uiStore, components.viewerStore))
            val ctrl = disposer.add(CharacterClassOptionsController(components.viewerStore))

            val expectedHistoryEntries: Int

            if (toPath != null) {
                // When navigating, the URL is pushed.
                expectedHistoryEntries = applicationUrl.historyEntries + 1
                viewerCtrl.setActiveTab(viewerCtrl.tabs.first { it.path == toPath })
            } else {
                // When we don't navigate, the URL is replaced.
                expectedHistoryEntries = applicationUrl.historyEntries
            }

            val characterClass = components.viewerStore.currentCharacterClass.value
            val sectionId = ctrl.currentSectionId.value
            val body = ctrl.currentBody.value

            val params = applicationUrl.pathAndParamsDeconstructed.params
            assertEquals(characterClass?.slug, params[ViewerStore.MODEL_PARAM])
            assertEquals(sectionId.name, params[ViewerStore.SECTION_ID_PARAM])
            assertEquals(body.toString(), params[ViewerStore.BODY_PARAM])
            assertEquals(expectedHistoryEntries, applicationUrl.historyEntries)
        }

    @Test
    fun url_parameters_reflect_changes_to_options_at_mesh_url() {
        url_parameters_reflect_changes_to_options(ViewerUrls.mesh)
    }

    @Test
    fun url_parameters_reflect_changes_to_options_at_texture_url() {
        url_parameters_reflect_changes_to_options(ViewerUrls.texture)
    }

    private fun url_parameters_reflect_changes_to_options(path: String) = testAsync {
        val applicationUrl = TestApplicationUrl("/${PwToolType.Viewer.slug}$path")
        components.applicationUrl = applicationUrl
        var expectedHistoryEntries = applicationUrl.historyEntries

        val ctrl = disposer.add(CharacterClassOptionsController(components.viewerStore))

        val characterClass = components.viewerStore.currentCharacterClass.value
        assertNotNull(characterClass)

        repeat(3) {
            // Change the section ID.
            val sectionId = SectionId.VALUES[
                    (SectionId.VALUES.indexOf(ctrl.currentSectionId.value) + 1) %
                            SectionId.VALUES.size
            ]
            ctrl.setCurrentSectionId(sectionId)
            expectedHistoryEntries++

            val params1 = applicationUrl.pathAndParamsDeconstructed.params
            assertEquals(characterClass.slug, params1[ViewerStore.MODEL_PARAM]) // Unchanged.
            assertEquals(sectionId.name, params1[ViewerStore.SECTION_ID_PARAM]) // Changed.
            assertEquals(expectedHistoryEntries, applicationUrl.historyEntries)

            // Change the body.
            val body = (ctrl.currentBody.value + 1) % characterClass.bodyStyleCount
            ctrl.setCurrentBody(body)
            expectedHistoryEntries++

            val params2 = applicationUrl.pathAndParamsDeconstructed.params
            assertEquals(characterClass.slug, params2[ViewerStore.MODEL_PARAM]) // Unchanged.
            assertEquals(sectionId.name, params2[ViewerStore.SECTION_ID_PARAM]) // Unchanged.
            assertEquals(body.toString(), params2[ViewerStore.BODY_PARAM]) // Changed.
            assertEquals(expectedHistoryEntries, applicationUrl.historyEntries)
        }
    }
}

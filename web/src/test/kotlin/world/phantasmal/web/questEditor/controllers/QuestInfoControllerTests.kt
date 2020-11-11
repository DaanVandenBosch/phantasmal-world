package world.phantasmal.web.questEditor.controllers

import world.phantasmal.lib.fileFormats.quest.Episode
import world.phantasmal.web.test.WebTestSuite
import world.phantasmal.web.test.createQuestModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class QuestInfoControllerTests : WebTestSuite() {
    @Test
    fun exposes_correct_model_before_and_after_a_quest_is_loaded() = asyncTest {
        val store = components.questEditorStore
        val ctrl = disposer.add(QuestInfoController(store))

        assertTrue(ctrl.unavailable.value)
        assertFalse(ctrl.enabled.value)

        store.setCurrentQuest(createQuestModel(
            id = 25,
            name = "A Quest",
            shortDescription = "A short description.",
            longDescription = "A long description.",
            episode = Episode.II
        ))

        assertFalse(ctrl.unavailable.value)
        assertTrue(ctrl.enabled.value)
        assertEquals("II", ctrl.episode.value)
        assertEquals(25, ctrl.id.value)
        assertEquals("A Quest", ctrl.name.value)
        assertEquals("A short description.", ctrl.shortDescription.value)
        assertEquals("A long description.", ctrl.longDescription.value)
    }
}

package world.phantasmal.web.questEditor.controllers

import world.phantasmal.lib.fileFormats.quest.Episode
import world.phantasmal.lib.fileFormats.quest.NpcType
import world.phantasmal.web.test.WebTestSuite
import world.phantasmal.web.test.createQuestModel
import world.phantasmal.web.test.createQuestNpcModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NpcCountsControllerTests : WebTestSuite() {
    @Test
    fun exposes_correct_model_before_and_after_a_quest_is_loaded() = asyncTest {
        val store = components.questEditorStore
        val ctrl = disposer.add(NpcCountsController(store))

        assertTrue(ctrl.unavailable.value)

        store.setCurrentQuest(createQuestModel(
            episode = Episode.I,
            npcs = listOf(
                createQuestNpcModel(NpcType.Scientist, Episode.I),
                createQuestNpcModel(NpcType.Nurse, Episode.I),
                createQuestNpcModel(NpcType.Nurse, Episode.I),
                createQuestNpcModel(NpcType.Principal, Episode.I),
                createQuestNpcModel(NpcType.Nurse, Episode.I),
                createQuestNpcModel(NpcType.Scientist, Episode.I),
            )
        ))

        assertFalse(ctrl.unavailable.value)
        assertEquals(3, ctrl.npcCounts.value.size)
        assertEquals("Principal", ctrl.npcCounts.value[0].name)
        assertEquals("1", ctrl.npcCounts.value[0].count)
        assertEquals("Scientist", ctrl.npcCounts.value[1].name)
        assertEquals("2", ctrl.npcCounts.value[1].count)
        assertEquals("Nurse", ctrl.npcCounts.value[2].name)
        assertEquals("3", ctrl.npcCounts.value[2].count)
    }
}

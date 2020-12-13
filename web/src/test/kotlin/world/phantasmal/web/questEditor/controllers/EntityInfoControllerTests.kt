package world.phantasmal.web.questEditor.controllers

import world.phantasmal.lib.fileFormats.Vec3
import world.phantasmal.lib.Episode
import world.phantasmal.lib.fileFormats.quest.NpcType
import world.phantasmal.lib.fileFormats.quest.QuestNpc
import world.phantasmal.testUtils.assertCloseTo
import world.phantasmal.web.questEditor.models.QuestNpcModel
import world.phantasmal.web.questEditor.models.WaveModel
import world.phantasmal.web.test.WebTestSuite
import world.phantasmal.web.test.createQuestModel
import world.phantasmal.web.test.createQuestNpcModel
import kotlin.math.PI
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EntityInfoControllerTests : WebTestSuite() {
    @Test
    fun test_unavailable_and_enabled() = asyncTest {
        val ctrl = disposer.add(EntityInfoController(components.questEditorStore))

        assertTrue(ctrl.unavailable.value)
        assertFalse(ctrl.enabled.value)

        val npc = createQuestNpcModel(NpcType.Principal, Episode.I)
        components.questEditorStore.setCurrentQuest(createQuestModel(npcs = listOf(npc)))

        assertTrue(ctrl.unavailable.value)
        assertTrue(ctrl.enabled.value)

        components.questEditorStore.setSelectedEntity(npc)

        assertFalse(ctrl.unavailable.value)
        assertTrue(ctrl.enabled.value)
    }

    @Test
    fun can_read_regular_properties() = asyncTest {
        val ctrl = disposer.add(EntityInfoController(components.questEditorStore))

        val questNpc = QuestNpc(NpcType.Booma, Episode.I, areaId = 10, wave = 5)
        questNpc.sectionId = 7
        questNpc.position = Vec3(8f, 16f, 32f)
        questNpc.rotation = Vec3(PI.toFloat() / 4, PI.toFloat() / 2, PI.toFloat())
        val npc = QuestNpcModel(questNpc, WaveModel(5, 10, 7))
        components.questEditorStore.setCurrentQuest(createQuestModel(npcs = listOf(npc)))
        components.questEditorStore.setSelectedEntity(npc)

        assertEquals("NPC", ctrl.type.value)
        assertEquals("Booma", ctrl.name.value)
        assertEquals("7", ctrl.sectionId.value)
        assertEquals("5", ctrl.wave.value)
        assertFalse(ctrl.waveHidden.value)
        assertEquals(8.0, ctrl.posX.value)
        assertEquals(16.0, ctrl.posY.value)
        assertEquals(32.0, ctrl.posZ.value)
        assertCloseTo(45.0, ctrl.rotX.value)
        assertCloseTo(90.0, ctrl.rotY.value)
        assertCloseTo(180.0, ctrl.rotZ.value)
    }

    @Test
    fun can_set_regular_properties_undo_and_redo() = asyncTest {
        val ctrl = disposer.add(EntityInfoController(components.questEditorStore))

        val npc = createQuestNpcModel(NpcType.Principal, Episode.I)
        components.questEditorStore.setCurrentQuest(createQuestModel(npcs = listOf(npc)))
        components.questEditorStore.setSelectedEntity(npc)

        ctrl.setPosX(3.15)
        ctrl.setPosY(4.15)
        ctrl.setPosZ(5.15)

        ctrl.setRotX(50.0)
        ctrl.setRotY(25.4)
        ctrl.setRotZ(12.5)

        assertEquals(3.15, ctrl.posX.value)
        assertEquals(4.15, ctrl.posY.value)
        assertEquals(5.15, ctrl.posZ.value)

        assertCloseTo(50.0, ctrl.rotX.value)
        assertCloseTo(25.4, ctrl.rotY.value)
        assertCloseTo(12.5, ctrl.rotZ.value)

        components.questEditorStore.makeMainUndoCurrent()
        components.questEditorStore.undo()
        components.questEditorStore.undo()
        components.questEditorStore.undo()
        components.questEditorStore.undo()
        components.questEditorStore.undo()
        components.questEditorStore.undo()

        assertEquals(0.0, ctrl.posX.value)
        assertEquals(0.0, ctrl.posY.value)
        assertEquals(0.0, ctrl.posZ.value)

        assertEquals(0.0, ctrl.rotX.value)
        assertEquals(0.0, ctrl.rotY.value)
        assertEquals(0.0, ctrl.rotZ.value)

        components.questEditorStore.redo()
        components.questEditorStore.redo()
        components.questEditorStore.redo()
        components.questEditorStore.redo()
        components.questEditorStore.redo()
        components.questEditorStore.redo()

        assertEquals(3.15, ctrl.posX.value)
        assertEquals(4.15, ctrl.posY.value)
        assertEquals(5.15, ctrl.posZ.value)

        assertCloseTo(50.0, ctrl.rotX.value)
        assertCloseTo(25.4, ctrl.rotY.value)
        assertCloseTo(12.5, ctrl.rotZ.value)
    }
}

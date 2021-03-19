package world.phantasmal.web.questEditor.models

import world.phantasmal.lib.Episode
import world.phantasmal.lib.fileFormats.quest.NpcType
import world.phantasmal.web.core.euler
import world.phantasmal.web.externals.three.Vector3
import world.phantasmal.web.test.WebTestSuite
import world.phantasmal.web.test.createQuestNpcModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class QuestEntityModelTests : WebTestSuite {
    @Test
    fun positions_are_updated_correctly_when_section_changes() = test {
        // Relative and world position start out the same.
        val entity = createQuestNpcModel(NpcType.AlRappy, Episode.I)
        entity.setPosition(Vector3(5.0, 5.0, 5.0))

        assertTrue(entity.position.value.equals(entity.worldPosition.value))

        // When section is initialized, relative position stays the same and world position changes.
        entity.initializeSection(SectionModel(
            20,
            Vector3(7.0, 7.0, 7.0),
            euler(.0, .0, .0),
            components.areaStore.getVariant(Episode.I, 0, 0)!!,
        ))

        assertEquals(5.0, entity.position.value.x)
        assertEquals(5.0, entity.position.value.y)
        assertEquals(5.0, entity.position.value.z)

        assertEquals(12.0, entity.worldPosition.value.x)
        assertEquals(12.0, entity.worldPosition.value.y)
        assertEquals(12.0, entity.worldPosition.value.z)

        // When section is then changed, relative position changes and world position stays the
        // same.
        entity.setSection(SectionModel(
            30,
            Vector3(11.0, 11.0, 11.0),
            euler(.0, .0, .0),
            components.areaStore.getVariant(Episode.I, 0, 0)!!,
        ))

        assertEquals(1.0, entity.position.value.x)
        assertEquals(1.0, entity.position.value.y)
        assertEquals(1.0, entity.position.value.z)

        assertEquals(12.0, entity.worldPosition.value.x)
        assertEquals(12.0, entity.worldPosition.value.y)
        assertEquals(12.0, entity.worldPosition.value.z)
    }
}

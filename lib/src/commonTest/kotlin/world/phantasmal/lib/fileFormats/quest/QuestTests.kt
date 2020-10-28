package world.phantasmal.lib.fileFormats.quest

import world.phantasmal.core.Success
import world.phantasmal.lib.test.asyncTest
import world.phantasmal.lib.test.readFile
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class QuestTests {
    @Test
    fun parseBinDatToQuest_with_towards_the_future() = asyncTest {
        val result = parseBinDatToQuest(readFile("/quest118_e.bin"), readFile("/quest118_e.dat"))

        assertTrue (result is Success)
        assertTrue(result.problems.isEmpty())

        val quest = result.value

        assertEquals("Towards the Future", quest.name)
        assertEquals("Challenge the\nnew simulator.", quest.shortDescription)
        assertEquals(
            "Client: Principal\nQuest: Wishes to have\nhunters challenge the\nnew simulator\nReward: ??? Meseta",
            quest.longDescription
        )
        assertEquals(Episode.I, quest.episode)
        assertEquals(277, quest.objects.size)
        // TODO: Test objects.
//        assertEquals(ObjectType.MenuActivation, quest.objects[0])
//        assertEquals(ObjectType.PlayerSet, quest.objects[4])
        assertEquals(216, quest.npcs.size)
        assertEquals(10, quest.mapDesignations.size)
        assertEquals(0, quest.mapDesignations[0])
        assertEquals(0, quest.mapDesignations[2])
        assertEquals(0, quest.mapDesignations[11])
        assertEquals(4, quest.mapDesignations[5])
        assertEquals(0, quest.mapDesignations[12])
        assertEquals(4, quest.mapDesignations[7])
        assertEquals(0, quest.mapDesignations[13])
        assertEquals(4, quest.mapDesignations[8])
        assertEquals(4, quest.mapDesignations[10])
        assertEquals(0, quest.mapDesignations[14])
    }
}

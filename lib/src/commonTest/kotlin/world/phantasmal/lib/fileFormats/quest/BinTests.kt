package world.phantasmal.lib.fileFormats.quest

import world.phantasmal.lib.test.asyncTest
import world.phantasmal.lib.test.readFile
import kotlin.test.Test
import kotlin.test.assertEquals

class BinTests {
    @Test
    fun parse_quest_towards_the_future() = asyncTest {
        val bin = parseBin(readFile("/quest118_e_decompressed.bin"))

        assertEquals(BinFormat.BB, bin.format)
        assertEquals(118, bin.questId)
        assertEquals(0, bin.language)
        assertEquals("Towards the Future", bin.questName)
        assertEquals("Challenge the\nnew simulator.", bin.shortDescription)
        assertEquals(
            "Client: Principal\nQuest: Wishes to have\nhunters challenge the\nnew simulator\nReward: ??? Meseta",
            bin.longDescription
        )
    }
}

package world.phantasmal.lib.fileFormats.quest

import world.phantasmal.lib.cursor.cursor
import world.phantasmal.lib.test.LibTestSuite
import world.phantasmal.lib.test.assertDeepEquals
import world.phantasmal.lib.test.readFile
import kotlin.test.Test
import kotlin.test.assertEquals

class BinTests : LibTestSuite() {
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

    @Test
    fun parse_and_write_towards_the_future() = parseAndWriteQuest("/quest118_e_decompressed.bin")

    @Test
    fun parse_and_write_seat_of_the_heart() = parseAndWriteQuest("/quest27_e_decompressed.bin")

    private fun parseAndWriteQuest(file: String) = asyncTest {
        val origBin = readFile(file)
        val newBin = writeBin(parseBin(origBin)).cursor()
        origBin.seekStart(0)

        assertDeepEquals(origBin, newBin)
    }
}

package world.phantasmal.lib.fileFormats.quest

import world.phantasmal.lib.test.asyncTest
import world.phantasmal.lib.test.readFile
import kotlin.test.Test
import kotlin.test.assertEquals

class DatTests {
    @Test
    fun parse_quest_towards_the_future() = asyncTest {
        val dat = parseDat(readFile("/quest118_e_decompressed.dat"))

        assertEquals(277, dat.objs.size)
        assertEquals(216, dat.npcs.size)
    }
}

package world.phantasmal.lib.fileFormats.quest

import world.phantasmal.lib.test.LibTestSuite
import world.phantasmal.lib.test.readFile
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class QstTests : LibTestSuite() {
    @Test
    fun parse_a_GC_quest() = asyncTest {
        val cursor = readFile("/lost_heat_sword_gc.qst")
        val qst = parseQst(cursor).unwrap()

        assertEquals(Version.GC, qst.version)
        assertTrue(qst.online)
        assertEquals(2, qst.files.size)
        assertEquals(58, qst.files[0].id)
        assertEquals("quest58.bin", qst.files[0].filename)
        assertEquals("PSO/Lost HEAT SWORD", qst.files[0].questName)
        assertEquals(58, qst.files[1].id)
        assertEquals("quest58.dat", qst.files[1].filename)
        assertEquals("PSO/Lost HEAT SWORD", qst.files[1].questName)
    }
}

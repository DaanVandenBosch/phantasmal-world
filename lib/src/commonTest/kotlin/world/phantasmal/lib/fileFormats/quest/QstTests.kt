package world.phantasmal.lib.fileFormats.quest

import world.phantasmal.lib.cursor.cursor
import world.phantasmal.lib.test.LibTestSuite
import world.phantasmal.lib.test.assertDeepEquals
import world.phantasmal.lib.test.readFile
import world.phantasmal.lib.test.testWithTetheallaQuests
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class QstTests : LibTestSuite {
    @Test
    fun parse_a_GC_quest() = testAsync {
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

    /**
     * Parse a file, convert the resulting structure to QST again and check whether the end result
     * is byte-for-byte equal to the original.
     */
    @Test
    fun parseQst_and_writeQst_with_all_tethealla_quests() = testAsync {
        testWithTetheallaQuests { path, _ ->
            if (EXCLUDED.any { it in path }) return@testWithTetheallaQuests

            try {
                val origQst = readFile(path)
                val parsedQst = parseQst(origQst).unwrap()
                val newQst = writeQst(parsedQst)
                origQst.seekStart(0)

                assertDeepEquals(origQst, newQst.cursor())
            } catch (e: Throwable) {
                throw Exception("""Failed for "$path": ${e.message}""", e)
            }
        }
    }

    companion object {
        // TODO: Figure out why we can't round-trip these quests.
        private val EXCLUDED = listOf(
            "/ep2/shop/gallon.qst",
            "/princ/ep1/",
            "/princ/ep4/",
            "/solo/ep1/04.qst", // Skip because it contains every chuck twice.
            "/fragmentofmemoryen.qst",
            "/lost havoc vulcan.qst",
            "/goodluck.qst",
            ".raw",
        )
    }
}

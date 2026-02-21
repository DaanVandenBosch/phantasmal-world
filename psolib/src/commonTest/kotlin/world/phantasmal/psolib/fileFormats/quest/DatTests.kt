package world.phantasmal.psolib.fileFormats.quest

import world.phantasmal.psolib.cursor.cursor
import world.phantasmal.psolib.test.LibTestSuite
import world.phantasmal.psolib.test.assertDeepEquals
import world.phantasmal.psolib.test.readFile
import kotlin.test.Test
import kotlin.test.assertEquals

class DatTests : LibTestSuite {
    @Test
    fun parse_quest_towards_the_future() = testAsync {
        val dat = parseDat(readFile("/quest118_e_decompressed.dat"))

        assertEquals(277, dat.objs.size)
        assertEquals(216, dat.npcs.size)
    }

    /**
     * Parse a file, convert the resulting structure to DAT again and check whether the end result
     * is byte-for-byte equal to the original.
     */
    @Test
    fun parse_dat_and_write_dat() = testAsync {
        val origDat = readFile("/quest118_e_decompressed.dat")
        val newDat = writeDat(parseDat(origDat)).cursor()
        origDat.seekStart(0)

        assertDeepEquals(origDat, newDat)
    }

    /**
     * Parse a file, modify the resulting structure, convert it to DAT again and check whether the
     * end result is byte-for-byte equal to the original except for the bytes that should be
     * changed.
     */
    @Test
    fun parse_modify_write_dat() = testAsync {
        val origDat = readFile("/quest118_e_decompressed.dat")
        val parsedDat = parseDat(origDat)
        origDat.seekStart(0)

        parsedDat.objs[9].data.setFloat(16, 13f)
        parsedDat.objs[9].data.setFloat(20, 17f)
        parsedDat.objs[9].data.setFloat(24, 19f)

        val newDat = writeDat(parsedDat).cursor()

        assertEquals(origDat.size, newDat.size)

        while (origDat.hasBytesLeft()) {
            if (origDat.position == 16 + 9 * OBJECT_BYTE_SIZE + 16) {
                origDat.seek(12)

                assertEquals(13f, newDat.float())
                assertEquals(17f, newDat.float())
                assertEquals(19f, newDat.float())
            } else {
                assertEquals(origDat.byte(), newDat.byte())
            }
        }
    }

    @Test
    fun parse_challenge_mode_quest_chl01() = testAsync {
        val dat = parseDat(readFile("/chl01.dat"))

        // Area 1 should have 8 rooms with specific room IDs and entry counts.
        val area1Spawns = dat.cmRandomSpawns.filter { it.areaId == 1 }
        assertEquals(8, area1Spawns.size)
        assertEquals(listOf(2, 4, 5, 7, 8, 10, 11, 16), area1Spawns.map { it.roomId })
        assertEquals(listOf(32, 32, 32, 32, 31, 23, 31, 27), area1Spawns.map { it.entries.size })

        // Verify config pool is parsed (Table 5A).
        assertEquals(2, dat.cmConfigPool.size)
        assertEquals(12, dat.cmConfigPool[0].entries.size)
        // Config IDs should be sequential 1..12.
        assertEquals((1..12).toList(), dat.cmConfigPool[0].entries.map { it.configId })

        // Verify monster mappings are parsed (Table 5B).
        assertEquals(2, dat.cmMonsterMappings.size)
        assertEquals(11, dat.cmMonsterMappings[0].entries.size)
        assertEquals(13, dat.cmMonsterMappings[1].entries.size)
    }

    /**
     * Parse a challenge mode DAT, write it back, and verify byte-for-byte equality.
     */
    @Test
    fun parse_cm_dat_and_write_dat() = testAsync {
        val origDat = readFile("/chl01.dat")
        val newDat = writeDat(parseDat(origDat)).cursor()
        origDat.seekStart(0)

        assertDeepEquals(origDat, newDat)
    }

}
package world.phantasmal.lib.fileFormats.quest

import world.phantasmal.lib.cursor.cursor
import world.phantasmal.lib.test.LibTestSuite
import world.phantasmal.lib.test.assertDeepEquals
import world.phantasmal.lib.test.readFile
import kotlin.test.Test
import kotlin.test.assertEquals

class DatTests : LibTestSuite() {
    @Test
    fun parse_quest_towards_the_future() = asyncTest {
        val dat = parseDat(readFile("/quest118_e_decompressed.dat"))

        assertEquals(277, dat.objs.size)
        assertEquals(216, dat.npcs.size)
    }

    /**
     * Parse a file, convert the resulting structure to DAT again and check whether the end result
     * is byte-for-byte equal to the original.
     */
    @Test
    fun parse_dat_and_write_dat() = asyncTest {
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
    fun parse_modify_write_dat() = asyncTest {
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
}

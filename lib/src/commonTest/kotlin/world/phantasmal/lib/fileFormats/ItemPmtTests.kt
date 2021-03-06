package world.phantasmal.lib.fileFormats

import world.phantasmal.lib.test.LibTestSuite
import world.phantasmal.lib.test.readFile
import kotlin.test.Test
import kotlin.test.assertEquals

class ItemPmtTests : LibTestSuite() {
    @Test
    fun parseBasicItemPmt() = testAsync {
        val itemPmt = parseItemPmt(readFile("/ItemPMT.bin"))

        val saber = itemPmt.weapons[1][0]

        assertEquals(177, saber.id)
        assertEquals(40, saber.minAtp)
        assertEquals(55, saber.maxAtp)
        assertEquals(30, saber.ata)
        assertEquals(35, saber.maxGrind)
        assertEquals(30, saber.reqAtp)
    }
}

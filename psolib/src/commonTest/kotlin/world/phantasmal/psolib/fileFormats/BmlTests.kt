package world.phantasmal.psolib.fileFormats

import world.phantasmal.core.Success
import world.phantasmal.psolib.test.LibTestSuite
import world.phantasmal.psolib.test.readFile
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class BmlTests : LibTestSuite {
    @Test
    fun parse_small_bml() = testAsync {
        val result = parseBml(readFile("/minimal.bml"))

        assertIs<Success<List<BmlEntry>>>(result)

        val entries = result.value

        assertEquals(2, entries.size)

        // Entry 1: model_a.nj with model data and texture data.
        assertEquals("model_a.nj", entries[0].name)
        assertEquals(16, entries[0].data.size)
        assertEquals(0xAA.toByte(), entries[0].data.getByte(0))
        assertEquals(8, entries[0].textureData.size)
        assertEquals(0xBB.toByte(), entries[0].textureData.getByte(0))

        // Entry 2: model_b.xj with model data only, no textures.
        assertEquals("model_b.xj", entries[1].name)
        assertEquals(24, entries[1].data.size)
        assertEquals(0xCC.toByte(), entries[1].data.getByte(0))
        assertEquals(0, entries[1].textureData.size)
    }
}

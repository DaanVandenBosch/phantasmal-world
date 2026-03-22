package world.phantasmal.psolib.fileFormats

import world.phantasmal.core.Success
import world.phantasmal.psolib.test.LibTestSuite
import world.phantasmal.psolib.test.readFile
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class GslTests : LibTestSuite {
    @Test
    fun parse_small_gsl() = testAsync {
        val result = parseGsl(readFile("/minimal.gsl"))

        assertIs<Success<List<GslEntry>>>(result)

        val entries = result.value

        assertEquals(3, entries.size)

        assertEquals("file_a.rel", entries[0].name)
        assertEquals(12, entries[0].data.size)
        assertEquals(0x41.toByte(), entries[0].data.getByte(0))

        assertEquals("file_b.rel", entries[1].name)
        assertEquals(8, entries[1].data.size)
        assertEquals(0x42.toByte(), entries[1].data.getByte(0))

        assertEquals("file_c.rel", entries[2].name)
        assertEquals(20, entries[2].data.size)
        assertEquals(0x43.toByte(), entries[2].data.getByte(0))
    }
}

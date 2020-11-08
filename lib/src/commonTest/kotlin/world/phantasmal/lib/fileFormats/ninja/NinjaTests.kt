package world.phantasmal.lib.fileFormats.ninja

import world.phantasmal.core.Success
import world.phantasmal.lib.test.LibTestSuite
import world.phantasmal.lib.test.readFile
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NinjaTests : LibTestSuite() {
    @Test
    fun can_parse_rag_rappy_model() = asyncTest {
        val result = parseNj(readFile("/RagRappy.nj"))

        assertTrue(result is Success)
        assertEquals(1, result.value.size)
    }
}

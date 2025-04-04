package world.phantasmal.psolib.fileFormats.ninja

import world.phantasmal.core.Success
import world.phantasmal.psolib.test.LibTestSuite
import world.phantasmal.psolib.test.readFile
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NinjaTests : LibTestSuite {
    @Test
    fun can_parse_rag_rappy_model() = testAsync {
        val result = parseNj(readFile("/RagRappy.nj"))

        assertTrue(result is Success)
        assertEquals(1, result.value.size)
    }
}

package world.phantasmal.webui

import world.phantasmal.webui.test.WebuiTestSuite
import kotlin.test.Test
import kotlin.test.assertEquals

class StandardExtensionsTests : WebuiTestSuite {
    @Test
    fun toRoundedString() = test {
        assertEquals("0.90", (0.9).toRoundedString(2))
        assertEquals("100.00", (99.99999999).toRoundedString(2))
        assertEquals("0.01", (0.01).toRoundedString(2))
        assertEquals("0.10", (0.1).toRoundedString(2))
        assertEquals("0", (0.01).toRoundedString(0))
        assertEquals("45450.010", (45450.01).toRoundedString(3))
        assertEquals("0.1", (0.05).toRoundedString(1))
        assertEquals("0.0", (0.04).toRoundedString(1))
    }
}

package world.phantasmal.lib.assembly

import world.phantasmal.lib.test.LibTestSuite
import world.phantasmal.testUtils.assertCloseTo
import kotlin.test.Test
import kotlin.test.assertEquals

class AssemblyTokenizationTests : LibTestSuite() {
    @Test
    fun valid_floats_are_parsed_as_Float32_tokens() {
        assertCloseTo(808.9f, (tokenizeLine("808.9")[0] as Token.Float32).value)
        assertCloseTo(-0.9f, (tokenizeLine("-0.9")[0] as Token.Float32).value)
        assertCloseTo(0.001f, (tokenizeLine("1e-3")[0] as Token.Float32).value)
        assertCloseTo(-600.0f, (tokenizeLine("-6e2")[0] as Token.Float32).value)
    }

    @Test
    fun invalid_floats_area_parsed_as_InvalidNumber_tokens_or_InvalidSection_tokens() {
        val tokens1 = tokenizeLine(" 808.9a ")

        assertEquals(1, tokens1.size)
        assertEquals(Token.InvalidNumber::class, tokens1[0]::class)
        assertEquals(2, tokens1[0].col)
        assertEquals(6, tokens1[0].len)

        val tokens2 = tokenizeLine("  -55e ")

        assertEquals(1, tokens2.size)
        assertEquals(Token.InvalidNumber::class, tokens2[0]::class)
        assertEquals(3, tokens2[0].col)
        assertEquals(4, tokens2[0].len)

        val tokens3 = tokenizeLine(".7429")

        assertEquals(1, tokens3.size)
        assertEquals(Token.InvalidSection::class, tokens3[0]::class)
        assertEquals(1, tokens3[0].col)
        assertEquals(5, tokens3[0].len)

        val tokens4 = tokenizeLine("\t\t\t4. test")

        assertEquals(2, tokens4.size)
        assertEquals(Token.InvalidNumber::class, tokens4[0]::class)
        assertEquals(4, tokens4[0].col)
        assertEquals(2, tokens4[0].len)
    }
}
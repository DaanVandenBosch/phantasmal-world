package world.phantasmal.psolib.asm

import world.phantasmal.psolib.test.LibTestSuite
import world.phantasmal.testUtils.assertCloseTo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AsmTokenizationTests : LibTestSuite {
    @Test
    fun hexadecimal_numbers_are_parsed_as_ints() {
        val tokenizer = LineTokenizer()

        tokenizer.testInt("0X00", 0x00)
        tokenizer.testInt("0x70", 0x70)
        tokenizer.testInt("0xa1", 0xA1)
        tokenizer.testInt("0xAB", 0xAB)
        tokenizer.testInt("0xAb", 0xAB)
        tokenizer.testInt("0xaB", 0xAB)
        tokenizer.testInt("0xff", 0xFF)
    }

    private fun LineTokenizer.testInt(line: String, value: Int) {
        tokenize(line)
        assertTrue(nextToken())
        assertEquals(Token.Int32, type)
        assertEquals(value, intValue)
        assertFalse(nextToken())
    }

    @Test
    fun valid_floats_are_parsed_as_Float32_tokens() {
        val tokenizer = LineTokenizer()

        tokenizer.testFloat("808.9", 808.9f)
        tokenizer.testFloat("-0.9", -0.9f)
        tokenizer.testFloat("1e-3", 0.001f)
        tokenizer.testFloat("-6e2", -600.0f)
    }

    private fun LineTokenizer.testFloat(line: String, value: Float) {
        tokenize(line)
        assertTrue(nextToken())
        assertEquals(Token.Float32, type)
        assertCloseTo(value, floatValue)
        assertFalse(nextToken())
    }

    @Test
    fun invalid_floats_area_parsed_as_InvalidNumber_tokens_or_InvalidSection_tokens() {
        val tokenizer = LineTokenizer()

        tokenizer.testInvalidFloat(" 808.9a ", Token.InvalidNumber, col = 2, len = 6)
        tokenizer.testInvalidFloat("  -55e ", Token.InvalidNumber, col = 3, len = 4)
        tokenizer.testInvalidFloat(".7429", Token.InvalidSection, col = 1, len = 5)
        tokenizer.testInvalidFloat(
            "\t\t\t4. test",
            Token.InvalidNumber,
            col = 4,
            len = 2,
            extraTokens = 1,
        )
    }

    private fun LineTokenizer.testInvalidFloat(
        line: String,
        type: Token,
        col: Int,
        len: Int,
        extraTokens: Int = 0,
    ) {
        tokenize(line)
        assertTrue(nextToken())
        assertEquals(type, this.type)
        assertEquals(col, this.col)
        assertEquals(len, this.len)
        repeat(extraTokens) { assertTrue(nextToken()) }
        assertFalse(nextToken())
    }

    @Test
    fun strings_are_parsed_as_Str_tokens() {
        val tokenizer = LineTokenizer()

        tokenizer.testString(""" "one line" """, "one line", col = 2, len = 10)
        tokenizer.testString(""" "two\nlines" """, "two\nlines", col = 2, len = 12)
        tokenizer.testString(
            """ "is \"this\" escaped?" """,
            "is \"this\" escaped?",
            col = 2,
            len = 22,
        )
    }

    private fun LineTokenizer.testString(
        line: String,
        value: String,
        col: Int,
        len: Int,
    ) {
        tokenize(line)
        assertTrue(nextToken())
        assertEquals(Token.Str, this.type)
        assertEquals(value, this.strValue)
        assertEquals(col, this.col)
        assertEquals(len, this.len)
        assertFalse(nextToken())
    }

    @Test
    fun valid_identifiers_are_parsed_as_Ident_tokens() {
        val tokenizer = LineTokenizer()

        tokenizer.testIdent("  opcode_mnemonic ", "opcode_mnemonic", col = 3, len = 15)
        tokenizer.testIdent("inst_1", "inst_1", col = 1, len = 6)
    }

    private fun LineTokenizer.testIdent(
        line: String,
        value: String,
        col: Int,
        len: Int,
    ) {
        tokenize(line)
        assertTrue(nextToken())
        assertEquals(Token.Ident, this.type)
        assertEquals(value, this.strValue)
        assertEquals(col, this.col)
        assertEquals(len, this.len)
        assertFalse(nextToken())
    }
}

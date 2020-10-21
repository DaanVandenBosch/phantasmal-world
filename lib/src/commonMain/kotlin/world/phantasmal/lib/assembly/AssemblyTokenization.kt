package world.phantasmal.lib.assembly

import world.phantasmal.core.isDigit

private val HEX_INT_REGEX = Regex("""^0x[\da-fA-F]+$""")
private val FLOAT_REGEX = Regex("""^-?\d+(\.\d+)?(e-?\d+)?$""")
private val IDENT_REGEX = Regex("""^[a-z][a-z0-9_=<>!]*$""")

sealed class Token(
    val col: Int,
    val len: Int,
)

class IntToken(
    col: Int,
    len: Int,
    val value: Int,
) : Token(col, len)

class FloatToken(
    col: Int,
    len: Int,
    val value: Float,
) : Token(col, len)

class InvalidNumberToken(
    col: Int,
    len: Int,
) : Token(col, len)

class RegisterToken(
    col: Int,
    len: Int,
    val value: Int,
) : Token(col, len)

class LabelToken(
    col: Int,
    len: Int,
    val value: Int,
) : Token(col, len)

sealed class SectionToken(col: Int, len: Int) : Token(col, len)

class CodeSectionToken(
    col: Int,
    len: Int,
) : SectionToken(col, len)

class DataSectionToken(
    col: Int,
    len: Int,
) : SectionToken(col, len)

class StringSectionToken(
    col: Int,
    len: Int,
) : SectionToken(col, len)

class InvalidSectionToken(
    col: Int,
    len: Int,
) : Token(col, len)

class StringToken(
    col: Int,
    len: Int,
    val value: String,
) : Token(col, len)

class UnterminatedStringToken(
    col: Int,
    len: Int,
    val value: String,
) : Token(col, len)

class IdentToken(
    col: Int,
    len: Int,
    val value: String,
) : Token(col, len)

class InvalidIdentToken(
    col: Int,
    len: Int,
) : Token(col, len)

class ArgSeparatorToken(
    col: Int,
    len: Int,
) : Token(col, len)

fun tokenizeLine(line: String): MutableList<Token> =
    LineTokenizer(line).tokenize()

private class LineTokenizer(private var line: String) {
    private var index = 0

    private val col: Int
        get() = index + 1

    private var mark = 0

    fun tokenize(): MutableList<Token> {
        val tokens = mutableListOf<Token>()

        while (hasNext()) {
            val char = peek()
            var token: Token

            if (char == '/') {
                skip()

                if (peek() == '/') {
                    // It's a comment.
                    break
                } else {
                    back()
                }
            }

            if (char.isWhitespace()) {
                skip()
                continue
            } else if (char == '-' || char.isDigit()) {
                token = tokenizeNumberOrLabel()
            } else if (char == ',') {
                token = ArgSeparatorToken(col, 1)
                skip()
            } else if (char == '.') {
                token = tokenizeSection()
            } else if (char == '"') {
                token = tokenizeString()
            } else if (char == 'r') {
                token = tokenizeRegisterOrIdent()
            } else {
                token = tokenizeIdent()
            }

            tokens.add(token)
        }

        return tokens
    }

    private fun hasNext(): Boolean = index < line.length

    private fun next(): Char = line[index++]

    private fun peek(): Char = line[index]

    private fun skip() {
        index++
    }

    private fun back() {
        index--
    }

    private fun mark() {
        mark = index
    }

    private fun markedLen(): Int = index - mark

    private fun slice(): String = line.substring(mark, index)

    private fun eatRestOfToken() {
        while (hasNext()) {
            val char = next()

            if (char == ',' || char.isWhitespace()) {
                back()
                break
            }
        }
    }

    private fun tokenizeNumberOrLabel(): Token {
        mark()
        val col = this.col
        skip()
        var isLabel = false

        while (hasNext()) {
            val char = peek()

            if (char == '.' || char == 'e') {
                return tokenizeFloat(col)
            } else if (char == 'x') {
                return tokenizeHexNumber(col)
            } else if (char == ':') {
                isLabel = true
                skip()
                break
            } else if (char == ',' || char.isWhitespace()) {
                break
            } else {
                skip()
            }
        }

        val value = slice().toIntOrNull()
            ?: return InvalidNumberToken(col, markedLen())

        return if (isLabel) {
            LabelToken(col, markedLen(), value)
        } else {
            IntToken(col, markedLen(), value)
        }
    }

    private fun tokenizeHexNumber(col: Int): Token {
        eatRestOfToken()
        val hexStr = slice()

        if (HEX_INT_REGEX.matches(hexStr)) {
            hexStr.toIntOrNull(16)?.let { value ->
                return IntToken(col, markedLen(), value)
            }
        }

        return InvalidNumberToken(col, markedLen())
    }

    private fun tokenizeFloat(col: Int): Token {
        eatRestOfToken()
        val floatStr = slice()

        if (FLOAT_REGEX.matches(floatStr)) {
            floatStr.toFloatOrNull()?.let { value ->
                return FloatToken(col, markedLen(), value)
            }
        }

        return InvalidNumberToken(col, markedLen())
    }

    private fun tokenizeRegisterOrIdent(): Token {
        val col = this.col
        skip()
        mark()
        var isRegister = false

        while (hasNext()) {
            val char = peek()

            if (char.isDigit()) {
                isRegister = true
                skip()
            } else {
                break
            }
        }

        return if (isRegister) {
            val value = slice().toInt()

            RegisterToken(col, markedLen() + 1, value)
        } else {
            back()
            tokenizeIdent()
        }
    }

    private fun tokenizeSection(): Token {
        val col = this.col
        mark()

        while (hasNext()) {
            if (peek().isWhitespace()) {
                break
            } else {
                skip()
            }
        }

        return when (slice()) {
            ".code" -> CodeSectionToken(col, 5)
            ".data" -> DataSectionToken(col, 5)
            ".string" -> StringSectionToken(col, 7)
            else -> InvalidSectionToken(col, markedLen())
        }
    }

    private fun tokenizeString(): Token {
        val col = this.col
        skip()
        mark()
        var prevWasBackSpace = false
        var terminated = false

        while (hasNext()) {
            when (peek()) {
                '\\' -> {
                    prevWasBackSpace = true
                }
                '"' -> {
                    if (!prevWasBackSpace) {
                        terminated = true
                        break
                    }

                    prevWasBackSpace = false
                }
                else -> {
                    prevWasBackSpace = false
                }
            }

            next()
        }

        val value = slice().replace("\\\"", "\"").replace("\\n", "\n")

        return if (terminated) {
            next()
            StringToken(col, markedLen() + 2, value)
        } else {
            UnterminatedStringToken(col, markedLen() + 1, value)
        }
    }

    private fun tokenizeIdent(): Token {
        val col = this.col
        mark()

        while (hasNext()) {
            val char = peek()

            if (char == ',' || char.isWhitespace()) {
                break
            } else if (char == '/') {
                skip()

                if (peek() == '/') {
                    back()
                    break
                }
            } else {
                skip()
            }
        }

        val value = slice()

        return if (IDENT_REGEX.matches(value)) {
            IdentToken(col, markedLen(), value)
        } else {
            InvalidIdentToken(col, markedLen())
        }
    }
}

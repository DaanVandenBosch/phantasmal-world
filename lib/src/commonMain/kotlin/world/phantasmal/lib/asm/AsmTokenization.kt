package world.phantasmal.lib.asm

import world.phantasmal.core.fastIsWhitespace
import world.phantasmal.core.isDigit

private val HEX_INT_REGEX = Regex("""^0[xX][0-9a-fA-F]+$""")
private val FLOAT_REGEX = Regex("""^-?\d+(\.\d+)?(e-?\d+)?$""")
private val IDENT_REGEX = Regex("""^[a-z][a-z0-9_=<>!]*$""")

sealed class Token {
    abstract val col: Int
    abstract val len: Int

    class Int32(
        override val col: Int,
        override val len: Int,
        val value: Int,
    ) : Token()

    class Float32(
        override val col: Int,
        override val len: Int,
        val value: Float,
    ) : Token()

    class InvalidNumber(
        override val col: Int,
        override val len: Int,
    ) : Token()

    class Register(
        override val col: Int,
        override val len: Int,
        val value: Int,
    ) : Token()

    class Label(
        override val col: Int,
        override val len: Int,
        val value: Int,
    ) : Token()

    sealed class Section : Token() {
        class Code(
            override val col: Int,
            override val len: Int,
        ) : Section()

        class Data(
            override val col: Int,
            override val len: Int,
        ) : Section()

        class Str(
            override val col: Int,
            override val len: Int,
        ) : Section()
    }

    class InvalidSection(
        override val col: Int,
        override val len: Int,
    ) : Token()

    class Str(
        override val col: Int,
        override val len: Int,
        val value: String,
    ) : Token()

    class UnterminatedString(
        override val col: Int,
        override val len: Int,
        val value: String,
    ) : Token()

    class Ident(
        override val col: Int,
        override val len: Int,
        val value: String,
    ) : Token()

    class InvalidIdent(
        override val col: Int,
        override val len: Int,
    ) : Token()

    class ArgSeparator(
        override val col: Int,
        override val len: Int,
    ) : Token()
}

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

            if (char.fastIsWhitespace()) {
                skip()
                continue
            } else if (char == '-' || char.isDigit()) {
                token = tokenizeNumberOrLabel()
            } else if (char == ',') {
                token = Token.ArgSeparator(col, 1)
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

            if (char == ',' || char.fastIsWhitespace()) {
                back()
                break
            }
        }
    }

    private fun tokenizeNumberOrLabel(): Token {
        mark()
        val col = this.col
        val firstChar = next()
        var isLabel = false

        while (hasNext()) {
            val char = peek()

            if (char == '.' || char == 'e') {
                return tokenizeFloat(col)
            } else if (firstChar == '0' && (char == 'x' || char == 'X')) {
                return tokenizeHexNumber(col)
            } else if (char == ':') {
                isLabel = true
                break
            } else if (char == ',' || char.fastIsWhitespace()) {
                break
            } else {
                skip()
            }
        }

        val value = slice().toIntOrNull()

        if (isLabel) {
            skip()
        }

        if (value == null) {
            return Token.InvalidNumber(col, markedLen())
        }

        return if (isLabel) {
            Token.Label(col, markedLen(), value)
        } else {
            Token.Int32(col, markedLen(), value)
        }
    }

    private fun tokenizeHexNumber(col: Int): Token {
        eatRestOfToken()
        val hexStr = slice()

        if (HEX_INT_REGEX.matches(hexStr)) {
            hexStr.drop(2).toIntOrNull(16)?.let { value ->
                return Token.Int32(col, markedLen(), value)
            }
        }

        return Token.InvalidNumber(col, markedLen())
    }

    private fun tokenizeFloat(col: Int): Token {
        eatRestOfToken()
        val floatStr = slice()

        if (FLOAT_REGEX.matches(floatStr)) {
            floatStr.toFloatOrNull()?.let { value ->
                return Token.Float32(col, markedLen(), value)
            }
        }

        return Token.InvalidNumber(col, markedLen())
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

            Token.Register(col, markedLen() + 1, value)
        } else {
            back()
            tokenizeIdent()
        }
    }

    private fun tokenizeSection(): Token {
        val col = this.col
        mark()

        while (hasNext()) {
            if (peek().fastIsWhitespace()) {
                break
            } else {
                skip()
            }
        }

        return when (slice()) {
            ".code" -> Token.Section.Code(col, 5)
            ".data" -> Token.Section.Data(col, 5)
            ".string" -> Token.Section.Str(col, 7)
            else -> Token.InvalidSection(col, markedLen())
        }
    }

    private fun tokenizeString(): Token {
        val col = this.col
        skip()
        mark()
        var prevWasBackSpace = false
        var terminated = false

        loop@ // Use label as workaround for https://youtrack.jetbrains.com/issue/KT-43943.
        while (hasNext()) {
            when (peek()) {
                '\\' -> {
                    prevWasBackSpace = true
                }
                '"' -> {
                    if (!prevWasBackSpace) {
                        terminated = true
                        break@loop
                    }

                    prevWasBackSpace = false
                }
                else -> {
                    prevWasBackSpace = false
                }
            }

            next()
        }

        val lenWithoutQuotes = markedLen()
        val value = slice().replace("\\\"", "\"").replace("\\n", "\n")

        return if (terminated) {
            next()
            Token.Str(col, lenWithoutQuotes + 2, value)
        } else {
            Token.UnterminatedString(col, lenWithoutQuotes + 1, value)
        }
    }

    private fun tokenizeIdent(): Token {
        val col = this.col
        mark()

        while (hasNext()) {
            val char = peek()

            if (char == ',' || char.fastIsWhitespace()) {
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
            Token.Ident(col, markedLen(), value)
        } else {
            Token.InvalidIdent(col, markedLen())
        }
    }
}

package world.phantasmal.lib.asm

import world.phantasmal.core.fastIsWhitespace
import world.phantasmal.core.fastReplace
import world.phantasmal.core.getCodePointAt
import world.phantasmal.core.isDigit

private val HEX_INT_REGEX = Regex("""^0[xX][0-9a-fA-F]+$""")
private val FLOAT_REGEX = Regex("""^-?\d+(\.\d+)?(e-?\d+)?$""")

enum class Token {
    Int32,
    Float32,
    InvalidNumber,
    Register,
    Label,
    CodeSection,
    DataSection,
    StrSection,
    InvalidSection,
    Str,
    UnterminatedStr,
    Ident,
    InvalidIdent,
    ArgSeparator,
}

class LineTokenizer {
    private var line = ""
    private var index = 0
    private var startIndex = 0

    private var value: Any? = null

    var type: Token? = null
        private set

    val col: Int get() = startIndex + 1
    val len: Int get() = index - startIndex

    fun tokenize(line: String) {
        this.line = line
        index = 0
        startIndex = 0
    }

    val intValue: Int
        get() {
            require(type === Token.Int32 || type === Token.Register || type === Token.Label)
            return value as Int
        }

    val floatValue: Float
        get() {
            require(type === Token.Float32)
            return value as Float
        }

    val strValue: String
        get() {
            require(
                type === Token.Str ||
                        type === Token.UnterminatedStr ||
                        type === Token.Ident ||
                        type === Token.InvalidIdent
            )
            return value as String
        }

    fun nextToken(): Boolean {
        type = null
        value = null

        while (hasNext()) {
            startIndex = index
            val char = peek()

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
            }

            if (char == '-' || char.isDigit()) {
                tokenizeNumberOrLabel()
            } else if (char == ',') {
                type = Token.ArgSeparator
                skip()
            } else if (char == '.') {
                tokenizeSection()
            } else if (char == '"') {
                tokenizeString()
            } else if (char == 'r') {
                tokenizeRegisterOrIdent()
            } else {
                tokenizeIdent()
            }

            break
        }

        return type != null
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

    private fun slice(from: Int = 0, to: Int = 0): String =
        line.substring(startIndex + from, index - to)

    private fun eatRestOfToken() {
        while (hasNext()) {
            val char = next()

            if (char == ',' || char.fastIsWhitespace()) {
                back()
                break
            }
        }
    }

    private fun tokenizeNumberOrLabel() {
        val firstChar = next()
        var isLabel = false

        while (hasNext()) {
            val char = peek()

            if (char == '.' || char == 'e') {
                tokenizeFloat()
                return
            } else if (firstChar == '0' && (char == 'x' || char == 'X')) {
                tokenizeHexNumber()
                return
            } else if (char == ':') {
                isLabel = true
                break
            } else if (char == ',' || char.fastIsWhitespace()) {
                break
            } else {
                skip()
            }
        }

        value = slice().toIntOrNull()

        if (isLabel) {
            skip()
        }

        type = when {
            value == null -> Token.InvalidNumber
            isLabel -> Token.Label
            else -> Token.Int32
        }
    }

    private fun tokenizeHexNumber() {
        eatRestOfToken()
        val hexStr = slice()

        if (HEX_INT_REGEX.matches(hexStr)) {
            value = hexStr.drop(2).toIntOrNull(16)

            if (value != null) {
                type = Token.Int32
                return
            }
        }

        type = Token.InvalidNumber
    }

    private fun tokenizeFloat() {
        eatRestOfToken()
        val floatStr = slice()

        if (FLOAT_REGEX.matches(floatStr)) {
            value = floatStr.toFloatOrNull()

            if (value != null) {
                type = Token.Float32
                return
            }
        }

        type = Token.InvalidNumber
    }

    private fun tokenizeRegisterOrIdent() {
        skip()
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

        if (isRegister) {
            value = slice(from = 1).toInt()
            type = Token.Register
        } else {
            back()
            tokenizeIdent()
        }
    }

    private fun tokenizeSection() {
        while (hasNext()) {
            if (peek().fastIsWhitespace()) {
                break
            } else {
                skip()
            }
        }

        type = when (slice()) {
            ".code" -> Token.CodeSection
            ".data" -> Token.DataSection
            ".string" -> Token.StrSection
            else -> Token.InvalidSection
        }
    }

    private fun tokenizeString() {
        skip()
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
                        skip()
                        terminated = true
                        break@loop
                    }

                    prevWasBackSpace = false
                }
                else -> {
                    prevWasBackSpace = false
                }
            }

            skip()
        }

        value = slice(from = 1, to = if (terminated) 1 else 0)
            .fastReplace("\\\"", "\"")
            .fastReplace("\\n", "\n")

        type = if (terminated) {
            Token.Str
        } else {
            Token.UnterminatedStr
        }
    }

    private fun tokenizeIdent() {
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

        val ident = slice()
        value = ident

        if (ident.getCodePointAt(0) !in ('a'.toInt())..('z'.toInt())) {
            type = Token.InvalidIdent
            return
        }

        for (i in 1 until ident.length) {
            when (ident.getCodePointAt(i)) {
                in ('0'.toInt())..('9'.toInt()),
                in ('a'.toInt())..('z'.toInt()),
                ('_').toInt(),
                ('=').toInt(),
                ('<').toInt(),
                ('>').toInt(),
                ('!').toInt(),
                -> {
                    // Valid character.
                }
                else -> {
                    type = Token.InvalidIdent
                    return
                }
            }
        }

        type = Token.Ident
    }
}

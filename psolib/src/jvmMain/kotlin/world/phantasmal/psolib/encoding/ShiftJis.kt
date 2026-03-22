package world.phantasmal.psolib.encoding

import java.nio.charset.Charset

private val shiftJisCharset: Charset = Charset.forName("Shift_JIS")

actual fun decodeShiftJis(bytes: ByteArray): String =
    String(bytes, shiftJisCharset)

actual fun encodeShiftJis(str: String): ByteArray =
    str.toByteArray(shiftJisCharset)

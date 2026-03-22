package world.phantasmal.psolib.encoding

import org.khronos.webgl.Int8Array
import org.khronos.webgl.Uint8Array

private val decoder = js("new TextDecoder('shift-jis')")
private val encoder = js("new TextEncoder()")

actual fun decodeShiftJis(bytes: ByteArray): String {
    val uint8 = Uint8Array(bytes.unsafeCast<Int8Array>().buffer)
    return decoder.decode(uint8) as String
}

actual fun encodeShiftJis(str: String): ByteArray {
    // TextEncoder only supports UTF-8. For Shift-JIS encoding, we manually convert
    // Unicode code points to Shift-JIS bytes.
    val result = mutableListOf<Byte>()

    for (char in str) {
        val cp = char.code

        if (cp < 0x80) {
            // ASCII range
            result.add(cp.toByte())
        } else if (cp in 0xFF61..0xFF9F) {
            // Half-width katakana
            result.add((cp - 0xFF61 + 0xA1).toByte())
        } else {
            // For non-ASCII, use the decoder to do a reverse lookup via a small trick:
            // encode as UTF-8 first, then we need actual Shift-JIS mapping.
            // Fall back to '?' for unmappable characters.
            val sjis = unicodeToShiftJis(cp)
            if (sjis != null) {
                if (sjis > 0xFF) {
                    result.add((sjis shr 8).toByte())
                    result.add((sjis and 0xFF).toByte())
                } else {
                    result.add(sjis.toByte())
                }
            } else {
                result.add('?'.code.toByte())
            }
        }
    }

    return result.toByteArray()
}

// Build reverse lookup table from Shift-JIS → Unicode using TextDecoder.
private val unicodeToSjisMap: Map<Int, Int> by lazy {
    val map = mutableMapOf<Int, Int>()

    // Scan all valid Shift-JIS lead byte ranges
    for (lead in 0x81..0x9F) {
        addShiftJisMappings(map, lead)
    }
    for (lead in 0xE0..0xEF) {
        addShiftJisMappings(map, lead)
    }

    map
}

private fun addShiftJisMappings(map: MutableMap<Int, Int>, lead: Int) {
    for (trail in 0x40..0xFC) {
        if (trail == 0x7F) continue
        val bytes = byteArrayOf(lead.toByte(), trail.toByte())
        val decoded = decodeShiftJis(bytes)
        if (decoded.length == 1 && decoded[0].code != 0xFFFD) {
            map[decoded[0].code] = (lead shl 8) or trail
        }
    }
}

private fun unicodeToShiftJis(codePoint: Int): Int? = unicodeToSjisMap[codePoint]

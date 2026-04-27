package world.phantasmal.psolib.encoding

/**
 * Decodes a Shift-JIS encoded byte array to a String.
 */
expect fun decodeShiftJis(bytes: ByteArray): String

/**
 * Encodes a String to Shift-JIS bytes.
 */
expect fun encodeShiftJis(str: String): ByteArray

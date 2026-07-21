package world.phantasmal.psolib.symbolchat

import world.phantasmal.psolib.buffer.Buffer

/**
 * Decoded `symbolchatcolli.prs` — the PSO client's built-in SymbolChat table
 * referenced by `Symbol Chat Object` (typeId 0x21) entries via the spec
 * `sc_index` low-16 field.
 *
 * The PRS-decompressed file is exactly 24 records × 104 bytes. Each record
 * is laid out as:
 *
 * ```
 * +0x00  4   valid_flag : u32   (always 1)
 * +0x04  40  unused             (0xCD MSVC heap-uninit pattern)
 * +0x2C  60  SymbolChatT (LE)   (the actual payload)
 * +0x68
 * ```
 *
 * The wrapping 44 bytes are an artifact of the original sega tool serializing
 * an over-sized struct without zeroing it; only the trailing 60 bytes are
 * meaningful and they are stored little-endian on every platform (the client
 * byteswaps as needed at load time on DC/GC).
 *
 * See `docs/symbol-chat-object.md` for the full analysis.
 */
class SymbolChatColliTable(private val buf: Buffer) {

    init {
        require(buf.size >= FILE_SIZE) {
            "symbolchatcolli buffer too small: ${buf.size} < $FILE_SIZE"
        }
    }

    /**
     * Returns the 60-byte SymbolChatT slice for [id], or `null` if [id] is out
     * of range. Out-of-range IDs in the wild (e.g. 1c2_e.qst spec3 sentinels
     * with `sc=30`) should be treated as "no symbol chat displayed", not as
     * an error.
     */
    fun entry(id: Int): Buffer? {
        if (id !in 0 until ENTRY_COUNT) return null
        return buf.slice(id * RECORD_SIZE + DATA_OFFSET, SYMBOL_CHAT_SIZE)
    }

    companion object {
        const val ENTRY_COUNT = 24
        const val RECORD_SIZE = 104
        const val DATA_OFFSET = 44
        const val SYMBOL_CHAT_SIZE = 60
        const val FILE_SIZE = ENTRY_COUNT * RECORD_SIZE  // 2496

        /**
         * Well-known out-of-range SC index used by existing quests (e.g. spec3
         * of `1c2_e.qst`) to mean "display nothing at this stage". Any value
         * >= [ENTRY_COUNT] works semantically (the client's reverse-spec
         * evaluation short-circuits once it reaches a triggered stage), but
         * qedit and vanilla quests consistently emit 30, so new edits should
         * match the convention for round-trip friendliness.
         */
        const val HIDE_SENTINEL_ID = 30
    }
}

package world.phantasmal.lib.cursor

/**
 * A cursor for reading and writing binary data.
 */
interface WritableCursor : Cursor {
    override var size: Int

    override fun seek(offset: Int): WritableCursor

    override fun seekStart(offset: Int): WritableCursor

    override fun seekEnd(offset: Int): WritableCursor

    /**
     * Writes an unsigned 8-bit integer and increments position by 1.
     */
    fun writeU8(value: UByte): WritableCursor

    /**
     * Writes an unsigned 16-bit integer and increments position by 2.
     */
    fun writeU16(value: UShort): WritableCursor

    /**
     * Writes an unsigned 32-bit integer and increments position by 4.
     */
    fun writeU32(value: UInt): WritableCursor

    /**
     * Writes a signed 8-bit integer and increments position by 1.
     */
    fun writeI8(value: Byte): WritableCursor

    /**
     * Writes a signed 16-bit integer and increments position by 2.
     */
    fun writeI16(value: Short): WritableCursor

    /**
     * Writes a signed 32-bit integer and increments position by 4.
     */
    fun writeI32(value: Int): WritableCursor

    /**
     * Writes a 32-bit floating point number and increments position by 4.
     */
    fun writeF32(value: Float): WritableCursor

    /**
     * Writes an array of unsigned 8-bit integers and increments position by the array's length.
     */
    fun writeU8Array(array: UByteArray): WritableCursor

    /**
     * Writes an array of unsigned 16-bit integers and increments position by twice the array's
     * length.
     */
    fun writeU16Array(array: UShortArray): WritableCursor

    /**
     * Writes an array of unsigned 32-bit integers and increments position by four times the array's
     * length.
     */
    fun writeU32Array(array: UIntArray): WritableCursor

    /**
     * Writes an array of signed 32-bit integers and increments position by four times the array's
     * length.
     */
    fun writeI32Array(array: IntArray): WritableCursor

    /**
     * Writes the contents of the given cursor from its position to its end. Increments this
     * cursor's and the given cursor's position by the size of the given cursor.
     */
    fun writeCursor(other: Cursor): WritableCursor

    /**
     * Writes [byteLength] characters of [str]. If [str] is shorter than [byteLength], nul bytes
     * will be inserted until [byteLength] bytes have been written.
     */
    fun writeStringAscii(str: String, byteLength: Int): WritableCursor

    /**
     * Writes characters of [str] without writing more than [byteLength] bytes. If less than
     * [byteLength] bytes can be written this way, nul bytes will be inserted until [byteLength]
     * bytes have been written.
     */
    fun writeStringUtf16(str: String, byteLength: Int): WritableCursor
}

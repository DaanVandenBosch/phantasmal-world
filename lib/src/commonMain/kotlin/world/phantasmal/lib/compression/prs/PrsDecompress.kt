package world.phantasmal.lib.compression.prs

import mu.KotlinLogging
import world.phantasmal.core.PwResult
import world.phantasmal.core.PwResultBuilder
import world.phantasmal.core.Severity
import world.phantasmal.core.Success
import world.phantasmal.lib.buffer.Buffer
import world.phantasmal.lib.cursor.Cursor
import world.phantasmal.lib.cursor.WritableCursor
import world.phantasmal.lib.cursor.cursor
import kotlin.math.floor
import kotlin.math.min

private val logger = KotlinLogging.logger {}

fun prsDecompress(cursor: Cursor): PwResult<Cursor> {
    try {
        val decompressor = PrsDecompressor(cursor)
        var i = 0

        while (true) {
            if (decompressor.readFlagBit() == 1) {
                // Single byte copy.
                decompressor.copyU8()
            } else {
                // Multi byte copy.
                var length: Int
                var offset: Int

                if (decompressor.readFlagBit() == 0) {
                    // Short copy.
                    length = (decompressor.readFlagBit() shl 1) or decompressor.readFlagBit()
                    length += 2

                    offset = decompressor.readU8().toInt() - 256
                } else {
                    // Long copy or end of file.
                    offset = decompressor.readU16().toInt()

                    // Two zero bytes implies that this is the end of the file.
                    if (offset == 0) {
                        break
                    }

                    // Do we need to read a length byte, or is it encoded in what we already have?
                    length = offset and 0b111
                    offset = offset ushr 3

                    if (length == 0) {
                        length = decompressor.readU8().toInt()
                        length += 1
                    } else {
                        length += 2
                    }

                    offset -= 8192
                }

                decompressor.offsetCopy(offset, length)
            }

            i++
        }

        return Success(decompressor.dst.seekStart(0))
    } catch (e: Throwable) {
        return PwResultBuilder<Cursor>(logger)
            .addProblem(Severity.Error, "PRS-compressed stream is corrupt.", cause = e)
            .failure()
    }
}

private class PrsDecompressor(cursor: Cursor) {
    private val src: Cursor = cursor
    val dst: WritableCursor =
        Buffer.withCapacity(floor(1.5 * cursor.size.toDouble()).toInt(), cursor.endianness).cursor()
    private var flags = 0
    private var flagBitsLeft = 0

    fun readFlagBit(): Int {
        // Fetch a new flag byte when the previous byte has been processed.
        if (flagBitsLeft == 0) {
            flags = readU8().toInt()
            flagBitsLeft = 8
        }

        val bit = flags and 1
        flags = flags ushr 1
        flagBitsLeft -= 1
        return bit
    }

    fun copyU8() {
        dst.writeU8(readU8())
    }

    fun readU8(): UByte = src.u8()

    fun readU16(): UShort = src.u16()

    fun offsetCopy(offset: Int, length: Int) {
        require(offset in -8192..0) {
            "offset was ${offset}, should be between -8192 and 0."
        }

        require(length in 1..256) {
            "length was ${length}, should be between 1 and 256."
        }

        // The length can be larger than -offset, in that case we copy -offset bytes size/-offset
        // times.
        val bufSize = min(-offset, length)

        dst.seek(offset)
        val buf = dst.take(bufSize)
        dst.seek(-offset - bufSize)

        repeat(length / bufSize) {
            dst.writeCursor(buf)
            buf.seekStart(0)
        }

        dst.writeCursor(buf.take(length % bufSize))
    }
}

package world.phantasmal.lib.compression.prs

import mu.KotlinLogging
import world.phantasmal.core.PwResult
import world.phantasmal.core.Severity
import world.phantasmal.core.Success
import world.phantasmal.lib.buffer.Buffer
import world.phantasmal.lib.cursor.Cursor
import world.phantasmal.lib.cursor.WritableCursor
import world.phantasmal.lib.cursor.cursor
import kotlin.math.min

private val logger = KotlinLogging.logger {}

// This code uses signed types for better KJS performance. In KJS unsigned types are always boxed.

fun prsDecompress(cursor: Cursor): PwResult<Cursor> =
    PrsDecompressor(cursor).decompress()

private class PrsDecompressor(private val src: Cursor) {
    private val dst: WritableCursor =
        Buffer.withCapacity(6 * src.size, src.endianness).cursor()
    private var flags = 0
    private var flagBitsLeft = 0

    fun decompress(): PwResult<Cursor> {
        try {
            while (true) {
                if (readFlagBit() == 1) {
                    // Single byte copy.
                    copyByte()
                } else {
                    // Multi byte copy.
                    if (readFlagBit() == 0) {
                        // Short copy.
                        val size = 2 + ((readFlagBit() shl 1) or readFlagBit())
                        val offset = readUByte() - 256

                        offsetCopy(offset, size)
                    } else {
                        // Long copy or end of file.
                        var offset = readUShort()

                        // Two zero bytes implies that this is the end of the file.
                        if (offset == 0) {
                            break
                        }

                        // Do we need to read a size byte, or is it encoded in what we already have?
                        var size = offset and 0b111
                        offset = offset ushr 3

                        if (size == 0) {
                            size = readUByte()
                            size += 1
                        } else {
                            size += 2
                        }

                        offset -= 8192

                        offsetCopy(offset, size)
                    }
                }
            }

            return Success(dst.seekStart(0))
        } catch (e: Throwable) {
            return PwResult.build<Cursor>(logger)
                .addProblem(Severity.Error, "PRS-compressed stream is corrupt.", cause = e)
                .failure()
        }
    }

    private fun readFlagBit(): Int {
        // Fetch a new flag byte when the previous byte has been processed.
        if (flagBitsLeft == 0) {
            flags = readUByte()
            flagBitsLeft = 8
        }

        val bit = flags and 1
        flags = flags ushr 1
        flagBitsLeft -= 1
        return bit
    }

    private fun copyByte() {
        dst.writeByte(src.byte())
    }

    private fun readUByte(): Int = src.byte().toInt() and 0xFF

    private fun readUShort(): Int = src.short().toInt() and 0xFFFF

    private fun offsetCopy(offset: Int, size: Int) {
        require(offset in -8192..0) {
            "offset was ${offset}, should be between -8192 and 0."
        }

        require(size in 1..256) {
            "size was ${size}, should be between 1 and 256."
        }

        // Size can be larger than -offset, in that case we copy -offset bytes size/-offset times.
        val bufSize = min(-offset, size)

        dst.seek(offset)
        val buf = dst.take(bufSize)
        dst.seek(-offset - bufSize)

        repeat(size / bufSize) {
            dst.writeCursor(buf)
            buf.seekStart(0)
        }

        dst.writeCursor(buf.take(size % bufSize))
    }
}

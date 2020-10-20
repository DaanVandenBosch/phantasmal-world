package world.phantasmal.lib.compression.prs

import mu.KotlinLogging
import world.phantasmal.core.PwResult
import world.phantasmal.core.PwResultBuilder
import world.phantasmal.core.Severity
import world.phantasmal.core.Success
import world.phantasmal.lib.buffer.Buffer
import world.phantasmal.lib.cursor.BufferCursor
import world.phantasmal.lib.cursor.Cursor
import world.phantasmal.lib.cursor.WritableCursor
import kotlin.math.floor
import kotlin.math.min

private val logger = KotlinLogging.logger {}

fun prsDecompress(cursor: Cursor): PwResult<Cursor> {
    try {
        val ctx = Context(cursor)

        while (true) {
            if (ctx.readFlagBit() == 1u) {
                // Single byte copy.
                ctx.copyU8()
            } else {
                // Multi byte copy.
                var length: UInt
                var offset: Int

                if (ctx.readFlagBit() == 0u) {
                    // Short copy.
                    length = (ctx.readFlagBit() shl 1) or ctx.readFlagBit()
                    length += 2u

                    offset = ctx.readU8().toInt() - 256
                } else {
                    // Long copy or end of file.
                    offset = ctx.readU16().toInt()

                    // Two zero bytes implies that this is the end of the file.
                    if (offset == 0) {
                        break
                    }

                    // Do we need to read a length byte, or is it encoded in what we already have?
                    length = (offset and 0b111).toUInt()
                    offset = offset shr 3

                    if (length == 0u) {
                        length = ctx.readU8().toUInt()
                        length += 1u
                    } else {
                        length += 2u
                    }

                    offset -= 8192
                }

                ctx.offsetCopy(offset, length)
            }
        }

        return Success(ctx.dst.seekStart(0u))
    } catch (e: Throwable) {
        return PwResultBuilder<Cursor>(logger)
            .addProblem(Severity.Error, "PRS-compressed stream is corrupt.", cause = e)
            .failure()
    }
}

class Context(cursor: Cursor) {
    private val src: Cursor = cursor
    val dst: WritableCursor = BufferCursor(
        Buffer.withCapacity(floor(1.5 * cursor.size.toDouble()).toUInt(), cursor.endianness),
    )
    private var flags = 0u
    private var flagBitsLeft = 0

    fun readFlagBit(): UInt {
        // Fetch a new flag byte when the previous byte has been processed.
        if (flagBitsLeft == 0) {
            flags = readU8().toUInt()
            flagBitsLeft = 8
        }

        val bit = flags and 1u
        flags = flags shr 1
        flagBitsLeft -= 1
        return bit
    }

    fun copyU8() {
        dst.writeU8(readU8())
    }

    fun readU8(): UByte = src.u8()

    fun readU16(): UShort = src.u16()

    fun offsetCopy(offset: Int, length: UInt) {
        require(offset in -8192..0) {
            "offset was ${offset}, should be between -8192 and 0."
        }

        require(length in 1u..256u) {
            "length was ${length}, should be between 1 and 256."
        }

        // The length can be larger than -offset, in that case we copy -offset bytes size/-offset times.
        val bufSize = min((-offset).toUInt(), length)

        dst.seek(offset)
        val buf = dst.take(bufSize)
        dst.seek(-offset - bufSize.toInt())

        repeat((length / bufSize).toInt()) {
            dst.writeCursor(buf)
            buf.seekStart(0u)
        }

        dst.writeCursor(buf.take(length % bufSize))
    }
}

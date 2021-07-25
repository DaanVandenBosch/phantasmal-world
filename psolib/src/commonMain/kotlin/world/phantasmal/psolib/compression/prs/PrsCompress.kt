package world.phantasmal.psolib.compression.prs

import world.phantasmal.psolib.buffer.Buffer
import world.phantasmal.psolib.cursor.Cursor
import world.phantasmal.psolib.cursor.WritableCursor
import world.phantasmal.psolib.cursor.cursor
import kotlin.math.max
import kotlin.math.min

// This code uses signed types for better KJS performance. In KJS unsigned types are always boxed.

fun prsCompress(cursor: Cursor): Cursor =
    PrsCompressor(cursor).compress()

private class PrsCompressor(private val src: Cursor) {
    private val dst: WritableCursor = Buffer.withCapacity(src.size, src.endianness).cursor()
    private var flags = 0
    private var flagBitsLeft = 0
    private var flagOffset = 0

    fun compress(): Cursor {
        val cmp = src.take(src.size)
        src.seekStart(0)

        while (src.hasBytesLeft()) {
            // Find the longest match.
            var bestOffset = 0
            var bestSize = 0
            val startPos = src.position
            val minOffset = max(0, startPos - min(0x800, src.bytesLeft))

            for (i in startPos - 255 downTo minOffset) {
                cmp.seekStart(i)
                var size = 0

                while (src.hasBytesLeft() &&
                    size < 255 &&
                    src.byte() == cmp.byte()
                ) {
                    size++
                }

                src.seekStart(startPos)

                if (size >= bestSize) {
                    bestOffset = i
                    bestSize = size

                    if (size >= 255) {
                        break
                    }
                }
            }

            if (bestSize < 3) {
                addByte(src.byte())
            } else {
                copy(bestOffset - src.position, bestSize)
                src.seek(bestSize)
            }
        }

        return finalize()
    }

    private fun finalize(): Cursor {
        writeControlBit(0)
        writeControlBit(1)

        flags = flags ushr flagBitsLeft
        val pos = dst.position
        dst.seekStart(flagOffset).writeByte(flags.toByte()).seekStart(pos)

        writeByte(0)
        writeByte(0)
        return dst.seekStart(0)
    }

    private fun writeControlBit(bit: Int) {
        if (flagBitsLeft == 0) {
            // Write out the flags to their position in the file, and store the next flags byte
            // position.
            val pos = dst.position
            dst.seekStart(flagOffset)
            dst.writeByte(flags.toByte())
            dst.seekStart(pos)
            dst.writeUByte(0u) // Placeholder for the next flags byte.
            flagOffset = pos
            flagBitsLeft = 8
        }

        flags = flags ushr 1

        if (bit != 0) {
            flags = flags or 0x80
        }

        flagBitsLeft--
    }

    private fun addByte(value: Byte) {
        writeControlBit(1)
        dst.writeByte(value)
    }

    private fun copy(offset: Int, size: Int) {
        if (offset > -256 && size <= 5) {
            shortCopy(offset, size)
        } else {
            longCopy(offset, size)
        }
    }

    private fun shortCopy(offset: Int, size: Int) {
        val s = size - 2
        writeControlBit(0)
        writeControlBit(0)
        writeControlBit(((s ushr 1) and 1))
        writeControlBit((s and 1))
        writeByte(offset)
    }

    private fun longCopy(offset: Int, size: Int) {
        writeControlBit(0)
        writeControlBit(1)

        if (size <= 9) {
            writeByte(((offset shl 3) and 0xF8) or ((size - 2) and 0b111))
            writeByte((offset ushr 5))
        } else {
            writeByte((offset shl 3) and 0xF8)
            writeByte((offset ushr 5))
            writeByte(size - 1)
        }
    }

    private fun writeByte(data: Int) {
        dst.writeByte(data.toByte())
    }
}

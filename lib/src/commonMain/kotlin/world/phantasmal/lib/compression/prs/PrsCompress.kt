package world.phantasmal.lib.compression.prs

import world.phantasmal.lib.Endianness
import world.phantasmal.lib.buffer.Buffer
import world.phantasmal.lib.cursor.Cursor
import world.phantasmal.lib.cursor.WritableCursor
import world.phantasmal.lib.cursor.cursor
import kotlin.math.max
import kotlin.math.min

fun prsCompress(cursor: Cursor): Cursor {
    val compressor = PrsCompressor(cursor.size, cursor.endianness)
    val comparisonCursor = cursor.take(cursor.size)
    cursor.seekStart(0)

    while (cursor.hasBytesLeft()) {
        // Find the longest match.
        var bestOffset = 0
        var bestSize = 0
        val startPos = cursor.position
        val minOffset = max(0, startPos - min(0x800, cursor.bytesLeft))

        for (i in startPos - 255 downTo minOffset) {
            comparisonCursor.seekStart(i)
            var size = 0

            while (cursor.hasBytesLeft() && size <= 254 && cursor.u8() == comparisonCursor.u8()) {
                size++
            }

            cursor.seekStart(startPos)

            if (size >= bestSize) {
                bestOffset = i
                bestSize = size

                if (size >= 255) {
                    break
                }
            }
        }

        if (bestSize < 3) {
            compressor.addU8(cursor.u8())
        } else {
            compressor.copy(bestOffset - cursor.position, bestSize)
            cursor.seek(bestSize)
        }
    }

    return compressor.finalize()
}

private class PrsCompressor(capacity: Int, endianness: Endianness) {
    private val output: WritableCursor = Buffer.withCapacity(capacity, endianness).cursor()
    private var flags = 0
    private var flagBitsLeft = 0
    private var flagOffset = 0

    fun addU8(value: UByte) {
        writeControlBit(1)
        writeU8(value)
    }

    fun copy(offset: Int, size: Int) {
        if (offset > -256 && size <= 5) {
            shortCopy(offset, size)
        } else {
            longCopy(offset, size)
        }
    }

    fun finalize(): Cursor {
        writeControlBit(0)
        writeControlBit(1)

        flags = flags ushr flagBitsLeft
        val pos = output.position
        output.seekStart(flagOffset).writeU8(flags.toUByte()).seekStart(pos)

        writeU8(0u)
        writeU8(0u)
        return output.seekStart(0)
    }

    private fun writeControlBit(bit: Int) {
        if (flagBitsLeft == 0) {
            // Write out the flags to their position in the file, and store the next flags byte
            // position.
            val pos = output.position
            output.seekStart(flagOffset)
            output.writeU8(flags.toUByte())
            output.seekStart(pos)
            output.writeU8(0u) // Placeholder for the next flags byte.
            flagOffset = pos
            flagBitsLeft = 8
        }

        flags = flags ushr 1

        if (bit!=0) {
            flags = flags or 0x80
        }

        flagBitsLeft--
    }

    private fun writeU8(data: UByte) {
        output.writeU8(data)
    }

    private fun writeU8(data: Int) {
        output.writeU8(data.toUByte())
    }

    private fun shortCopy(offset: Int, size: Int) {
        val s = size - 2
        writeControlBit(0)
        writeControlBit(0)
        writeControlBit(((s ushr 1) and 1) )
        writeControlBit((s and 1))
        writeU8(offset and 0xFF)
    }

    private fun longCopy(offset: Int, size: Int) {
        writeControlBit(0)
        writeControlBit(1)

        if (size <= 9) {
            writeU8(((offset shl 3) and 0xF8) or ((size - 2) and 0x07))
            writeU8((offset ushr 5) and 0xFF)
        } else {
            writeU8((offset shl 3) and 0xF8)
            writeU8((offset ushr 5) and 0xFF)
            writeU8(size - 1)
        }
    }
}

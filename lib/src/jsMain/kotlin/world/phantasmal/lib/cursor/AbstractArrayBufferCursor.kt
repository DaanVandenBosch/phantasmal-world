package world.phantasmal.lib.cursor

import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.DataView

abstract class AbstractArrayBufferCursor
protected constructor(endianness: Endianness, offset: UInt) : AbstractWritableCursor(offset) {
    private var littleEndian: Boolean = endianness == Endianness.Little
    protected abstract val backingBuffer: ArrayBuffer
    protected abstract val dv: DataView

    override var endianness: Endianness
        get() = if (littleEndian) Endianness.Little else Endianness.Big
        set(value) {
            littleEndian = value == Endianness.Little
        }

    override fun u8(): UByte {
        requireSize(1u)
        val r = dv.getUint8(absolutePosition.toInt())
        position++
        return r.toUByte()
    }

    override fun u16(): UShort {
        requireSize(2u)
        val r = dv.getUint16(absolutePosition.toInt(), littleEndian)
        position += 2u
        return r.toUShort()
    }

    override fun u32(): UInt {
        requireSize(4u)
        val r = dv.getUint32(absolutePosition.toInt(), littleEndian)
        position += 4u
        return r.toUInt()
    }

    override fun i8(): Byte {
        requireSize(1u)
        val r = dv.getInt8(absolutePosition.toInt())
        position++
        return r
    }

    override fun i16(): Short {
        requireSize(2u)
        val r = dv.getInt16(absolutePosition.toInt(), littleEndian)
        position += 2u
        return r
    }

    override fun i32(): Int {
        requireSize(4u)
        val r = dv.getInt32(absolutePosition.toInt(), littleEndian)
        position += 4u
        return r
    }

    override fun f32(): Float {
        requireSize(4u)
        val r = dv.getFloat32(absolutePosition.toInt(), littleEndian)
        position += 4u
        return r
    }

    override fun u8Array(n: UInt): UByteArray {
        requireSize(n)

        val array = UByteArray(n.toInt())

        for (i in 0 until n.toInt()) {
            array[i] = dv.getUint8(absolutePosition.toInt()).toUByte()
            position++
        }

        return array
    }

    override fun u16Array(n: UInt): UShortArray {
        requireSize(2u * n)

        val array = UShortArray(n.toInt())

        for (i in 0 until n.toInt()) {
            array[i] = dv.getUint16(absolutePosition.toInt(), littleEndian).toUShort()
            position += 2u
        }

        return array
    }

    override fun u32Array(n: UInt): UIntArray {
        requireSize(4u * n)

        val array = UIntArray(n.toInt())

        for (i in 0 until n.toInt()) {
            array[i] = dv.getUint32(absolutePosition.toInt(), littleEndian).toUInt()
            position += 4u
        }

        return array
    }

    override fun i32Array(n: UInt): IntArray {
        requireSize(4u * n)

        val array = IntArray(n.toInt())

        for (i in 0 until n.toInt()) {
            array[i] = dv.getInt32(absolutePosition.toInt(), littleEndian)
            position += 4u
        }

        return array
    }

    override fun writeU8(value: UByte): WritableCursor {
        requireSize(1u)
        dv.setUint8(absolutePosition.toInt(), value.toByte())
        position++
        return this
    }

    override fun writeU16(value: UShort): WritableCursor {
        requireSize(2u)
        dv.setUint16(absolutePosition.toInt(), value.toShort(), littleEndian)
        position += 2u
        return this
    }

    override fun writeU32(value: UInt): WritableCursor {
        requireSize(4u)
        dv.setUint32(absolutePosition.toInt(), value.toInt(), littleEndian)
        position += 4u
        return this
    }

    override fun writeI8(value: Byte): WritableCursor {
        requireSize(1u)
        dv.setInt8(absolutePosition.toInt(), value)
        position++
        return this
    }

    override fun writeI16(value: Short): WritableCursor {
        requireSize(2u)
        dv.setInt16(absolutePosition.toInt(), value, littleEndian)
        position += 2u
        return this
    }

    override fun writeI32(value: Int): WritableCursor {
        requireSize(4u)
        dv.setInt32(absolutePosition.toInt(), value, littleEndian)
        position += 4u
        return this
    }

    override fun writeF32(value: Float): WritableCursor {
        requireSize(4u)
        dv.setFloat32(absolutePosition.toInt(), value, littleEndian)
        position += 4u
        return this
    }
}

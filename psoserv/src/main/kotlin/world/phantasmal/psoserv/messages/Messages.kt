package world.phantasmal.psoserv.messages

import world.phantasmal.psolib.buffer.Buffer

fun messageString(
    code: Int,
    size: Int,
    name: String? = null,
    vararg props: Pair<String, Any>,
): String =
    buildString {
        append(name ?: "Message")
        append("[0x")
        append(code.toString(16).uppercase().padStart(4, '0'))
        append(",size=")
        append(size)

        if (props.isNotEmpty()) {
            props.joinTo(this, prefix = ",", separator = ",") { (prop, value) -> "$prop=$value" }
        }

        append("]")
    }

data class Header(val code: Int, val size: Int)

interface Message {
    val buffer: Buffer
    val code: Int
    val size: Int
    val headerSize: Int
    val bodySize: Int get() = size - headerSize
}

interface MessageDescriptor<out MessageType : Message> {
    val headerSize: Int

    fun readHeader(buffer: Buffer): Header

    fun readMessage(buffer: Buffer): MessageType
}

interface InitEncryptionMessage : Message {
    val serverKey: ByteArray
    val clientKey: ByteArray
}

interface RedirectMessage : Message {
    var ipAddress: ByteArray
    var port: Int
}

abstract class AbstractMessage(override val headerSize: Int) : Message {
    override fun toString(): String = messageString()

    protected fun uByte(offset: Int) = buffer.getUByte(headerSize + offset)
    protected fun uShort(offset: Int) = buffer.getUShort(headerSize + offset)
    protected fun uInt(offset: Int) = buffer.getUInt(headerSize + offset)
    protected fun byte(offset: Int) = buffer.getByte(headerSize + offset)
    protected fun short(offset: Int) = buffer.getShort(headerSize + offset)
    protected fun int(offset: Int) = buffer.getInt(headerSize + offset)
    protected fun byteArray(offset: Int, size: Int) = ByteArray(size) { byte(offset + it) }
    protected fun stringAscii(offset: Int, maxByteLength: Int, nullTerminated: Boolean) =
        buffer.getStringAscii(headerSize + offset, maxByteLength, nullTerminated)

    protected fun setByte(offset: Int, value: Byte) {
        buffer.setByte(headerSize + offset, value)
    }

    protected fun setShort(offset: Int, value: Short) {
        buffer.setShort(headerSize + offset, value)
    }

    protected fun setByteArray(offset: Int, array: ByteArray) {
        for ((index, byte) in array.withIndex()) {
            setByte(offset + index, byte)
        }
    }

    protected fun messageString(vararg props: Pair<String, Any>): String =
        messageString(code, size, this::class.simpleName, *props)
}

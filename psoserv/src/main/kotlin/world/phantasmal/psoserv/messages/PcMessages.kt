package world.phantasmal.psoserv.messages

import world.phantasmal.psolib.Endianness
import world.phantasmal.psolib.buffer.Buffer
import world.phantasmal.psolib.cursor.WritableCursor
import world.phantasmal.psolib.cursor.cursor
import world.phantasmal.psoserv.roundToBlockSize

private const val INIT_MSG_SIZE: Int = 64
private const val KEY_SIZE: Int = 4

const val PC_HEADER_SIZE: Int = 4
const val PC_MSG_SIZE_POS: Int = 0
const val PC_MSG_CODE_POS: Int = 2

sealed class PcMessage(override val buffer: Buffer) : AbstractMessage(PC_HEADER_SIZE) {
    override val code: Int get() = buffer.getUByte(PC_MSG_CODE_POS).toInt()
    override val size: Int get() = buffer.getUShort(PC_MSG_SIZE_POS).toInt()

    class InitEncryption(buffer: Buffer) : PcMessage(buffer), InitEncryptionMessage {
        override val serverKey: ByteArray
            get() = byteArray(INIT_MSG_SIZE, size = KEY_SIZE)
        override val clientKey: ByteArray
            get() = byteArray(INIT_MSG_SIZE + KEY_SIZE, size = KEY_SIZE)

        constructor(message: String, serverKey: ByteArray, clientKey: ByteArray) : this(
            buf(0x02, INIT_MSG_SIZE + 2 * KEY_SIZE) {
                require(message.length <= INIT_MSG_SIZE)
                require(serverKey.size == KEY_SIZE)
                require(clientKey.size == KEY_SIZE)

                writeStringAscii(message, byteLength = INIT_MSG_SIZE)
                writeByteArray(serverKey)
                writeByteArray(clientKey)
            }
        )
    }

    class Login(buffer: Buffer) : PcMessage(buffer) {
        constructor() : this(buf(0x04))
    }

    class PatchListStart(buffer: Buffer) : PcMessage(buffer) {
        constructor() : this(buf(0x0B))
    }

    class PatchListEnd(buffer: Buffer) : PcMessage(buffer) {
        constructor() : this(buf(0x0D))
    }

    class PatchDone(buffer: Buffer) : PcMessage(buffer) {
        constructor() : this(buf(0x12))
    }

    class PatchListOk(buffer: Buffer) : PcMessage(buffer)

    class WelcomeMessage(buffer: Buffer) : PcMessage(buffer) {
        constructor(message: String) : this(
            buf(0x13, roundToBlockSize(2 * message.length, 4)) {
                writeStringUtf16(message, roundToBlockSize(2 * message.length, 4))
            }
        )
    }

    class Redirect(buffer: Buffer) : PcMessage(buffer), RedirectMessage {
        override var ipAddress: ByteArray
            get() = byteArray(0, size = 4)
            set(value) {
                require(value.size == 4)
                setByteArray(0, value)
            }
        override var port: Int
            get() {
                buffer.endianness = Endianness.Big
                val p = uShort(4).toInt()
                buffer.endianness = Endianness.Little
                return p
            }
            set(value) {
                require(value in 0..65535)
                buffer.endianness = Endianness.Big
                setShort(4, value.toShort())
                buffer.endianness = Endianness.Little
            }

        constructor(ipAddress: ByteArray, port: Int) : this(
            buf(0x14, 8) {
                require(ipAddress.size == 4)
                require(port in 0..65535)

                writeByteArray(ipAddress)
                endianness = Endianness.Big
                writeShort(port.toShort())
                endianness = Endianness.Little
                writeShort(0) // Padding.
            }
        )

        override fun toString(): String =
            messageString(
                "ipAddress" to ipAddress.joinToString(".") { it.toUByte().toString() },
                "port" to port,
            )
    }

    class Unknown(buffer: Buffer) : PcMessage(buffer)

    companion object {
        fun readHeader(buffer: Buffer, offset: Int = 0): Header {
            val size = buffer.getUShort(offset + PC_MSG_SIZE_POS).toInt()
            val code = buffer.getUByte(offset + PC_MSG_CODE_POS).toInt()
            // Ignore flag byte at position 3.
            return Header(code, size)
        }

        fun fromBuffer(buffer: Buffer): PcMessage =
            when (buffer.getUByte(PC_MSG_CODE_POS).toInt()) {
                0x02 -> InitEncryption(buffer)
                0x04 -> Login(buffer)
                0x0B -> PatchListStart(buffer)
                0x0D -> PatchListEnd(buffer)
                0x10 -> PatchListOk(buffer)
                0x12 -> PatchDone(buffer)
                0x13 -> WelcomeMessage(buffer)
                0x14 -> Redirect(buffer)
                else -> Unknown(buffer)
            }

        protected fun buf(
            code: Int,
            bodySize: Int = 0,
            writeBody: WritableCursor.() -> Unit = {},
        ): Buffer {
            val size = PC_HEADER_SIZE + bodySize
            val buffer = Buffer.withSize(size)

            val cursor = buffer.cursor()
                // Write Header
                .writeShort(size.toShort())
                .writeByte(code.toByte())
                .writeByte(0) // Flags

            cursor.writeBody()

            require(cursor.position == buffer.size) {
                "Message buffer should be filled completely, only ${cursor.position} / ${buffer.size} bytes written."
            }

            return buffer
        }
    }
}

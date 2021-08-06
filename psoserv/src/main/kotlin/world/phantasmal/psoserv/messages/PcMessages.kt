package world.phantasmal.psoserv.messages

import world.phantasmal.psolib.Endianness
import world.phantasmal.psolib.buffer.Buffer
import world.phantasmal.psolib.cursor.WritableCursor
import world.phantasmal.psolib.cursor.cursor
import world.phantasmal.psoserv.alignToWidth

private const val INIT_MSG_SIZE: Int = 64
private const val KEY_SIZE: Int = 4

const val PC_HEADER_SIZE: Int = 4
const val PC_MSG_SIZE_POS: Int = 0
const val PC_MSG_CODE_POS: Int = 2
const val PC_MSG_FLAGS_POS: Int = 3
const val PC_MSG_BODY_POS: Int = 4

object PcMessageDescriptor : MessageDescriptor<PcMessage> {
    override val headerSize: Int = PC_HEADER_SIZE

    override fun readHeader(buffer: Buffer): Header {
        val size = buffer.getUShort(PC_MSG_SIZE_POS).toInt()
        val code = buffer.getUByte(PC_MSG_CODE_POS).toInt()
        val flags = buffer.getUByte(PC_MSG_FLAGS_POS).toInt()
        return Header(code, size, flags)
    }

    override fun readMessage(buffer: Buffer): PcMessage =
        when (buffer.getUByte(PC_MSG_CODE_POS).toInt()) {
            0x02 -> PcMessage.InitEncryption(buffer)
            0x04 -> PcMessage.Login(buffer)
            0x0B -> PcMessage.PatchListStart(buffer)
            0x0D -> PcMessage.PatchListEnd(buffer)
            0x10 -> PcMessage.PatchListOk(buffer)
            0x12 -> PcMessage.PatchDone(buffer)
            0x13 -> PcMessage.WelcomeMessage(buffer)
            0x14 -> PcMessage.Redirect(buffer)
            else -> PcMessage.Unknown(buffer)
        }
}

sealed class PcMessage(override val buffer: Buffer) : AbstractMessage(PC_HEADER_SIZE) {
    override val code: Int get() = buffer.getUByte(PC_MSG_CODE_POS).toInt()
    override val size: Int get() = buffer.getUShort(PC_MSG_SIZE_POS).toInt()
    override val flags: Int get() = buffer.getUByte(PC_MSG_FLAGS_POS).toInt()

    // 0x02
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

    // 0x04
    class Login(buffer: Buffer) : PcMessage(buffer) {
        constructor() : this(buf(0x04))
    }

    // 0x0B
    class PatchListStart(buffer: Buffer) : PcMessage(buffer) {
        constructor() : this(buf(0x0B))
    }

    // 0x0D
    class PatchListEnd(buffer: Buffer) : PcMessage(buffer) {
        constructor() : this(buf(0x0D))
    }

    // 0x10
    class PatchListOk(buffer: Buffer) : PcMessage(buffer)

    // 0x12
    class PatchDone(buffer: Buffer) : PcMessage(buffer) {
        constructor() : this(buf(0x12))
    }

    // 0x13
    class WelcomeMessage(buffer: Buffer) : PcMessage(buffer) {
        constructor(message: String) : this(
            buf(0x13, alignToWidth(2 * message.length, 4)) {
                writeStringUtf16(message, alignToWidth(2 * message.length, 4))
            }
        )
    }

    // 0x14
    class Redirect(buffer: Buffer) : PcMessage(buffer), RedirectMessage {
        override var ipAddress: ByteArray
            get() = byteArray(0, size = 4)
            set(value) {
                require(value.size == 4)
                setByteArray(0, value)
            }
        override var port: UShort
            get() {
                buffer.endianness = Endianness.Big
                val p = uShort(4)
                buffer.endianness = Endianness.Little
                return p
            }
            set(value) {
                buffer.endianness = Endianness.Big
                setShort(4, value.toShort())
                buffer.endianness = Endianness.Little
            }

        constructor(ipAddress: ByteArray, port: UShort) : this(
            buf(0x14, 8) {
                require(ipAddress.size == 4)

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
        private fun buf(
            code: Int,
            bodySize: Int = 0,
            writeBody: (WritableCursor.() -> Unit)? = null,
        ): Buffer {
            val size = PC_HEADER_SIZE + bodySize
            val buffer = Buffer.withSize(size)
                // Write header.
                .setShort(PC_MSG_SIZE_POS, size.toShort())
                .setByte(PC_MSG_CODE_POS, code.toByte())
                .setByte(PC_MSG_FLAGS_POS, 0) // Flags

            if (writeBody != null) {
                val cursor = buffer.cursor(PC_MSG_BODY_POS)
                cursor.writeBody()

                require(cursor.position == bodySize) {
                    "Message buffer should be filled completely, only ${cursor.position} / $bodySize bytes written."
                }
            }

            return buffer
        }
    }
}

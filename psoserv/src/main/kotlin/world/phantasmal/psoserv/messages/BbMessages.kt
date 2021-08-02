package world.phantasmal.psoserv.messages

import world.phantasmal.psolib.buffer.Buffer
import world.phantasmal.psolib.cursor.Cursor
import world.phantasmal.psolib.cursor.WritableCursor
import world.phantasmal.psolib.cursor.cursor

private const val INIT_MSG_SIZE: Int = 96
private const val KEY_SIZE: Int = 48

const val BB_HEADER_SIZE: Int = 8
const val BB_MSG_SIZE_POS: Int = 0
const val BB_MSG_CODE_POS: Int = 2

object BbMessageDescriptor : MessageDescriptor<BbMessage> {
    override val headerSize: Int = BB_HEADER_SIZE

    override fun readHeader(buffer: Buffer): Header {
        val size = buffer.getUShort(BB_MSG_SIZE_POS).toInt()
        val code = buffer.getUShort(BB_MSG_CODE_POS).toInt()
        // Ignore 4 flag bytes.
        return Header(code, size)
    }

    override fun readMessage(buffer: Buffer): BbMessage =
        when (buffer.getUShort(BB_MSG_CODE_POS).toInt()) {
            // Sorted by low-order byte, then high-order byte.
            0x0003 -> BbMessage.InitEncryption(buffer)
            0x0005 -> BbMessage.Disconnect(buffer)
            0x0007 -> BbMessage.BlockList(buffer)
            0x0010 -> BbMessage.MenuSelect(buffer)
            0x0019 -> BbMessage.Redirect(buffer)
            0x0083 -> BbMessage.LobbyList(buffer)
            0x0093 -> BbMessage.Authenticate(buffer)
            0x00A0 -> BbMessage.ShipList(buffer)
            0x01DC -> BbMessage.GuildCardHeader(buffer)
            0x02DC -> BbMessage.GuildCardChunk(buffer)
            0x03DC -> BbMessage.GetGuildCardChunk(buffer)
            0x00E0 -> BbMessage.GetAccount(buffer)
            0x00E2 -> BbMessage.Account(buffer)
            0x00E3 -> BbMessage.CharSelect(buffer)
            0x00E4 -> BbMessage.CharSelectAck(buffer)
            0x00E5 -> BbMessage.CharData(buffer)
            0x00E6 -> BbMessage.AuthData(buffer)
            0x01E8 -> BbMessage.Checksum(buffer)
            0x02E8 -> BbMessage.ChecksumAck(buffer)
            0x03E8 -> BbMessage.GetGuildCardHeader(buffer)
            0x01EB -> BbMessage.FileList(buffer)
            0x02EB -> BbMessage.FileChunk(buffer)
            0x03EB -> BbMessage.GetFileChunk(buffer)
            0x04EB -> BbMessage.GetFileList(buffer)
            else -> BbMessage.Unknown(buffer)
        }
}

sealed class BbMessage(override val buffer: Buffer) : AbstractMessage(BB_HEADER_SIZE) {
    override val code: Int get() = buffer.getUShort(BB_MSG_CODE_POS).toInt()
    override val size: Int get() = buffer.getUShort(BB_MSG_SIZE_POS).toInt()

    class InitEncryption(buffer: Buffer) : BbMessage(buffer), InitEncryptionMessage {
        override val serverKey: ByteArray
            get() = byteArray(INIT_MSG_SIZE, size = KEY_SIZE)
        override val clientKey: ByteArray
            get() = byteArray(INIT_MSG_SIZE + KEY_SIZE, size = KEY_SIZE)

        constructor(message: String, serverKey: ByteArray, clientKey: ByteArray) : this(
            buf(0x0003, INIT_MSG_SIZE + 2 * KEY_SIZE) {
                require(message.length <= INIT_MSG_SIZE)
                require(serverKey.size == KEY_SIZE)
                require(clientKey.size == KEY_SIZE)

                writeStringAscii(message, byteLength = INIT_MSG_SIZE)
                writeByteArray(serverKey)
                writeByteArray(clientKey)
            }
        )
    }

    class Disconnect(buffer: Buffer) : BbMessage(buffer) {
        constructor() : this(buf(0x0005))
    }

    class BlockList(buffer: Buffer) : BbMessage(buffer) {
        constructor(shipName: String, blocks: List<String>) : this(
            buf(0x0007, (blocks.size + 1) * 44, flags = blocks.size) {
                var index = 0
                writeInt(0x00040000) // Menu type.
                writeInt(index++)
                writeShort(0) // Flags.
                writeStringUtf16(shipName, byteLength = 34)

                for (ship in blocks) {
                    writeInt(MenuType.Block.toInt())
                    writeInt(index++)
                    writeShort(0) // Flags.
                    writeStringUtf16(ship, byteLength = 34)
                }
            }
        )
    }

    class MenuSelect(buffer: Buffer) : BbMessage(buffer) {
        val menuType: MenuType get() = MenuType.fromInt(int(0))
        val itemNo: Int get() = int(4)

        constructor(menuType: MenuType, itemNo: Int) : this(
            buf(0x0010, 8) {
                writeInt(menuType.toInt())
                writeInt(itemNo)
            }
        )

        override fun toString(): String =
            messageString("menuType" to menuType, "itemNo" to itemNo)
    }

    class Redirect(buffer: Buffer) : BbMessage(buffer), RedirectMessage {
        override var ipAddress: ByteArray
            get() = byteArray(0, size = 4)
            set(value) {
                require(value.size == 4)
                setByteArray(0, value)
            }
        override var port: Int
            get() = uShort(4).toInt()
            set(value) {
                require(value in 0..65535)
                setShort(4, value.toShort())
            }

        constructor(ipAddress: ByteArray, port: Int) : this(
            buf(0x0019, 8) {
                require(ipAddress.size == 4)
                require(port in 0..65535)

                writeByteArray(ipAddress)
                writeShort(port.toShort())
                writeShort(0) // Padding.
            }
        )

        override fun toString(): String =
            messageString(
                "ipAddress" to ipAddress.joinToString(".") { it.toUByte().toString() },
                "port" to port,
            )
    }

    class LobbyList(buffer: Buffer) : BbMessage(buffer) {
        constructor() : this(
            buf(0x0083, 192) {
                repeat(15) {
                    writeInt(MenuType.Lobby.toInt())
                    writeInt(it + 1) // Item no.
                    writeInt(0) // Padding.
                }
                // 12 zero bytes of padding.
                writeInt(0)
                writeInt(0)
                writeInt(0)
            }
        )
    }

    // 0x0093
    class Authenticate(buffer: Buffer) : BbMessage(buffer) {
        val guildCard: Int get() = int(4)
        val version: Short get() = short(8)
        val teamId: Int get() = int(16)
        val userName: String
            get() = stringAscii(offset = 20, maxByteLength = 16, nullTerminated = true)
        val password: String
            get() = stringAscii(offset = 68, maxByteLength = 16, nullTerminated = true)
        val magic: Int get() = int(132) // Should be 0xDEADBEEF
        val charSlot: Int get() = byte(136).toInt()
        val charSelected: Boolean get() = byte(137).toInt() != 0
    }

    class ShipList(buffer: Buffer) : BbMessage(buffer) {
        constructor(ships: List<String>) : this(
            buf(0x00A0, (ships.size + 1) * 44, flags = ships.size) {
                var index = 0
                writeInt(MenuType.Ship.toInt())
                writeInt(index++)
                writeShort(4) // Flags
                writeStringUtf16("SHIP/US", byteLength = 34)

                for (ship in ships) {
                    writeInt(MenuType.Ship.toInt())
                    writeInt(index++)
                    writeShort(0) // Flags
                    writeStringUtf16(ship, byteLength = 34)
                }
            }
        )
    }

    class GuildCardHeader(buffer: Buffer) : BbMessage(buffer) {
        val guildCardSize: Int get() = int(4)
        val checksum: Int get() = int(8)

        constructor(guildCardSize: Int, checksum: Int) : this(
            buf(0x01DC, 12) {
                writeInt(1)
                writeInt(guildCardSize)
                writeInt(checksum)
            }
        )

        override fun toString(): String =
            messageString("guildCardSize" to guildCardSize, "checksum" to checksum)
    }

    class GuildCardChunk(buffer: Buffer) : BbMessage(buffer) {
        val chunkNo: Int get() = int(4)

        constructor(chunkNo: Int, chunk: Cursor) : this(
            buf(0x02DC, 8 + chunk.size) {
                writeInt(0)
                writeInt(chunkNo)
                writeCursor(chunk)
            }
        )

        override fun toString(): String =
            messageString("chunkNo" to chunkNo)
    }

    // 0x03DC
    class GetGuildCardChunk(buffer: Buffer) : BbMessage(buffer) {
        val chunkNo: Int get() = int(4)
        val cont: Boolean get() = int(8) != 0

        override fun toString(): String =
            messageString("chunkNo" to chunkNo, "cont" to cont)
    }

    // 0x00E0
    class GetAccount(buffer: Buffer) : BbMessage(buffer)

    class Account(buffer: Buffer) : BbMessage(buffer) {
        constructor(guildCard: Int, teamId: Int) : this(
            buf(0x00E2, 2804) {
                // 276 Bytes of unknown data.
                repeat(69) { writeInt(0) }
                writeByteArray(DEFAULT_KEYBOARD_GAMEPAD_CONFIG)
                writeInt(guildCard)
                writeInt(teamId)
                // 2092 Bytes of team data.
                repeat(523) { writeInt(0) }
                // Enable all team rewards.
                writeUInt(0xFFFFFFFFu)
                writeUInt(0xFFFFFFFFu)
            }
        )
    }

    // 0x00E3
    class CharSelect(buffer: Buffer) : BbMessage(buffer) {
        val slot: Int get() = uByte(0).toInt()
        val selected: Boolean get() = byte(4).toInt() != 0

        override fun toString(): String =
            messageString("slot" to slot, "select" to selected)
    }

    class CharSelectAck(buffer: Buffer) : BbMessage(buffer) {
        constructor(slot: Int, status: CharSelectStatus) : this(
            buf(0x00E4, 8) {
                writeInt(slot)
                writeInt(
                    when (status) {
                        CharSelectStatus.Update -> 0
                        CharSelectStatus.Select -> 1
                        CharSelectStatus.Nonexistent -> 2
                    }
                )
            }
        )
    }

    class CharData(buffer: Buffer) : BbMessage(buffer) {
        constructor(char: PsoCharacter) : this(
            buf(0x00E5, 128) {
                writeInt(char.slot)
                writeInt(char.exp)
                writeInt(char.level)
                writeStringAscii(char.guildCardString, byteLength = 16)
                repeat(2) { writeInt(0) } // Unknown.
                writeInt(char.nameColor)
                writeInt(char.model)
                repeat(3) { writeInt(0) } // Unused.
                writeInt(char.nameColorChecksum)
                writeByte(char.sectionId.toByte())
                writeByte(char.characterClass.toByte())
                writeByte(0) // V2 flags.
                writeByte(0) // Version.
                writeInt(0) // V1 flags.
                writeShort(char.costume.toShort())
                writeShort(char.skin.toShort())
                writeShort(char.face.toShort())
                writeShort(char.head.toShort())
                writeShort(char.hair.toShort())
                writeShort(char.hairRed.toShort())
                writeShort(char.hairGreen.toShort())
                writeShort(char.hairBlue.toShort())
                writeFloat(char.propX.toFloat())
                writeFloat(char.propY.toFloat())
                writeStringUtf16(char.name, byteLength = 32)
                writeInt(char.playTime)
            }
        )
    }

    class AuthData(buffer: Buffer) : BbMessage(buffer) {
        constructor(
            status: AuthStatus,
            guildCard: Int,
            teamId: Int,
            slot: Int,
            selected: Boolean,
        ) : this(
            buf(0x00E6, 60) {
                writeInt(
                    when (status) {
                        AuthStatus.Success -> 0
                        AuthStatus.Error -> 1
                        AuthStatus.Nonexistent -> 8
                    }
                )
                writeInt(0x10000)
                writeInt(guildCard)
                writeInt(teamId)
                writeInt(
                    if (status == AuthStatus.Success) (0xDEADBEEF).toInt() else 0
                )
                writeByte(slot.toByte())
                writeByte(if (selected) 1 else 0)
                // 34 Bytes of unknown data.
                writeShort(0)
                repeat(8) { writeInt(0) }
                writeInt(0x102)
            }
        )
    }

    class Checksum(buffer: Buffer) : BbMessage(buffer) {
        constructor(checksum: Int) : this(
            buf(0x01E8, 8) {
                writeInt(checksum)
                writeInt(0) // Padding.
            }
        )

        val checksum: Int get() = int(0)
    }

    class ChecksumAck(buffer: Buffer) : BbMessage(buffer) {
        constructor(success: Boolean) : this(
            buf(0x02E8, 4) {
                writeInt(if (success) 1 else 0)
            }
        )
    }

    class GetGuildCardHeader(buffer: Buffer) : BbMessage(buffer) {
        constructor() : this(buf(0x03E8))
    }

    class FileList(buffer: Buffer) : BbMessage(buffer) {
        constructor(entries: List<FileListEntry>) : this(
            buf(0x01EB, entries.size * 76, flags = entries.size) {
                for (entry in entries) {
                    writeInt(entry.size)
                    writeInt(entry.checksum)
                    writeInt(entry.offset)
                    writeStringAscii(entry.filename, byteLength = 64)
                }
            }
        )
    }

    class FileChunk(buffer: Buffer) : BbMessage(buffer) {
        constructor(chunkNo: Int, chunk: Cursor) : this(
            buf(0x02EB, 4 + chunk.size) {
                writeInt(chunkNo)
                writeCursor(chunk)
            }
        )
    }

    class GetFileChunk(buffer: Buffer) : BbMessage(buffer) {
        constructor() : this(buf(0x03EB))
    }

    class GetFileList(buffer: Buffer) : BbMessage(buffer) {
        constructor() : this(buf(0x04EB))
    }

    class Unknown(buffer: Buffer) : BbMessage(buffer)

    companion object {
        private fun buf(
            code: Int,
            bodySize: Int = 0,
            flags: Int = 0,
            writeBody: WritableCursor.() -> Unit = {},
        ): Buffer {
            val size = BB_HEADER_SIZE + bodySize
            val buffer = Buffer.withSize(size)

            val cursor = buffer.cursor()
                // Write header.
                .writeShort(size.toShort())
                .writeShort(code.toShort())
                .writeInt(flags)

            cursor.writeBody()

            require(cursor.position == buffer.size) {
                "Message buffer should be filled completely, only ${cursor.position} / ${buffer.size} bytes written."
            }

            return buffer
        }
    }
}

enum class AuthStatus {
    Success, Error, Nonexistent
}

class PsoCharacter(
    val slot: Int,
    val exp: Int,
    val level: Int,
    val guildCardString: String,
    val nameColor: Int,
    val model: Int,
    val nameColorChecksum: Int,
    val sectionId: Int,
    val characterClass: Int,
    val costume: Int,
    val skin: Int,
    val face: Int,
    val head: Int,
    val hair: Int,
    val hairRed: Int,
    val hairGreen: Int,
    val hairBlue: Int,
    val propX: Double,
    val propY: Double,
    val name: String,
    val playTime: Int,
)

class GuildCardEntry(
    val playerTag: Int,
    val serialNumber: Int,
    val name: String,
    val description: String,
    val sectionId: Int,
    val characterClass: Int,
)

class GuildCard(
    val entries: List<GuildCardEntry>
)

class FileListEntry(
    val size: Int,
    val checksum: Int,
    val offset: Int,
    val filename: String,
)

enum class CharSelectStatus {
    Update, Select, Nonexistent
}

enum class MenuType(private val type: Int) {
    Lobby(-1),
    InfoDesk(0),
    Block(1),
    Game(2),
    QuestCategory(3),
    Quest(4),
    Ship(5),
    GameType(6),
    Gm(7),
    Unknown(Int.MIN_VALUE);

    fun toInt(): Int = type

    companion object {
        fun fromInt(type: Int): MenuType = when (type) {
            -1 -> Lobby
            0 -> InfoDesk
            1 -> Block
            2 -> Game
            3 -> QuestCategory
            4 -> Quest
            5 -> Ship
            6 -> GameType
            7 -> Gm
            else -> Unknown
        }
    }
}

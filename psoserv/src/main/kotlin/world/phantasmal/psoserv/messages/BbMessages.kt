package world.phantasmal.psoserv.messages

import world.phantasmal.psolib.buffer.Buffer
import world.phantasmal.psolib.cursor.Cursor
import world.phantasmal.psolib.cursor.WritableCursor
import world.phantasmal.psolib.cursor.cursor
import world.phantasmal.psoserv.utils.toHex

private const val INIT_MSG_SIZE: Int = 96
private const val KEY_SIZE: Int = 48

const val BB_HEADER_SIZE: Int = 8
const val BB_MSG_SIZE_POS: Int = 0
const val BB_MSG_CODE_POS: Int = 2
const val BB_MSG_FLAGS_POS: Int = 4
const val BB_MSG_BODY_POS: Int = 8

object BbMessageDescriptor : MessageDescriptor<BbMessage> {
    override val headerSize: Int = BB_HEADER_SIZE

    override fun readHeader(buffer: Buffer): Header {
        val size = buffer.getUShort(BB_MSG_SIZE_POS).toInt()
        val code = buffer.getUShort(BB_MSG_CODE_POS).toInt()
        val flags = buffer.getInt(BB_MSG_FLAGS_POS)
        return Header(code, size, flags)
    }

    override fun readMessage(buffer: Buffer): BbMessage =
        when (buffer.getUShort(BB_MSG_CODE_POS).toInt()) {
            // Sorted by low-order byte, then high-order byte.
            0x0003 -> BbMessage.InitEncryption(buffer)
            0x0005 -> BbMessage.Disconnect(buffer)
            0x0007 -> BbMessage.BlockList(buffer)
            0x0010 -> BbMessage.MenuSelect(buffer)
            0x0019 -> BbMessage.Redirect(buffer)
            0x001D -> BbMessage.Ping(buffer)
            0x0060 -> BbMessage.Broadcast(buffer)
            0x0061 -> BbMessage.CharData(buffer)
            0x0064 -> BbMessage.JoinParty(buffer)
            0x0067 -> BbMessage.JoinLobby(buffer)
            0x0068 -> BbMessage.JoinedLobby(buffer)
            0x0083 -> BbMessage.LobbyList(buffer)
            0x0093 -> BbMessage.Authenticate(buffer)
            0x0095 -> BbMessage.GetCharData(buffer)
            0x00A0 -> BbMessage.ShipList(buffer)
            0x00C1 -> BbMessage.CreateParty(buffer)
            0x01DC -> BbMessage.GuildCardHeader(buffer)
            0x02DC -> BbMessage.GuildCardChunk(buffer)
            0x03DC -> BbMessage.GetGuildCardChunk(buffer)
            0x00E0 -> BbMessage.GetAccount(buffer)
            0x00E2 -> BbMessage.Account(buffer)
            0x00E3 -> BbMessage.CharSelect(buffer)
            0x00E4 -> BbMessage.CharSelectAck(buffer)
            0x00E5 -> BbMessage.Char(buffer)
            0x00E6 -> BbMessage.AuthData(buffer)
            0x00E7 -> BbMessage.FullCharacterData(buffer)
            0x01E8 -> BbMessage.Checksum(buffer)
            0x02E8 -> BbMessage.ChecksumAck(buffer)
            0x03E8 -> BbMessage.GetGuildCardHeader(buffer)
            0x01EB -> BbMessage.FileList(buffer)
            0x02EB -> BbMessage.FileChunk(buffer)
            0x03EB -> BbMessage.GetFileChunk(buffer)
            0x04EB -> BbMessage.GetFileList(buffer)
            else -> BbMessage.Unknown(buffer)
        }

    override fun createInitEncryption(serverKey: ByteArray, clientKey: ByteArray) =
        BbMessage.InitEncryption(
            "Phantasy Star Online Blue Burst Game Server. Copyright 1999-2004 SONICTEAM.",
            serverKey,
            clientKey,
        )
}

sealed class BbMessage(override val buffer: Buffer) : AbstractMessage(BB_HEADER_SIZE) {
    override val code: Int get() = buffer.getUShort(BB_MSG_CODE_POS).toInt()
    override val size: Int get() = buffer.getUShort(BB_MSG_SIZE_POS).toInt()
    override val flags: Int get() = buffer.getInt(BB_MSG_FLAGS_POS)

    // 0x0003
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

    // 0x0005
    class Disconnect(buffer: Buffer) : BbMessage(buffer) {
        constructor() : this(buf(0x0005))
    }

    // 0x0007
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

    // 0x0010
    class MenuSelect(buffer: Buffer) : BbMessage(buffer) {
        val menuType: MenuType get() = MenuType.fromInt(int(0))
        val itemId: Int get() = int(4)

        constructor(menuType: MenuType, itemId: Int) : this(
            buf(0x0010, 8) {
                writeInt(menuType.toInt())
                writeInt(itemId)
            }
        )

        override fun toString(): String =
            messageString("menuType" to menuType, "itemId" to itemId)
    }

    // 0x0019
    class Redirect(buffer: Buffer) : BbMessage(buffer), RedirectMessage {
        override var ipAddress: ByteArray
            get() = byteArray(0, size = 4)
            set(value) {
                require(value.size == 4)
                setByteArray(0, value)
            }
        override var port: UShort
            get() = uShort(4)
            set(value) = setUShort(4, value)

        constructor(ipAddress: ByteArray, port: UShort) : this(
            buf(0x0019, 8) {
                require(ipAddress.size == 4)

                writeByteArray(ipAddress)
                writeUShort(port)
                writeShort(0) // Padding.
            }
        )

        override fun toString(): String =
            messageString(
                "ipAddress" to ipAddress.joinToString(".") { it.toUByte().toString() },
                "port" to port,
            )
    }

    // 0x001D
    class Ping(buffer: Buffer) : BbMessage(buffer) {
        constructor() : this(buf(0x001D))
    }

    // 0x0060
    class Broadcast(buffer: Buffer) : BbMessage(buffer) {
        var subType: UByte
            get() = uByte(0)
            set(value) = setUByte(0, value)
        var subSize: UByte
            get() = uByte(1)
            set(value) = setUByte(1, value)
        var clientId: UByte
            get() = uByte(2)
            set(value) = setUByte(2, value)

        override fun toString(): String =
            messageString(
                "subType" to subType.toHex(),
                "subSize" to subSize,
                "clientId" to clientId,
            )
    }

    // 0x0061
    class CharData(buffer: Buffer) : BbMessage(buffer)

    // 0x0064
    class JoinParty(buffer: Buffer) : BbMessage(buffer) {
        constructor(
            players: List<LobbyPlayer>,
            clientId: Byte,
            leaderId: Byte,
            difficulty: Byte,
            battleMode: Boolean,
            event: Byte,
            sectionId: Byte,
            challengeMode: Boolean,
            prngSeed: Int,
            episode: Byte,
            soloMode: Boolean,
        ) : this(
            buf(0x0064, 416, flags = players.size) {
                require(players.size <= 4)

                repeat(32) { writeInt(0) } // Map layout

                for (player in players) {
                    player.write(this)
                }

                // Empty player slots.
                repeat((4 - players.size) * (LobbyPlayer.SIZE / 4)) { writeInt(0) }

                writeByte(clientId)
                writeByte(leaderId)
                writeByte(1) // Unknown
                writeByte(difficulty)
                writeByte(if (battleMode) 1 else 0)
                writeByte(event)
                writeByte(sectionId)
                writeByte(if (challengeMode) 1 else 0)
                writeInt(prngSeed)
                writeByte(episode)
                writeByte(1) // Unknown
                writeByte(if (soloMode) 1 else 0)
                writeByte(0) // Unused
            }
        )
    }

    abstract class AbstractJoinLobby(buffer: Buffer) : BbMessage(buffer) {
        val playerCount: Int get() = flags
        var clientId: UByte
            get() = uByte(0)
            set(value) = setUByte(0, value)
        var leaderId: UByte
            get() = uByte(1)
            set(value) = setUByte(1, value)
        var lobbyNo: UByte
            get() = uByte(3)
            set(value) = setUByte(3, value)
        var blockNo: UShort
            get() = uShort(4)
            set(value) = setUShort(4, value)

        override fun toString(): String =
            messageString(
                "playerCount" to playerCount,
                "clientId" to clientId,
                "leaderId" to leaderId,
                "lobbyNo" to lobbyNo,
                "blockNo" to blockNo,
            )

        companion object {
            @JvmStatic
            protected fun joinLobbyBuf(
                code: Int,
                clientId: Byte,
                leaderId: Byte,
                disableUdp: Boolean,
                lobbyNo: UByte,
                blockNo: UShort,
                event: UShort,
                players: List<LobbyPlayer>,
            ): Buffer =
                buf(code, 12 + players.size * (LobbyPlayer.SIZE + 1244), flags = players.size) {
                    writeByte(clientId)
                    writeByte(leaderId)
                    writeByte(if (disableUdp) 1 else 0)
                    writeUByte(lobbyNo)
                    writeUShort(blockNo)
                    writeUShort(event)
                    writeInt(0) // Unused.

                    for (player in players) {
                        player.write(this)
                        repeat(311) { writeInt(0) } // Inventory and character data.
                    }
                }
        }
    }

    // 0x0067
    class JoinLobby(buffer: Buffer) : AbstractJoinLobby(buffer) {
        constructor(
            clientId: Byte,
            leaderId: Byte,
            disableUdp: Boolean,
            lobbyNo: UByte,
            blockNo: UShort,
            event: UShort,
            players: List<LobbyPlayer>,
        ) : this(
            joinLobbyBuf(
                0x0067,
                clientId,
                leaderId,
                disableUdp,
                lobbyNo,
                blockNo,
                event,
                players,
            )
        )
    }

    // 0x0068
    class JoinedLobby(buffer: Buffer) : AbstractJoinLobby(buffer) {
        constructor(
            clientId: Byte,
            leaderId: Byte,
            disableUdp: Boolean,
            lobbyNo: UByte,
            blockNo: UShort,
            event: UShort,
            player: LobbyPlayer,
        ) : this(
            joinLobbyBuf(
                0x0068,
                clientId,
                leaderId,
                disableUdp,
                lobbyNo,
                blockNo,
                event,
                listOf(player),
            )
        )

        override fun toString(): String =
            messageString(
                "clientId" to clientId,
                "leaderId" to leaderId,
                "lobbyNo" to lobbyNo,
                "blockNo" to blockNo,
            )
    }

    // 0x0083
    class LobbyList(buffer: Buffer) : BbMessage(buffer) {
        constructor(lobbyIds: List<Int>) : this(
            buf(0x0083, 192) {
                for (lobbyId in lobbyIds) {
                    writeInt(MenuType.Lobby.toInt())
                    writeInt(lobbyId) // Item ID.
                    writeInt(0) // Padding.
                }
                // 12 zero bytes of padding.
                // TODO: Is this necessary?
                writeInt(0)
                writeInt(0)
                writeInt(0)
            }
        )
    }

    // 0x0093
    class Authenticate(buffer: Buffer) : BbMessage(buffer) {
        val guildCardNo: Int get() = int(4)
        val version: Short get() = short(8)
        val teamId: Int get() = int(16)
        val username: String get() = stringAscii(offset = 20, maxByteLength = 16)
        val password: String get() = stringAscii(offset = 68, maxByteLength = 16)
        val magic: Int get() = int(132) // Should be 0xDEADBEEF
        val charSlot: Int get() = byte(136).toInt()
        val charSelected: Boolean get() = byte(137).toInt() != 0

        override fun toString(): String =
            messageString(
                "guildCardNo" to guildCardNo,
                "version" to version,
                "teamId" to teamId,
                "username" to username,
                "charSlot" to charSlot,
                "charSelected" to charSelected,
            )
    }

    // 0x0095
    class GetCharData(buffer: Buffer) : BbMessage(buffer) {
        constructor() : this(buf(0x0095))
    }

    // 0x00A0
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

    // 0x00C1
    class CreateParty(buffer: Buffer) : BbMessage(buffer) {
        var name: String
            get() = stringUtf16(8, 32)
            set(value) = setStringUtf16(8, value, 32)
        var password: String
            get() = stringUtf16(40, 32)
            set(value) = setStringUtf16(40, value, 32)
        var difficulty: Byte
            get() = byte(72)
            set(value) = setByte(72, value)
        var battleMode: Boolean
            get() = byte(73).toInt() != 0
            set(value) = setByte(73, if (value) 1 else 0)
        var challengeMode: Boolean
            get() = byte(74).toInt() != 0
            set(value) = setByte(74, if (value) 1 else 0)
        var episode: Byte
            get() = byte(75)
            set(value) = setByte(75, value)
        var soloMode: Boolean
            get() = byte(76).toInt() != 0
            set(value) = setByte(76, if (value) 1 else 0)

        override fun toString(): String =
            messageString(
                "name" to name,
                "difficulty" to difficulty,
                "battleMode" to battleMode,
                "challengeMode" to challengeMode,
                "episode" to episode,
                "soloMode" to soloMode,
            )
    }

    // 0x01DC
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

    // 0x02DC
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
    class GetAccount(buffer: Buffer) : BbMessage(buffer) {
        constructor() : this(buf(0x00E1))
    }

    // 0x00E2
    class Account(buffer: Buffer) : BbMessage(buffer) {
        constructor(guildCardNo: Int, teamId: Int) : this(
            buf(0x00E2, 2804) {
                // 276 Bytes of unknown data.
                repeat(69) { writeInt(0) }
                writeByteArray(DEFAULT_KEYBOARD_CONFIG)
                writeByteArray(DEFAULT_GAMEPAD_CONFIG)
                writeInt(guildCardNo)
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

    // 0x00E4
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

    // 0x00E5
    class Char(buffer: Buffer) : BbMessage(buffer) {
        constructor(char: PsoCharacter) : this(
            buf(0x00E5, 128) {
                char.write(this)
            }
        )
    }

    // 0x00E6
    class AuthData(buffer: Buffer) : BbMessage(buffer) {
        var status: AuthStatus
            get() = when (val value = int(0)) {
                0 -> AuthStatus.Success
                1 -> AuthStatus.Error
                8 -> AuthStatus.Nonexistent
                else -> AuthStatus.Unknown(value)
            }
            set(status) = setInt(0, when (status) {
                AuthStatus.Success -> 0
                AuthStatus.Error -> 1
                AuthStatus.Nonexistent -> 8
                is AuthStatus.Unknown -> status.value
            })

        constructor(
            status: AuthStatus,
            guildCardNo: Int,
            teamId: Int,
            charSlot: Int,
            charSelected: Boolean,
        ) : this(
            buf(0x00E6, 60) {
                writeInt(
                    when (status) {
                        AuthStatus.Success -> 0
                        AuthStatus.Error -> 1
                        AuthStatus.Nonexistent -> 8
                        is AuthStatus.Unknown -> status.value
                    }
                )
                writeInt(0x10000)
                writeInt(guildCardNo)
                writeInt(teamId)
                writeInt(
                    if (status == AuthStatus.Success) (0xDEADBEEF).toInt() else 0
                )
                writeByte(charSlot.toByte())
                writeByte(if (charSelected) 1 else 0)
                // 34 Bytes of unknown data.
                writeShort(0)
                repeat(8) { writeInt(0) }
                writeInt(0x102)
            }
        )

        override fun toString(): String =
            messageString("status" to status)
    }

    // 0x00E7
    class FullCharacterData(buffer: Buffer) : BbMessage(buffer) {
        constructor(char: PsoCharData) : this(
            buf(0x00E7, 14744) {
                repeat(211) { writeInt(0) }
                char.write(this)
                repeat(1275) { writeInt(0) }
                writeStringUtf16(char.name, byteLength = 48)
                repeat(8) { writeInt(0) } // Team name
                repeat(44) { writeInt(0) } // Guild card description
                writeShort(0) // Reserved
                writeByte(char.sectionId)
                writeByte(char.charClass)
                repeat(1403) { writeInt(0) }
                writeByteArray(DEFAULT_KEYBOARD_CONFIG)
                writeByteArray(DEFAULT_GAMEPAD_CONFIG)
                repeat(527) { writeInt(0) }
            }
        )
    }

    // 0x01E8
    class Checksum(buffer: Buffer) : BbMessage(buffer) {
        constructor(checksum: Int) : this(
            buf(0x01E8, 8) {
                writeInt(checksum)
                writeInt(0) // Padding.
            }
        )

        val checksum: Int get() = int(0)
    }

    // 0x02E8
    class ChecksumAck(buffer: Buffer) : BbMessage(buffer) {
        constructor(success: Boolean) : this(
            buf(0x02E8, 4) {
                writeInt(if (success) 1 else 0)
            }
        )
    }

    // 0x03E8
    class GetGuildCardHeader(buffer: Buffer) : BbMessage(buffer) {
        constructor() : this(buf(0x03E8))
    }

    // 0x01EB
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

    // 0x02EB
    class FileChunk(buffer: Buffer) : BbMessage(buffer) {
        constructor(chunkNo: Int, chunk: Cursor) : this(
            buf(0x02EB, 4 + chunk.size) {
                writeInt(chunkNo)
                writeCursor(chunk)
            }
        )
    }

    // 0x03EB
    class GetFileChunk(buffer: Buffer) : BbMessage(buffer) {
        constructor() : this(buf(0x03EB))
    }

    // 0x04EB
    class GetFileList(buffer: Buffer) : BbMessage(buffer) {
        constructor() : this(buf(0x04EB))
    }

    class Unknown(buffer: Buffer) : BbMessage(buffer)

    companion object {
        private fun buf(
            code: Int,
            bodySize: Int = 0,
            flags: Int = 0,
            writeBody: (WritableCursor.() -> Unit)? = null,
        ): Buffer {
            val size = BB_HEADER_SIZE + bodySize
            val buffer = Buffer.withSize(size)
                .setShort(BB_MSG_SIZE_POS, size.toShort())
                .setShort(BB_MSG_CODE_POS, code.toShort())
                .setInt(BB_MSG_FLAGS_POS, flags)

            if (writeBody != null) {
                val cursor = buffer.cursor(BB_MSG_BODY_POS)
                cursor.writeBody()

                require(cursor.position == bodySize) {
                    "Message buffer should be filled completely, only ${cursor.position} / $bodySize bytes written."
                }
            }

            return buffer
        }
    }
}

sealed class AuthStatus {
    object Success : AuthStatus()
    object Error : AuthStatus()
    object Nonexistent : AuthStatus()
    class Unknown(val value: Int) : AuthStatus() {
        override fun toString(): String = "Unknown[$value]"
    }

    override fun toString(): String = this::class.simpleName!!
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
) {
    fun write(cursor: WritableCursor) {
        with(cursor) {
            writeInt(slot)
            writeInt(exp)
            writeInt(level)
            writeStringAscii(guildCardString, byteLength = 16)
            repeat(2) { writeInt(0) } // Unknown.
            writeInt(nameColor)
            writeInt(model)
            repeat(3) { writeInt(0) } // Unused.
            writeInt(nameColorChecksum)
            writeByte(sectionId.toByte())
            writeByte(characterClass.toByte())
            writeByte(0) // V2 flags.
            writeByte(0) // Version.
            writeInt(0) // V1 flags.
            writeShort(costume.toShort())
            writeShort(skin.toShort())
            writeShort(face.toShort())
            writeShort(head.toShort())
            writeShort(hair.toShort())
            writeShort(hairRed.toShort())
            writeShort(hairGreen.toShort())
            writeShort(hairBlue.toShort())
            writeFloat(propX.toFloat())
            writeFloat(propY.toFloat())
            writeStringUtf16(name, byteLength = 32)
            writeInt(playTime)
        }
    }
}

class GuildCardEntry(
    val playerTag: Int,
    val serialNumber: Int,
    val name: String,
    val description: String,
    val sectionId: Int,
    val charClass: Int,
)

class GuildCard(
    val entries: List<GuildCardEntry>,
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

class PsoCharData(
    val hp: Short,
    val level: Int,
    val exp: Int,
    val sectionId: Byte,
    val charClass: Byte,
    val name: String,
) {
    fun write(cursor: WritableCursor) {
        with(cursor) {
            repeat(3) { writeShort(0) } // ATP/MST/EVP
            writeShort(hp)
            repeat(3) { writeShort(0) } // DFP/ATA/LCK
            writeShort(0) // Unknown
            repeat(2) { writeInt(0) } // Unknown
            writeInt(level)
            writeInt(exp)
            writeInt(0) // Meseta
            repeat(4) { writeInt(0) } // Guild card
            repeat(2) { writeInt(0) } // Unknown
            writeInt(0) // Name color
            writeByte(0) // Extra model
            repeat(3) { writeByte(0) } // Unused
            repeat(3) { writeInt(0) } // Unused
            writeInt(0) // Name color checksum
            writeByte(sectionId)
            writeByte(charClass)
            writeByte(0) // V2 flags
            writeByte(0) // Version
            writeInt(0) // V1 flags
            writeShort(0) // Costume
            writeShort(0) // Skin
            writeShort(0) // Face
            writeShort(0) // Head
            writeShort(0) // Hair
            writeShort(0) // Hair red
            writeShort(0) // Hair green
            writeShort(0) // Hair blue
            writeFloat(0.5f) // Prop x
            writeFloat(0.5f) // Prop y
            writeStringUtf16(name, 32)
            repeat(58) { writeInt(0) } // Config
            repeat(5) { writeInt(0) } // Tech levels
        }
    }
}

class PlayerHeader(
    val playerTag: Int,
    val guildCardNo: Int,
    val clientId: UByte,
    val charName: String,
) {
    fun write(cursor: WritableCursor) {
        with(cursor) {
            writeInt(playerTag)
            writeInt(guildCardNo)
            repeat(5) { writeInt(0) } // Unknown.
            writeUByte(clientId)
            repeat(3) { writeByte(0) } // Unknown.
            writeStringUtf16(charName, 32)
            writeInt(0) // Unknown.
            repeat(311) { writeInt(0) }
        }
    }

    companion object {
        const val SIZE: Int = 1312
    }
}

class LobbyPlayer(
    val playerTag: Int,
    val guildCardNo: Int,
    val clientId: Byte,
    val charName: String,
) {
    fun write(cursor: WritableCursor) {
        with(cursor) {
            writeInt(playerTag)
            writeInt(guildCardNo)
            repeat(5) { writeInt(0) } // Unknown.
            writeByte(clientId)
            repeat(3) { writeByte(0) } // Unknown.
            writeStringUtf16(charName, 32)
            writeInt(0) // Unknown.
        }
    }

    companion object {
        const val SIZE: Int = 68
    }
}

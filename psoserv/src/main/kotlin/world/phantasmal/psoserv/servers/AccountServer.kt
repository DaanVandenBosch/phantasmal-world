package world.phantasmal.psoserv.servers

import world.phantasmal.core.math.clamp
import world.phantasmal.psolib.Endianness
import world.phantasmal.psolib.buffer.Buffer
import world.phantasmal.psolib.cursor.cursor
import world.phantasmal.psoserv.encryption.BbCipher
import world.phantasmal.psoserv.encryption.Cipher
import world.phantasmal.psoserv.messages.*
import world.phantasmal.psoserv.utils.crc32Checksum

class AccountServer(
    bindPair: Inet4Pair,
    private val ships: List<ShipInfo>,
) : GameServer<BbMessage>("account", bindPair) {

    override val messageDescriptor = BbMessageDescriptor

    override fun createCipher() = BbCipher()

    override fun createClientReceiver(
        ctx: ClientContext<BbMessage>,
        serverCipher: Cipher,
        clientCipher: Cipher,
    ): ClientReceiver<BbMessage> = object : ClientReceiver<BbMessage> {
        private val guildCardBuffer = Buffer.withSize(54672)
        private var fileChunkNo = 0
        private var guildCard: Int = -1
        private var teamId: Int = -1
        private var slot: Int = 0
        private var charSelected: Boolean = false

        init {
            ctx.send(
                BbMessage.InitEncryption(
                    "Phantasy Star Online Blue Burst Game Server. Copyright 1999-2004 SONICTEAM.",
                    serverCipher.key,
                    clientCipher.key,
                ),
                encrypt = false,
            )
        }

        override fun process(message: BbMessage): Boolean = when (message) {
            is BbMessage.Authenticate -> {
                // TODO: Actual authentication.
                guildCard = message.guildCard
                teamId = message.teamId
                ctx.send(
                    BbMessage.AuthData(
                        AuthStatus.Success,
                        guildCard,
                        teamId,
                        slot,
                        charSelected,
                    )
                )

                // When the player has selected a character, we send him the list of ships to
                // choose from.
                if (message.charSelected) {
                    ctx.send(BbMessage.ShipList(ships.map { it.uiName }))
                }

                true
            }

            is BbMessage.GetAccount -> {
                // TODO: Send correct guild card number and team ID.
                ctx.send(BbMessage.Account(0, 0))

                true
            }

            is BbMessage.CharSelect -> {
                if (message.selected) {
                    // Player has chosen a character.
                    // TODO: Verify slot.
                    if (slot in 0..3) {
                        slot = message.slot
                        charSelected = true
                        ctx.send(
                            BbMessage.AuthData(
                                AuthStatus.Success,
                                guildCard,
                                teamId,
                                slot,
                                charSelected,
                            )
                        )
                        ctx.send(
                            BbMessage.CharSelectAck(slot, CharSelectStatus.Select)
                        )
                    } else {
                        ctx.send(
                            BbMessage.CharSelectAck(slot, CharSelectStatus.Nonexistent)
                        )
                    }
                } else {
                    // Player is previewing characters.
                    // TODO: Look up character data.
                    ctx.send(
                        BbMessage.Char(
                            PsoCharacter(
                                slot = message.slot,
                                exp = 0,
                                level = 0,
                                guildCardString = "",
                                nameColor = 0,
                                model = 0,
                                nameColorChecksum = 0,
                                sectionId = message.slot,
                                characterClass = message.slot,
                                costume = 0,
                                skin = 0,
                                face = 0,
                                head = 0,
                                hair = 0,
                                hairRed = 0,
                                hairGreen = 0,
                                hairBlue = 0,
                                propX = 0.5,
                                propY = 0.5,
                                name = "Phantasmal ${message.slot}",
                                playTime = 0,
                            )
                        )
                    )
                }

                true
            }

            is BbMessage.Checksum -> {
                // TODO: Checksum checking.
                ctx.send(BbMessage.ChecksumAck(true))

                true
            }

            is BbMessage.GetGuildCardHeader -> {
                ctx.send(
                    BbMessage.GuildCardHeader(
                        guildCardBuffer.size,
                        crc32Checksum(guildCardBuffer),
                    )
                )

                true
            }

            is BbMessage.GetGuildCardChunk -> {
                if (message.cont) {
                    val offset = clamp(
                        message.chunkNo * MAX_CHUNK_SIZE,
                        min = 0,
                        max = guildCardBuffer.size,
                    )
                    val size = (guildCardBuffer.size - offset).coerceAtMost(MAX_CHUNK_SIZE)

                    ctx.send(
                        BbMessage.GuildCardChunk(
                            message.chunkNo,
                            guildCardBuffer.cursor(offset, size),
                        )
                    )
                }

                true
            }

            is BbMessage.GetFileList -> {
                fileChunkNo = 0

                ctx.send(BbMessage.FileList(FILE_LIST))

                true
            }

            is BbMessage.GetFileChunk -> {
                val offset = (fileChunkNo * MAX_CHUNK_SIZE).coerceAtMost(
                    FILE_BUFFER.size
                )
                val size = (FILE_BUFFER.size - offset).coerceAtMost(
                    MAX_CHUNK_SIZE
                )

                ctx.send(BbMessage.FileChunk(fileChunkNo, FILE_BUFFER.cursor(offset, size)))

                if (offset + size < FILE_BUFFER.size) {
                    fileChunkNo++
                }

                true
            }

            is BbMessage.MenuSelect -> {
                if (message.menuType == MenuType.Ship) {
                    ships.getOrNull(message.itemId - 1)?.let { ship ->
                        ctx.send(
                            BbMessage.Redirect(
                                ship.bindPair.address.address,
                                ship.bindPair.port.toUShort(),
                            )
                        )
                    }

                    // Disconnect.
                    false
                } else {
                    true
                }
            }

            is BbMessage.Disconnect -> false

            else -> ctx.unexpectedMessage(message)
        }
    }

    companion object {
        private const val MAX_CHUNK_SIZE: Int = 0x6800
        private val FILE_LIST: List<FileListEntry>
        private val FILE_BUFFER: Buffer

        init {
            val filenames = listOf(
                "BattleParamEntry.dat",
                "BattleParamEntry_ep4.dat",
                "BattleParamEntry_ep4_on.dat",
                "BattleParamEntry_lab.dat",
                "BattleParamEntry_lab_on.dat",
                "BattleParamEntry_on.dat",
                "ItemMagEdit.prs",
                "ItemPMT.prs",
                "PlyLevelTbl.prs",
            )
            val fileBuffers = mutableListOf<Buffer>()

            val fileList = mutableListOf<FileListEntry>()
            var offset = 0

            for (filename in filenames) {
                val data = Buffer.fromResource("/world/phantasmal/psoserv/$filename")
                fileList.add(FileListEntry(data.size, crc32Checksum(data), offset, filename))
                fileBuffers.add(data)
                offset += data.size
            }

            FILE_LIST = fileList

            FILE_BUFFER = Buffer.withSize(offset, Endianness.Little)
            offset = 0

            for (data in fileBuffers) {
                data.copyInto(FILE_BUFFER, destinationOffset = offset)
                offset += data.size
            }
        }
    }
}

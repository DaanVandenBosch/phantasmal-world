package world.phantasmal.psoserv.servers.account

import mu.KLogger
import world.phantasmal.core.math.clamp
import world.phantasmal.psolib.Endianness
import world.phantasmal.psolib.buffer.Buffer
import world.phantasmal.psolib.cursor.cursor
import world.phantasmal.psoserv.messages.*
import world.phantasmal.psoserv.servers.FinalServerState
import world.phantasmal.psoserv.servers.ServerState
import world.phantasmal.psoserv.servers.ServerStateContext
import world.phantasmal.psoserv.servers.SocketSender
import world.phantasmal.psoserv.utils.crc32Checksum

class AccountContext(
    logger: KLogger,
    socketSender: SocketSender<BbMessage>,
    var guildCard: Int = -1,
    var teamId: Int = -1,
    var slot: Int = 0,
    var charSelected: Boolean = false,
) : ServerStateContext<BbMessage>(logger, socketSender)

sealed class AccountState(ctx: AccountContext) :
    ServerState<BbMessage, AccountContext, AccountState>(ctx) {

    class Authentication(ctx: AccountContext) : AccountState(ctx) {
        override fun process(message: BbMessage): AccountState =
            if (message is BbMessage.Authenticate) {
                // TODO: Actual authentication.
                ctx.guildCard = message.guildCard
                ctx.teamId = message.teamId
                ctx.send(
                    BbMessage.AuthData(
                        BbAuthStatus.Success,
                        ctx.guildCard,
                        ctx.teamId,
                        ctx.slot,
                        ctx.charSelected,
                    )
                )

                Account(ctx)
            } else {
                unexpectedMessage(message)
            }
    }

    class Account(ctx: AccountContext) : AccountState(ctx) {
        override fun process(message: BbMessage): AccountState =
            when (message) {
                is BbMessage.GetAccount -> {
                    // TODO: Send correct guild card number and team ID.
                    ctx.send(BbMessage.Account(0, 0))

                    this
                }

                is BbMessage.CharSelect -> {
                    if (message.select) {
                        // TODO: Verify slot.
                        if (ctx.slot in 0..3) {
                            ctx.slot = message.slot
                            ctx.charSelected = true
                            ctx.send(
                                BbMessage.AuthData(
                                    BbAuthStatus.Success,
                                    ctx.guildCard,
                                    ctx.teamId,
                                    ctx.slot,
                                    ctx.charSelected,
                                )
                            )
                            ctx.send(
                                BbMessage.CharSelectAck(ctx.slot, BbCharSelectStatus.Select)
                            )
                        } else {
                            ctx.send(
                                BbMessage.CharSelectAck(ctx.slot, BbCharSelectStatus.Nonexistent)
                            )
                        }
                    } else {
                        // TODO: Look up character data.
                        ctx.send(
                            BbMessage.CharData(
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

                    this
                }

                is BbMessage.Checksum -> {
                    // TODO: Checksum checking.
                    ctx.send(BbMessage.ChecksumAck(true))

                    GuildCardData(ctx)
                }

                is BbMessage.Disconnect -> Final(ctx)

                else -> unexpectedMessage(message)
            }
    }

    class GuildCardData(ctx: AccountContext) : AccountState(ctx) {
        private val guildCardBuffer = Buffer.withSize(54672)

        override fun process(message: BbMessage): AccountState =
            when (message) {
                is BbMessage.GetGuildCardHeader -> {
                    ctx.send(
                        BbMessage.GuildCardHeader(
                            guildCardBuffer.size,
                            crc32Checksum(guildCardBuffer),
                        )
                    )

                    this
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

                        this
                    } else {
                        DownloadFiles(ctx)
                    }
                }

                else -> unexpectedMessage(message)
            }

        companion object {
            private const val MAX_CHUNK_SIZE: Int = 0x6800
        }
    }

    class DownloadFiles(ctx: AccountContext) : AccountState(ctx) {
        private var fileChunkNo = 0

        override fun process(message: BbMessage): AccountState =
            when (message) {
                is BbMessage.GetFileList -> {
                    ctx.send(BbMessage.FileList(FILE_LIST))

                    this
                }

                is BbMessage.GetFileChunk -> {
                    val offset = (fileChunkNo * MAX_CHUNK_SIZE).coerceAtMost(FILE_BUFFER.size)
                    val size = (FILE_BUFFER.size - offset).coerceAtMost(MAX_CHUNK_SIZE)

                    ctx.send(BbMessage.FileChunk(fileChunkNo, FILE_BUFFER.cursor(offset, size)))

                    if (offset + size < FILE_BUFFER.size) {
                        fileChunkNo++
                    }

                    this
                }

                is BbMessage.Disconnect -> Final(ctx)

                else -> unexpectedMessage(message)
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

    class Final(ctx: AccountContext) : AccountState(ctx), FinalServerState {
        override fun process(message: BbMessage): AccountState =
            unexpectedMessage(message)
    }
}

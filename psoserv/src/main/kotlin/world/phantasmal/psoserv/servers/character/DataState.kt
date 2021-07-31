package world.phantasmal.psoserv.servers.character

import world.phantasmal.core.math.clamp
import world.phantasmal.psolib.buffer.Buffer
import world.phantasmal.psolib.cursor.cursor
import world.phantasmal.psoserv.messages.BbAuthenticationStatus
import world.phantasmal.psoserv.messages.BbMessage
import world.phantasmal.psoserv.messages.PsoCharacter
import world.phantasmal.psoserv.servers.FinalServerState
import world.phantasmal.psoserv.servers.Server
import world.phantasmal.psoserv.servers.ServerState
import kotlin.math.min

class DataContext(
    private val sender: Server.ClientSender,
) {
    fun send(message: BbMessage, encrypt: Boolean = true) {
        sender.send(message, encrypt)
    }
}

sealed class DataState : ServerState<BbMessage, DataState>() {
    class Authentication(private val ctx: DataContext) : DataState() {
        override fun process(message: BbMessage): DataState =
            if (message is BbMessage.Authenticate) {
                // TODO: Actual authentication.
                ctx.send(
                    BbMessage.AuthenticationResponse(
                        BbAuthenticationStatus.Success,
                        message.guildCard,
                        message.teamId,
                    )
                )

                Account(ctx)
            } else {
                unexpectedMessage(message)
            }
    }

    class Account(private val ctx: DataContext) : DataState() {
        override fun process(message: BbMessage): DataState =
            if (message is BbMessage.GetAccount) {
                // TODO: Send correct guild card number and team ID.
                ctx.send(BbMessage.Account(0, 0))

                CharacterSelect(ctx)
            } else {
                unexpectedMessage(message)
            }
    }

    class CharacterSelect(private val ctx: DataContext) : DataState() {
        override fun process(message: BbMessage): DataState =
            when (message) {
                is BbMessage.CharacterSelect -> {
                    // TODO: Look up character data.
                    ctx.send(
                        BbMessage.CharacterSelectResponse(
                            PsoCharacter(
                                slot = message.slot,
                                exp = 0,
                                level = 1,
                                guildCardString = "",
                                nameColor = 0,
                                model = 0,
                                nameColorChecksum = 0,
                                sectionId = 0,
                                characterClass = 0,
                                costume = 0,
                                skin = 0,
                                face = 0,
                                head = 0,
                                hair = 0,
                                hairRed = 0,
                                hairGreen = 0,
                                hairBlue = 0,
                                propX = 1.0,
                                propY = 1.0,
                                name = "Phantasmal ${message.slot}",
                                playTime = 0,
                            )
                        )
                    )

                    this
                }

                is BbMessage.Checksum -> {
                    // TODO: Checksum checking.
                    ctx.send(BbMessage.ChecksumResponse(true))

                    DataDownload(ctx)
                }

                else -> unexpectedMessage(message)
            }
    }

    class DataDownload(private val ctx: DataContext) : DataState() {
        private val guildCardBuffer = Buffer.withSize(54672)
        private val fileBuffer = Buffer.withSize(0)
        private var fileChunkNo = 0

        override fun process(message: BbMessage): DataState =
            when (message) {
                is BbMessage.GetGuildCardHeader -> {
                    ctx.send(
                        BbMessage.GuildCardHeader(
                            guildCardBuffer.size,
                            crc32(guildCardBuffer),
                        )
                    )

                    this
                }

                is BbMessage.GetGuildCardChunk -> {
                    if (message.cont) {
                        val offset =
                            clamp(message.chunkNo * MAX_CHUNK_SIZE, 0, guildCardBuffer.size)
                        val size = min(guildCardBuffer.size - offset, MAX_CHUNK_SIZE)

                        ctx.send(
                            BbMessage.GuildCardChunk(
                                message.chunkNo,
                                guildCardBuffer.cursor(offset, size),
                            )
                        )
                    }

                    this
                }

                is BbMessage.GetFileList -> {
                    ctx.send(BbMessage.FileList())

                    this
                }

                is BbMessage.GetFileChunk -> {
                    val offset = min(fileChunkNo * MAX_CHUNK_SIZE, fileBuffer.size)
                    val size = min(fileBuffer.size - offset, MAX_CHUNK_SIZE)

                    ctx.send(BbMessage.FileChunk(fileChunkNo, fileBuffer.cursor(offset, size)))

                    if (offset + size < fileBuffer.size) {
                        fileChunkNo++
                    }

                    this
                }

                else -> unexpectedMessage(message)
            }

        private fun crc32(data: Buffer): Int {
            val cursor = data.cursor()
            var cs = 0xFFFFFFFFu

            while (cursor.hasBytesLeft()) {
                cs = cs xor cursor.uByte().toUInt()

                for (i in 0..7) {
                    cs = if ((cs and 1u) == 0u) {
                        cs shr 1
                    } else {
                        (cs shr 1) xor 0xEDB88320u
                    }
                }
            }

            return (cs xor 0xFFFFFFFFu).toInt()
        }

        companion object {
            private const val MAX_CHUNK_SIZE: Int = 0x6800
        }
    }

    object Final : DataState(), FinalServerState {
        override fun process(message: BbMessage): DataState =
            unexpectedMessage(message)
    }
}

package world.phantasmal.psoserv.servers

import world.phantasmal.core.math.clamp
import world.phantasmal.psolib.Endianness
import world.phantasmal.psolib.buffer.Buffer
import world.phantasmal.psolib.cursor.cursor
import world.phantasmal.psoserv.data.AccountData
import world.phantasmal.psoserv.data.AccountStore
import world.phantasmal.psoserv.data.LogInResult
import world.phantasmal.psoserv.encryption.BbCipher
import world.phantasmal.psoserv.encryption.Cipher
import world.phantasmal.psoserv.messages.*
import world.phantasmal.psoserv.utils.crc32Checksum

class AccountServer(
    private val accountStore: AccountStore,
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
        private var accountData: AccountData? = null
        private val guildCardBuffer = Buffer.withSize(54672)
        private var fileChunkNo = 0
        private var charSlot: Int = 0
        private var charSelected: Boolean = false

        override fun process(message: BbMessage): Boolean = when (message) {
            is BbMessage.Authenticate -> {
                val accountData = accountStore.getAccountData(message.username, message.password)
                this.accountData = accountData

                when (accountData.logIn(message.password)) {
                    LogInResult.Ok -> {
                        charSlot = message.charSlot
                        charSelected = message.charSelected

                        val account = accountData.account
                        ctx.send(
                            BbMessage.AuthData(
                                AuthStatus.Success,
                                account.guildCardNo,
                                account.teamId,
                                charSlot,
                                charSelected,
                            )
                        )

                        // When the player has selected a character, we send him the list of ships
                        // to choose from.
                        if (charSelected) {
                            ctx.send(BbMessage.ShipList(ships.map { it.uiName }))
                        }
                    }
                    LogInResult.BadPassword -> {
                        ctx.send(
                            BbMessage.AuthData(
                                AuthStatus.Nonexistent,
                                message.guildCardNo,
                                message.teamId,
                                message.charSlot,
                                message.charSelected,
                            )
                        )
                    }
                    LogInResult.AlreadyLoggedIn -> {
                        ctx.send(
                            BbMessage.AuthData(
                                AuthStatus.Error,
                                message.guildCardNo,
                                message.teamId,
                                message.charSlot,
                                message.charSelected,
                            )
                        )
                    }
                }

                true
            }

            is BbMessage.GetAccount -> {
                accountData?.account?.let {
                    ctx.send(BbMessage.Account(it.guildCardNo, it.teamId))
                }

                true
            }

            is BbMessage.CharSelect -> {
                val account = accountData?.account

                if (account != null && message.slot in account.characters.indices) {
                    if (message.selected) {
                        // Player has chosen a character.
                        charSlot = message.slot
                        charSelected = true
                        ctx.send(
                            BbMessage.AuthData(
                                AuthStatus.Success,
                                account.guildCardNo,
                                account.teamId,
                                charSlot,
                                charSelected,
                            )
                        )
                        ctx.send(
                            BbMessage.CharSelectAck(charSlot, CharSelectStatus.Select)
                        )
                    } else {
                        // Player is previewing characters.
                        val char = account.characters[message.slot]
                        ctx.send(
                            BbMessage.Char(
                                PsoCharacter(
                                    slot = message.slot,
                                    exp = char.exp,
                                    level = char.level - 1,
                                    guildCardString = "",
                                    nameColor = 0,
                                    model = 0,
                                    nameColorChecksum = 0,
                                    sectionId = char.sectionId.ordinal,
                                    characterClass = 0,
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
                                    name = char.name,
                                    playTime = 0,
                                )
                            )
                        )
                    }
                } else {
                    ctx.send(
                        BbMessage.CharSelectAck(message.slot, CharSelectStatus.Nonexistent)
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

                    // Log out and disconnect.
                    logOut()
                    false
                } else {
                    true
                }
            }

            is BbMessage.Disconnect -> {
                // Log out and disconnect.
                logOut()
                false
            }

            else -> ctx.unexpectedMessage(message)
        }

        override fun connectionClosed() {
            logOut()
        }

        private fun logOut() {
            try {
                accountData?.let(AccountData::logOut)
            } finally {
                accountData = null
            }
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

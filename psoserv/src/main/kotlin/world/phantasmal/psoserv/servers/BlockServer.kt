package world.phantasmal.psoserv.servers

import world.phantasmal.psoserv.data.AccountData
import world.phantasmal.psoserv.data.AccountStore
import world.phantasmal.psoserv.data.LogInResult
import world.phantasmal.psoserv.encryption.BbCipher
import world.phantasmal.psoserv.encryption.Cipher
import world.phantasmal.psoserv.messages.*

class BlockServer(
    private val accountStore: AccountStore,
    name: String,
    bindPair: Inet4Pair,
    private val blockId: Int,
) : GameServer<BbMessage>(name, bindPair) {

    override val messageDescriptor = BbMessageDescriptor

    override fun createCipher() = BbCipher()

    override fun createClientReceiver(
        ctx: ClientContext<BbMessage>,
        serverCipher: Cipher,
        clientCipher: Cipher,
    ): ClientReceiver<BbMessage> = object : ClientReceiver<BbMessage> {
        private var accountData: AccountData? = null

        override fun process(message: BbMessage): Boolean = when (message) {
            is BbMessage.Authenticate -> {
                val accountData = accountStore.getAccountData(message.username, message.password)
                this.accountData = accountData

                when (accountData.logIn(message.password)) {
                    LogInResult.Ok -> {
                        val char = accountData.account.characters.getOrNull(message.charSlot)

                        if (char == null) {
                            ctx.send(
                                BbMessage.AuthData(
                                    AuthStatus.Nonexistent,
                                    message.guildCardNo,
                                    message.teamId,
                                    message.charSlot,
                                    message.charSelected,
                                )
                            )
                        } else {
                            accountData.setPlaying(char, blockId)
                            val account = accountData.account
                            ctx.send(
                                BbMessage.AuthData(
                                    AuthStatus.Success,
                                    account.guildCardNo,
                                    account.teamId,
                                    message.charSlot,
                                    message.charSelected,
                                )
                            )

                            ctx.send(BbMessage.LobbyList())
                            ctx.send(
                                BbMessage.FullCharacterData(
                                    PsoCharData(
                                        hp = 0,
                                        level = char.level - 1,
                                        exp = char.exp,
                                        sectionId = char.sectionId.ordinal.toByte(),
                                        charClass = 0,
                                        name = char.name,
                                    ),
                                )
                            )
                            ctx.send(BbMessage.GetCharData())
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

            is BbMessage.CharData -> {
                ctx.send(
                    BbMessage.JoinLobby(
                        clientId = 0u,
                        leaderId = 0u,
                        disableUdp = true,
                        lobbyNo = 0u,
                        blockNo = blockId.toUShort(),
                        event = 0u,
                        players = accountStore.getPlayingAccountsForBlock(blockId).map {
                            LobbyPlayer(
                                playerTag = 0,
                                guildCardNo = it.account.guildCardNo,
                                clientId = 0,
                                charName = it.char.name,
                            )
                        }
                    )
                )

                true
            }

            is BbMessage.CreateParty -> {
                true
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
}

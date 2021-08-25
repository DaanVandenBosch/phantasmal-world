package world.phantasmal.psoserv.servers

import world.phantasmal.psolib.Episode
import world.phantasmal.psoserv.data.*
import world.phantasmal.psoserv.encryption.BbCipher
import world.phantasmal.psoserv.encryption.Cipher
import world.phantasmal.psoserv.messages.*

class BlockServer(
    private val store: Store,
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
        private var client: Client? = null

        override fun process(message: BbMessage): Boolean {
            when (message) {
                is BbMessage.Authenticate -> {
                    val result = store.authenticate(
                        message.username,
                        message.password,
                        ctx::send,
                    )

                    when (result) {
                        is AuthResult.Ok -> {
                            client = result.client

                            val account = result.client.account
                            val char = account.characters.getOrNull(message.charSlot)

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
                                result.client.setPlaying(char, blockId)
                                ctx.send(
                                    BbMessage.AuthData(
                                        AuthStatus.Success,
                                        account.guildCardNo,
                                        account.teamId,
                                        message.charSlot,
                                        message.charSelected,
                                    )
                                )

                                val lobbies = store.getLobbies(blockId)
                                ctx.send(BbMessage.LobbyList(lobbies.map { it.id }))

                                ctx.send(
                                    BbMessage.FullCharacterData(
                                        // TODO: Fill in char data correctly.
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
                        AuthResult.BadPassword -> {
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
                        AuthResult.AlreadyLoggedIn -> {
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

                    return true
                }

                is BbMessage.CharData -> {
                    val client = client
                        ?: return false

                    val lobby = store.joinFirstAvailableLobby(blockId, client)
                        ?: return false

                    val clientId = client.id

                    ctx.send(
                        BbMessage.JoinLobby(
                            clientId = clientId,
                            leaderId = 0, // TODO: What should leaderId be in lobbies?
                            disableUdp = true,
                            lobbyNo = lobby.id.toUByte(),
                            blockNo = blockId.toUShort(),
                            event = 0u,
                            players = lobby.getClients().mapNotNull { c ->
                                c.playing?.let {
                                    LobbyPlayer(
                                        playerTag = 0,
                                        guildCardNo = it.account.guildCardNo,
                                        clientId = c.id,
                                        charName = it.char.name,
                                    )
                                }
                            }
                        )
                    )

                    // Notify other clients.
                    client.playing?.let { playingAccount ->
                        val joinedMessage = BbMessage.JoinedLobby(
                            clientId = clientId,
                            leaderId = 0, // TODO: What should leaderId be in lobbies?
                            disableUdp = true,
                            lobbyNo = lobby.id.toUByte(),
                            blockNo = blockId.toUShort(),
                            event = 0u,
                            player = LobbyPlayer(
                                playerTag = 0,
                                guildCardNo = playingAccount.account.guildCardNo,
                                clientId = clientId,
                                charName = playingAccount.char.name,
                            ),
                        )
                        lobby.broadcastMessage(joinedMessage, exclude = client)
                    }

                    return true
                }

                is BbMessage.CreateParty -> {
                    val client = client
                        ?: return false
                    val difficulty = when (message.difficulty.toInt()) {
                        0 -> Difficulty.Normal
                        1 -> Difficulty.Hard
                        2 -> Difficulty.VHard
                        3 -> Difficulty.Ultimate
                        else -> return false
                    }
                    val episode = when (message.episode.toInt()) {
                        1 -> Episode.I
                        2 -> Episode.II
                        3 -> Episode.IV
                        else -> return false
                    }
                    val mode = when {
                        message.battleMode -> Mode.Battle
                        message.challengeMode -> Mode.Challenge
                        message.soloMode -> Mode.Solo
                        else -> Mode.Normal
                    }

                    val result = store.createAndJoinParty(
                        blockId,
                        message.name,
                        message.password,
                        difficulty,
                        episode,
                        mode,
                        client,
                    )

                    when (result) {
                        is CreateAndJoinPartyResult.Ok -> {
                            val party = result.party
                            val details = party.details

                            // TODO: Send lobby leave message to all clients.

                            ctx.send(BbMessage.JoinParty(
                                players = party.getClients().mapNotNull { c ->
                                    c.playing?.let {
                                        LobbyPlayer(
                                            playerTag = 0,
                                            guildCardNo = it.account.guildCardNo,
                                            clientId = c.id,
                                            charName = it.char.name,
                                        )
                                    }
                                },
                                clientId = client.id,
                                leaderId = party.leaderId,
                                difficulty = when (details.difficulty) {
                                    Difficulty.Normal -> 0
                                    Difficulty.Hard -> 1
                                    Difficulty.VHard -> 2
                                    Difficulty.Ultimate -> 3
                                },
                                battleMode = details.mode == Mode.Battle,
                                event = 0,
                                sectionId = 0,
                                challengeMode = details.mode == Mode.Challenge,
                                prngSeed = 0,
                                episode = when (details.episode) {
                                    Episode.I -> 1
                                    Episode.II -> 2
                                    Episode.IV -> 3
                                },
                                soloMode = details.mode == Mode.Solo,
                            ))

                            // TODO: Send player join message to other clients.

                            return true
                        }
                        is CreateAndJoinPartyResult.NameInUse -> {
                            // TODO: Just send message instead of disconnecting.
                            return false
                        }
                        is CreateAndJoinPartyResult.AlreadyInParty -> {
                            logger.warn {
                                "${client.account} tried to create a party while in a party."
                            }
                            return true
                        }
                    }
                }

                is BbMessage.Broadcast -> {
                    // TODO: Verify broadcast messages.
                    client?.lop?.broadcastMessage(message, client)
                    return true
                }

                is BbMessage.Disconnect -> {
                    // Log out and disconnect.
                    logOut()
                    return false
                }

                else -> return ctx.unexpectedMessage(message)
            }
        }

        override fun connectionClosed() {
            logOut()
        }

        private fun logOut() {
            try {
                client?.let(store::logOut)
            } finally {
                client = null
            }
        }
    }
}

package world.phantasmal.psoserv.servers

import world.phantasmal.psoserv.encryption.BbCipher
import world.phantasmal.psoserv.encryption.Cipher
import world.phantasmal.psoserv.messages.AuthStatus
import world.phantasmal.psoserv.messages.BbMessage
import world.phantasmal.psoserv.messages.BbMessageDescriptor
import world.phantasmal.psoserv.messages.PsoCharData

class BlockServer(
    name: String,
    bindPair: Inet4Pair,
    private val blockNo: Int,
) : GameServer<BbMessage>(name, bindPair) {

    override val messageDescriptor = BbMessageDescriptor

    override fun createCipher() = BbCipher()

    override fun createClientReceiver(
        ctx: ClientContext<BbMessage>,
        serverCipher: Cipher,
        clientCipher: Cipher,
    ): ClientReceiver<BbMessage> = object : ClientReceiver<BbMessage> {
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
                ctx.send(
                    BbMessage.AuthData(
                        AuthStatus.Success,
                        message.guildCard,
                        message.teamId,
                        message.charSlot,
                        message.charSelected,
                    )
                )
                ctx.send(BbMessage.LobbyList())
                ctx.send(
                    BbMessage.FullCharacterData(
                        PsoCharData(
                            hp = 20,
                            level = 0,
                            exp = 0,
                        )
                    )
                )
                ctx.send(BbMessage.GetCharData())

                true
            }

            is BbMessage.CharData -> {
                ctx.send(
                    BbMessage.JoinLobby(
                        clientId = 0u,
                        leaderId = 0u,
                        disableUdp = true,
                        lobbyNo = 0u,
                        blockNo = blockNo.toUShort(),
                        event = 0u,
                    )
                )

                true
            }

            else -> ctx.unexpectedMessage(message)
        }
    }
}

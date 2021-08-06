package world.phantasmal.psoserv.servers

import world.phantasmal.psoserv.encryption.BbCipher
import world.phantasmal.psoserv.encryption.Cipher
import world.phantasmal.psoserv.messages.AuthStatus
import world.phantasmal.psoserv.messages.BbMessage
import world.phantasmal.psoserv.messages.BbMessageDescriptor
import java.net.Inet4Address

class AuthServer(
    bindPair: Inet4Pair,
    private val accountServerAddress: Inet4Address,
    private val accountServerPort: Int,
) : GameServer<BbMessage>("auth", bindPair) {

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
                        slot = 0,
                        selected = false,
                    )
                )
                ctx.send(
                    BbMessage.Redirect(accountServerAddress.address, accountServerPort.toUShort())
                )

                // Disconnect.
                false
            }

            else -> ctx.unexpectedMessage(message)
        }
    }
}

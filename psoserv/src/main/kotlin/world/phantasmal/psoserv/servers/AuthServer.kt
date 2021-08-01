package world.phantasmal.psoserv.servers

import world.phantasmal.psoserv.encryption.BbCipher
import world.phantasmal.psoserv.encryption.Cipher
import world.phantasmal.psoserv.messages.BbAuthStatus
import world.phantasmal.psoserv.messages.BbMessage
import world.phantasmal.psoserv.messages.BbMessageDescriptor
import java.net.Inet4Address

class AuthServer(
    name: String,
    bindPair: Inet4Pair,
    private val accountServerAddress: Inet4Address,
    private val accountServerPort: Int,
) : GameServer<BbMessage>(name, bindPair) {

    override val messageDescriptor = BbMessageDescriptor

    override fun createCipher() = BbCipher()

    override fun createClientReceiver(
        sender: ClientSender<BbMessage>,
        serverCipher: Cipher,
        clientCipher: Cipher,
    ): ClientReceiver<BbMessage> = object : ClientReceiver<BbMessage> {
        init {
            sender.send(
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
                send(
                    BbMessage.AuthData(
                        BbAuthStatus.Success,
                        message.guildCard,
                        message.teamId,
                        slot = 0,
                        selected = false,
                    )
                )
                send(
                    BbMessage.Redirect(accountServerAddress.address, accountServerPort)
                )

                // Disconnect.
                false
            }

            else -> unexpectedMessage(message)
        }

        private fun send(message: BbMessage) {
            sender.send(message)
        }
    }
}

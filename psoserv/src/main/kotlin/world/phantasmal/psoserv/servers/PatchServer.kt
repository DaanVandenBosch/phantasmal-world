package world.phantasmal.psoserv.servers

import world.phantasmal.psoserv.encryption.Cipher
import world.phantasmal.psoserv.encryption.PcCipher
import world.phantasmal.psoserv.messages.PcMessage
import world.phantasmal.psoserv.messages.PcMessageDescriptor

class PatchServer(
    bindPair: Inet4Pair,
    private val welcomeMessage: String,
) : GameServer<PcMessage>("patch", bindPair) {

    override val messageDescriptor = PcMessageDescriptor

    override fun createCipher() = PcCipher()

    override fun createClientReceiver(
        ctx: ClientContext<PcMessage>,
        serverCipher: Cipher,
        clientCipher: Cipher,
    ): ClientReceiver<PcMessage> = object : ClientReceiver<PcMessage> {
        init {
            ctx.send(
                PcMessage.InitEncryption(
                    "Patch Server. Copyright SonicTeam, LTD. 2001",
                    serverCipher.key,
                    clientCipher.key,
                ),
                encrypt = false,
            )
        }

        override fun process(message: PcMessage): Boolean = when (message) {
            is PcMessage.InitEncryption -> {
                ctx.send(PcMessage.Login())

                true
            }

            is PcMessage.Login -> {
                ctx.send(PcMessage.WelcomeMessage(welcomeMessage))
                ctx.send(PcMessage.PatchListStart())
                ctx.send(PcMessage.PatchListEnd())

                true
            }

            is PcMessage.PatchListOk -> {
                ctx.send(PcMessage.PatchDone())

                // Disconnect.
                false
            }

            else -> ctx.unexpectedMessage(message)
        }
    }
}

package world.phantasmal.psoserv.servers.patch

import world.phantasmal.psoserv.encryption.Cipher
import world.phantasmal.psoserv.encryption.PcCipher
import world.phantasmal.psoserv.messages.PcMessage
import world.phantasmal.psoserv.messages.PcMessageDescriptor
import world.phantasmal.psoserv.servers.GameServer
import world.phantasmal.psoserv.servers.Inet4Pair
import world.phantasmal.psoserv.servers.SocketSender

class PatchServer(
    name: String,
    bindPair: Inet4Pair,
    private val welcomeMessage: String,
) : GameServer<PcMessage, PatchState>(name, bindPair) {

    override val messageDescriptor = PcMessageDescriptor

    override fun createCipher() = PcCipher()

    override fun initializeState(
        sender: SocketSender<PcMessage>,
        serverCipher: Cipher,
        clientCipher: Cipher,
    ): PatchState {
        val ctx = PatchContext(logger, sender, welcomeMessage)

        ctx.send(
            PcMessage.InitEncryption(
                "Patch Server. Copyright SonicTeam, LTD. 2001",
                serverCipher.key,
                clientCipher.key,
            ),
            encrypt = false,
        )

        return PatchState.Welcome(ctx)
    }
}

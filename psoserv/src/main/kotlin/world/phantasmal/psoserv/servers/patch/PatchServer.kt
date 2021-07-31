package world.phantasmal.psoserv.servers.patch

import mu.KotlinLogging
import world.phantasmal.psolib.buffer.Buffer
import world.phantasmal.psoserv.encryption.PcCipher
import world.phantasmal.psoserv.messages.Header
import world.phantasmal.psoserv.messages.PcMessage
import world.phantasmal.psoserv.servers.Server
import java.net.InetAddress

class PatchServer(address: InetAddress, port: Int, private val welcomeMessage: String) :
    Server<PcMessage, PatchState>(KotlinLogging.logger {}, address, port) {

    override fun createCipher() = PcCipher()

    override fun initializeState(sender: ClientSender): PatchState {
        val ctx = PatchContext(sender, welcomeMessage)

        ctx.send(
            PcMessage.InitEncryption(
                "Patch Server. Copyright SonicTeam, LTD. 2001",
                sender.serverCipher.key,
                sender.clientCipher.key,
            ),
            encrypt = false,
        )

        return PatchState.Welcome(ctx)
    }

    override fun readHeader(buffer: Buffer): Header =
        PcMessage.readHeader(buffer)

    override fun readMessage(buffer: Buffer): PcMessage =
        PcMessage.fromBuffer(buffer)
}

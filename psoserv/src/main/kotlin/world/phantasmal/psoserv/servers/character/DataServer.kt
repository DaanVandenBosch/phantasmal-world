package world.phantasmal.psoserv.servers.character

import mu.KotlinLogging
import world.phantasmal.psoserv.messages.BbMessage
import world.phantasmal.psoserv.servers.BbServer
import java.net.InetAddress

class DataServer(address: InetAddress, port: Int) :
    BbServer<DataState>(KotlinLogging.logger {}, address, port) {

    override fun initializeState(sender: ClientSender): DataState {
        val ctx = DataContext(sender)

        ctx.send(
            BbMessage.InitEncryption(
                "Phantasy Star Online Blue Burst Game Server. Copyright 1999-2004 SONICTEAM.",
                sender.serverCipher.key,
                sender.clientCipher.key,
            ),
            encrypt = false,
        )

        return DataState.Authentication(ctx)
    }
}

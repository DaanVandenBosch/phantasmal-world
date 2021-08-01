package world.phantasmal.psoserv.servers.character

import world.phantasmal.psoserv.encryption.Cipher
import world.phantasmal.psoserv.messages.BbMessage
import world.phantasmal.psoserv.servers.BbServer
import world.phantasmal.psoserv.servers.Inet4Pair
import world.phantasmal.psoserv.servers.SocketSender

class DataServer(
    name: String,
    bindPair: Inet4Pair,
) : BbServer<DataState>(name, bindPair) {

    override fun initializeState(
        sender: SocketSender<BbMessage>,
        serverCipher: Cipher,
        clientCipher: Cipher,
    ): DataState {
        val ctx = DataContext(logger, sender)

        ctx.send(
            BbMessage.InitEncryption(
                "Phantasy Star Online Blue Burst Game Server. Copyright 1999-2004 SONICTEAM.",
                serverCipher.key,
                clientCipher.key,
            ),
            encrypt = false,
        )

        return DataState.Authentication(ctx)
    }
}

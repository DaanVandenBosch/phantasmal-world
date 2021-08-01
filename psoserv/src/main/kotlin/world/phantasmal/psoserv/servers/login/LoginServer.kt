package world.phantasmal.psoserv.servers.login

import world.phantasmal.psoserv.encryption.Cipher
import world.phantasmal.psoserv.messages.BbMessage
import world.phantasmal.psoserv.servers.BbServer
import world.phantasmal.psoserv.servers.Inet4Pair
import world.phantasmal.psoserv.servers.SocketSender
import java.net.Inet4Address

class LoginServer(
    name: String,
    bindPair: Inet4Pair,
    private val dataServerAddress: Inet4Address,
    private val dataServerPort: Int,
) : BbServer<LoginState>(name, bindPair) {

    override fun initializeState(
        sender: SocketSender<BbMessage>,
        serverCipher: Cipher,
        clientCipher: Cipher,
    ): LoginState {
        val ctx = LoginContext(logger, sender, dataServerAddress.address, dataServerPort)

        ctx.send(
            BbMessage.InitEncryption(
                "Phantasy Star Online Blue Burst Game Server. Copyright 1999-2004 SONICTEAM.",
                serverCipher.key,
                clientCipher.key,
            ),
            encrypt = false,
        )

        return LoginState.Authentication(ctx)
    }
}

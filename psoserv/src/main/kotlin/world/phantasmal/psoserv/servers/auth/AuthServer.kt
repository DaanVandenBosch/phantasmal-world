package world.phantasmal.psoserv.servers.auth

import world.phantasmal.psoserv.encryption.Cipher
import world.phantasmal.psoserv.messages.BbMessage
import world.phantasmal.psoserv.servers.BbServer
import world.phantasmal.psoserv.servers.Inet4Pair
import world.phantasmal.psoserv.servers.SocketSender
import java.net.Inet4Address

class AuthServer(
    name: String,
    bindPair: Inet4Pair,
    private val dataServerAddress: Inet4Address,
    private val dataServerPort: Int,
) : BbServer<AuthState>(name, bindPair) {

    override fun initializeState(
        sender: SocketSender<BbMessage>,
        serverCipher: Cipher,
        clientCipher: Cipher,
    ): AuthState {
        val ctx = AuthContext(logger, sender, dataServerAddress.address, dataServerPort)

        ctx.send(
            BbMessage.InitEncryption(
                "Phantasy Star Online Blue Burst Game Server. Copyright 1999-2004 SONICTEAM.",
                serverCipher.key,
                clientCipher.key,
            ),
            encrypt = false,
        )

        return AuthState.Authentication(ctx)
    }
}

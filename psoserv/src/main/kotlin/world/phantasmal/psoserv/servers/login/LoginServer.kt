package world.phantasmal.psoserv.servers.login

import mu.KotlinLogging
import world.phantasmal.psoserv.messages.BbMessage
import world.phantasmal.psoserv.servers.BbServer
import java.net.Inet4Address
import java.net.InetAddress

class LoginServer(
    address: InetAddress,
    port: Int,
    private val characterServerAddress: Inet4Address,
    private val characterServerPort: Int,
) : BbServer<LoginState>(KotlinLogging.logger {}, address, port) {

    override fun initializeState(sender: ClientSender): LoginState {
        val ctx = LoginContext(sender, characterServerAddress.address, characterServerPort)

        ctx.send(
            BbMessage.InitEncryption(
                "Phantasy Star Online Blue Burst Game Server. Copyright 1999-2004 SONICTEAM.",
                sender.serverCipher.key,
                sender.clientCipher.key,
            ),
            encrypt = false,
        )

        return LoginState.Authentication(ctx)
    }
}

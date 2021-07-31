package world.phantasmal.psoserv.servers.login

import world.phantasmal.psoserv.messages.BbAuthenticationStatus
import world.phantasmal.psoserv.messages.BbMessage
import world.phantasmal.psoserv.servers.FinalServerState
import world.phantasmal.psoserv.servers.Server
import world.phantasmal.psoserv.servers.ServerState

class LoginContext(
    private val sender: Server.ClientSender,
    val characterServerAddress: ByteArray,
    val characterServerPort: Int,
) {
    fun send(message: BbMessage, encrypt: Boolean = true) {
        sender.send(message, encrypt)
    }
}

sealed class LoginState : ServerState<BbMessage, LoginState>() {
    class Authentication(private val ctx: LoginContext) : LoginState() {
        override fun process(message: BbMessage): LoginState =
            if (message is BbMessage.Authenticate) {
                // TODO: Actual authentication.
                ctx.send(
                    BbMessage.AuthenticationResponse(
                        BbAuthenticationStatus.Success,
                        message.guildCard,
                        message.teamId,
                    )
                )
                ctx.send(
                    BbMessage.Redirect(
                        ctx.characterServerAddress,
                        ctx.characterServerPort,
                    )
                )

                Final
            } else {
                unexpectedMessage(message)
            }
    }

    object Final : LoginState(), FinalServerState {
        override fun process(message: BbMessage): LoginState =
            unexpectedMessage(message)
    }
}

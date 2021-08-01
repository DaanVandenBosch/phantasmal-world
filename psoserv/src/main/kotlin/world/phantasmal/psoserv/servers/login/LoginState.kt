package world.phantasmal.psoserv.servers.login

import mu.KLogger
import world.phantasmal.psoserv.messages.BbAuthenticationStatus
import world.phantasmal.psoserv.messages.BbMessage
import world.phantasmal.psoserv.servers.FinalServerState
import world.phantasmal.psoserv.servers.ServerState
import world.phantasmal.psoserv.servers.ServerStateContext
import world.phantasmal.psoserv.servers.SocketSender

class LoginContext(
    logger: KLogger,
    socketSender: SocketSender<BbMessage>,
    val characterServerAddress: ByteArray,
    val characterServerPort: Int,
) : ServerStateContext<BbMessage>(logger, socketSender)

sealed class LoginState(ctx: LoginContext) : ServerState<BbMessage, LoginContext, LoginState>(ctx) {
    class Authentication(ctx: LoginContext) : LoginState(ctx) {
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

                Final(ctx)
            } else {
                unexpectedMessage(message)
            }
    }

    class Final(ctx: LoginContext) : LoginState(ctx), FinalServerState {
        override fun process(message: BbMessage): LoginState =
            unexpectedMessage(message)
    }
}

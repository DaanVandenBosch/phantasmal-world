package world.phantasmal.psoserv.servers.auth

import mu.KLogger
import world.phantasmal.psoserv.messages.BbAuthenticationStatus
import world.phantasmal.psoserv.messages.BbMessage
import world.phantasmal.psoserv.servers.FinalServerState
import world.phantasmal.psoserv.servers.ServerState
import world.phantasmal.psoserv.servers.ServerStateContext
import world.phantasmal.psoserv.servers.SocketSender

class AuthContext(
    logger: KLogger,
    socketSender: SocketSender<BbMessage>,
    val accountServerAddress: ByteArray,
    val accountServerPort: Int,
) : ServerStateContext<BbMessage>(logger, socketSender)

sealed class AuthState(ctx: AuthContext) :
    ServerState<BbMessage, AuthContext, AuthState>(ctx) {

    class Authentication(ctx: AuthContext) : AuthState(ctx) {
        override fun process(message: BbMessage): AuthState =
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
                        ctx.accountServerAddress,
                        ctx.accountServerPort,
                    )
                )

                Final(ctx)
            } else {
                unexpectedMessage(message)
            }
    }

    class Final(ctx: AuthContext) : AuthState(ctx), FinalServerState {
        override fun process(message: BbMessage): AuthState =
            unexpectedMessage(message)
    }
}

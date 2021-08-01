package world.phantasmal.psoserv.servers.patch

import mu.KLogger
import world.phantasmal.psoserv.messages.PcMessage
import world.phantasmal.psoserv.servers.FinalServerState
import world.phantasmal.psoserv.servers.ServerState
import world.phantasmal.psoserv.servers.ServerStateContext
import world.phantasmal.psoserv.servers.SocketSender

class PatchContext(
    logger: KLogger,
    socketSender: SocketSender<PcMessage>,
    val welcomeMessage: String,
) : ServerStateContext<PcMessage>(logger, socketSender)

sealed class PatchState(ctx: PatchContext) : ServerState<PcMessage, PatchContext, PatchState>(ctx) {
    class Welcome(ctx: PatchContext) : PatchState(ctx) {
        override fun process(message: PcMessage): PatchState =
            if (message is PcMessage.InitEncryption) {
                ctx.send(PcMessage.Login())

                Login(ctx)
            } else {
                unexpectedMessage(message)
            }
    }

    class Login(ctx: PatchContext) : PatchState(ctx) {
        override fun process(message: PcMessage): PatchState =
            if (message is PcMessage.Login) {
                ctx.send(PcMessage.WelcomeMessage(ctx.welcomeMessage))
                ctx.send(PcMessage.PatchListStart())
                ctx.send(PcMessage.PatchListEnd())

                PatchListDone(ctx)
            } else {
                unexpectedMessage(message)
            }
    }

    class PatchListDone(ctx: PatchContext) : PatchState(ctx) {
        override fun process(message: PcMessage): PatchState =
            if (message is PcMessage.PatchListOk) {
                ctx.send(PcMessage.PatchDone())

                Final(ctx)
            } else {
                unexpectedMessage(message)
            }
    }

    class Final(ctx: PatchContext) : PatchState(ctx), FinalServerState {
        override fun process(message: PcMessage): PatchState =
            unexpectedMessage(message)
    }
}

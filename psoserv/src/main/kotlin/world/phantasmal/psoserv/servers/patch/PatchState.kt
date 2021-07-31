package world.phantasmal.psoserv.servers.patch

import world.phantasmal.psoserv.messages.PcMessage
import world.phantasmal.psoserv.servers.FinalServerState
import world.phantasmal.psoserv.servers.Server
import world.phantasmal.psoserv.servers.ServerState

class PatchContext(
    private val sender: Server.ClientSender,
    val welcomeMessage: String,
) {
    fun send(message: PcMessage, encrypt: Boolean = true) {
        sender.send(message, encrypt)
    }
}

sealed class PatchState : ServerState<PcMessage, PatchState>() {
    class Welcome(private val ctx: PatchContext) : PatchState() {
        override fun process(message: PcMessage): PatchState =
            if (message is PcMessage.InitEncryption) {
                ctx.send(PcMessage.Login())

                Login(ctx)
            } else {
                unexpectedMessage(message)
            }
    }

    class Login(private val ctx: PatchContext) : PatchState() {
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

    class PatchListDone(private val ctx: PatchContext) : PatchState() {
        override fun process(message: PcMessage): PatchState =
            if (message is PcMessage.PatchListOk) {
                ctx.send(PcMessage.PatchDone())

                Final
            } else {
                unexpectedMessage(message)
            }
    }

    object Final : PatchState(), FinalServerState {
        override fun process(message: PcMessage): PatchState =
            unexpectedMessage(message)
    }
}

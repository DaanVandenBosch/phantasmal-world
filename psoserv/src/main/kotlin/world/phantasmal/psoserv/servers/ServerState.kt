package world.phantasmal.psoserv.servers

import mu.KLogger
import world.phantasmal.psoserv.messages.Message

abstract class ServerStateContext<MessageType : Message>(
    val logger: KLogger,
    private val socketSender: SocketSender<MessageType>,
) {
    fun send(message: MessageType, encrypt: Boolean = true) {
        socketSender.sendMessage(message, encrypt)
    }
}

abstract class ServerState<MessageType, ContextType, Self>(
    protected val ctx: ContextType,
) where MessageType : Message,
        ContextType : ServerStateContext<MessageType>,
        Self : ServerState<MessageType, ContextType, Self> {

    init {
        ctx.logger.debug { "Transitioning to ${this::class.simpleName} state." }
    }

    abstract fun process(message: MessageType): Self

    protected fun unexpectedMessage(message: MessageType): Self {
        ctx.logger.debug { "Unexpected message: $message." }
        @Suppress("UNCHECKED_CAST")
        return this as Self
    }
}

interface FinalServerState

package world.phantasmal.psoserv.servers

import mu.KotlinLogging
import world.phantasmal.psoserv.messages.Message

abstract class ServerState<ClientMsgType : Message, Self : ServerState<ClientMsgType, Self>> {
    private val logger = KotlinLogging.logger(this::class.qualifiedName!!)

    init {
        logger.trace { "Transitioning to ${this::class.simpleName}." }
    }

    abstract fun process(message: ClientMsgType): Self

    protected fun unexpectedMessage(message: ClientMsgType): Self {
        logger.debug { "Unexpected message: $message." }
        @Suppress("UNCHECKED_CAST")
        return this as Self
    }
}

interface FinalServerState

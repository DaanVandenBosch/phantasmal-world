package world.phantasmal.psoserv.servers

import world.phantasmal.psoserv.messages.Message

interface SocketSender<MessageType : Message> {
    fun sendMessage(message: MessageType, encrypt: Boolean = true)
}

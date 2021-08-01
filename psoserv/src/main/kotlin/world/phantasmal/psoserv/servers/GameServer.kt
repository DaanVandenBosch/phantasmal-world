package world.phantasmal.psoserv.servers

import world.phantasmal.psoserv.encryption.Cipher
import world.phantasmal.psoserv.messages.Message
import world.phantasmal.psoserv.messages.MessageDescriptor
import java.net.Socket

interface ClientReceiver<MessageType : Message> {
    fun process(message: MessageType): Boolean
}

interface ClientSender<MessageType : Message> {
    fun send(message: MessageType, encrypt: Boolean = true)
}

abstract class GameServer<MessageType : Message>(
    name: String,
    bindPair: Inet4Pair,
) : Server(name, bindPair) {

    private var connectionCounter = 0

    protected abstract val messageDescriptor: MessageDescriptor<MessageType>

    override fun clientConnected(clientSocket: Socket) {
        // Handle each client connection in its own thread.
        val thread = Thread { GameClientHandler(clientSocket).listen() }
        thread.name = "${name}_client_${connectionCounter++}"
        thread.start()
    }

    protected abstract fun createCipher(): Cipher

    protected abstract fun createClientReceiver(
        sender: ClientSender<MessageType>,
        serverCipher: Cipher,
        clientCipher: Cipher,
    ): ClientReceiver<MessageType>

    protected fun unexpectedMessage(message: MessageType): Boolean {
        logger.debug { "Unexpected message: $message." }
        return true
    }

    private inner class GameClientHandler(socket: Socket) :
        SocketHandler<MessageType>(logger, socket) {

        private val serverCipher = createCipher()
        private val clientCipher = createCipher()

        private val handler: ClientReceiver<MessageType> =
            createClientReceiver(
                object : ClientSender<MessageType> {
                    override fun send(message: MessageType, encrypt: Boolean) {
                        sendMessage(message, encrypt)
                    }
                },
                serverCipher,
                clientCipher,
            )

        override val messageDescriptor = this@GameServer.messageDescriptor

        override val readDecryptCipher = clientCipher
        override val readEncryptCipher: Cipher? = null
        override val writeEncryptCipher: Cipher = serverCipher

        override fun processMessage(message: MessageType): ProcessResult =
            if (handler.process(message)) {
                ProcessResult.Ok
            } else {
                // Give the client some time to disconnect.
                Thread.sleep(100)

                // Close the connection.
                ProcessResult.Done
            }
    }
}

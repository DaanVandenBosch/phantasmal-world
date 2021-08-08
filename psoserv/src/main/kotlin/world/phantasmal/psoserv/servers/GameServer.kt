package world.phantasmal.psoserv.servers

import mu.KLogger
import world.phantasmal.psoserv.encryption.Cipher
import world.phantasmal.psoserv.messages.Message
import world.phantasmal.psoserv.messages.MessageDescriptor
import java.net.Socket

abstract class GameServer<MessageType : Message>(
    name: String,
    bindPair: Inet4Pair,
) : Server(name, bindPair) {

    private var connectionCounter = 0

    protected abstract val messageDescriptor: MessageDescriptor<MessageType>

    override fun clientConnected(clientSocket: Socket) {
        // Handle each client connection in its own thread.
        val client = "${name}_client_${connectionCounter++}"
        val thread = Thread { GameClientHandler(client, clientSocket).listen() }
        thread.name = client
        thread.start()
    }

    protected abstract fun createCipher(): Cipher

    protected abstract fun createClientReceiver(
        ctx: ClientContext<MessageType>,
        serverCipher: Cipher,
        clientCipher: Cipher,
    ): ClientReceiver<MessageType>

    protected interface ClientReceiver<MessageType : Message> {
        fun process(message: MessageType): Boolean
        fun connectionClosed() {}
    }

    protected class ClientContext<MessageType : Message>(
        private val logger: KLogger,
        private val handler: SocketHandler<MessageType>,
    ) {
        fun send(message: MessageType, encrypt: Boolean = true) {
            handler.sendMessage(message, encrypt)
        }

        fun unexpectedMessage(message: MessageType): Boolean {
            logger.debug { "Unexpected message: $message." }
            return true
        }
    }

    private inner class GameClientHandler(client: String, socket: Socket) :
        SocketHandler<MessageType>(client, socket) {

        private val serverCipher = createCipher()
        private val clientCipher = createCipher()

        private val clientContext = ClientContext(logger, this)
        private val receiver = createClientReceiver(clientContext, serverCipher, clientCipher)

        override val messageDescriptor = this@GameServer.messageDescriptor

        override val readDecryptCipher = clientCipher
        override val readEncryptCipher: Cipher? = null
        override val writeEncryptCipher: Cipher = serverCipher

        override fun processMessage(message: MessageType): ProcessResult =
            if (receiver.process(message)) {
                ProcessResult.Ok
            } else {
                // Give the client some time to disconnect.
                Thread.sleep(100)

                // Close the connection.
                ProcessResult.Done
            }

        override fun socketClosed() {
            receiver.connectionClosed()
        }
    }
}

package world.phantasmal.psoserv.servers

import world.phantasmal.psolib.buffer.Buffer
import world.phantasmal.psoserv.encryption.Cipher
import world.phantasmal.psoserv.messages.*
import java.net.Socket

class ProxyServer(
    name: String,
    bindPair: Inet4Pair,
    private val remotePair: Inet4Pair,
    private val messageDescriptor: MessageDescriptor<Message>,
    private val createCipher: (key: ByteArray) -> Cipher,
    private val redirectMap: Map<Inet4Pair, Inet4Pair> = emptyMap(),
) : Server(name, bindPair) {

    override fun clientConnected(clientSocket: Socket) {
        val serverSocket = Socket(remotePair.address, remotePair.port)
        logger.info {
            "Connected to server ${serverSocket.inetAddress}:${serverSocket.port}."
        }

        // Listen to server on this thread.
        // Don't start listening to the client until encryption is initialized.
        ServerHandler(serverSocket, clientSocket).listen()
    }

    private inner class ServerHandler(
        serverSocket: Socket,
        private val clientSocket: Socket,
    ) : ProxySocketHandler("${name}_server", serverSocket) {

        private var clientHandler: ClientHandler? = null

        // The first message sent by the server is always unencrypted and initializes the
        // encryption. We don't start listening to the client until the encryption is
        // initialized.
        override var readDecryptCipher: Cipher? = null
        override var readEncryptCipher: Cipher? = null
        override val writeEncryptCipher: Cipher? = null

        override fun processMessage(message: Message): ProcessResult {
            when (message) {
                is InitEncryptionMessage -> if (readDecryptCipher == null) {
                    readDecryptCipher = createCipher(message.serverKey)
                    readEncryptCipher = createCipher(message.serverKey)

                    val clientDecryptCipher = createCipher(message.clientKey)
                    val clientEncryptCipher = createCipher(message.clientKey)

                    logger.info {
                        "Encryption initialized, start listening to client."
                    }

                    // Start listening to client on another thread.
                    val clientListener = ClientHandler(
                        clientSocket,
                        this,
                        clientDecryptCipher,
                        clientEncryptCipher,
                    )
                    this.clientHandler = clientListener
                    val thread = Thread(clientListener::listen)
                    thread.name = "${name}_client"
                    thread.start()
                }

                is RedirectMessage -> {
                    val oldAddress = Inet4Pair(message.ipAddress, message.port.toInt())

                    redirectMap[oldAddress]?.let { newAddress ->
                        logger.debug {
                            "Rewriting redirect from $oldAddress to $newAddress."
                        }

                        message.ipAddress = newAddress.address.address
                        message.port = newAddress.port.toUShort()

                        return ProcessResult.Changed
                    }
                }
            }

            return ProcessResult.Ok
        }

        override fun processRawBytes(buffer: Buffer, offset: Int, size: Int) {
            clientHandler?.writeBytes(buffer, offset, size)
        }

        override fun socketClosed() {
            clientHandler?.stop()
            clientHandler = null
        }
    }

    private inner class ClientHandler(
        clientSocket: Socket,
        private val serverHandler: ServerHandler,
        override val readDecryptCipher: Cipher,
        override val readEncryptCipher: Cipher,
    ) : ProxySocketHandler("${name}_client", clientSocket) {

        override val writeEncryptCipher: Cipher? = null

        override fun processMessage(message: Message): ProcessResult = ProcessResult.Ok

        override fun processRawBytes(buffer: Buffer, offset: Int, size: Int) {
            serverHandler.writeBytes(buffer, offset, size)
        }

        override fun socketClosed() {
            serverHandler.stop()
        }
    }

    private abstract inner class ProxySocketHandler(name: String, socket: Socket) :
        SocketHandler<Message>(name, socket) {

        override val messageDescriptor = this@ProxyServer.messageDescriptor

        override fun logMessageTooLarge(code: Int, size: Int) {
            logger.warn {
                val message = messageString(code, size)
                "Sending $message with size ${size}B. Skipping because it's too large."
            }
        }

        override fun logMessageReceived(message: Message) {
            logger.trace { "Sent $message." }
        }
    }
}

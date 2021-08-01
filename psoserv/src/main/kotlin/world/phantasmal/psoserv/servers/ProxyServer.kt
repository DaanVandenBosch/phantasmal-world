package world.phantasmal.psoserv.servers

import mu.KotlinLogging
import world.phantasmal.psolib.buffer.Buffer
import world.phantasmal.psoserv.encryption.Cipher
import world.phantasmal.psoserv.messages.InitEncryptionMessage
import world.phantasmal.psoserv.messages.Message
import world.phantasmal.psoserv.messages.MessageDescriptor
import world.phantasmal.psoserv.messages.RedirectMessage
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.net.SocketTimeoutException

class ProxyServer(
    private val name: String,
    private val bindPair: Inet4Pair,
    private val remotePair: Inet4Pair,
    private val messageDescriptor: MessageDescriptor<Message>,
    private val createCipher: (key: ByteArray) -> Cipher,
    private val redirectMap: Map<Inet4Pair, Inet4Pair> = emptyMap(),
) {
    private val logger = KotlinLogging.logger(name)
    private val proxySocket = ServerSocket()

    @Volatile
    private var running = false

    fun start() {
        logger.info { "Starting." }

        proxySocket.bind(bindPair)

        running = true

        // Accept client connections on a dedicated thread.
        val thread = Thread(::acceptConnections)
        thread.name = name
        thread.start()
    }

    fun stop() {
        logger.info { "Stopping." }

        // Signal to the connection thread that it should stop.
        running = false

        // Closing the server socket will generate a SocketException on the connection thread which
        // will then shut down.
        proxySocket.close()
    }

    private fun acceptConnections() {
        if (running) {
            logger.info { "Accepting connections." }

            while (running) {
                try {
                    val clientSocket = proxySocket.accept()
                    logger.info {
                        "New client connection from ${clientSocket.inetAddress}:${clientSocket.port}."
                    }

                    val serverSocket = Socket(remotePair.address, remotePair.port)
                    logger.info {
                        "Connected to server ${serverSocket.inetAddress}:${serverSocket.port}."
                    }

                    // Listen to server on this thread.
                    // Don't start listening to the client until encryption is initialized.
                    ServerHandler(serverSocket, clientSocket).listen()
                } catch (e: SocketTimeoutException) {
                    // Retry after timeout.
                    continue
                } catch (e: InterruptedException) {
                    logger.error(e) {
                        "Interrupted while trying to accept client connections on $bindPair, stopping."
                    }
                    break
                } catch (e: SocketException) {
                    // Don't log if we're not running anymore because that means this exception was
                    // probably generated by a socket.close() call.
                    if (running) {
                        logger.error(e) {
                            "Exception while trying to accept client connections on $bindPair, stopping."
                        }
                    }
                    break
                } catch (e: Throwable) {
                    logger.error(e) {
                        "Exception while trying to accept client connections on $bindPair."
                    }
                }
            }
        }

        logger.info { "Stopped." }
    }

    private inner class ServerHandler(
        serverSocket: Socket,
        private val clientSocket: Socket,
    ) : SocketHandler<Message>(logger, serverSocket) {

        private var clientHandler: ClientHandler? = null

        override val messageDescriptor = this@ProxyServer.messageDescriptor

        // The first message sent by the server is always unencrypted and initializes the
        // encryption. We don't start listening to the client until the encryption is
        // initialized.
        override var decryptCipher: Cipher? = null
        override var encryptCipher: Cipher? = null

        override fun processMessage(message: Message): ProcessResult {
            when (message) {
                is InitEncryptionMessage -> if (decryptCipher == null) {
                    decryptCipher = createCipher(message.serverKey)
                    encryptCipher = createCipher(message.serverKey)

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
                    thread.name = "$name client"
                    thread.start()
                }

                is RedirectMessage -> {
                    val oldAddress = Inet4Pair(message.ipAddress, message.port)

                    redirectMap[oldAddress]?.let { newAddress ->
                        logger.debug {
                            "Rewriting redirect from $oldAddress to $newAddress."
                        }

                        message.ipAddress = newAddress.address.address
                        message.port = newAddress.port

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
        override val decryptCipher: Cipher,
        override val encryptCipher: Cipher,
    ) : SocketHandler<Message>(logger, clientSocket) {

        override val messageDescriptor = this@ProxyServer.messageDescriptor

        override fun processMessage(message: Message): ProcessResult = ProcessResult.Ok

        override fun processRawBytes(buffer: Buffer, offset: Int, size: Int) {
            serverHandler.writeBytes(buffer, offset, size)
        }

        override fun socketClosed() {
            serverHandler.stop()
        }
    }
}

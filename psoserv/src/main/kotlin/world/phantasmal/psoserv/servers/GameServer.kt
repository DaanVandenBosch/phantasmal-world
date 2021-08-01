package world.phantasmal.psoserv.servers

import world.phantasmal.psoserv.encryption.Cipher
import world.phantasmal.psoserv.messages.Message
import world.phantasmal.psoserv.messages.MessageDescriptor
import java.net.Socket

abstract class GameServer<MessageType, StateType>(
    name: String,
    bindPair: Inet4Pair,
) : Server(name, bindPair)
        where MessageType : Message,
              StateType : ServerState<MessageType, *, StateType> {

    private var connectionCounter = 0

    protected abstract val messageDescriptor: MessageDescriptor<MessageType>

    override fun clientConnected(clientSocket: Socket) {
        // Handle each client connection in its own thread.
        val thread = Thread {
            val handler = ClientHandler(clientSocket)
            handler.initializeState()
            handler.listen()
        }
        thread.name = "${name}_client_${connectionCounter++}"
        thread.start()
    }

    protected abstract fun createCipher(): Cipher

    protected abstract fun initializeState(
        sender: SocketSender<MessageType>,
        serverCipher: Cipher,
        clientCipher: Cipher,
    ): StateType

    private inner class ClientHandler(socket: Socket) : SocketHandler<MessageType>(logger, socket) {
        private val serverCipher = createCipher()
        private val clientCipher = createCipher()

        private var state: StateType? = null

        override val messageDescriptor = this@GameServer.messageDescriptor

        override val readDecryptCipher = clientCipher
        override val readEncryptCipher: Cipher? = null
        override val writeEncryptCipher: Cipher = serverCipher

        fun initializeState() {
            state = initializeState(this, serverCipher, clientCipher)
        }

        override fun processMessage(message: MessageType): ProcessResult {
            state = state!!.process(message)

            return if (state is FinalServerState) {
                // Give the client some time to disconnect.
                Thread.sleep(100)

                // Close the connection.
                ProcessResult.Done
            } else {
                ProcessResult.Ok
            }
        }
    }
}

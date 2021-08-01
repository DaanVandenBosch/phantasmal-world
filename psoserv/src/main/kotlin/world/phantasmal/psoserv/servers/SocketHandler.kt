package world.phantasmal.psoserv.servers

import mu.KLogger
import world.phantasmal.psolib.Endianness
import world.phantasmal.psolib.buffer.Buffer
import world.phantasmal.psoserv.encryption.Cipher
import world.phantasmal.psoserv.messages.Message
import world.phantasmal.psoserv.messages.MessageDescriptor
import world.phantasmal.psoserv.messages.messageString
import world.phantasmal.psoserv.roundToBlockSize
import java.net.Socket
import java.net.SocketException
import kotlin.math.min

abstract class SocketHandler<MessageType : Message>(
    protected val logger: KLogger,
    private val socket: Socket,
) : SocketSender<MessageType> {
    private val sockName: String = "${socket.remoteSocketAddress}"
    private val headerSize: Int get() = messageDescriptor.headerSize

    @Volatile
    private var running = false

    protected abstract val messageDescriptor: MessageDescriptor<MessageType>
    protected abstract val readDecryptCipher: Cipher?

    /**
     * Used by proxy servers to re-encrypt changed messages before sending them to the client.
     */
    protected abstract val readEncryptCipher: Cipher?

    protected abstract val writeEncryptCipher: Cipher?

    fun listen() {
        logger.info { "Listening to $sockName." }
        running = true

        try {
            val readBuffer = Buffer.withCapacity(BUFFER_CAPACITY, Endianness.Little)
            val headerBuffer = Buffer.withSize(headerSize, Endianness.Little)

            readLoop@ while (true) {
                // Read from socket.
                val readSize = socket.read(readBuffer, BUFFER_CAPACITY - readBuffer.size)

                if (readSize == -1) {
                    // Close the connection if no more bytes available.
                    break@readLoop
                }

                // Process buffer contents.
                var offset = 0
                var bytesToSkip = 0

                bufferLoop@ while (offset + headerSize <= readBuffer.size) {
                    // Remember the current cipher in a local variable because processing a message
                    // might change it.
                    val decryptCipher = readDecryptCipher
                    val encryptCipher = readEncryptCipher

                    // Read header.
                    readBuffer.copyInto(headerBuffer, offset = offset, size = headerSize)

                    // Decrypt header.
                    decryptCipher?.let {
                        check(decryptCipher.blockSize == headerSize)
                        decryptCipher.decrypt(headerBuffer)
                    }

                    val (code, size) = messageDescriptor.readHeader(headerBuffer)
                    val encryptedSize = roundToBlockSize(size, decryptCipher?.blockSize ?: 1)
                    // Bytes available for the next message.
                    val available = readBuffer.size - offset

                    when {
                        // Don't parse the message when it's too large.
                        encryptedSize > BUFFER_CAPACITY -> {
                            logger.warn {
                                val message = messageString(code, size)
                                "Receiving $message, too large: ${size}B. Skipping."
                            }

                            bytesToSkip = encryptedSize - available

                            decryptCipher?.advance(
                                blocks = (encryptedSize - headerSize) / decryptCipher.blockSize,
                            )

                            encryptCipher?.advance(
                                blocks = encryptedSize / encryptCipher.blockSize,
                            )

                            break@bufferLoop
                        }

                        // Parse message when we have enough bytes available.
                        available >= encryptedSize -> {
                            val messageBuffer = readBuffer.copy(offset, encryptedSize)

                            // Decrypt before parsing if necessary.
                            // Copy the already decrypted header first, then decrypt the rest. We
                            // don't simply decrypt the entire message buffer again, because the PC
                            // cipher is stateful.
                            headerBuffer.copyInto(messageBuffer)
                            decryptCipher?.decrypt(
                                messageBuffer,
                                offset = headerSize,
                                blocks = (encryptedSize - headerSize) / decryptCipher.blockSize,
                            )

                            try {
                                val message = messageDescriptor.readMessage(messageBuffer)
                                logger.trace { "Received $message." }

                                when (processMessage(message)) {
                                    ProcessResult.Ok -> {
                                        // Advance the encryption cipher, then continue.
                                        encryptCipher?.advance(
                                            blocks = encryptedSize / encryptCipher.blockSize,
                                        )
                                    }
                                    ProcessResult.Changed -> {
                                        // Copy changes to the read buffer and encrypt them if
                                        // necessary.
                                        messageBuffer.copyInto(
                                            readBuffer,
                                            destinationOffset = offset,
                                        )
                                        encryptCipher?.encrypt(
                                            readBuffer,
                                            offset,
                                            blocks = encryptedSize / encryptCipher.blockSize,
                                        )
                                    }
                                    ProcessResult.Done -> {
                                        // Close the connection.
                                        break@readLoop
                                    }
                                }
                            } catch (e: Throwable) {
                                logger.error(e) { "Error while processing message from $sockName." }
                            }

                            offset += encryptedSize
                        }

                        // Not enough bytes available.
                        else -> break@bufferLoop
                    }
                }

                processRawBytes(readBuffer, 0, readSize)

                if (bytesToSkip > 0) {
                    // Just pass the raw bytes through to the raw bytes handler and don't parse
                    // them.
                    while (bytesToSkip > 0) {
                        readBuffer.size = 0
                        val read = socket.read(readBuffer, min(BUFFER_CAPACITY, bytesToSkip))

                        if (read == -1) {
                            logger.warn {
                                "Expected to skip $bytesToSkip more bytes, but $sockName stopped sending."
                            }

                            // Close the connection.
                            break@readLoop
                        }

                        bytesToSkip -= read

                        processRawBytes(readBuffer, 0, read)
                    }

                    readBuffer.size = 0
                } else {
                    // If we didn't have enough bytes available, shift the unparsed bytes to the
                    // front of the buffer before we read more bytes. If we don't do this, we can
                    // end up in an infinite loop.
                    val unparsed = readBuffer.size - offset

                    if (unparsed > 0) {
                        readBuffer.copyInto(readBuffer, offset = offset, size = unparsed)
                        readBuffer.size = unparsed
                    } else {
                        readBuffer.size = 0
                    }
                }
            }
        } catch (e: InterruptedException) {
            logger.error(e) { "Interrupted while listening to $sockName, closing connection." }
        } catch (e: SocketException) {
            // Don't log if we're not running anymore because that means this exception was probably
            // generated by a socket.close() call.
            if (running) {
                logger.error(e) { "Error while listening to $sockName, closing connection." }
            }
        } catch (e: Throwable) {
            logger.error(e) { "Error while listening to $sockName, closing connection." }
        } finally {
            running = false

            try {
                if (socket.isClosed) {
                    logger.info { "Connection to $sockName was closed." }
                } else {
                    logger.info { "Closing connection to $sockName." }
                    socket.close()
                }
            } finally {
                socketClosed()
            }
        }
    }

    fun writeBytes(buffer: Buffer, offset: Int, size: Int) {
        socket.write(buffer, offset, size)
    }

    fun stop() {
        running = false
        socket.close()
    }

    override fun sendMessage(message: MessageType, encrypt: Boolean) {
        logger.trace {
            "Sending $message${if (encrypt) "" else " (unencrypted)"}."
        }

        if (message.buffer.size != message.size) {
            logger.warn {
                "Message size of $message is ${message.size}B, but wrote ${message.buffer.size} bytes."
            }
        }

        val cipher = writeEncryptCipher
        val buffer: Buffer

        if (encrypt) {
            checkNotNull(cipher)
            // Pad buffer before encrypting.
            val initialSize = message.buffer.size
            buffer = message.buffer.copy(
                size = roundToBlockSize(initialSize, cipher.blockSize)
            )
            cipher.encrypt(buffer)
        } else {
            buffer = message.buffer
        }

        socket.write(buffer, 0, buffer.size)
    }

    protected abstract fun processMessage(message: MessageType): ProcessResult

    protected open fun processRawBytes(buffer: Buffer, offset: Int, size: Int) {
        // Do nothing.
    }

    protected open fun socketClosed() {
        // Do nothing.
    }

    protected enum class ProcessResult {
        Ok, Changed, Done
    }

    companion object {
        private const val BUFFER_CAPACITY: Int = 32768
    }
}
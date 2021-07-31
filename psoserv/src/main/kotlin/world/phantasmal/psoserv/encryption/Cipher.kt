package world.phantasmal.psoserv.encryption

import world.phantasmal.psolib.buffer.Buffer

interface Cipher {
    val blockSize: Int
    val key: ByteArray

    fun encrypt(data: Buffer, offset: Int = 0, blocks: Int = data.size / blockSize)
    fun decrypt(data: Buffer, offset: Int = 0, blocks: Int = data.size / blockSize)

    /**
     * Advance by the given number of blocks, used by stateful ciphers such as the PC cipher.
     */
    fun advance(blocks: Int)
}

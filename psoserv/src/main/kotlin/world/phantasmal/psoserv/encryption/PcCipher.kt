package world.phantasmal.psoserv.encryption

import world.phantasmal.psolib.Endianness
import world.phantasmal.psolib.buffer.Buffer
import java.lang.Integer.divideUnsigned
import kotlin.random.Random

class PcCipher(override val key: ByteArray = createKey()) : Cipher {
    private val subKeys: IntArray
    private var position = 56

    override val blockSize: Int = 4

    init {
        require(key.size == KEY_SIZE) {
            "Expected key of size ${KEY_SIZE}, but was ${key.size}."
        }

        val intKey = Buffer.fromByteArray(key, Endianness.Little).getInt(0)
        subKeys = createSubKeys(intKey)
    }

    override fun encrypt(data: Buffer, offset: Int, blocks: Int) {
        require(offset >= 0)
        require(blocks >= 0)
        val limit = offset + blocks * blockSize
        require(limit <= data.size)

        var i = offset

        while (i < limit) {
            data.setInt(i, data.getInt(i) xor getNextKey())
            i += blockSize
        }
    }

    override fun decrypt(data: Buffer, offset: Int, blocks: Int) {
        encrypt(data, offset, blocks)
    }

    override fun advance(blocks: Int) {
        repeat(blocks) {
            getNextKey()
        }
    }

    private fun getNextKey(): Int {
        if (position == 56) {
            mixKeys(subKeys)
            position = 1
        }

        return subKeys[position++]
    }

    companion object {
        const val KEY_SIZE: Int = 4

        fun createKey(): ByteArray = Random.nextBytes(KEY_SIZE)

        private fun mixKeys(subKeys: IntArray) {
            var esi: Int
            var ebp: Int
            var edi = 1
            var edx = 0x18
            var eax = edi

            while (edx > 0) {
                esi = subKeys[eax + 0x1F]
                ebp = subKeys[eax]
                ebp -= esi
                subKeys[eax] = ebp
                eax++
                edx--
            }

            edi = 0x19
            edx = 0x1F
            eax = edi

            while (edx > 0) {
                esi = subKeys[eax - 0x18]
                ebp = subKeys[eax]
                ebp -= esi
                subKeys[eax] = ebp
                eax++
                edx--
            }
        }

        private fun createSubKeys(key: Int): IntArray {
            val subKeys = IntArray(57)
            var eax: Int
            var edx: Int
            var var1: Int
            var esi = 1
            var ebx = key
            var edi = 0x15
            subKeys[56] = ebx
            subKeys[55] = ebx

            while (edi <= 0x46E) {
                eax = edi
                var1 = divideUnsigned(eax, 55)
                edx = eax - var1 * 55
                ebx -= esi
                edi += 0x15
                subKeys[edx] = esi
                esi = ebx
                ebx = subKeys[edx]
            }

            mixKeys(subKeys)
            mixKeys(subKeys)
            mixKeys(subKeys)
            mixKeys(subKeys)

            return subKeys
        }
    }
}

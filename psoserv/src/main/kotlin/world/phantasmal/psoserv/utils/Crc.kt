package world.phantasmal.psoserv.utils

import world.phantasmal.psolib.buffer.Buffer
import world.phantasmal.psolib.cursor.cursor

fun crc32Checksum(data: Buffer): Int {
    val cursor = data.cursor()
    var cs = 0xFFFFFFFFu

    while (cursor.hasBytesLeft()) {
        cs = cs xor cursor.uByte().toUInt()

        for (i in 0..7) {
            cs = if ((cs and 1u) == 0u) {
                cs shr 1
            } else {
                (cs shr 1) xor 0xEDB88320u
            }
        }
    }

    return (cs xor 0xFFFFFFFFu).toInt()
}

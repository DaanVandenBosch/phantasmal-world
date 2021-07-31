package world.phantasmal.psoserv.servers

import world.phantasmal.psolib.buffer.Buffer
import java.net.Socket

fun Socket.read(buffer: Buffer, size: Int): Int {
    val read = getInputStream().read(buffer.byteArray, buffer.size, size)

    if (read != -1) {
        buffer.size += read
    }

    return read
}

fun Socket.write(buffer: Buffer, offset: Int, size: Int) {
    getOutputStream().write(buffer.byteArray, offset, size)
}

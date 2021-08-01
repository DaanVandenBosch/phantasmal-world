package world.phantasmal.psoserv.servers

import mu.KLogger
import world.phantasmal.psolib.buffer.Buffer
import world.phantasmal.psoserv.encryption.BbCipher
import world.phantasmal.psoserv.messages.BbMessage
import world.phantasmal.psoserv.messages.BbMessageDescriptor
import world.phantasmal.psoserv.messages.Header
import java.net.InetAddress

abstract class BbServer<StateType : ServerState<BbMessage, StateType>>(
    logger: KLogger,
    address: InetAddress,
    port: Int,
) : Server<BbMessage, StateType>(logger, address, port) {

    override fun createCipher() = BbCipher()

    override fun readHeader(buffer: Buffer): Header =
        BbMessageDescriptor.readHeader(buffer)

    override fun readMessage(buffer: Buffer): BbMessage =
        BbMessageDescriptor.readMessage(buffer)
}

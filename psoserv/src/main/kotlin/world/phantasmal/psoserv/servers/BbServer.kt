package world.phantasmal.psoserv.servers

import world.phantasmal.psoserv.encryption.BbCipher
import world.phantasmal.psoserv.messages.BbMessage
import world.phantasmal.psoserv.messages.BbMessageDescriptor

abstract class BbServer<StateType : ServerState<BbMessage, *, StateType>>(
    name: String,
    bindPair: Inet4Pair,
) : GameServer<BbMessage, StateType>(name, bindPair) {

    override val messageDescriptor = BbMessageDescriptor

    override fun createCipher() = BbCipher()
}

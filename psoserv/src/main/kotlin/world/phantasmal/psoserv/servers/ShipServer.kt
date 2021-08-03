package world.phantasmal.psoserv.servers

import world.phantasmal.psoserv.encryption.BbCipher
import world.phantasmal.psoserv.encryption.Cipher
import world.phantasmal.psoserv.messages.AuthStatus
import world.phantasmal.psoserv.messages.BbMessage
import world.phantasmal.psoserv.messages.BbMessageDescriptor
import world.phantasmal.psoserv.messages.MenuType

class ShipServer(
    name: String,
    bindPair: Inet4Pair,
    private val uiName: String,
    private val blocks: List<BlockInfo>,
) : GameServer<BbMessage>(name, bindPair) {

    override val messageDescriptor = BbMessageDescriptor

    override fun createCipher() = BbCipher()

    override fun createClientReceiver(
        sender: ClientSender<BbMessage>,
        serverCipher: Cipher,
        clientCipher: Cipher,
    ): ClientReceiver<BbMessage> = object : ClientReceiver<BbMessage> {
        init {
            sender.send(
                BbMessage.InitEncryption(
                    "Phantasy Star Online Blue Burst Game Server. Copyright 1999-2004 SONICTEAM.",
                    serverCipher.key,
                    clientCipher.key,
                ),
                encrypt = false,
            )
        }

        override fun process(message: BbMessage): Boolean = when (message) {
            is BbMessage.Authenticate -> {
                // TODO: Actual authentication.
                send(
                    BbMessage.AuthData(
                        AuthStatus.Success,
                        message.guildCard,
                        message.teamId,
                        message.charSlot,
                        message.charSelected,
                    )
                )
                send(
                    BbMessage.BlockList(uiName, blocks.map { it.uiName })
                )

                true
            }

            is BbMessage.MenuSelect -> {
                if (message.menuType == MenuType.Block) {
                    blocks.getOrNull(message.itemId - 1)?.let { block ->
                        send(
                            BbMessage.Redirect(block.bindPair.address.address, block.bindPair.port)
                        )
                    }

                    // Disconnect.
                    false
                } else {
                    true
                }
            }

            else -> unexpectedMessage(message)
        }

        private fun send(message: BbMessage) {
            sender.send(message)
        }
    }
}

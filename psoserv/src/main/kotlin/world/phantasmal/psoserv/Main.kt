package world.phantasmal.psoserv

import mu.KotlinLogging
import world.phantasmal.psoserv.encryption.BbCipher
import world.phantasmal.psoserv.encryption.PcCipher
import world.phantasmal.psoserv.messages.BB_HEADER_SIZE
import world.phantasmal.psoserv.messages.BbMessage
import world.phantasmal.psoserv.messages.PC_HEADER_SIZE
import world.phantasmal.psoserv.messages.PcMessage
import world.phantasmal.psoserv.servers.ProxyServer
import world.phantasmal.psoserv.servers.character.DataServer
import world.phantasmal.psoserv.servers.login.LoginServer
import world.phantasmal.psoserv.servers.patch.PatchServer
import java.net.Inet4Address
import java.net.InetAddress

private const val PATCH_SERVER_PORT: Int = 11_000
private const val LOGIN_SERVER_PORT: Int = 12_000
private const val DATA_SERVER_PORT: Int = 12_001
private val LOGGER = KotlinLogging.logger {}

fun main() {
    LOGGER.info { "Initializing." }

    if (true) {
        // System property java.net.preferIPv6Addresses should be false.
        val characterServerAddress = InetAddress.getLoopbackAddress() as? Inet4Address
            ?: error("Couldn't get IPv4 address of character server.")

        PatchServer(
            InetAddress.getLoopbackAddress(),
            port = PATCH_SERVER_PORT,
            welcomeMessage = "Welcome to Phantasmal World.",
        )

        LoginServer(
            InetAddress.getLoopbackAddress(),
            port = LOGIN_SERVER_PORT,
            characterServerAddress,
            DATA_SERVER_PORT,
        )

        DataServer(InetAddress.getLoopbackAddress(), port = DATA_SERVER_PORT)
    } else {
        val loopback = InetAddress.getLoopbackAddress() as Inet4Address
        val redirectMap = mapOf(
            Pair(loopback, 21_001) to Pair(loopback, 11_001),
            Pair(loopback, 22_001) to Pair(loopback, 12_001),
        )
        ProxyServer(
            proxyAddress = InetAddress.getLoopbackAddress(),
            proxyPort = 11_000,
            serverAddress = InetAddress.getLoopbackAddress(),
            serverPort = 21_000,
            PcMessage::fromBuffer,
            ::PcCipher,
            headerSize = PC_HEADER_SIZE,
            PcMessage::readHeader,
            redirectMap,
        )
        ProxyServer(
            proxyAddress = InetAddress.getLoopbackAddress(),
            proxyPort = 11_001,
            serverAddress = InetAddress.getLoopbackAddress(),
            serverPort = 21_001,
            PcMessage::fromBuffer,
            ::PcCipher,
            headerSize = PC_HEADER_SIZE,
            PcMessage::readHeader,
            redirectMap,
        )
        ProxyServer(
            proxyAddress = InetAddress.getLoopbackAddress(),
            proxyPort = 12_000,
            serverAddress = InetAddress.getLoopbackAddress(),
            serverPort = 22_000,
            BbMessage::fromBuffer,
            ::BbCipher,
            headerSize = BB_HEADER_SIZE,
            BbMessage::readHeader,
            redirectMap,
        )
        ProxyServer(
            proxyAddress = InetAddress.getLoopbackAddress(),
            proxyPort = 12_001,
            serverAddress = InetAddress.getLoopbackAddress(),
            serverPort = 22_001,
            BbMessage::fromBuffer,
            ::BbCipher,
            headerSize = BB_HEADER_SIZE,
            BbMessage::readHeader,
            redirectMap,
        )
//        ProxyServer(
//            proxyAddress = InetAddress.getLoopbackAddress(),
//            proxyPort = 13_001,
//            serverAddress = InetAddress.getByName("74.91.125.137"),
//            serverPort = 13_001,
//        )
//        ProxyServer(
//            proxyAddress = InetAddress.getLoopbackAddress(),
//            proxyPort = 14_000,
//            serverAddress = InetAddress.getByName("74.91.125.137"),
//            serverPort = 14_000,
//        )
//        ProxyServer(
//            proxyAddress = InetAddress.getLoopbackAddress(),
//            proxyPort = 14_001,
//            serverAddress = InetAddress.getByName("74.91.125.137"),
//            serverPort = 14_001,
//        )
    }

    LOGGER.info { "Initialization finished." }
}

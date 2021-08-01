package world.phantasmal.psoserv

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import world.phantasmal.psoserv.encryption.BbCipher
import world.phantasmal.psoserv.encryption.Cipher
import world.phantasmal.psoserv.encryption.PcCipher
import world.phantasmal.psoserv.messages.BbMessageDescriptor
import world.phantasmal.psoserv.messages.Message
import world.phantasmal.psoserv.messages.MessageDescriptor
import world.phantasmal.psoserv.messages.PcMessageDescriptor
import world.phantasmal.psoserv.servers.*
import world.phantasmal.psoserv.servers.account.AccountServer
import world.phantasmal.psoserv.servers.auth.AuthServer
import world.phantasmal.psoserv.servers.patch.PatchServer
import java.io.File
import java.net.Inet4Address

// System property java.net.preferIPv6Addresses should be false.
private val DEFAULT_ADDRESS: Inet4Address = inet4Loopback()
private const val DEFAULT_PATCH_PORT: Int = 11_000
private const val DEFAULT_LOGIN_PORT: Int = 12_000
private const val DEFAULT_DATA_PORT: Int = 12_001

private val LOGGER = KotlinLogging.logger("main")

fun main(args: Array<String>) {
    LOGGER.info { "Initializing." }

    var configFile: File? = null

    for (arg in args) {
        val split = arg.split('=')

        if (split.size == 2) {
            val (param, value) = split

            when (param) {
                "--config" -> {
                    configFile = File(value)
                }
            }
        }
    }

    if (configFile == null) {
        configFile = File("config.json").takeIf { it.isFile }
    }

    val config: Config

    if (configFile != null) {
        LOGGER.info { "Using configuration file $configFile." }

        val json = Json {
            ignoreUnknownKeys = true
        }

        config = json.decodeFromString(configFile.readText())
    } else {
        config = Config()
    }

    val server = initialize(config)

    LOGGER.info { "Starting up." }

    server.start()
}

private class PhantasmalServer(
    private val servers: List<Server>,
    private val proxyServers: List<ProxyServer>,
) {
    fun start() {
        servers.forEach(Server::start)
        proxyServers.forEach(ProxyServer::start)
    }

    fun stop() {
        servers.forEach(Server::stop)
        proxyServers.forEach(ProxyServer::stop)
    }
}

private fun initialize(config: Config): PhantasmalServer {
    val defaultAddress = config.address?.let(::inet4Address) ?: DEFAULT_ADDRESS

    val dataAddress = config.account?.address?.let(::inet4Address) ?: defaultAddress
    val dataPort = config.account?.port ?: DEFAULT_DATA_PORT

    val servers = mutableListOf<Server>()

    // If no proxy config is specified, we run a regular PSO server by default.
    val run = config.proxy == null || !config.proxy.run

    if (config.patch == null && run || config.patch?.run == true) {
        val bindPair = Inet4Pair(
            config.patch?.address?.let(::inet4Address) ?: defaultAddress,
            config.patch?.port ?: DEFAULT_PATCH_PORT,
        )

        LOGGER.info { "Configuring patch server to bind to $bindPair." }

        servers.add(
            PatchServer(
                name = "patch",
                bindPair,
                welcomeMessage = config.patch?.welcomeMessage ?: "Welcome to Phantasmal World.",
            )
        )
    }

    if (config.auth == null && run || config.auth?.run == true) {
        val bindPair = Inet4Pair(
            config.auth?.address?.let(::inet4Address) ?: defaultAddress,
            config.auth?.port ?: DEFAULT_LOGIN_PORT,
        )

        LOGGER.info { "Configuring auth server to bind to $bindPair." }

        servers.add(
            AuthServer(
                name = "auth",
                bindPair,
                dataServerAddress = dataAddress,
                dataServerPort = dataPort,
            )
        )
    }

    if (config.account == null && run || config.account?.run == true) {
        val bindPair = Inet4Pair(
            config.account?.address?.let(::inet4Address) ?: defaultAddress,
            config.account?.port ?: DEFAULT_DATA_PORT,
        )

        LOGGER.info { "Configuring account server to bind to $bindPair." }

        servers.add(
            AccountServer(
                name = "account",
                bindPair,
            )
        )
    }

    val proxyServers = config.proxy?.let(::initializeProxy) ?: emptyList()

    return PhantasmalServer(servers, proxyServers)
}

private fun initializeProxy(config: ProxyConfig): List<ProxyServer> {
    if (!config.run) {
        return emptyList()
    }

    val defaultBindAddress = config.bindAddress?.let(::inet4Address) ?: DEFAULT_ADDRESS
    val defaultRemoteAddress = config.remoteAddress?.let(::inet4Address) ?: DEFAULT_ADDRESS
    val redirectMap = mutableMapOf<Inet4Pair, Inet4Pair>()
    val proxyServers = mutableListOf<ProxyServer>()
    var nameI = 1

    for (psc in config.servers) {
        val name = psc.name ?: "proxy_${nameI++}"
        val bindPair = Inet4Pair(
            psc.bindAddress?.let(::inet4Address) ?: defaultBindAddress,
            psc.bindPort,
        )
        val remotePair = Inet4Pair(
            psc.remoteAddress?.let(::inet4Address) ?: defaultRemoteAddress,
            psc.remotePort,
        )
        redirectMap[remotePair] = bindPair

        val messageDescriptor: MessageDescriptor<Message>
        val createCipher: (key: ByteArray) -> Cipher

        when (psc.version) {
            GameVersionConfig.PC -> {
                messageDescriptor = PcMessageDescriptor
                createCipher = ::PcCipher
            }
            GameVersionConfig.BB -> {
                messageDescriptor = BbMessageDescriptor
                createCipher = ::BbCipher
            }
        }

        LOGGER.info {
            """Configuring proxy server "$name" to bind to $bindPair and proxy remote server $remotePair."""
        }

        proxyServers.add(
            ProxyServer(
                name,
                bindPair,
                remotePair,
                messageDescriptor,
                createCipher,
                redirectMap,
            )
        )
    }

    return proxyServers
}
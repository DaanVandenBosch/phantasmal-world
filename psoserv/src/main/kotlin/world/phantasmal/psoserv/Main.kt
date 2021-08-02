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
import java.io.File
import java.net.Inet4Address

// System property java.net.preferIPv6Addresses should be false.
private val DEFAULT_ADDRESS: Inet4Address = inet4Loopback()
private const val DEFAULT_PATCH_PORT: Int = 11_000
private const val DEFAULT_LOGIN_PORT: Int = 12_000
private const val DEFAULT_ACCOUNT_PORT: Int = 12_001
private const val DEFAULT_FIRST_SHIP_PORT: Int = 12_010

private val LOGGER = KotlinLogging.logger("main")

fun main(args: Array<String>) {
    try {
        LOGGER.info { "Initializing." }

        // Try to get config file location from arguments first.
        var configFile: File? = null

        // Parse arguments.
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

        // Try default config file location if no file specified with --config argument.
        if (configFile == null) {
            configFile = File("config.json").takeIf { it.isFile }
        }

        val config: Config

        // Parse the config file if we found one, otherwise use default config.
        if (configFile != null) {
            LOGGER.info { "Using configuration file $configFile." }

            val json = Json {
                ignoreUnknownKeys = true
            }

            config = json.decodeFromString(configFile.readText())
        } else {
            config = DEFAULT_CONFIG
        }

        // Initialize and start the server.
        val server = initialize(config)

        LOGGER.info { "Starting up." }

        server.start()
    } catch (e: Throwable) {
        LOGGER.error(e) { "Failed to start up." }
    }
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

    val accountAddress = config.account?.address?.let(::inet4Address) ?: defaultAddress
    val accountPort = config.account?.port ?: DEFAULT_ACCOUNT_PORT

    var shipI = 1
    var shipPort = DEFAULT_FIRST_SHIP_PORT

    val ships = config.ships.filter { it.run }.map { shipCfg ->
        val ship = ShipInfo(
            name = shipCfg.name ?: "ship_$shipI",
            uiName = shipCfg.uiName ?: "Ship $shipI",
            bindPair = Inet4Pair(
                shipCfg.address?.let(::inet4Address) ?: defaultAddress,
                shipCfg.port ?: shipPort++,
            )
        )
        shipI++
        ship
    }

    val servers = mutableListOf<Server>()

    if (config.patch?.run == true) {
        val bindPair = Inet4Pair(
            config.patch.address?.let(::inet4Address) ?: defaultAddress,
            config.patch.port ?: DEFAULT_PATCH_PORT,
        )

        LOGGER.info { "Configuring patch server to bind to $bindPair." }

        servers.add(
            PatchServer(
                bindPair,
                welcomeMessage = config.patch.welcomeMessage ?: "Welcome to Phantasmal World.",
            )
        )
    }

    if (config.auth?.run == true) {
        val bindPair = Inet4Pair(
            config.auth.address?.let(::inet4Address) ?: defaultAddress,
            config.auth.port ?: DEFAULT_LOGIN_PORT,
        )

        LOGGER.info { "Configuring auth server to bind to $bindPair." }
        LOGGER.info {
            "Auth server will redirect to account server at $accountAddress:$accountPort."
        }

        servers.add(
            AuthServer(
                bindPair,
                accountServerAddress = accountAddress,
                accountServerPort = accountPort,
            )
        )
    }

    if (config.account?.run == true) {
        val bindPair = Inet4Pair(
            config.account.address?.let(::inet4Address) ?: defaultAddress,
            config.account.port ?: DEFAULT_ACCOUNT_PORT,
        )

        LOGGER.info { "Configuring account server to bind to $bindPair." }
        LOGGER.info {
            "Account server will redirect to ${ships.size} ship servers: ${
                ships.joinToString { """"${it.name}" (${it.bindPair})""" }
            }."
        }

        servers.add(
            AccountServer(
                bindPair,
                ships,
            )
        )
    }

    for (ship in ships) {
        LOGGER.info {
            """Configuring ship server ${ship.name} ("${ship.uiName}") to bind to ${ship.bindPair}."""
        }

        servers.add(
            ShipServer(
                ship.name,
                ship.bindPair,
                ship.uiName,
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
        if (!psc.run) {
            continue
        }

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

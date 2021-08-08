package world.phantasmal.psoserv

import com.typesafe.config.ConfigFactory
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.hocon.Hocon
import kotlinx.serialization.hocon.decodeFromConfig
import mu.KotlinLogging
import world.phantasmal.psoserv.data.AccountStore
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
            configFile = File("psoserv.conf").takeIf { it.isFile }
        }

        // Parse the config file if we found one, otherwise use default config.
        val config: Config = if (configFile != null) {
            LOGGER.info { "Using configuration file $configFile." }

            if (!configFile.exists()) {
                error(""""$configFile" does not exist.""")
            } else if (!configFile.isFile) {
                error(""""$configFile" is not a file.""")
            } else if (!configFile.canRead()) {
                error("""Don't have permission to read "$configFile".""")
            }

            @OptIn(ExperimentalSerializationApi::class)
            Hocon.decodeFromConfig(ConfigFactory.parseFile(configFile))
        } else {
            DEFAULT_CONFIG
        }

        // Initialize and start the server.
        val accountStore = AccountStore(LOGGER)
        val servers = initialize(config, accountStore)

        if (servers.isEmpty()) {
            LOGGER.info { "No servers configured, stopping." }
        } else {
            LOGGER.info { "Starting up." }

            servers.forEach(Server::start)
        }
    } catch (e: Throwable) {
        LOGGER.error(e) { "Failed to start up." }
    }
}

private fun initialize(config: Config, accountStore: AccountStore): List<Server> {
    val address = config.address?.let(::inet4Address) ?: DEFAULT_ADDRESS

    LOGGER.info { "Binding to $address." }

    val accountPort = config.account?.port ?: DEFAULT_ACCOUNT_PORT

    // Maps block name to block.
    val blocks: Map<String, BlockInfo> = run {
        var blockI = 1
        var blockPort = DEFAULT_FIRST_SHIP_PORT + config.ships.size
        val blocks = mutableMapOf<String, BlockInfo>()

        for (blockCfg in config.blocks) {
            val block = BlockInfo(
                name = validateName("Block", blockCfg.name) ?: "block_$blockI",
                uiName = blockCfg.uiName ?: "BLOCK${blockI.toString(2).padStart(2, '0')}",
                bindPair = Inet4Pair(address, blockCfg.port ?: blockPort++),
            )
            blockI++

            require(blocks.put(block.name, block) == null) {
                """Duplicate block with name ${block.name}."""
            }
        }

        blocks
    }

    val ships: List<ShipInfo> = run {
        var shipI = 1
        var shipPort = DEFAULT_FIRST_SHIP_PORT

        config.ships.map { shipCfg ->
            val ship = ShipInfo(
                name = validateName("Ship", shipCfg.name) ?: "ship_$shipI",
                uiName = shipCfg.uiName ?: "Ship $shipI",
                bindPair = Inet4Pair(address, shipCfg.port ?: shipPort++),
                blocks = shipCfg.blocks.map {
                    blocks[it] ?: error("""No block with name $it.""")
                },
            )
            shipI++
            ship
        }
    }

    val servers = mutableListOf<Server>()

    if (config.patch != null) {
        val bindPair = Inet4Pair(address, config.patch.port ?: DEFAULT_PATCH_PORT)

        LOGGER.info { "Configuring patch server to bind to port ${bindPair.port}." }

        servers.add(
            PatchServer(
                bindPair,
                welcomeMessage = config.patch.welcomeMessage ?: "Welcome to Phantasmal World.",
            )
        )
    }

    if (config.auth != null) {
        val bindPair = Inet4Pair(address, config.auth.port ?: DEFAULT_LOGIN_PORT)

        LOGGER.info { "Configuring auth server to bind to port ${bindPair.port}." }
        LOGGER.info {
            "Auth server will redirect to account server on port $accountPort."
        }

        servers.add(
            AuthServer(
                bindPair,
                accountServerAddress = address,
                accountServerPort = accountPort,
            )
        )
    }

    if (config.account != null) {
        val bindPair = Inet4Pair(address, config.account.port ?: DEFAULT_ACCOUNT_PORT)

        LOGGER.info { "Configuring account server to bind to port ${bindPair.port}." }
        LOGGER.info {
            "Account server will redirect to ${ships.size} ship servers: ${
                ships.joinToString { """${it.name} (port ${it.bindPair.port})""" }
            }."
        }

        servers.add(
            AccountServer(
                accountStore,
                bindPair,
                ships,
            )
        )
    }

    for (ship in ships) {
        LOGGER.info {
            """Configuring ship server ${ship.name} ("${ship.uiName}") to bind to port ${ship.bindPair.port}."""
        }

        servers.add(
            ShipServer(
                ship.name,
                ship.bindPair,
                ship.uiName,
                ship.blocks,
            )
        )
    }

    for ((index, block) in blocks.values.withIndex()) {
        LOGGER.info {
            """Configuring block server ${block.name} ("${block.uiName}") to bind to port ${block.bindPair.port}."""
        }

        servers.add(
            BlockServer(
                accountStore,
                block.name,
                block.bindPair,
                blockId = index + 1,
            )
        )
    }

    config.proxy?.let { proxyConfig ->
        servers.addAll(initializeProxy(proxyConfig, address))
    }

    return servers
}

private fun initializeProxy(config: ProxyConfig, address: Inet4Address): List<ProxyServer> {
    val defaultRemoteAddress = config.remoteAddress?.let(::inet4Address) ?: DEFAULT_ADDRESS
    val redirectMap = mutableMapOf<Inet4Pair, Inet4Pair>()
    val proxyServers = mutableListOf<ProxyServer>()
    var nameI = 1

    for (psc in config.servers) {
        val name = validateName("Proxy server", psc.name) ?: "proxy_${nameI++}"
        val bindPair = Inet4Pair(address, psc.bindPort)
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

private fun validateName(whichName: String, name: String?): String? =
    if (name == null) {
        null
    } else {
        check(Regex("[a-zA-Z0-9_-]+").matches(name)) {
            """$whichName name "$name" should contain only alpha-numeric characters, minus (-) or underscore (_)."""
        }
        name
    }

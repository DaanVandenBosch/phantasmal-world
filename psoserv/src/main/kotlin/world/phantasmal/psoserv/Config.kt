package world.phantasmal.psoserv

import kotlinx.serialization.Serializable

val DEFAULT_CONFIG: Config = Config(
    address = null,
    patch = PatchServerConfig(),
    auth = ServerConfig(),
    account = ServerConfig(),
    proxy = null,
    ships = listOf(ShipServerConfig(blocks = listOf("block_1"))),
    blocks = listOf(BlockServerConfig(name = "block_1"))
)

@Serializable
class Config(
    /**
     * Default address used by any servers when no server-specific address is provided. Itself
     * defaults to the loopback address localhost/127.0.0.1.
     */
    val address: String? = null,
    val patch: PatchServerConfig? = null,
    val auth: ServerConfig? = null,
    val account: ServerConfig? = null,
    val ships: List<ShipServerConfig> = emptyList(),
    val blocks: List<BlockServerConfig> = emptyList(),
    val proxy: ProxyConfig? = null,
)

@Serializable
class ServerConfig(
    val port: Int? = null,
)

@Serializable
class ShipServerConfig(
    /**
     * Name for internal use, e.g. logging.
     */
    val name: String? = null,
    /**
     * Name shown to players.
     */
    val uiName: String? = null,
    val port: Int? = null,
    /**
     * List of internal block names. This ship will redirect to only these blocks.
     */
    val blocks: List<String> = emptyList(),
)

@Serializable
class BlockServerConfig(
    /**
     * Name for internal use, e.g. logging.
     */
    val name: String? = null,
    /**
     * Name shown to players.
     */
    val uiName: String? = null,
    val port: Int? = null,
)

@Serializable
class PatchServerConfig(
    /**
     * Sent to players when they connect to the patch server.
     */
    val welcomeMessage: String? = null,
    val port: Int? = null,
)

@Serializable
class ProxyConfig(
    val remoteAddress: String? = null,
    val servers: List<ProxyServerConfig> = emptyList(),
)

@Serializable
class ProxyServerConfig(
    /**
     * Name for internal use, e.g. logging.
     */
    val name: String? = null,
    /**
     * Determines how messages are interpreted and which encryption is used.
     */
    val version: GameVersionConfig,
    val bindPort: Int,
    /**
     * The address of the server that's being proxied.
     */
    val remoteAddress: String? = null,
    /**
     * The port of the server that's being proxied.
     */
    val remotePort: Int,
)

@Serializable
enum class GameVersionConfig {
    PC, BB
}

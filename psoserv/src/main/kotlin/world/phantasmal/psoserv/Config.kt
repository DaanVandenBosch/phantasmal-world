package world.phantasmal.psoserv

import kotlinx.serialization.Serializable

@Serializable
class Config(
    val address: String? = null,
    val patch: PatchServerConfig? = null,
    val auth: ServerConfig? = null,
    val account: ServerConfig? = null,
    val proxy: ProxyConfig? = null,
)

@Serializable
class ServerConfig(
    val run: Boolean = true,
    val address: String? = null,
    val port: Int? = null,
)

@Serializable
class PatchServerConfig(
    val run: Boolean = true,
    val welcomeMessage: String? = null,
    val address: String? = null,
    val port: Int? = null,
)

@Serializable
class ProxyConfig(
    val run: Boolean = true,
    val bindAddress: String? = null,
    val remoteAddress: String? = null,
    val servers: List<ProxyServerConfig> = emptyList(),
)

@Serializable
class ProxyServerConfig(
    val name: String? = null,
    val version: GameVersionConfig,
    val bindAddress: String? = null,
    val bindPort: Int,
    val remoteAddress: String? = null,
    val remotePort: Int,
)

@Serializable
enum class GameVersionConfig {
    PC, BB
}

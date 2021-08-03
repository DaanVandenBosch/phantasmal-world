package world.phantasmal.psoserv.servers

class ShipInfo(
    val name: String,
    val uiName: String,
    val bindPair: Inet4Pair,
    val blocks: List<BlockInfo>,
)

class BlockInfo(
    val name: String,
    val uiName: String,
    val bindPair: Inet4Pair,
)

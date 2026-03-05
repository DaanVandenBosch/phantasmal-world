package world.phantasmal.web.questEditor.loading

import org.khronos.webgl.ArrayBuffer
import world.phantasmal.psolib.Endianness
import world.phantasmal.psolib.Episode
import world.phantasmal.psolib.buffer.Buffer
import world.phantasmal.psolib.cursor.Cursor
import world.phantasmal.psolib.cursor.cursor
import world.phantasmal.psolib.fileFormats.quest.Quest
import world.phantasmal.psolib.fileFormats.quest.parseBinDatToQuest
import world.phantasmal.psolib.fileFormats.quest.parseQstToQuest
import world.phantasmal.web.core.loading.AssetLoader
import world.phantasmal.web.core.loading.LoadingCache
import world.phantasmal.webui.DisposableContainer

private class CityConfig(
    val binPrefix: String,
    val npcDat: String,
    val npcDatOffline: String,
    val objDat: String,
    val objDatOffline: String,
    val hasUltimate: Boolean,
)

private val CITY_CONFIGS = mapOf(
    Episode.I to CityConfig(
        binPrefix = "map_city",
        npcDat = "map_city00_00e.dat",
        npcDatOffline = "map_city00_00e_s.dat",
        objDat = "map_city00_00o.dat",
        objDatOffline = "map_city00_00o_s.dat",
        hasUltimate = false,
    ),
    Episode.II to CityConfig(
        binPrefix = "map_labo",
        npcDat = "map_labo00_00e.dat",
        npcDatOffline = "map_labo00_00e_s.dat",
        objDat = "map_labo00_00o.dat",
        objDatOffline = "map_labo00_00o_s.dat",
        hasUltimate = true,
    ),
    Episode.IV to CityConfig(
        binPrefix = "map_city02",
        npcDat = "map_city02_00_00e.dat",
        npcDatOffline = "map_city02_00_00e_s.dat",
        objDat = "map_city02_00_00o.dat",
        objDatOffline = "map_city02_00_00o_s.dat",
        hasUltimate = false,
    ),
)

// DAT table header constants.
private const val DAT_HEADER_SIZE = 16
private const val DAT_ENTITY_TYPE_OBJ = 1
private const val DAT_ENTITY_TYPE_NPC = 2

class QuestLoader(private val assetLoader: AssetLoader) : DisposableContainer() {
    private val cache = addDisposable(
        LoadingCache<String, ArrayBuffer>(
            { path -> assetLoader.loadArrayBuffer("/quests$path") },
            { /* Nothing to dispose. */ }
        )
    )

    suspend fun loadDefaultQuest(episode: Episode): Quest {
        val ver = episode.toInt()
        return loadQuest("/defaults/default_ep_$ver.qst")
    }

    suspend fun loadCityLobbyQuest(
        episode: Episode,
        offline: Boolean = false,
        ultimate: Boolean = false,
    ): Quest {
        val config = CITY_CONFIGS[episode]
            ?: error("Episode $episode not supported.")

        // Build bin filename: {prefix}_{on|off}_j[_u].bin
        val onOff = if (offline) "off" else "on"
        val ult = if (ultimate && config.hasUltimate) "_u" else ""
        val binName = "${config.binPrefix}_${onOff}_j${ult}.bin"

        val npcName = if (offline) config.npcDatOffline else config.npcDat
        val objName = if (offline) config.objDatOffline else config.objDat

        val binBuf = assetLoader.loadArrayBuffer("/city/$binName")
        val npcBuf = assetLoader.loadArrayBuffer("/city/$npcName")
        val objBuf = assetLoader.loadArrayBuffer("/city/$objName")

        val datCursor = synthesizeDat(
            objData = Buffer.fromArrayBuffer(objBuf, Endianness.Little),
            npcData = Buffer.fromArrayBuffer(npcBuf, Endianness.Little),
        )

        val questResult = parseBinDatToQuest(
            binCursor = binBuf.cursor(Endianness.Little),
            datCursor = datCursor,
            lenient = true,
            compressed = false,
        )

        val quest = questResult.unwrap()

        // Lobby .bin files may lack SET_EPISODE, so force the correct episode.
        quest.episode = episode

        for (npc in quest.npcs) {
            npc.episode = episode
        }

        return quest
    }

    private suspend fun loadQuest(path: String): Quest =
        parseQstToQuest(cache.get(path).cursor(Endianness.Little)).unwrap().quest
}

/**
 * Synthesize a standard DAT file from raw object and NPC entity arrays.
 * Format: [obj table header + obj data] [npc table header + npc data] [16-byte zero terminator]
 */
private fun synthesizeDat(objData: Buffer, npcData: Buffer): Cursor {
    val objSize = objData.size
    val npcSize = npcData.size
    val totalSize = DAT_HEADER_SIZE + objSize + DAT_HEADER_SIZE + npcSize + DAT_HEADER_SIZE

    val buf = Buffer.withSize(totalSize, Endianness.Little)
    var offset = 0

    // Object table header.
    buf.setInt(offset, DAT_ENTITY_TYPE_OBJ)
    buf.setInt(offset + 4, DAT_HEADER_SIZE + objSize)
    buf.setInt(offset + 8, 0) // areaId
    buf.setInt(offset + 12, objSize)
    offset += DAT_HEADER_SIZE

    // Object data.
    objData.copyInto(buf, destinationOffset = offset)
    offset += objSize

    // NPC table header.
    buf.setInt(offset, DAT_ENTITY_TYPE_NPC)
    buf.setInt(offset + 4, DAT_HEADER_SIZE + npcSize)
    buf.setInt(offset + 8, 0) // areaId
    buf.setInt(offset + 12, npcSize)
    offset += DAT_HEADER_SIZE

    // NPC data.
    npcData.copyInto(buf, destinationOffset = offset)
    offset += npcSize

    // Terminator: 16 zero bytes (already zero from Buffer.withSize).

    return buf.cursor()
}
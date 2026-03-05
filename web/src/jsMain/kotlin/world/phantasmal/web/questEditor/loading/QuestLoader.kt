package world.phantasmal.web.questEditor.loading

import kotlinx.coroutines.await
import org.khronos.webgl.ArrayBuffer
import world.phantasmal.core.externals.browser.FileSystemDirectoryHandle
import world.phantasmal.core.externals.browser.arrayBuffer
import world.phantasmal.psolib.Endianness
import world.phantasmal.psolib.Episode
import world.phantasmal.psolib.buffer.Buffer
import world.phantasmal.psolib.cursor.Cursor
import world.phantasmal.psolib.cursor.cursor
import world.phantasmal.psolib.fileFormats.quest.NPC_BYTE_SIZE
import world.phantasmal.psolib.fileFormats.quest.OBJECT_BYTE_SIZE
import world.phantasmal.psolib.fileFormats.quest.Quest
import world.phantasmal.psolib.fileFormats.quest.parseBinDatToQuest
import world.phantasmal.psolib.fileFormats.quest.parseQstToQuest
import world.phantasmal.web.core.loading.AssetLoader
import world.phantasmal.web.core.loading.LoadingCache
import world.phantasmal.webui.DisposableContainer

class CityLobbyResult(
    val quest: Quest,
    val binName: String,
    val npcDatName: String,
    val objDatName: String,
)

private val EPISODE_TO_BIN_PREFIX = mapOf(
    Episode.I to "map_city",
    Episode.II to "map_labo",
    Episode.IV to "map_city02",
)

private val EPISODE_TO_DAT_PREFIX = mapOf(
    Episode.I to "map_city00_00",
    Episode.II to "map_labo00_00",
    Episode.IV to "map_city02_00_00",
)

/** Language suffixes to try when looking for bin files. */
private val BIN_LANG_SUFFIXES = listOf("j", "e")

private val BIN_PREFIX_TO_EPISODE = mapOf(
    "map_city" to Episode.I,
    "map_labo" to Episode.II,
    "map_city02" to Episode.IV,
)

/**
 * Parse a city lobby bin filename to extract episode, online/offline, and ultimate.
 * Expected pattern: `{prefix}_{on|off}_{lang}[_u].bin`
 * Returns null if the filename doesn't match.
 */
fun parseCityBinFilename(binName: String): CityBinInfo? {
    val regex = Regex("""^(map_city02|map_city|map_labo)_(on|off)_[a-z](_u)?\.bin$""")
    val match = regex.matchEntire(binName) ?: return null

    val prefix = match.groupValues[1]
    val episode = BIN_PREFIX_TO_EPISODE[prefix] ?: return null
    val offline = match.groupValues[2] == "off"
    val ultimate = match.groupValues[3] == "_u"

    return CityBinInfo(
        episode = episode,
        offline = offline,
        ultimate = ultimate,
    )
}

class CityBinInfo(
    val episode: Episode,
    val offline: Boolean,
    val ultimate: Boolean,
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
        gameDirHandle: FileSystemDirectoryHandle,
        episode: Episode,
        offline: Boolean = false,
        ultimate: Boolean = false,
    ): CityLobbyResult {
        val binPrefix = EPISODE_TO_BIN_PREFIX[episode]
            ?: error("Episode $episode not supported.")
        val datPrefix = EPISODE_TO_DAT_PREFIX[episode]
            ?: error("Episode $episode not supported.")

        val onOff = if (offline) "off" else "on"
        val ult = if (ultimate && episode == Episode.II) "_u" else ""
        val offlineSuffix = if (offline) "_s" else ""

        val npcDatName = "${datPrefix}e${offlineSuffix}.dat"
        val objDatName = "${datPrefix}o${offlineSuffix}.dat"

        // Support both: user picked game root (has data/ subdir) or data/ directly.
        val dataDir = try {
            gameDirHandle.getDirectoryHandle("data").await()
        } catch (_: Throwable) {
            gameDirHandle
        }

        // Try different language suffixes for the bin file.
        var binName: String? = null
        var binBuf: ArrayBuffer? = null

        for (lang in BIN_LANG_SUFFIXES) {
            val candidate = "${binPrefix}_${onOff}_${lang}${ult}.bin"
            try {
                binBuf = dataDir.getFileHandle(candidate).await()
                    .getFile().await().arrayBuffer().await()
                binName = candidate
                break
            } catch (_: Throwable) {
                // File doesn't exist with this language suffix, try next.
            }
        }

        if (binName == null || binBuf == null) {
            val tried = BIN_LANG_SUFFIXES.joinToString { "${binPrefix}_${onOff}_${it}${ult}.bin" }
            error("City bin file not found. Tried: $tried")
        }

        val npcBuf = dataDir.getFileHandle(npcDatName).await().getFile().await().arrayBuffer().await()
        val objBuf = dataDir.getFileHandle(objDatName).await().getFile().await().arrayBuffer().await()

        val datCursor = synthesizeDat(
            objData = Buffer.fromArrayBuffer(objBuf, Endianness.Little),
            npcData = Buffer.fromArrayBuffer(npcBuf, Endianness.Little),
        )

        val quest = parseBinDatToQuest(
            binCursor = binBuf.cursor(Endianness.Little),
            datCursor = datCursor,
            lenient = true,
            compressed = false,
        ).unwrap()

        // Lobby .bin files may lack SET_EPISODE, so force the correct episode.
        quest.episode = episode

        for (npc in quest.npcs) {
            npc.episode = episode
        }

        return CityLobbyResult(quest, binName, npcDatName, objDatName)
    }

    private suspend fun loadQuest(path: String): Quest =
        parseQstToQuest(cache.get(path).cursor(Endianness.Little)).unwrap().quest
}

/**
 * Extract raw entity data from a quest, returning separate object and NPC buffers
 * suitable for writing back to city lobby dat files.
 */
fun extractRawEntityData(quest: Quest): Pair<Buffer, Buffer> {
    val objSize = quest.objects.size * OBJECT_BYTE_SIZE
    val npcSize = quest.npcs.size * NPC_BYTE_SIZE

    val objBuf = Buffer.withSize(objSize, Endianness.Little)
    var offset = 0
    for (obj in quest.objects) {
        obj.data.copyInto(objBuf, destinationOffset = offset)
        offset += OBJECT_BYTE_SIZE
    }

    val npcBuf = Buffer.withSize(npcSize, Endianness.Little)
    offset = 0
    for (npc in quest.npcs) {
        npc.data.copyInto(npcBuf, destinationOffset = offset)
        offset += NPC_BYTE_SIZE
    }

    return Pair(objBuf, npcBuf)
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
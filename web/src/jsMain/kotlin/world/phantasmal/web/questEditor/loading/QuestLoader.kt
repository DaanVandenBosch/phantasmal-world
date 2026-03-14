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
import world.phantasmal.psolib.asm.BytecodeIr
import world.phantasmal.psolib.asm.dataFlowAnalysis.FloorMapping
import world.phantasmal.psolib.fileFormats.quest.*
import world.phantasmal.web.core.loading.AssetLoader
import world.phantasmal.web.core.loading.LoadingCache
import world.phantasmal.webui.DisposableContainer

/**
 * Result of loading a free roam quest (city/lab or multi-floor field area).
 */
class FreeRoamResult(
    val quest: Quest,
    val binName: String,
    /** Map of floorId → (objDatName, npcDatName) for saving back. */
    val datFilesByFloor: Map<Int, Pair<String, String>>,
)

/** Language suffixes to try when looking for bin files. */
private val BIN_LANG_SUFFIXES = listOf("j", "e")

private val CITY_BIN_PREFIX_TO_EPISODE = mapOf(
    "map_city" to Episode.I,
    "map_labo" to Episode.II,
    "map_city02" to Episode.IV,
)

// Note: Only EP2 has field free roam bins in standard BB data.
// EP1 (forest/cave/machine/ancient) entries are kept for custom server compatibility.
private val FIELD_BIN_PREFIX_TO_EPISODE = mapOf(
    "map_forest" to Episode.I,
    "map_cave" to Episode.I,
    "map_machine" to Episode.I,
    "map_ancient" to Episode.I,    // EP1 Ruins (not EP2!)
    "map_ruin" to Episode.II,      // EP2 VR Temple (bin is map_ruin, dat token is ruins01)
    "map_space" to Episode.II,     // EP2 VR Spaceship
    "map_jungle" to Episode.II,    // EP2 CCA + Jungle + Mountain + Seaside
    "map_seabed" to Episode.II,
)

/**
 * Parsed info from a free roam bin filename.
 */
sealed class FreeRoamBinInfo(val episode: Episode, val ultimate: Boolean) {
    /** City/Lab bin: has on/off (online/offline) mode, single floor (floor 0). */
    class City(episode: Episode, val offline: Boolean, ultimate: Boolean)
        : FreeRoamBinInfo(episode, ultimate) {
        val binPrefix: String = when (episode) {
            Episode.I -> "map_city"
            Episode.II -> "map_labo"
            Episode.IV -> "map_city02"
        }
    }

    /** Field bin: multi-floor with per-floor dat files, no on/off distinction. */
    class Field(episode: Episode, val prefix: String, ultimate: Boolean, val floorRange: IntRange)
        : FreeRoamBinInfo(episode, ultimate)
}

/**
 * Parse a free roam bin filename (city or field).
 *
 * City pattern: `map_{prefix}_{on|off}_{lang}[_u].bin`
 * Field pattern: `map_{prefix}_{lang}[_u].bin`
 *
 * Returns null if the filename doesn't match any known free roam bin pattern.
 */
fun parseFreeRoamBinFilename(binName: String): FreeRoamBinInfo? {
    // Try city pattern first (more specific due to on/off).
    CITY_BIN_REGEX.matchEntire(binName)?.let { match ->
        val prefix = match.groupValues[1]
        val episode = CITY_BIN_PREFIX_TO_EPISODE[prefix] ?: return null
        val offline = match.groupValues[2] == "off"
        val ultimate = match.groupValues[3] == "_u"
        return FreeRoamBinInfo.City(episode, offline, ultimate)
    }

    // Try field pattern.
    FIELD_BIN_REGEX.matchEntire(binName)?.let { match ->
        val prefix = match.groupValues[1]
        val episode = FIELD_BIN_PREFIX_TO_EPISODE[prefix] ?: return null
        val ultimate = match.groupValues[2] == "_u"
        val floorRange = FIELD_BIN_PREFIX_TO_FLOORS[prefix] ?: return null
        return FreeRoamBinInfo.Field(episode, prefix, ultimate, floorRange)
    }

    return null
}

private val CITY_BIN_REGEX =
    Regex("""^(map_city02|map_city|map_labo)_(on|off)_[a-z](_u)?\.bin$""")

private val FIELD_BIN_REGEX =
    Regex("""^(map_forest|map_cave|map_machine|map_ancient|map_ruin|map_space|map_jungle|map_seabed)_[a-z](_u)?\.bin$""")

/**
 * Info for loading a dat-only free roam area (no bin script available).
 * Used for EP1 field areas and EP4 field areas that lack bin files.
 */
class DatOnlyAreaInfo(
    val episode: Episode,
    val floorRange: IntRange,
)

/**
 * Result of loading a dat-only free roam area.
 */
class DatOnlyResult(
    val quest: Quest,
    /** Map of floorId → (objDatName, npcDatName) for saving back. */
    val datFilesByFloor: Map<Int, Pair<String, String>>,
)

/**
 * Parse a free roam dat filename to determine the area group.
 * Pattern: `map_{token}_{variants}{o|e}[_s].dat`
 *
 * Returns null if the filename doesn't match a known free roam dat pattern.
 */
fun parseFreeRoamDatFilename(datName: String): DatOnlyAreaInfo? {
    val match = FREE_ROAM_DAT_REGEX.matchEntire(datName) ?: return null
    val tokenBase = match.groupValues[1]
    val (episode, floorRange) = DAT_TOKEN_BASE_TO_AREA_GROUP[tokenBase] ?: return null
    return DatOnlyAreaInfo(episode, floorRange)
}

private val FREE_ROAM_DAT_REGEX =
    Regex("""^map_([a-z]+)\d+_.*[oe](_s)?\.dat$""")

/** Maps dat token base (without digit suffix) → (episode, floor range). */
private val DAT_TOKEN_BASE_TO_AREA_GROUP: Map<String, Pair<Episode, IntRange>> = mapOf(
    // EP1 field (no bin available)
    "forest" to Pair(Episode.I, 1..2),
    "cave" to Pair(Episode.I, 3..5),
    "machine" to Pair(Episode.I, 6..7),
    "ancient" to Pair(Episode.I, 8..10),
    // EP2 field (has bin, but dat-only is still valid for entity-only editing)
    "ruins" to Pair(Episode.II, 1..2),
    "space" to Pair(Episode.II, 3..4),
    "jungle" to Pair(Episode.II, 5..9),
    "seabed" to Pair(Episode.II, 10..11),
    // EP4 field (no bin available)
    "wilds" to Pair(Episode.IV, 1..4),
    "crater" to Pair(Episode.IV, 5..5),
    "desert" to Pair(Episode.IV, 6..8),
)

/** Maps field bin prefix → floor range covered by that area group. */
private val FIELD_BIN_PREFIX_TO_FLOORS = mapOf(
    "map_forest" to 1..2,     // Forest 1-2
    "map_cave" to 3..5,       // Cave 1-3
    "map_machine" to 6..7,    // Mine 1-2
    "map_ancient" to 8..10,   // Ruins 1-3
    "map_ruin" to 1..2,       // EP2 VR Temple Alpha-Beta
    "map_space" to 3..4,      // EP2 VR Spaceship Alpha-Beta
    "map_jungle" to 5..9,     // EP2 CCA + Jungle + Mountain + Seaside
    "map_seabed" to 10..11,   // EP2 Seabed Upper-Lower
)

// DAT table header constants.
private const val DAT_HEADER_SIZE = 16
private const val DAT_ENTITY_TYPE_OBJ = 1
private const val DAT_ENTITY_TYPE_NPC = 2
private const val DAT_ENTITY_TYPE_EVENT = 3

/**
 * A floor's raw entity data for synthesizing into standard DAT format.
 */
class DatFloorSection(
    val floorId: Int,
    val objData: Buffer,
    val npcData: Buffer,
    /** Raw .evt file content (EventsSectionHeader + Event1Entry[] + action stream). */
    val eventData: Buffer = Buffer.withSize(0, Endianness.Little),
)

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

    /**
     * Load a free roam quest (city/lab or multi-floor field area) from
     * separate bin + per-floor dat files.
     * Uses default variants (v1[0], v2[0]) for each floor's dat files.
     */
    suspend fun loadFreeRoamQuest(
        gameDirHandle: FileSystemDirectoryHandle,
        info: FreeRoamBinInfo,
        selectedV1: Int = 0,
        selectedV2: Int = 0,
    ): FreeRoamResult {
        val episode = info.episode
        val ult = if (info.ultimate && episode == Episode.II) "_u" else ""

        // Support both: user picked game root (has data/ subdir) or data/ directly.
        val dataDir = try {
            gameDirHandle.getDirectoryHandle("data").await()
        } catch (_: Throwable) {
            gameDirHandle
        }

        // Find the bin file (try language suffixes).
        var binName: String? = null
        var binBuf: ArrayBuffer? = null

        for (lang in BIN_LANG_SUFFIXES) {
            val candidate = when (info) {
                is FreeRoamBinInfo.City -> {
                    val onOff = if (info.offline) "off" else "on"
                    "${info.binPrefix}_${onOff}_${lang}${ult}.bin"
                }
                is FreeRoamBinInfo.Field -> "${info.prefix}_${lang}${ult}.bin"
            }
            try {
                binBuf = dataDir.getFileHandle(candidate).await()
                    .getFile().await().arrayBuffer().await()
                binName = candidate
                break
            } catch (_: Throwable) {
                // Try next language suffix.
            }
        }

        if (binName == null || binBuf == null) {
            val tried = BIN_LANG_SUFFIXES.joinToString { lang ->
                when (info) {
                    is FreeRoamBinInfo.City -> {
                        val onOff = if (info.offline) "off" else "on"
                        "${info.binPrefix}_${onOff}_${lang}${ult}.bin"
                    }
                    is FreeRoamBinInfo.Field -> "${info.prefix}_${lang}${ult}.bin"
                }
            }
            error("Free roam bin file not found. Tried: $tried")
        }

        // Determine floor range: city/lab = floor 0 only, field = from mapping table.
        val floorRange = when (info) {
            is FreeRoamBinInfo.City -> 0..0
            is FreeRoamBinInfo.Field -> info.floorRange
        }

        // Offline city dat files have a "_s" suffix before the extension.
        val offlineSuffix = if (info is FreeRoamBinInfo.City && info.offline) "_s" else ""

        // Load per-floor dat files using SetDataTable info.
        val sections = mutableListOf<DatFloorSection>()
        val datFilesByFloor = mutableMapOf<Int, Pair<String, String>>()

        for (floor in floorRange) {
            val floorInfo = getFloorFileInfo(episode, floor) ?: continue

            val v1 = if (selectedV1 in floorInfo.v1Values) selectedV1
                     else floorInfo.v1Values.firstOrNull() ?: 0
            val v2 = if (selectedV2 in floorInfo.v2Values) selectedV2
                     else floorInfo.v2Values.firstOrNull() ?: 0

            val objDatBase = resolveDatFilename(floorInfo, v1, v2, DatFileType.OBJECTS)
            val npcDatBase = resolveDatFilename(floorInfo, v1, v2, DatFileType.ENEMIES)

            // Apply offline suffix if needed (e.g., map_city00_00o.dat → map_city00_00o_s.dat).
            val objDatName = if (offlineSuffix.isNotEmpty()) objDatBase.replace(".dat", "${offlineSuffix}.dat") else objDatBase
            val npcDatName = if (offlineSuffix.isNotEmpty()) npcDatBase.replace(".dat", "${offlineSuffix}.dat") else npcDatBase

            val objBuf = try {
                dataDir.getFileHandle(objDatName).await().getFile().await().arrayBuffer().await()
            } catch (_: Throwable) {
                // EP1 Forest object dat files are server-side only.
                // Other EP1 areas have object dats in the client.
                if (objDatName.startsWith("map_forest")) {
                    try { assetLoader.loadArrayBuffer("/maps/ep1-dat/$objDatName") } catch (_: Throwable) { null }
                } else null
            }

            val npcBuf = try {
                dataDir.getFileHandle(npcDatName).await().getFile().await().arrayBuffer().await()
            } catch (_: Throwable) {
                // All EP1 NPC/enemy dat files are server-side only (not in standard BB client).
                if (episode == Episode.I) {
                    try { assetLoader.loadArrayBuffer("/maps/ep1-dat/$npcDatName") } catch (_: Throwable) { null }
                } else null
            }

            sections.add(
                DatFloorSection(
                    floorId = floor,
                    objData = objBuf?.let { Buffer.fromArrayBuffer(it, Endianness.Little) }
                        ?: Buffer.withSize(0, Endianness.Little),
                    npcData = npcBuf?.let { Buffer.fromArrayBuffer(it, Endianness.Little) }
                        ?: Buffer.withSize(0, Endianness.Little),
                )
            )

            datFilesByFloor[floor] = Pair(objDatName, npcDatName)
        }

        val datCursor = synthesizeDat(sections)

        val quest = parseBinDatToQuest(
            binCursor = binBuf.cursor(Endianness.Little),
            datCursor = datCursor,
            lenient = true,
            compressed = false,
        ).unwrap()

        // Free roam bin files may lack SET_EPISODE, so force the correct episode.
        quest.episode = episode

        for (npc in quest.npcs) {
            npc.episode = episode
        }

        // Free roam bins often lack map_designate; build FloorMappings if needed.
        if (quest.floorMappings.isEmpty()) {
            val episodeInt = when (episode) {
                Episode.I -> 0
                Episode.II -> 1
                Episode.IV -> 2
            }
            quest.floorMappings = floorRange.mapNotNull { floor ->
                val mapId = getMapId(episodeInt, floor)
                if (mapId != 0) {
                    val floorInfo = getFloorFileInfo(episode, floor)
                    val v1 = if (floorInfo != null && selectedV1 in floorInfo.v1Values) selectedV1
                             else floorInfo?.v1Values?.firstOrNull() ?: 0
                    FloorMapping(floor, mapId, floor, v1)
                } else null
            }
        }

        return FreeRoamResult(quest, binName, datFilesByFloor)
    }

    /**
     * Load a dat-only free roam area (no bin script).
     * Dat files are loaded from the user's game directory for all episodes.
     * Evt files: EP1 from bundled assets (not in standard BB client), EP4+ from user directory.
     * Constructs a Quest with empty BytecodeIr and manual FloorMappings.
     */
    suspend fun loadDatOnlyQuest(
        gameDirHandle: FileSystemDirectoryHandle,
        info: DatOnlyAreaInfo,
        selectedV1: Int = 0,
        selectedV2: Int = 0,
    ): DatOnlyResult {
        val episode = info.episode

        // Support both: user picked game root (has data/ subdir) or data/ directly.
        val dataDir = try {
            gameDirHandle.getDirectoryHandle("data").await()
        } catch (_: Throwable) {
            gameDirHandle
        }

        val sections = mutableListOf<DatFloorSection>()
        val datFilesByFloor = mutableMapOf<Int, Pair<String, String>>()

        for (floor in info.floorRange) {
            val floorInfo = getFloorFileInfo(episode, floor) ?: continue

            val v1 = if (selectedV1 in floorInfo.v1Values) selectedV1
                     else floorInfo.v1Values.firstOrNull() ?: 0
            val v2 = if (selectedV2 in floorInfo.v2Values) selectedV2
                     else floorInfo.v2Values.firstOrNull() ?: 0

            val objDatName = resolveDatFilename(floorInfo, v1, v2, DatFileType.OBJECTS)
            val npcDatName = resolveDatFilename(floorInfo, v1, v2, DatFileType.ENEMIES)

            // Object dats: Forest is server-side only, other EP1 areas have them in the client.
            val objBuf = try {
                dataDir.getFileHandle(objDatName).await().getFile().await().arrayBuffer().await()
            } catch (_: Throwable) {
                if (objDatName.startsWith("map_forest")) {
                    try { assetLoader.loadArrayBuffer("/maps/ep1-dat/$objDatName") } catch (_: Throwable) { null }
                } else null
            }
            // NPC/enemy dats: all EP1 areas are server-side only.
            val npcBuf = try {
                dataDir.getFileHandle(npcDatName).await().getFile().await().arrayBuffer().await()
            } catch (_: Throwable) {
                if (episode == Episode.I) {
                    try { assetLoader.loadArrayBuffer("/maps/ep1-dat/$npcDatName") } catch (_: Throwable) { null }
                } else null
            }

            // Evt files: EP1 from bundled assets (not in standard BB client),
            // EP4+ from user's game directory.
            val evtName = resolveDatFilename(floorInfo, v1, v2, DatFileType.EVENTS)
            val evtBuf = if (episode == Episode.I) {
                try {
                    assetLoader.loadArrayBuffer("/maps/ep1-events/$evtName")
                } catch (_: Throwable) { null }
            } else {
                try {
                    dataDir.getFileHandle(evtName).await().getFile().await().arrayBuffer().await()
                } catch (_: Throwable) { null }
            }

            sections.add(
                DatFloorSection(
                    floorId = floor,
                    objData = objBuf?.let { Buffer.fromArrayBuffer(it, Endianness.Little) }
                        ?: Buffer.withSize(0, Endianness.Little),
                    npcData = npcBuf?.let { Buffer.fromArrayBuffer(it, Endianness.Little) }
                        ?: Buffer.withSize(0, Endianness.Little),
                    eventData = evtBuf?.let { Buffer.fromArrayBuffer(it, Endianness.Little) }
                        ?: Buffer.withSize(0, Endianness.Little),
                )
            )

            datFilesByFloor[floor] = Pair(objDatName, npcDatName)
        }

        val datCursor = synthesizeDat(sections)
        val dat = parseDat(datCursor)

        val objects = dat.objs.mapTo(mutableListOf()) { QuestObject(it.areaId, it.data) }
        val npcs = dat.npcs.mapTo(mutableListOf()) { QuestNpc(episode, it.areaId, it.data) }

        // Build FloorMappings: for free roam, floorId == areaId.
        val episodeInt = when (episode) {
            Episode.I -> 0
            Episode.II -> 1
            Episode.IV -> 2
        }
        val floorMappings = info.floorRange.mapNotNull { floor ->
            val mapId = getMapId(episodeInt, floor)
            if (mapId != 0) {
                val floorInfo = getFloorFileInfo(episode, floor)
                val v1 = if (floorInfo != null && selectedV1 in floorInfo.v1Values) selectedV1
                         else floorInfo?.v1Values?.firstOrNull() ?: 0
                FloorMapping(floor, mapId, floor, v1)
            } else null
        }

        val quest = Quest(
            id = 0,
            language = 0,
            name = "Free Roam",
            shortDescription = "",
            longDescription = "",
            episode = episode,
            objects = objects,
            npcs = npcs,
            events = dat.events,
            datUnknowns = dat.unknowns,
            bytecodeIr = BytecodeIr(emptyList()),
            shopItems = uintArrayOf(),
            floorMappings = floorMappings,
        )

        return DatOnlyResult(quest, datFilesByFloor)
    }

    private suspend fun loadQuest(path: String): Quest =
        parseQstToQuest(cache.get(path).cursor(Endianness.Little)).unwrap().quest
}

/**
 * Extract raw entity data from a quest, split by floor (areaId).
 * Returns a map of floorId → (objBuffer, npcBuffer) for writing back to per-floor dat files.
 */
fun extractRawEntityDataByFloor(quest: Quest): Map<Int, Pair<Buffer, Buffer>> {
    val objsByFloor = quest.objects.groupBy { it.areaId }
    val npcsByFloor = quest.npcs.groupBy { it.areaId }
    val allFloorIds = (objsByFloor.keys + npcsByFloor.keys).sorted()

    return allFloorIds.associateWith { floorId ->
        val floorObjs = objsByFloor[floorId] ?: emptyList()
        val floorNpcs = npcsByFloor[floorId] ?: emptyList()

        val objBuf = Buffer.withSize(floorObjs.size * OBJECT_BYTE_SIZE, Endianness.Little)
        var offset = 0
        for (obj in floorObjs) {
            obj.data.copyInto(objBuf, destinationOffset = offset)
            offset += OBJECT_BYTE_SIZE
        }

        val npcBuf = Buffer.withSize(floorNpcs.size * NPC_BYTE_SIZE, Endianness.Little)
        offset = 0
        for (npc in floorNpcs) {
            npc.data.copyInto(npcBuf, destinationOffset = offset)
            offset += NPC_BYTE_SIZE
        }

        Pair(objBuf, npcBuf)
    }
}

/**
 * Synthesize a standard DAT file from raw object and NPC entity arrays (single floor, areaId=0).
 */
private fun synthesizeDat(objData: Buffer, npcData: Buffer): Cursor =
    synthesizeDat(listOf(DatFloorSection(floorId = 0, objData, npcData)))

/**
 * Synthesize a standard DAT file from multiple floors' raw entity data.
 *
 * Format per floor: [obj header + obj data] [npc header + npc data]
 * Followed by a 16-byte zero terminator.
 *
 * Each header's areaId field is set to the floor's floorId.
 */
fun synthesizeDat(sections: List<DatFloorSection>): Cursor {
    val totalSize = sections.sumOf { section ->
        val objSize = section.objData.size
        val npcSize = section.npcData.size
        val evtSize = section.eventData.size
        (if (objSize > 0) DAT_HEADER_SIZE + objSize else 0) +
            (if (npcSize > 0) DAT_HEADER_SIZE + npcSize else 0) +
            (if (evtSize > 0) DAT_HEADER_SIZE + evtSize else 0)
    } + DAT_HEADER_SIZE // terminator

    val buf = Buffer.withSize(totalSize, Endianness.Little)
    var offset = 0

    for (section in sections) {
        val objSize = section.objData.size
        val npcSize = section.npcData.size
        val evtSize = section.eventData.size

        if (objSize > 0) {
            buf.setInt(offset, DAT_ENTITY_TYPE_OBJ)
            buf.setInt(offset + 4, DAT_HEADER_SIZE + objSize)
            buf.setInt(offset + 8, section.floorId)
            buf.setInt(offset + 12, objSize)
            offset += DAT_HEADER_SIZE

            section.objData.copyInto(buf, destinationOffset = offset)
            offset += objSize
        }

        if (npcSize > 0) {
            buf.setInt(offset, DAT_ENTITY_TYPE_NPC)
            buf.setInt(offset + 4, DAT_HEADER_SIZE + npcSize)
            buf.setInt(offset + 8, section.floorId)
            buf.setInt(offset + 12, npcSize)
            offset += DAT_HEADER_SIZE

            section.npcData.copyInto(buf, destinationOffset = offset)
            offset += npcSize
        }

        if (evtSize > 0) {
            buf.setInt(offset, DAT_ENTITY_TYPE_EVENT)
            buf.setInt(offset + 4, DAT_HEADER_SIZE + evtSize)
            buf.setInt(offset + 8, section.floorId)
            buf.setInt(offset + 12, evtSize)
            offset += DAT_HEADER_SIZE

            section.eventData.copyInto(buf, destinationOffset = offset)
            offset += evtSize
        }
    }

    // Terminator: 16 zero bytes (already zero from Buffer.withSize).

    return buf.cursor()
}
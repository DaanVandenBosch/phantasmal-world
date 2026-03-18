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
import world.phantasmal.web.core.loading.GslArchive
import world.phantasmal.web.core.loading.LoadingCache
import world.phantasmal.webui.DisposableContainer

// ---- Free roam area info ----

/**
 * Unified info for a free roam area, regardless of whether the user opened a bin or dat file.
 */
class FreeRoamAreaInfo(
    val episode: Episode,
    val floorRange: IntRange,
    /** Bin prefix for looking up the bin file, e.g. "map_forest", "map_city". */
    val binPrefix: String? = null,
    /** Dat token base for looking up per-floor dat/evt files, e.g. "forest", "cave". */
    val tokenBase: String? = null,
    /** Whether this is a city/lab area (has online/offline mode). */
    val isCity: Boolean = false,
    val offline: Boolean = false,
    val ultimate: Boolean = false,
)

/**
 * Result of loading a free roam quest.
 */
class FreeRoamResult(
    val quest: Quest,
    /** The bin filename that was loaded, or null if no bin was available. */
    val binName: String?,
    /** Map of floorId → (objDatName, npcDatName) for saving back. */
    val datFilesByFloor: Map<Int, Pair<String, String>>,
)

// ---- Filename parsing ----

/** Language suffixes to try when looking for bin files. */
private val BIN_LANG_SUFFIXES = listOf("j", "e")

private val CITY_BIN_REGEX =
    Regex("""^(map_city02|map_city|map_labo)_(on|off)_[a-z](_u)?\.bin$""")

private val FIELD_BIN_REGEX =
    Regex("""^(map_forest|map_cave|map_machine|map_ancient|map_ruin|map_space|map_jungle|map_seabed)_[a-z](_u)?\.bin$""")

/**
 * Captures the full dat token including digits, e.g. "forest01", "city00", "city02", "wilds01".
 */
private val FREE_ROAM_DAT_REGEX =
    Regex("""^map_([a-z]+\d*)_.*[oe](_s)?\.dat$""")

/**
 * Single source of truth for all free roam area definitions.
 * Each entry: group key → (episode, floor range, bin prefix or null, isCity).
 */
private data class AreaDef(
    val episode: Episode,
    val floorRange: IntRange,
    val binPrefix: String?,
    val isCity: Boolean = false,
)

private val AREA_DEFS: Map<String, AreaDef> = mapOf(
    // EP1
    "city00"  to AreaDef(Episode.I,  0..0,   "map_city",    isCity = true),
    "forest"  to AreaDef(Episode.I,  1..2,   "map_forest"),
    "cave"    to AreaDef(Episode.I,  3..5,   "map_cave"),
    "machine" to AreaDef(Episode.I,  6..7,   "map_machine"),
    "ancient" to AreaDef(Episode.I,  8..10,  "map_ancient"),
    // EP2
    "labo"    to AreaDef(Episode.II, 0..0,   "map_labo",    isCity = true),
    "ruins"   to AreaDef(Episode.II, 1..2,   "map_ruin"),  // dat "ruins" → bin "map_ruin"
    "space"   to AreaDef(Episode.II, 3..4,   "map_space"),
    "jungle"  to AreaDef(Episode.II, 5..9,   "map_jungle"),
    "seabed"  to AreaDef(Episode.II, 10..11, "map_seabed"),
    // EP4
    "city02"  to AreaDef(Episode.IV, 0..0,   "map_city02",  isCity = true),
    "wilds"   to AreaDef(Episode.IV, 1..4,   null),
    "crater"  to AreaDef(Episode.IV, 5..5,   null),
    "desert"  to AreaDef(Episode.IV, 6..8,   null),
)

/** Reverse lookup: bin prefix → (group key, AreaDef). Built from AREA_DEFS. */
private val BIN_PREFIX_TO_AREA: Map<String, Pair<String, AreaDef>> =
    AREA_DEFS.entries
        .filter { it.value.binPrefix != null }
        .associate { (key, def) -> def.binPrefix!! to Pair(key, def) }

/**
 * Resolves a dat token (captured from filename) to an area group key.
 * Exact match first (handles city00 vs city02), then strips trailing digits.
 */
private fun datTokenToGroupKey(fullToken: String): String {
    if (fullToken in AREA_DEFS) return fullToken
    return fullToken.trimEnd { it.isDigit() }
}

/**
 * Parse a free roam filename (bin or dat) into a unified [FreeRoamAreaInfo].
 * Returns null if the filename doesn't match any known free roam pattern.
 */
fun parseFreeRoamFilename(fileName: String): FreeRoamAreaInfo? {
    // Try city bin pattern.
    CITY_BIN_REGEX.matchEntire(fileName)?.let { match ->
        val prefix = match.groupValues[1]
        val (groupKey, def) = BIN_PREFIX_TO_AREA[prefix] ?: return null
        val offline = match.groupValues[2] == "off"
        val ultimate = match.groupValues[3] == "_u"
        return FreeRoamAreaInfo(
            def.episode, def.floorRange, binPrefix = prefix, tokenBase = groupKey,
            isCity = true, offline = offline, ultimate = ultimate,
        )
    }

    // Try field bin pattern.
    FIELD_BIN_REGEX.matchEntire(fileName)?.let { match ->
        val prefix = match.groupValues[1]
        val (groupKey, def) = BIN_PREFIX_TO_AREA[prefix] ?: return null
        val ultimate = match.groupValues[2] == "_u"
        return FreeRoamAreaInfo(
            def.episode, def.floorRange, binPrefix = prefix, tokenBase = groupKey,
            ultimate = ultimate,
        )
    }

    // Try dat pattern.
    FREE_ROAM_DAT_REGEX.matchEntire(fileName)?.let { match ->
        val fullToken = match.groupValues[1]
        val groupKey = datTokenToGroupKey(fullToken)
        val def = AREA_DEFS[groupKey] ?: return null
        return FreeRoamAreaInfo(
            def.episode, def.floorRange, binPrefix = def.binPrefix, tokenBase = groupKey,
            isCity = def.isCity,
        )
    }

    return null
}

// ---- DAT table header constants ----

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

// ---- Quest Loader ----

class QuestLoader(private val assetLoader: AssetLoader) : DisposableContainer() {
    private val cache = addDisposable(
        LoadingCache<String, ArrayBuffer>(
            { path -> assetLoader.loadArrayBuffer("/quests$path") },
            { /* Nothing to dispose. */ }
        )
    )

    /**
     * Read a file from the data directory, falling back to data.gsl if the loose file is not found.
     */
    private suspend fun readDataFile(
        dataDir: FileSystemDirectoryHandle,
        gsl: GslArchive?,
        fileName: String,
    ): ArrayBuffer? {
        // Try loose file first (Ephinea overrides).
        try {
            return dataDir.getFileHandle(fileName).await()
                .getFile().await().arrayBuffer().await()
        } catch (_: Throwable) {
            // Fall through to GSL.
        }
        // Fall back to data.gsl archive.
        return gsl?.readFile(fileName)
    }

    suspend fun loadDefaultQuest(episode: Episode): Quest {
        val ver = episode.toInt()
        return loadQuest("/defaults/city_ep_$ver.qst")
    }

    /**
     * Load a free roam quest from the user's data directory (with data.gsl fallback).
     *
     * Loads per-floor dat (obj + npc + evt) files and the bin script for the area.
     * Works for all episodes and all area types (city, field).
     */
    suspend fun loadFreeRoamQuest(
        gameDirHandle: FileSystemDirectoryHandle,
        info: FreeRoamAreaInfo,
        selectedV1: Int = 0,
        selectedV2: Int = 0,
    ): FreeRoamResult {
        val episode = info.episode

        // Support both: user picked game root (has data/ subdir) or data/ directly.
        val dataDir = try {
            gameDirHandle.getDirectoryHandle("data").await()
        } catch (_: Throwable) {
            gameDirHandle
        }

        // Open data.gsl for fallback reads.
        val gsl = GslArchive.open(dataDir)

        // Load the bin file.
        val (binBuf, binName) = findBinFile(dataDir, gsl, info)

        // Offline city dat files have a "_s" suffix before the extension.
        val offlineSuffix = if (info.isCity && info.offline) "_s" else ""

        // Load per-floor dat/evt files.
        val sections = mutableListOf<DatFloorSection>()
        val datFilesByFloor = mutableMapOf<Int, Pair<String, String>>()

        for (floor in info.floorRange) {
            val floorInfo = getFloorFileInfo(episode, floor) ?: continue

            val v1 = if (selectedV1 in floorInfo.v1Values) selectedV1
                else floorInfo.v1Values.firstOrNull() ?: 0
            val v2 = if (selectedV2 in floorInfo.v2Values) selectedV2
                else floorInfo.v2Values.firstOrNull() ?: 0

            val objDatBase = resolveDatFilename(floorInfo, v1, v2, DatFileType.OBJECTS)
            val npcDatBase = resolveDatFilename(floorInfo, v1, v2, DatFileType.ENEMIES)
            val evtName = resolveDatFilename(floorInfo, v1, v2, DatFileType.EVENTS)

            // Apply offline suffix if needed (e.g., map_city00_00o.dat → map_city00_00o_s.dat).
            val objDatName = if (offlineSuffix.isNotEmpty())
                objDatBase.replace(".dat", "${offlineSuffix}.dat") else objDatBase
            val npcDatName = if (offlineSuffix.isNotEmpty())
                npcDatBase.replace(".dat", "${offlineSuffix}.dat") else npcDatBase

            val objBuf = readDataFile(dataDir, gsl, objDatName)
            val npcBuf = readDataFile(dataDir, gsl, npcDatName)
            val evtBuf = readDataFile(dataDir, gsl, evtName)

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

        val quest = if (binBuf != null) {
            val shiftJis = binName?.substringBeforeLast('.')?.endsWith("_j") == true
            val q = parseBinDatToQuest(
                binCursor = binBuf.cursor(Endianness.Little),
                datCursor = datCursor,
                lenient = true,
                compressed = false,
                shiftJis = shiftJis,
            ).unwrap()

            q.episode = episode
            // Free roam bin files may have invalid language values; clamp to 0.
            if (q.language < 0) q.language = 0
            for (npc in q.npcs) { npc.episode = episode }

            // Always use our floor mappings — the bin's map_designate may not match
            // the floor range we loaded.
            q.floorMappings = buildFloorMappings(episode, info.floorRange, selectedV1)
            q
        } else {
            val dat = parseDat(datCursor)
            val objects = dat.objs.mapTo(mutableListOf()) { QuestObject(it.areaId, it.data) }
            val npcs = dat.npcs.mapTo(mutableListOf()) { QuestNpc(episode, it.areaId, it.data) }

            Quest(
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
                floorMappings = buildFloorMappings(episode, info.floorRange, selectedV1),
            )
        }

        return FreeRoamResult(quest, binName, datFilesByFloor)
    }

    /**
     * Find the bin file for a free roam area. Tries loose files first, then data.gsl.
     */
    private suspend fun findBinFile(
        dataDir: FileSystemDirectoryHandle,
        gsl: GslArchive?,
        info: FreeRoamAreaInfo,
    ): Pair<ArrayBuffer?, String?> {
        val binPrefix = info.binPrefix ?: return Pair(null, null)
        val ult = if (info.ultimate && info.episode == Episode.II) "_u" else ""

        if (info.isCity) {
            val onOff = if (info.offline) "off" else "on"
            for (lang in BIN_LANG_SUFFIXES) {
                val candidate = "${binPrefix}_${onOff}_${lang}${ult}.bin"
                val buf = readDataFile(dataDir, gsl, candidate)
                if (buf != null) return Pair(buf, candidate)
            }
        } else {
            for (lang in BIN_LANG_SUFFIXES) {
                val candidate = "${binPrefix}_${lang}${ult}.bin"
                val buf = readDataFile(dataDir, gsl, candidate)
                if (buf != null) return Pair(buf, candidate)
                // Also try without ultimate suffix if ult is set.
                if (ult.isNotEmpty()) {
                    val nonUlt = "${binPrefix}_${lang}.bin"
                    val buf2 = readDataFile(dataDir, gsl, nonUlt)
                    if (buf2 != null) return Pair(buf2, nonUlt)
                }
            }
        }

        return Pair(null, null)
    }

    private fun buildFloorMappings(
        episode: Episode,
        floorRange: IntRange,
        selectedV1: Int,
    ): List<FloorMapping> {
        val episodeInt = when (episode) {
            Episode.I -> 0
            Episode.II -> 1
            Episode.IV -> 2
        }
        return floorRange.mapNotNull { floor ->
            val mapId = getMapId(episodeInt, floor)
            if (mapId != 0) {
                val floorInfo = getFloorFileInfo(episode, floor)
                val v1 = if (floorInfo != null && selectedV1 in floorInfo.v1Values) selectedV1
                    else floorInfo?.v1Values?.firstOrNull() ?: 0
                FloorMapping(floor, mapId, floor, v1)
            } else null
        }
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
 * Synthesize a standard DAT file from multiple floors' raw entity data.
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

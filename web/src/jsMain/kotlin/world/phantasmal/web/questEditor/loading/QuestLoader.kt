package world.phantasmal.web.questEditor.loading

import kotlinx.coroutines.await
import org.khronos.webgl.ArrayBuffer
import world.phantasmal.core.externals.browser.FileSystemDirectoryHandle
import world.phantasmal.core.externals.browser.arrayBuffer
import world.phantasmal.psolib.Endianness
import world.phantasmal.psolib.Episode
import world.phantasmal.psolib.buffer.Buffer
import world.phantasmal.psolib.cursor.cursor
import world.phantasmal.psolib.asm.BytecodeIr
import world.phantasmal.psolib.asm.dataFlowAnalysis.FloorMapping
import world.phantasmal.psolib.fileFormats.quest.*
import world.phantasmal.web.core.loading.AssetLoader
import world.phantasmal.web.core.loading.GslArchive
import world.phantasmal.web.core.loading.LoadingCache
import world.phantasmal.webui.DisposableContainer

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
        } catch (_: Exception) {
            // Fall through to GSL.
        }
        // Fall back to data.gsl archive.
        return gsl?.readFile(fileName)
    }

    suspend fun loadDefaultQuest(episode: Episode): Quest {
        val ver = episode.toInt()
        return loadQuest("/defaults/default_ep_$ver.qst")
    }

    suspend fun loadCityQuest(episode: Episode): Quest {
        val ver = episode.toInt()
        return loadQuest("/city/city_ep_$ver.qst")
    }

    suspend fun loadLobbyQuest(variant: Int): Quest =
        loadQuest("/lobby/lobby_${variant.toString().padStart(2, '0')}.qst")

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
        } catch (_: Exception) {
            gameDirHandle
        }

        // Open data.gsl for fallback reads.
        val gsl = GslArchive.open(dataDir)

        // Load the bin file.
        val (binBuf, binName) = findBinFile(dataDir, gsl, info)

        // Offline city dat files have a "_s" suffix before the extension.
        val offlineSuffix = if (info.isCity && info.offline) "_s" else ""

        // Load per-floor dat/evt files.
        val (sections, datFilesByFloor) = loadFloorSections(
            dataDir, gsl, info, offlineSuffix, selectedV1, selectedV2,
        )

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
     * Loads per-floor dat (obj + npc) and evt files for each floor in the area's floor range.
     * Returns the list of [DatFloorSection]s and a map of floorId to (objDatName, npcDatName).
     */
    private suspend fun loadFloorSections(
        dataDir: FileSystemDirectoryHandle,
        gsl: GslArchive?,
        info: FreeRoamAreaInfo,
        offlineSuffix: String,
        selectedV1: Int,
        selectedV2: Int,
    ): Pair<List<DatFloorSection>, Map<Int, Triple<String, String, String>>> {
        val sections = mutableListOf<DatFloorSection>()
        val datFilesByFloor = mutableMapOf<Int, Triple<String, String, String>>()

        for (floor in info.floorRange) {
            val floorInfo = getFloorFileInfo(info.episode, floor) ?: continue

            val v1 = resolveVariant(selectedV1, floorInfo.v1Values)
            val v2 = resolveVariant(selectedV2, floorInfo.v2Values)

            val objDatBase = resolveDatFilename(floorInfo, v1, v2, DatFileType.OBJECTS)
            val npcDatBase = resolveDatFilename(floorInfo, v1, v2, DatFileType.ENEMIES)
            val evtName = resolveDatFilename(floorInfo, v1, v2, DatFileType.EVENTS)

            // Apply offline suffix if needed (e.g., map_city00_00o.dat -> map_city00_00o_s.dat).
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

            datFilesByFloor[floor] = Triple(objDatName, npcDatName, evtName)
        }

        return Pair(sections, datFilesByFloor)
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
        val ult = if (info.ultimate) "_u" else ""

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
        return floorRange.mapNotNull { floor ->
            val mapId = getMapId(episode, floor) ?: return@mapNotNull null
            val floorInfo = getFloorFileInfo(episode, floor)
            val v1 = resolveVariant(selectedV1, floorInfo?.v1Values)
            FloorMapping(floor, mapId, floor, v1)
        }
    }

    private fun resolveVariant(selected: Int, available: List<Int>?): Int =
        if (available != null && selected in available) selected
        else available?.firstOrNull() ?: 0

    private suspend fun loadQuest(path: String): Quest =
        parseQstToQuest(cache.get(path).cursor(Endianness.Little)).unwrap().quest
}

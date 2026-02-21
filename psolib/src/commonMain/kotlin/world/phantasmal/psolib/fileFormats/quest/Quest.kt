package world.phantasmal.psolib.fileFormats.quest

import mu.KotlinLogging
import world.phantasmal.core.*
import world.phantasmal.psolib.Episode
import world.phantasmal.psolib.asm.BytecodeIr
import world.phantasmal.psolib.asm.InstructionSegment
import world.phantasmal.psolib.asm.OP_SET_EPISODE
import world.phantasmal.psolib.asm.dataFlowAnalysis.ControlFlowGraph
import world.phantasmal.psolib.asm.dataFlowAnalysis.FloorMapping
import world.phantasmal.psolib.asm.dataFlowAnalysis.getFloorMappings
import world.phantasmal.psolib.asm.dataFlowAnalysis.getMapDesignations
import world.phantasmal.psolib.buffer.Buffer
import world.phantasmal.psolib.compression.prs.prsCompress
import world.phantasmal.psolib.compression.prs.prsDecompress
import world.phantasmal.psolib.cursor.Cursor
import world.phantasmal.psolib.cursor.cursor

private val logger = KotlinLogging.logger {}

class Quest(
    var id: Int,
    var language: Int,
    var name: String,
    var shortDescription: String,
    var longDescription: String,
    var episode: Episode,
    val objects: MutableList<QuestObject>,
    val npcs: MutableList<QuestNpc>,
    val events: MutableList<DatEvent>,
    /**
     * (Partial) raw DAT data that can't be parsed yet by Phantasmal.
     */
    val datUnknowns: MutableList<DatUnknown>,
    var bytecodeIr: BytecodeIr,
    val shopItems: UIntArray,
    val mapDesignations: MutableMap<Int, MutableSet<Int>>,
    val floorMappings: List<FloorMapping> = emptyList(),
)

/**
 * High level quest parsing function that delegates to [parseBin] and [parseDat].
 */
fun parseBinDatToQuest(
    binCursor: Cursor,
    datCursor: Cursor,
    lenient: Boolean = false,
): PwResult<Quest> {
    val result = PwResult.build<Quest>(logger)

    // Decompress and parse files.
    val binDecompressed = prsDecompress(binCursor)
    result.addResult(binDecompressed)

    if (binDecompressed !is Success) {
        return result.failure()
    }

    val bin = parseBin(binDecompressed.value)

    val datDecompressed = prsDecompress(datCursor)
    result.addResult(datDecompressed)

    if (datDecompressed !is Success) {
        return result.failure()
    }

    val dat = parseDat(datDecompressed.value)
    val objects = dat.objs.mapTo(mutableListOf()) { QuestObject(it.areaId, it.data) }
    // Initialize NPCs with random episode and correct it later.
    val npcs = dat.npcs.mapTo(mutableListOf()) { QuestNpc(Episode.I, it.areaId, it.data) }

    // Extract episode and map designations from byte code.
    var episode = Episode.I
    var mapDesignations = mutableMapOf<Int, MutableSet<Int>>()
    var floorMappings = emptyList<FloorMapping>()

    val parseBytecodeResult = parseBytecode(
        bin.bytecode,
        bin.labelOffsets,
        extractScriptEntryPoints(objects, npcs),
        bin.format == BinFormat.DC_GC,
        lenient,
    )

    result.addResult(parseBytecodeResult)

    if (parseBytecodeResult !is Success) {
        return result.failure()
    }

    val bytecodeIr = parseBytecodeResult.value

    if (bytecodeIr.segments.isEmpty()) {
        result.addProblem(Severity.Warning, "File contains no instruction labels.")
    } else {
        val instructionSegments = bytecodeIr.instructionSegments()

        var label0Segment: InstructionSegment? = null

        for (segment in instructionSegments) {
            if (0 in segment.labels) {
                label0Segment = segment
                break
            }
        }

        if (label0Segment != null) {
            episode = getEpisode(result, label0Segment)

            for (npc in npcs) {
                npc.episode = episode
            }

            // Pass all instruction segments instead of just label 0 segment to support multi-floor quests
            floorMappings = getFloorMappings(instructionSegments) { ControlFlowGraph.create(bytecodeIr) }

            // Update NPC gameAreaId based on floor mappings from map_designate instructions
            // gameAreaId is used for NPC type detection, while areaId remains as floorId for variant mapping
            if (floorMappings.isNotEmpty()) {
                for (npc in npcs) {
                    /*
                     * Use FloorMapping.floorId to match NPC.areaId.
                     *
                     * Reason:
                     * - FloorMapping.floorId represents the original logical floor.
                     * - NPC.areaId also stores the original floor ID.
                     * - FloorMapping.areaId is derived from mapId and may differ
                     *   from floorId when multiple floors share the same map.
                     *
                     * Example:
                     *   Floor 17 -> Tower (mapId 35), variant 0
                     *   Floor 16 -> Tower (mapId 35), variant 1
                     */
                    val mapping = floorMappings.find { it.floorId == npc.areaId }
                    if (mapping != null) {
                        npc.gameAreaId = mapping.areaId
                    }
                }
            }

            // Create mapDesignations based on floor mappings if available
            if (floorMappings.isNotEmpty()) {
                // For bb_map_designate, use floor ID as key with its corresponding variant
                mapDesignations = mutableMapOf()
                for (mapping in floorMappings) {
                    mapDesignations.getOrPut(mapping.areaId) { mutableSetOf() }.add(mapping.variantId)
                }
            } else {
                // Fall back to original logic for non-bb_map_designate quests
                mapDesignations =
                    getMapDesignations(label0Segment) { ControlFlowGraph.create(bytecodeIr) }
            }
        } else {
            result.addProblem(Severity.Warning, "No instruction segment for label 0 found.")
        }
    }

    return result.success(Quest(
        id = bin.questId,
        language = bin.language,
        name = bin.questName,
        shortDescription = bin.shortDescription,
        longDescription = bin.longDescription,
        episode,
        objects,
        npcs,
        events = dat.events,
        datUnknowns = dat.unknowns,
        bytecodeIr,
        shopItems = bin.shopItems,
        mapDesignations,
        floorMappings,
    ))
}

class QuestData(
    val quest: Quest,
    val version: Version,
    val online: Boolean,
)

/**
 * High level .qst parsing function that delegates to [parseQst], [parseBin] and [parseDat].
 */
fun parseQstToQuest(cursor: Cursor, lenient: Boolean = false): PwResult<QuestData> {
    val result = PwResult.build<QuestData>(logger)

    // Extract contained .dat and .bin files.
    val qstResult = parseQst(cursor)
    result.addResult(qstResult)

    if (qstResult !is Success) {
        return result.failure()
    }

    val version = qstResult.value.version
    val online = qstResult.value.online
    val files = qstResult.value.files
    var datFile: QstContainedFile? = null
    var binFile: QstContainedFile? = null

    for (file in files) {
        val fileName = file.filename.trim().lowercase()

        if (fileName.endsWith(".dat")) {
            datFile = file
        } else if (fileName.endsWith(".bin")) {
            binFile = file
        }
    }

    if (datFile == null) {
        return result.addProblem(Severity.Error, "File contains no DAT file.").failure()
    }

    if (binFile == null) {
        return result.addProblem(Severity.Error, "File contains no BIN file.").failure()
    }

    val questResult = parseBinDatToQuest(
        binFile.data.cursor(),
        datFile.data.cursor(),
        lenient,
    )
    result.addResult(questResult)

    if (questResult !is Success) {
        return result.failure()
    }

    return result.success(QuestData(
        questResult.value,
        version,
        online,
    ))
}

/**
 * Defaults to episode I.
 */
private fun getEpisode(rb: PwResultBuilder<*>, func0Segment: InstructionSegment): Episode {
    val setEpisode = func0Segment.instructions.find {
        it.opcode == OP_SET_EPISODE
    }

    if (setEpisode == null) {
        logger.debug { "Function 0 has no set_episode instruction." }
        return Episode.I
    }

    return when (val episode = setEpisode.args[0].value) {
        0 -> Episode.I
        1 -> Episode.II
        2 -> Episode.IV
        else -> {
            rb.addProblem(
                Severity.Warning,
                "Unknown episode $episode in function 0 set_episode instruction."
            )
            Episode.I
        }
    }
}

private fun extractScriptEntryPoints(
    objects: List<QuestObject>,
    npcs: List<QuestNpc>,
): Set<Int> {
    val entryPoints = mutableSetOf(0)

    objects.forEach { obj ->
        obj.scriptLabel?.let(entryPoints::add)
        obj.scriptLabel2?.let(entryPoints::add)
    }

    npcs.forEach { npc ->
        entryPoints.add(npc.scriptLabel)
    }

    return entryPoints
}

/**
 * Returns a .bin and .dat file in that order.
 */
fun writeQuestToBinDat(quest: Quest, version: Version): Pair<Buffer, Buffer> {
    val dat = writeDat(DatFile(
        objs = quest.objects.mapTo(mutableListOf()) { DatEntity(it.areaId, it.data) },
        npcs = quest.npcs.mapTo(mutableListOf()) { DatEntity(it.areaId, it.data) },
        events = quest.events,
        unknowns = quest.datUnknowns,
    ))

    val binFormat = when (version) {
        Version.DC, Version.GC -> BinFormat.DC_GC
        Version.PC -> BinFormat.PC
        Version.BB -> BinFormat.BB
    }

    val (bytecode, labelOffsets) = writeBytecode(quest.bytecodeIr, binFormat == BinFormat.DC_GC)

    val bin = writeBin(BinFile(
        binFormat,
        quest.id,
        quest.language,
        quest.name,
        quest.shortDescription,
        quest.longDescription,
        bytecode,
        labelOffsets,
        quest.shopItems,
    ))

    return Pair(bin, dat)
}

/**
 * Creates a .qst file from [quest].
 */
fun writeQuestToQst(quest: Quest, filename: String, version: Version, online: Boolean): Buffer {
    val (bin, dat) = writeQuestToBinDat(quest, version)

    val baseFilename = (filenameBase(filename) ?: filename).take(11)
    val questName = quest.name.take(if (version == Version.BB) 23 else 31)

    return writeQst(QstContent(
        version,
        online,
        files = listOf(
            QstContainedFile(
                id = quest.id,
                filename = "$baseFilename.dat",
                questName = questName,
                data = prsCompress(dat.cursor()).buffer(),
            ),
            QstContainedFile(
                id = quest.id,
                filename = "$baseFilename.bin",
                questName = questName,
                data = prsCompress(bin.cursor()).buffer(),
            ),
        ),
    ))
}

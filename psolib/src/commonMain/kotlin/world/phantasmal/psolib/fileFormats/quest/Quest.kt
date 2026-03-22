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
import world.phantasmal.psolib.buffer.Buffer
import world.phantasmal.psolib.compression.prs.prsCompress
import world.phantasmal.psolib.compression.prs.prsDecompress
import world.phantasmal.psolib.cursor.Cursor
import world.phantasmal.psolib.cursor.cursor

private val logger = KotlinLogging.logger {}

/**
 * Groups challenge mode specific data from the DAT file.
 */
data class QuestChallengeData(
    val cmRandomSpawns: List<DatCmRandomSpawn> = emptyList(),
    val cmMonsterMappings: List<DatCmMonsterMapping> = emptyList(),
    val cmConfigPool: List<DatCmConfigPool> = emptyList(),
)

class Quest(
    var id: Int,
    var language: Int,
    var name: String,
    var shortDescription: String,
    var longDescription: String,
    var episode: Episode,
    val objects: MutableList<QuestObject>,
    val npcs: MutableList<QuestNpc>,
    val events: List<DatEvent>,
    /**
     * (Partial) raw DAT data that can't be parsed yet by Phantasmal.
     */
    val datUnknowns: List<DatUnknown>,
    val challengeData: QuestChallengeData = QuestChallengeData(),
    var bytecodeIr: BytecodeIr,
    val shopItems: UIntArray,
    var floorMappings: List<FloorMapping> = emptyList(),
    var bytecodeOffset: Int? = null,
    /** Whether DC/GC text fields use Shift-JIS encoding (Japanese). */
    var shiftJis: Boolean = false,
    /** BIN format detected during parsing. Used to restore the correct version on save. */
    var binFormat: BinFormat = BinFormat.BB,
)

/**
 * High level quest parsing function that delegates to [parseBin] and [parseDat].
 */
fun parseBinDatToQuest(
    binCursor: Cursor,
    datCursor: Cursor,
    lenient: Boolean = false,
    compressed: Boolean = true,
    shiftJis: Boolean = false,
): PwResult<Quest> {
    val result = PwResult.build<Quest>(logger)

    // Decompress and parse files.
    val binData: Cursor

    if (compressed) {
        val binDecompressed = prsDecompress(binCursor)
        result.addResult(binDecompressed)

        if (binDecompressed !is Success) {
            return result.failure()
        }

        binData = binDecompressed.value
    } else {
        binData = binCursor
    }

    val bin = parseBin(binData, shiftJis)

    val datData: Cursor

    if (compressed) {
        val datDecompressed = prsDecompress(datCursor)
        result.addResult(datDecompressed)

        if (datDecompressed !is Success) {
            return result.failure()
        }

        datData = datDecompressed.value
    } else {
        datData = datCursor
    }

    val dat = parseDat(datData)
    val objects = dat.objs.mapTo(mutableListOf()) { QuestObject(it.areaId, it.data) }
    // Initialize NPCs with random episode and correct it later.
    val npcs = dat.npcs.mapTo(mutableListOf()) { QuestNpc(Episode.I, it.areaId, it.data) }

    // Extract episode and map designations from byte code.
    var episode = Episode.I
    var floorMappings = emptyList<FloorMapping>()

    val parseBytecodeResult = parseBytecode(
        bin.bytecode,
        bin.labelOffsets,
        extractScriptEntryPoints(objects, npcs),
        bin.format.stringEncoding,
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

            // Extract floor mappings from all instruction segments
            floorMappings = getFloorMappings(instructionSegments) {
                ControlFlowGraph.create(bytecodeIr)
            }

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
        challengeData = QuestChallengeData(
            cmRandomSpawns = dat.cmRandomSpawns,
            cmMonsterMappings = dat.cmMonsterMappings,
            cmConfigPool = dat.cmConfigPool,
        ),
        bytecodeIr,
        shopItems = bin.shopItems,
        floorMappings,
        bytecodeOffset = bin.bytecodeOffset,
        shiftJis = bin.shiftJis,
        binFormat = bin.format,
    ))
}

class BinDatQuestData(
    val quest: Quest,
    val compressed: Boolean,
)

/**
 * Detects whether the BIN data is PRS-compressed or raw by checking the first 4 bytes.
 * An uncompressed BIN starts with its bytecode offset (468, 920, or 4652).
 */
private fun looksLikeUncompressedBin(binCursor: Cursor): Boolean {
    if (binCursor.bytesLeft < 4) return false
    val firstInt = binCursor.int()
    binCursor.seekStart(0)
    return firstInt == 468 || firstInt == 920 || firstInt == 4652
}

/**
 * Attempts to parse BIN/DAT data, catching any exceptions and converting them to a [Failure].
 */
private fun tryParseBinDat(
    binCursor: Cursor,
    datCursor: Cursor,
    lenient: Boolean,
    compressed: Boolean,
    shiftJis: Boolean,
): PwResult<BinDatQuestData> =
    try {
        val result = parseBinDatToQuest(binCursor, datCursor, lenient, compressed, shiftJis)

        if (result is Success) {
            Success(BinDatQuestData(result.value, compressed), result.problems)
        } else {
            result as Failure
        }
    } catch (e: Exception) {
        PwResult.build<BinDatQuestData>(logger)
            .addProblem(Severity.Error, "Couldn't parse file.", cause = e)
            .failure()
    }

/**
 * Auto-detects whether BIN/DAT files are PRS-compressed or raw, then parses.
 */
fun parseBinDatToQuestAutoDetect(
    binCursor: Cursor,
    datCursor: Cursor,
    lenient: Boolean = false,
    shiftJis: Boolean = false,
): PwResult<BinDatQuestData> {
    val compressed = !looksLikeUncompressedBin(binCursor)

    val result = tryParseBinDat(binCursor, datCursor, lenient, compressed, shiftJis)

    if (result is Success) return result

    // If detection was wrong, try the other mode.
    binCursor.seekStart(0)
    datCursor.seekStart(0)

    val retryResult = tryParseBinDat(binCursor, datCursor, lenient, !compressed, shiftJis)

    if (retryResult is Success) return retryResult

    // Both attempts failed. Report each attempt's errors with labels so the user
    // can tell which compression assumption failed and why.
    val firstLabel = if (compressed) "compressed" else "uncompressed"
    val retryLabel = if (compressed) "uncompressed" else "compressed"
    return PwResult.build<BinDatQuestData>(logger)
        .addProblem(Severity.Error, "Parsing as $firstLabel (auto-detected) failed:")
        .addResult(result)
        .addProblem(Severity.Error, "Retry as $retryLabel also failed:")
        .addResult(retryResult)
        .failure()
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
    var datCount = 0
    var binCount = 0

    for (file in files) {
        val fileName = file.filename.trim().lowercase()

        if (fileName.endsWith(".dat")) {
            if (++datCount > 1) result.addProblem(Severity.Warning, "QST contains multiple DAT files; using last: ${file.filename.trim()}.")
            datFile = file
        } else if (fileName.endsWith(".bin")) {
            if (++binCount > 1) result.addProblem(Severity.Warning, "QST contains multiple BIN files; using last: ${file.filename.trim()}.")
            binFile = file
        }
    }

    if (datFile == null) {
        return result.addProblem(Severity.Error, "File contains no DAT file.").failure()
    }

    if (binFile == null) {
        return result.addProblem(Severity.Error, "File contains no BIN file.").failure()
    }

    val binBase = binFile.filename.trim().substringBeforeLast('.')
    val shiftJis = binBase.endsWith("_j")

    val questResult = parseBinDatToQuestAutoDetect(
        binFile.data.cursor(),
        datFile.data.cursor(),
        lenient,
        shiftJis = shiftJis,
    )
    result.addResult(questResult)

    if (questResult !is Success) {
        return result.failure()
    }

    return result.success(QuestData(
        questResult.value.quest,
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

    if (setEpisode.args.isEmpty()) {
        rb.addProblem(Severity.Warning, "set_episode instruction in function 0 has no arguments.")
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
        cmRandomSpawns = quest.challengeData.cmRandomSpawns,
        cmMonsterMappings = quest.challengeData.cmMonsterMappings,
        cmConfigPool = quest.challengeData.cmConfigPool,
    ))

    val binFormat = when (version) {
        Version.DC, Version.GC -> BinFormat.DC_GC
        Version.PC -> BinFormat.PC
        Version.BB -> BinFormat.BB
    }

    val (bytecode, labelOffsets) = writeBytecode(quest.bytecodeIr, binFormat.stringEncoding)

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
        quest.bytecodeOffset,
        shiftJis = quest.shiftJis,
    ))

    return Pair(bin, dat)
}

/**
 * Creates a .qst file from [quest].
 */
fun writeQuestToQst(
    quest: Quest,
    filename: String,
    version: Version,
    online: Boolean,
    compressed: Boolean = true,
): Buffer {
    val (bin, dat) = writeQuestToBinDat(quest, version)

    val baseFilename = (filenameBase(filename) ?: filename).take(11)
    val questName = quest.name.take(if (version == Version.BB) 23 else 31)

    fun maybeCompress(buf: Buffer): Buffer =
        if (compressed) prsCompress(buf.cursor()).buffer() else buf

    return writeQst(QstContent(
        version,
        online,
        files = listOf(
            QstContainedFile(
                id = quest.id,
                filename = "$baseFilename.dat",
                questName = questName,
                data = maybeCompress(dat),
            ),
            QstContainedFile(
                id = quest.id,
                filename = "$baseFilename.bin",
                questName = questName,
                data = maybeCompress(bin),
            ),
        ),
    ))
}

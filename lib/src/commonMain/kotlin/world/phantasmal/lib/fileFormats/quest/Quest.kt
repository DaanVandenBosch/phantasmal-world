package world.phantasmal.lib.fileFormats.quest

import mu.KotlinLogging
import world.phantasmal.core.PwResult
import world.phantasmal.core.PwResultBuilder
import world.phantasmal.core.Severity
import world.phantasmal.core.Success
import world.phantasmal.lib.assembly.InstructionSegment
import world.phantasmal.lib.assembly.OP_SET_EPISODE
import world.phantasmal.lib.assembly.Segment
import world.phantasmal.lib.assembly.dataFlowAnalysis.getMapDesignations
import world.phantasmal.lib.compression.prs.prsDecompress
import world.phantasmal.lib.cursor.Cursor

private val logger = KotlinLogging.logger {}

class Quest(
    var id: Int,
    var language: Int,
    var name: String,
    var shortDescription: String,
    var longDescription: String,
    var episode: Episode,
    val objects: List<QuestObject>,
    val npcs: List<QuestNpc>,
    val events: List<DatEvent>,
    val datUnknowns: List<DatUnknown>,
    val byteCodeIr: List<Segment>,
    val shopItems: UIntArray,
    val mapDesignations: Map<Int, Int>,
)

fun parseBinDatToQuest(
    binCursor: Cursor,
    datCursor: Cursor,
    lenient: Boolean = false,
): PwResult<Quest> {
    val rb = PwResultBuilder<Quest>(logger)

    // Decompress and parse files.
    val binDecompressed = prsDecompress(binCursor)
    rb.addResult(binDecompressed)

    if (binDecompressed !is Success) {
        return rb.failure()
    }

    val bin = parseBin(binDecompressed.value)

    val datDecompressed = prsDecompress(datCursor)
    rb.addResult(datDecompressed)

    if (datDecompressed !is Success) {
        return rb.failure()
    }

    val dat = parseDat(datDecompressed.value)
    val objects = dat.objs.map { QuestObject(it.areaId, it.data) }
    // Initialize NPCs with random episode and correct it later.
    val npcs = dat.npcs.map { QuestNpc(Episode.I, it.areaId, it.data) }

    // Extract episode and map designations from byte code.
    var episode = Episode.I
    var mapDesignations = emptyMap<Int, Int>()

    val parseByteCodeResult = parseByteCode(
        bin.byteCode,
        bin.labelOffsets,
        extractScriptEntryPoints(objects, npcs),
        bin.format == BinFormat.DC_GC,
        lenient,
    )

    rb.addResult(parseByteCodeResult)

    if (parseByteCodeResult !is Success) {
        return rb.failure()
    }

    val byteCodeIr = parseByteCodeResult.value

    if (byteCodeIr.isEmpty()) {
        rb.addProblem(Severity.Warning, "File contains no instruction labels.")
    } else {
        val instructionSegments = byteCodeIr.filterIsInstance<InstructionSegment>()

        var label0Segment: InstructionSegment? = null

        for (segment in instructionSegments) {
            if (0 in segment.labels) {
                label0Segment = segment
                break
            }
        }

        if (label0Segment != null) {
            episode = getEpisode(rb, label0Segment)

            for (npc in npcs) {
                npc.episode = episode
            }

            mapDesignations = getMapDesignations(instructionSegments, label0Segment)
        } else {
            rb.addProblem(Severity.Warning, "No instruction segment for label 0 found.")
        }
    }

    return rb.success(Quest(
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
        byteCodeIr,
        shopItems = bin.shopItems,
        mapDesignations,
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

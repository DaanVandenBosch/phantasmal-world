package world.phantasmal.psolib.fileFormats.quest

import world.phantasmal.core.Success
import world.phantasmal.psolib.Endianness
import world.phantasmal.psolib.Episode
import world.phantasmal.psolib.asm.*
import world.phantasmal.psolib.buffer.Buffer
import world.phantasmal.psolib.cursor.cursor
import world.phantasmal.psolib.fileFormats.parseGsl
import world.phantasmal.psolib.test.LibTestSuite
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Rebuilds the bundled city .qst files from real game data.
 *
 * For each episode:
 * 1. Reads the city bin file from game data (contains all NPC script labels).
 * 2. Reads the city dat files (obj + npc) from game data (loose files, then data.gsl fallback).
 * 3. Reads the default quest to get its richer label 0 (initialization logic).
 * 4. Replaces the city bin's label 0 with the default quest's label 0.
 * 5. Writes the result as a BB-format .qst file.
 *
 * Run with: ./gradlew :psolib:jvmTest --tests "*.GenerateCityQst"
 *
 * After running, copy the generated files from tmp/ to
 * web/src/jsMain/resources/assets/quests/city/
 */
class GenerateCityQst : LibTestSuite {

    // Override with PSO_GAME_DATA_DIR; defaults to a path that exists on the original author's
    // machine. Tests already skip cleanly when the directory is absent (see early-return below).
    private val gameDataDir =
        File(System.getenv("PSO_GAME_DATA_DIR") ?: "D:/PSO/EphineaPSO2/data")

    /** Cached GSL entries, loaded lazily. */
    private val gslEntries: Map<String, Buffer> by lazy {
        val gslFile = File(gameDataDir, "data.gsl")
        if (!gslFile.exists()) {
            emptyMap()
        } else {
            val gslBuf = Buffer.fromByteArray(gslFile.readBytes())
            val result = parseGsl(gslBuf.cursor())
            assertTrue(result is Success, "Failed to parse data.gsl")
            result.value.associate { it.name to it.data }
        }
    }

    /**
     * Read a file from the game data directory. Falls back to data.gsl if the loose file
     * doesn't exist.
     */
    private fun readGameFile(name: String): Buffer? {
        val file = File(gameDataDir, name)
        if (file.exists()) {
            return Buffer.fromByteArray(file.readBytes())
        }
        return gslEntries[name]
    }

    private fun readGameFileRequired(name: String): Buffer =
        readGameFile(name) ?: error("Game file not found: $name (not loose, not in data.gsl)")

    /**
     * Read a default quest .qst from the web module resources.
     */
    private fun readDefaultQst(episode: Episode): Quest {
        val ver = episode.toInt()
        val path = "web/src/jsMain/resources/assets/quests/defaults/default_ep_$ver.qst"
        val projectRoot = File(System.getProperty("user.dir")).let {
            if (it.name == "psolib") it.parentFile else it
        }
        val file = File(projectRoot, path)
        assertTrue(file.exists(), "Default quest not found: ${file.absolutePath}")
        val buf = Buffer.fromByteArray(file.readBytes())
        val result = parseQstToQuest(buf.cursor())
        assertTrue(result is Success, "Failed to parse default quest: ${result.problems}")
        return result.value.quest
    }

    /**
     * Synthesize a standard DAT buffer from raw per-floor entity data.
     * Each floor contributes obj and npc sections with proper DAT headers.
     */
    private fun synthesizeDat(
        floorId: Int,
        objData: Buffer,
        npcData: Buffer,
    ): Buffer {
        val totalSize =
            (if (objData.size > 0) DAT_HEADER_SIZE + objData.size else 0) +
                (if (npcData.size > 0) DAT_HEADER_SIZE + npcData.size else 0) +
                DAT_HEADER_SIZE // terminator

        val buf = Buffer.withSize(totalSize, Endianness.Little)
        var offset = 0

        fun writeSection(entityType: Int, data: Buffer) {
            if (data.size > 0) {
                buf.setInt(offset, entityType)
                buf.setInt(offset + 4, DAT_HEADER_SIZE + data.size)
                buf.setInt(offset + 8, floorId)
                buf.setInt(offset + 12, data.size)
                offset += DAT_HEADER_SIZE
                data.copyInto(buf, destinationOffset = offset)
                offset += data.size
            }
        }

        writeSection(DAT_ENTITY_TYPE_OBJ, objData)
        writeSection(DAT_ENTITY_TYPE_NPC, npcData)
        // Terminator: remaining bytes are already zero.

        return buf
    }

    /**
     * Build a city quest by combining:
     * - City bin file (complete script with all NPC labels)
     * - City dat files (objects + NPCs from free roam data)
     * - Default quest's label 0 (richer initialization)
     */
    private fun buildCityQuest(
        episode: Episode,
        binFileName: String,
        objDatFileName: String,
        npcDatFileName: String,
        shiftJis: Boolean,
    ): Quest {
        // Read city bin.
        val binBuf = readGameFileRequired(binFileName)
        println("  Bin: $binFileName (${binBuf.size} bytes)")

        // Read city dat files.
        val objBuf = readGameFileRequired(objDatFileName)
        val npcBuf = readGameFileRequired(npcDatFileName)
        println("  Obj: $objDatFileName (${objBuf.size} bytes, ${objBuf.size / OBJECT_BYTE_SIZE} objects)")
        println("  Npc: $npcDatFileName (${npcBuf.size} bytes, ${npcBuf.size / NPC_BYTE_SIZE} NPCs)")

        // Synthesize DAT.
        val datBuf = synthesizeDat(floorId = 0, objBuf, npcBuf)

        // Parse bin + dat into a quest.
        val result = parseBinDatToQuest(
            binCursor = binBuf.cursor(),
            datCursor = datBuf.cursor(),
            lenient = true,
            compressed = false,
            shiftJis = shiftJis,
        )
        assertTrue(result is Success, "Failed to parse city bin+dat: ${result.problems}")
        val cityQuest = result.value

        // Override episode (bin may not have correct set_episode).
        cityQuest.episode = episode
        for (npc in cityQuest.npcs) {
            npc.episode = episode
        }

        println("  Parsed: ${cityQuest.npcs.size} NPCs, ${cityQuest.objects.size} objects")
        println("  Labels in script: ${cityQuest.bytecodeIr.segments.flatMap { it.labels }.sorted()}")

        // Collect NPC script labels to verify coverage.
        val npcLabels = cityQuest.npcs
            .filter { it.scriptLabel > 0 }
            .map { it.scriptLabel }
            .distinct()
            .sorted()
        val scriptLabels = cityQuest.bytecodeIr.segments.flatMap { it.labels }.toSet()
        val missingLabels = npcLabels.filter { it !in scriptLabels }
        println("  NPC script labels: $npcLabels")
        if (missingLabels.isNotEmpty()) {
            println("  WARNING: Missing labels for NPCs: $missingLabels")
        } else {
            println("  All NPC labels present in script.")
        }

        // Load default quest and merge its label 0.
        // The default label 0 may reference other labels (e.g., set_floor_handler 0, 150).
        // Those referenced labels and their segments must also be brought over, remapped to
        // free slots if they conflict with existing city labels.
        val defaultQuest = readDefaultQst(episode)
        mergeDefaultLabel0(cityQuest, defaultQuest, scriptLabels)

        // Set reasonable quest metadata.
        cityQuest.id = 0
        cityQuest.language = 1 // English
        cityQuest.name = "City"
        cityQuest.shortDescription = ""
        cityQuest.longDescription = ""
        // Force BB format for the bundled .qst.
        cityQuest.binFormat = BinFormat.BB
        cityQuest.bytecodeOffset = null // Use standard BB offset.
        cityQuest.shiftJis = false

        return cityQuest
    }

    /**
     * Collect all labels referenced by instructions in [segment] via label-typed parameters.
     * Does NOT include the segment's own labels.
     */
    private fun collectReferencedLabels(segment: InstructionSegment): Set<Int> {
        val refs = mutableSetOf<Int>()
        for (inst in segment.instructions) {
            for (i in inst.opcode.params.indices) {
                val paramType = inst.opcode.params[i].type
                if (paramType is LabelType) {
                    for (arg in inst.getArgs(i)) {
                        refs.add(arg.coerceInt())
                    }
                }
            }
        }
        return refs
    }

    /**
     * Find the smallest label >= [startFrom] that is not in [usedLabels].
     */
    private fun findFreeLabel(usedLabels: Set<Int>, startFrom: Int = 1): Int {
        var candidate = startFrom
        while (candidate in usedLabels) candidate++
        return candidate
    }

    /**
     * Replace label references in all instructions of [segment].
     * Returns a copy with remapped args.
     */
    private fun remapLabelsInSegment(
        segment: InstructionSegment,
        labelMap: Map<Int, Int>,
    ): InstructionSegment {
        if (labelMap.isEmpty()) return segment

        val newInstructions = segment.instructions.map { inst ->
            // Build a set of arg indices that are label-typed.
            val labelArgIndices = mutableSetOf<Int>()
            var argOffset = 0
            for (i in inst.opcode.params.indices) {
                val paramType = inst.opcode.params[i].type
                val paramArgs = inst.getArgs(i)
                if (paramType is LabelType) {
                    for (j in paramArgs.indices) {
                        labelArgIndices.add(argOffset + j)
                    }
                }
                argOffset += paramArgs.size
            }

            var changed = false
            val newArgs = inst.args.mapIndexed { idx, arg ->
                if (idx in labelArgIndices && arg is IntArg) {
                    val mapped = labelMap[arg.value]
                    if (mapped != null && mapped != arg.value) {
                        changed = true
                        IntArg(mapped)
                    } else arg
                } else arg
            }
            if (changed) Instruction(inst.opcode, newArgs, inst.valid, inst.srcLoc) else inst
        }

        return InstructionSegment(
            segment.labels.toMutableList(),
            newInstructions.toMutableList(),
            segment.srcLoc,
        )
    }

    /**
     * Merge the default quest's label 0 (and any segments it references) into the city quest.
     * Remaps conflicting labels to free slots.
     */
    private fun mergeDefaultLabel0(
        cityQuest: Quest,
        defaultQuest: Quest,
        cityLabels: Set<Int>,
    ) {
        val defaultLabel0 = defaultQuest.bytecodeIr.segments
            .filterIsInstance<InstructionSegment>()
            .firstOrNull { 0 in it.labels }

        if (defaultLabel0 == null) {
            println("  WARNING: Default quest has no label 0!")
            return
        }

        // Find labels referenced by default label 0 (e.g., set_floor_handler 0, 150).
        val referencedLabels = collectReferencedLabels(defaultLabel0)
        // Exclude label 0 itself and labels that are built-in / already in city script.
        val externalRefs = referencedLabels - 0

        println("  Default label 0 references labels: $referencedLabels")

        // Build a mapping for labels that need remapping.
        val allUsedLabels = cityLabels.toMutableSet()
        allUsedLabels.add(0) // label 0 is always used
        val labelRemap = mutableMapOf<Int, Int>()

        // For each external reference, check if it conflicts with city labels.
        // If it does, find a free slot; if not, keep the original label.
        // Either way, we need to bring the referenced segment over.
        val extraSegments = mutableListOf<Segment>()

        for (refLabel in externalRefs.sorted()) {
            // Find the segment for this label in the default quest.
            val refSegment = defaultQuest.bytecodeIr.segments
                .firstOrNull { refLabel in it.labels }

            if (refSegment == null) {
                println("  Label $refLabel referenced by default label 0 but not found in default quest")
                continue
            }

            if (refLabel in cityLabels) {
                // Conflict! Remap to a free slot.
                val freeLabel = findFreeLabel(allUsedLabels)
                println("  Label $refLabel conflicts with city script, remapping to $freeLabel")
                labelRemap[refLabel] = freeLabel
                allUsedLabels.add(freeLabel)

                // Copy the segment with the new label.
                val copy = refSegment.copy()
                copy.labels.clear()
                copy.labels.add(freeLabel)
                extraSegments.add(copy)
            } else {
                // No conflict, keep original label.
                allUsedLabels.add(refLabel)
                extraSegments.add(refSegment.copy())
                println("  Bringing over label $refLabel from default quest (no conflict)")
            }
        }

        // Remap label references in default label 0 if needed.
        val mergedLabel0 = if (labelRemap.isNotEmpty()) {
            remapLabelsInSegment(defaultLabel0, labelRemap)
        } else {
            defaultLabel0
        }

        // Build the new segment list.
        val newSegments = cityQuest.bytecodeIr.segments.toMutableList()
        val cityLabel0Index = newSegments.indexOfFirst { it is InstructionSegment && 0 in it.labels }

        if (cityLabel0Index >= 0) {
            println("  Replacing city label 0 with default quest label 0")
            newSegments[cityLabel0Index] = mergedLabel0
        } else {
            println("  City script has no label 0, prepending default label 0")
            newSegments.add(0, mergedLabel0)
        }

        // Insert extra segments right after label 0.
        val insertPos = if (cityLabel0Index >= 0) cityLabel0Index + 1 else 1
        for ((i, seg) in extraSegments.withIndex()) {
            newSegments.add(insertPos + i, seg)
        }

        cityQuest.bytecodeIr = BytecodeIr(newSegments)
    }

    private fun writeQuest(quest: Quest, outputName: String) {
        val qstBuf = writeQuestToQst(quest, outputName, Version.BB, online = true)

        val projectRoot = File(System.getProperty("user.dir")).let {
            if (it.name == "psolib") it.parentFile else it
        }
        val outputFile = File(projectRoot, "tmp/$outputName.qst")
        outputFile.parentFile.mkdirs()
        outputFile.writeBytes(qstBuf.byteArray.copyOf(qstBuf.size))

        println("  Output: ${outputFile.absolutePath} (${outputFile.length()} bytes)")
    }

    @Test
    fun generate_ep1_city() = testAsync {
        if (!gameDataDir.exists()) { println("Skipping: game data not found at $gameDataDir"); return@testAsync }
        println("=== EP1 City (Pioneer II) ===")
        // English bin exists in data.gsl but not as loose file.
        // Loose Ephinea file is Japanese with extended NPC labels.
        // Use English bin from data.gsl (map_city_on_e.bin) for text,
        // but the Ephinea Japanese loose file (map_city_on_j.bin) has more labels.
        // Strategy: use the Ephinea Japanese bin (most complete labels) since text
        // comes from built-in client functions anyway.
        val quest = buildCityQuest(
            episode = Episode.I,
            binFileName = "map_city_on_j.bin",
            objDatFileName = "map_city00_00o.dat",
            npcDatFileName = "map_city00_00e.dat",
            shiftJis = true,
        )
        writeQuest(quest, "city_ep_1")
        println("Done.\n")
    }

    @Test
    fun generate_ep2_city() = testAsync {
        if (!gameDataDir.exists()) { println("Skipping: game data not found at $gameDataDir"); return@testAsync }
        println("=== EP2 City (Lab) ===")
        // Ephinea loose file (map_labo_on_j.bin) is BB format with Japanese text.
        // English bin exists both loose (50300) and in data.gsl (50920).
        // Use Ephinea's Japanese loose file for most complete/up-to-date labels.
        val quest = buildCityQuest(
            episode = Episode.II,
            binFileName = "map_labo_on_j.bin",
            objDatFileName = "map_labo00_00o.dat",
            npcDatFileName = "map_labo00_00e.dat",
            shiftJis = true,
        )
        writeQuest(quest, "city_ep_2")
        println("Done.\n")
    }

    @Test
    fun generate_ep4_city() = testAsync {
        if (!gameDataDir.exists()) { println("Skipping: game data not found at $gameDataDir"); return@testAsync }
        println("=== EP4 City (Pioneer II) ===")
        // Only Japanese bin exists (loose and data.gsl). No English version.
        val quest = buildCityQuest(
            episode = Episode.IV,
            binFileName = "map_city02_on_j.bin",
            objDatFileName = "map_city02_00_00o.dat",
            npcDatFileName = "map_city02_00_00e.dat",
            shiftJis = true,
        )
        writeQuest(quest, "city_ep_4")
        println("Done.\n")
    }

    @Test
    fun list_unknown_opcodes() = testAsync {
        if (!gameDataDir.exists()) { println("Skipping: game data not found at $gameDataDir"); return@testAsync }
        data class BinInfo(val episode: String, val file: String, val shiftJis: Boolean)

        val bins = listOf(
            BinInfo("EP1", "map_city_on_j.bin", true),
            BinInfo("EP2", "map_labo_on_j.bin", true),
            BinInfo("EP4", "map_city02_on_j.bin", true),
        )

        for ((ep, fileName, sj) in bins) {
            val binBuf = readGameFileRequired(fileName)
            val bin = parseBin(binBuf.cursor(), shiftJis = sj)
            val ir = parseBytecode(
                bin.bytecode,
                bin.labelOffsets,
                emptySet(),
                bin.format.stringEncoding,
                lenient = true,
            )
            assertTrue(ir is Success)

            val unknowns = ir.value.segments
                .filterIsInstance<InstructionSegment>()
                .flatMap { it.instructions }
                .filter { !it.opcode.known }
                .map { it.opcode }
                .distinctBy { it.code }
                .sortedBy { it.code }

            println("$ep ($fileName): ${unknowns.size} unknown opcodes")
            for (op in unknowns) {
                println("  ${op.mnemonic}  (0x${op.code.toString(16)})")
            }
        }
    }
}

package world.phantasmal.psolib.compatibility

import world.phantasmal.psolib.Episode
import world.phantasmal.psolib.asm.*
import world.phantasmal.psolib.fileFormats.quest.Quest
import world.phantasmal.psolib.fileFormats.quest.QuestNpc
import world.phantasmal.psolib.fileFormats.quest.QuestObject

/**
 * PSO Quest compatibility checker.
 * Validates if a quest is compatible with different PSO versions.
 */
class CompatibilityChecker(
    private val floorDataProvider: FloorDataProvider = NoRestrictionFloorDataProvider,
) {
    /**
     * Check compatibility of a quest with a specific PSO version.
     */
    fun checkCompatibility(
        version: PSOVersion,
        quest: Quest,
    ): CompatibilityResult {
        val builder = CompatibilityResultBuilder(version)

        // Check bytecode/script compatibility
        checkBytecodeCompatibility(version, quest, builder)

        // Check NPC compatibility
        checkNpcCompatibility(version, quest, builder)

        // Check object compatibility
        checkObjectCompatibility(version, quest, builder)

        return builder.build()
    }

    /**
     * Check compatibility with all PSO versions.
     */
    fun checkAllVersions(quest: Quest): Map<PSOVersion, CompatibilityResult> {
        return PSOVersion.entries.associateWith { version ->
            checkCompatibility(version, quest)
        }
    }

    private fun checkBytecodeCompatibility(
        version: PSOVersion,
        quest: Quest,
        builder: CompatibilityResultBuilder,
    ) {
        val bytecodeIr = quest.bytecodeIr
        val segments = bytecodeIr.segments

        // Check if label 0 exists
        val hasLabel0 = segments.any { segment ->
            segment is InstructionSegment && 0 in segment.labels
        }

        if (!hasLabel0) {
            builder.addError(
                ProblemType.MISSING_LABEL_0,
                "Label 0 does not exist (quest entry point required)"
            )
        }

        // Collect all labels for reference checking
        val allLabels = mutableSetOf<Int>()
        val dataLabels = mutableSetOf<Int>()
        val referencedDataLabels = mutableSetOf<Int>()

        segments.forEach { segment ->
            when (segment) {
                is InstructionSegment -> allLabels.addAll(segment.labels)
                is DataSegment -> {
                    allLabels.addAll(segment.labels)
                    dataLabels.addAll(segment.labels)
                }
                is StringSegment -> allLabels.addAll(segment.labels)
            }
        }

        // Check each instruction segment
        segments.filterIsInstance<InstructionSegment>().forEach { segment ->
            segment.instructions.forEach { instruction ->
                checkInstruction(version, instruction, quest.episode, allLabels, referencedDataLabels, builder)
            }
        }

        // Check for unused data labels
        checkUnusedDataLabels(dataLabels, referencedDataLabels, builder)
    }

    private fun checkInstruction(
        version: PSOVersion,
        instruction: Instruction,
        episode: Episode,
        allLabels: Set<Int>,
        referencedDataLabels: MutableSet<Int>,
        builder: CompatibilityResultBuilder,
    ) {
        val opcode = instruction.opcode
        val lineNo = instruction.srcLoc?.mnemonic?.lineNo
        val location = lineNo?.let { ProblemLocation.ScriptLine(it) }

        // Check for unknown opcodes (unknown_* prefix)
        if (opcode.mnemonic.startsWith("unknown_", ignoreCase = true)) {
            builder.addWarning(
                ProblemType.UNKNOWN_OPCODE,
                "Unknown opcode \"${opcode.mnemonic}\"" + (lineNo?.let { " at line $it" } ?: ""),
                location
            )
        }

        // Check BB-specific opcodes (mnemonic starts with "bb_" or "BB_")
        // These opcodes are only available in Blue Burst (v4)
        if (opcode.mnemonic.startsWith("bb_", ignoreCase = true) && version != PSOVersion.BLUE_BURST) {
            builder.addError(
                ProblemType.OPCODE_VERSION_MISMATCH,
                "Opcode not supported \"${opcode.mnemonic}\"" + (lineNo?.let { " at line $it" } ?: ""),
                location
            )
        }

        // Check conversion opcodes (may have issues in V1)
        if (opcode.code in CONVERT_OPCODES) {
            if (version.verId < 2) {
                builder.addWarning(
                    ProblemType.CONVERSION_WARNING,
                    "Opcode '${opcode.mnemonic}' may have conversion issues in V1",
                    location
                )
            }
        }

        // Check set_episode
        if (opcode == OP_SET_EPISODE) {
            checkEpisodeParameter(version, instruction, location, builder)
        }

        // Check special opcode warning (0xF8EE)
        if (opcode.code == SPECIAL_WARNING_OPCODE) {
            builder.addWarning(
                ProblemType.SPECIAL_OPCODE_WARNING,
                "Opcode 0xF8EE detected - may cause compatibility issues",
                location
            )
        }

        // Check arguments
        instruction.args.forEachIndexed { argIndex, arg ->
            // Check string bracket matching for all string arguments
            if (arg is StringArg) {
                checkStringBrackets(arg.value, lineNo, builder)
            }

            // Check label references for integer arguments
            if (arg is IntArg && argIndex < opcode.params.size) {
                val param = opcode.params[argIndex]
                when (param.type) {
                    is ILabelType -> {
                        // Instruction label reference (call, jmp, etc.)
                        if (arg.value !in allLabels) {
                            // Missing label reference is an error - will cause script failure
                            builder.addError(
                                ProblemType.LABEL_NOT_FOUND,
                                "Label ${arg.value} not found",
                                location
                            )
                        }
                    }

                    is DLabelType -> {
                        // Data label reference - track for unused data label checking
                        referencedDataLabels.add(arg.value)
                        if (arg.value !in allLabels) {
                            // Missing data label reference is an error
                            builder.addError(
                                ProblemType.LABEL_NOT_FOUND,
                                "Data label ${arg.value} not found",
                                location
                            )
                        }
                    }

                    is SLabelType -> {
                        // String label reference
                        if (arg.value !in allLabels) {
                            builder.addError(
                                ProblemType.LABEL_NOT_FOUND,
                                "String label ${arg.value} not found",
                                location
                            )
                        }
                    }

                    else -> {
                        // Other parameter types don't reference labels
                    }
                }
            }
        }
    }

    private fun checkEpisodeParameter(
        version: PSOVersion,
        instruction: Instruction,
        location: ProblemLocation?,
        builder: CompatibilityResultBuilder,
    ) {
        if (instruction.args.isEmpty()) return

        val episodeArg = instruction.args[0]
        if (episodeArg !is IntArg) return

        val episodeValue = episodeArg.value

        when {
            version.verId < 2 && episodeValue != 0 -> {
                builder.addError(
                    ProblemType.EPISODE_NOT_SUPPORTED,
                    "Episode parameter $episodeValue not supported in V1 (only Episode 1 supported)",
                    location
                )
            }

            version == PSOVersion.PC && episodeValue == 2 -> {
                builder.addError(
                    ProblemType.EPISODE_NOT_SUPPORTED,
                    "Episode 4 not supported in PC version",
                    location
                )
            }
        }
    }

    private fun checkStringBrackets(
        str: String,
        lineNo: Int?,
        builder: CompatibilityResultBuilder,
    ) {
        var bracketCount = 0
        for (char in str) {
            when (char) {
                '<' -> bracketCount++
                '>' -> bracketCount--
            }
        }

        if (bracketCount != 0) {
            builder.addWarning(
                ProblemType.INVALID_ARGUMENT,
                "Unmatched brackets in string parameter" + (lineNo?.let { " at line $it" } ?: ""),
                lineNo?.let { ProblemLocation.ScriptLine(it) }
            )
        }
    }

    private fun checkUnusedDataLabels(
        dataLabels: Set<Int>,
        referencedLabels: Set<Int>,
        builder: CompatibilityResultBuilder,
    ) {
        val unusedLabels = dataLabels - referencedLabels
        unusedLabels.forEach { label ->
            builder.addWarning(
                ProblemType.UNUSED_DATA_LABEL,
                "Data label $label is defined but never referenced",
                ProblemLocation.DataLabel(label.toString())
            )
        }
    }

    private fun checkNpcCompatibility(
        version: PSOVersion,
        quest: Quest,
        builder: CompatibilityResultBuilder,
    ) {
        val episode = quest.episode
        val episodeInt = when (episode) {
            Episode.I -> 0
            Episode.II -> 1
            Episode.IV -> 2
        }

        // Collect all labels from bytecode for NPC action label checking
        val allLabels = mutableSetOf<Int>()
        quest.bytecodeIr.segments.forEach { segment ->
            when (segment) {
                is InstructionSegment -> allLabels.addAll(segment.labels)
                is DataSegment -> allLabels.addAll(segment.labels)
                is StringSegment -> allLabels.addAll(segment.labels)
            }
        }

        // Group NPCs by area for counting
        val npcsByArea = quest.npcs.groupBy { it.areaId }

        quest.npcs.forEachIndexed { index, npc ->
            checkNpc(version, npc, index, episodeInt, allLabels, builder)
        }

        // Check NPC count per area
        npcsByArea.forEach { (areaId, npcs) ->
            if (npcs.size > MAX_ENTITIES_PER_AREA) {
                builder.addWarning(
                    ProblemType.TOO_MANY_MONSTERS,
                    "Area $areaId has too many NPCs (${npcs.size} > $MAX_ENTITIES_PER_AREA)",
                    ProblemLocation.Floor(areaId)
                )
            }
        }
    }

    private fun checkNpc(
        version: PSOVersion,
        npc: QuestNpc,
        index: Int,
        episodeInt: Int,
        allLabels: Set<Int>,
        builder: CompatibilityResultBuilder,
    ) {
        val location = ProblemLocation.Monster(index, npc.areaId)
        val skin = npc.skin

        // Check NPC script label for Pioneer 2/Lab (area 0) only
        // NPCs (not enemies) can have action labels that trigger scripts
        if (npc.areaId == 0 && skin !in DefaultLabels.ENEMY_IDS) {
            val scriptLabel = npc.scriptLabel
            if (scriptLabel > 0) {
                // Determine if this label is built-in or requires script definition
                val isBaseLabel = DefaultLabels.isBaseLabel(scriptLabel, episodeInt)
                val isExtraLabel = DefaultLabels.isExtraLabel(scriptLabel, episodeInt)

                when {
                    // Base labels are built-in for all versions
                    isBaseLabel -> {
                        // No check needed, built-in for all versions
                    }

                    // Extra labels are only built-in for GC Ep1&2 (ver=3)
                    // Other versions (including BB which uses DC V2 rules) need script definition
                    isExtraLabel -> {
                        if (version.verId != 3) {
                            // Extra label in non-GC version - must be defined in script
                            if (scriptLabel !in allLabels) {
                                builder.addWarning(
                                    ProblemType.NPC_ACTION_LABEL_NOT_FOUND,
                                    "Label $scriptLabel not found for NPC #$index on floor ${npc.areaId}",
                                    location
                                )
                            }
                        }
                        // GC has these built-in, no check needed
                    }

                    // Custom label (not in any default table) - must be defined in script
                    else -> {
                        if (scriptLabel !in allLabels) {
                            builder.addWarning(
                                ProblemType.NPC_ACTION_LABEL_NOT_FOUND,
                                "Label $scriptLabel not found for NPC #$index on floor ${npc.areaId}",
                                location
                            )
                        }
                    }
                }
            }
        }

        // Check Skin 51 (special NPC skin)
        if (skin == 51) {
            checkSkin51(version, npc, index, episodeInt, builder)
        }

        // Check floor-specific monster restrictions
        if (npc.areaId < 50) {
            val allowedMonsters = floorDataProvider.getFloorMonsters(npc.areaId, version.verId)
            if (allowedMonsters != null && allowedMonsters.isNotEmpty() && skin !in allowedMonsters) {
                builder.addWarning(
                    ProblemType.MONSTER_FLOOR_MISMATCH,
                    "Monster skin $skin may not spawn correctly on this floor",
                    location
                )
            }
        }
    }

    private fun checkSkin51(
        version: PSOVersion,
        npc: QuestNpc,
        index: Int,
        episodeInt: Int,
        builder: CompatibilityResultBuilder,
    ) {
        val location = ProblemLocation.Monster(index, npc.areaId)

        when {
            version.verId < 2 -> {
                builder.addWarning(
                    ProblemType.SKIN_NOT_SUPPORTED,
                    "Skin 51 not supported in V1",
                    location
                )
            }

            episodeInt == 1 -> { // Episode 2
                builder.addWarning(
                    ProblemType.SKIN_NOT_SUPPORTED,
                    "Skin 51 may not work properly in Episode 2",
                    location
                )
            }

            else -> {
                // Get subtype from rotation.x field (data offset 32)
                val subtype = npc.data.getInt(32)
                when {
                    subtype > 15 -> {
                        builder.addError(
                            ProblemType.SKIN_51_INVALID_SUBTYPE,
                            "Skin 51 invalid subtype $subtype (must be 0-15)",
                            location
                        )
                    }

                    !floorDataProvider.isValidNPC51(npc.areaId, subtype) -> {
                        builder.addError(
                            ProblemType.SKIN_51_INVALID_SUBTYPE,
                            "Skin 51 subtype $subtype not valid for floor ${npc.areaId}",
                            location
                        )
                    }
                }
            }
        }
    }

    private fun checkObjectCompatibility(
        version: PSOVersion,
        quest: Quest,
        builder: CompatibilityResultBuilder,
    ) {
        // Group objects by area for counting
        val objectsByArea = quest.objects.groupBy { it.areaId }

        quest.objects.forEachIndexed { index, obj ->
            checkObject(version, obj, index, builder)
        }

        // Check object count per area
        objectsByArea.forEach { (areaId, objects) ->
            if (objects.size > MAX_ENTITIES_PER_AREA) {
                builder.addWarning(
                    ProblemType.TOO_MANY_OBJECTS,
                    "Area $areaId has too many objects (${objects.size} > $MAX_ENTITIES_PER_AREA)",
                    ProblemLocation.Floor(areaId)
                )
            }
        }
    }

    private fun checkObject(
        version: PSOVersion,
        obj: QuestObject,
        index: Int,
        builder: CompatibilityResultBuilder,
    ) {
        val location = ProblemLocation.Object(index, obj.areaId)

        // Get object skin/type ID
        val skinId = obj.data.getShort(0).toInt()

        // Check floor-specific object restrictions
        if (obj.areaId < 50) {
            val allowedObjects = floorDataProvider.getFloorObjects(obj.areaId, version.verId)
            if (allowedObjects != null && allowedObjects.isNotEmpty() && skinId !in allowedObjects) {
                builder.addWarning(
                    ProblemType.OBJECT_FLOOR_MISMATCH,
                    "Object skin $skinId may not work correctly on this floor",
                    location
                )
            }
        }
    }

    companion object {
        private const val MAX_ENTITIES_PER_AREA = 400

        // Opcodes that may have conversion issues in V1
        private val CONVERT_OPCODES = setOf(
            0x66, 0x6D, 0x79, 0x7C, 0x7D, 0x7F,
            0x84, 0x87, 0xA8, 0xC0, 0xCD, 0xCE
        )

        private const val SPECIAL_WARNING_OPCODE = 0xF8EE
    }
}
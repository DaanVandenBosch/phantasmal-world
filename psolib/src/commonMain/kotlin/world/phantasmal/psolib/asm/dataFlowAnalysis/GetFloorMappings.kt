package world.phantasmal.psolib.asm.dataFlowAnalysis

import kotlinx.serialization.Serializable
import mu.KotlinLogging
import world.phantasmal.psolib.Episode
import world.phantasmal.psolib.asm.*
import world.phantasmal.psolib.fileFormats.quest.findEpisodeByMapId
import world.phantasmal.psolib.fileFormats.quest.getAreaIdByMapId
import world.phantasmal.psolib.fileFormats.quest.getMapId

private val logger = KotlinLogging.logger {}

/**
 * Maps a quest floor to a specific map area and variant.
 *
 * Multiple floors can reference the same map with different variants. For example, Phantasmal
 * World #4 (EP2) has:
 * ```
 * bb_map_designate 0, 18, 0, 0   // floor 0  → Lab (0x12), variant 0
 * bb_map_designate 17, 35, 0, 0  // floor 17 → Tower (0x23), variant 0
 * bb_map_designate 16, 35, 1, 0  // floor 16 → Tower (0x23), variant 1
 * ```
 * Here floors 16 and 17 both map to Tower (areaId 17) with different variants.
 *
 * @property floorId Unique floor ID from the quest bytecode (matches NPC/Object areaId in dat).
 * @property mapId Game-internal map ID (e.g., 0x23 = Tower).
 * @property areaId Logical area ID derived from [mapId] (an area may span multiple floors).
 * @property variantId Map variant on this floor.
 * @property mapEpisode Episode this map belongs to (derived from [mapId]), used when a quest
 *   references maps from a different episode.
 */
@Serializable
data class FloorMapping(
    val floorId: Int,
    val mapId: Int,
    val areaId: Int,
    val variantId: Int,
    val mapEpisode: Episode? = null,
)

/**
 * Extracts [FloorMapping]s from quest bytecode instructions.
 *
 * Data sources (in descending priority):
 * 1. **`bb_map_designate`** (BB) — immediate args: floorId, mapId, variantId.
 * 2. **`map_designate` / `map_designate_ex`** (DC/GC/PC) — register-based args, requires CFG
 *    analysis to resolve values.
 * 3. **`set_floor_handler`** (all versions) — low-priority fallback that only fills in floors
 *    not already covered by the above. Variant defaults to 0.
 *
 * Higher-priority opcodes unconditionally overwrite lower-priority entries for the same floor.
 * The result is a flat list sorted by floorId, with one [FloorMapping] per unique floor.
 */
fun getFloorMappings(
    instructionSegments: List<InstructionSegment>,
    createCfg: () -> ControlFlowGraph,
): List<FloorMapping> {
    // Keyed by floorId so that higher-priority opcodes can overwrite lower-priority ones.
    val floorMappings = mutableMapOf<Int, FloorMapping>()

    // Find label 0 segment for episode info
    val func0Segment = instructionSegments.find { 0 in it.labels }
    val episode = func0Segment?.let { getEpisode(it) } ?: Episode.I

    var cfg: ControlFlowGraph? = null

    for (segment in instructionSegments) {
        val instructions = segment.instructions
        for (instIdx in instructions.indices) {
            val inst = instructions[instIdx]
            when (inst.opcode) {
                OP_MAP_DESIGNATE,
                OP_MAP_DESIGNATE_EX -> {
                    if (cfg == null) {
                        cfg = createCfg()
                    }

                    // These opcodes read consecutive registers starting from the given register
                    val baseRegister = (inst.args[0] as IntArg).value

                    // Get floor ID from base register
                    val floorIdValues = getRegisterValue(cfg, inst, baseRegister)
                    if (floorIdValues.size != 1L) {
                        logger.warn { "Could not determine floor ID from register R$baseRegister for ${inst.opcode.mnemonic}" }
                        continue
                    }

                    val floorId = floorIdValues[0]!!

                    // map_designate(R) reads R+0=floorId, R+1=unused, R+2=variantId, R+3=unknown.
                    // map_designate_ex(R) reads R+0=floorId, R+1=mapId, R+2=unknown, R+3=variantId, R+4=unknown.
                    // For map_designate, the mapId is resolved from the episode and floorId
                    // (the opcode only reassigns the variant for the floor's default map).
                    val mapId: Int
                    if (inst.opcode == OP_MAP_DESIGNATE) {
                        mapId = getMapId(episode, floorId) ?: floorId
                    } else {
                        val mapIdValues = getRegisterValue(cfg, inst, baseRegister + 1)
                        if (mapIdValues.size != 1L) {
                            logger.warn { "Could not determine map ID from register R${baseRegister + 1} for ${inst.opcode.mnemonic}" }
                            continue
                        }
                        mapId = mapIdValues[0]!!
                    }

                    // Variant register offset differs: map_designate uses R+2, map_designate_ex uses R+3.
                    val variantRegister = baseRegister + (if (inst.opcode == OP_MAP_DESIGNATE) 2 else 3)
                    val variantIdValues = getRegisterValue(cfg, inst, variantRegister)
                    if (variantIdValues.size != 1L) {
                        logger.warn { "Could not determine variant ID from register R$variantRegister for ${inst.opcode.mnemonic}" }
                        continue
                    }

                    val areaId = getAreaIdByMapId(mapId)
                    val variantId = variantIdValues[0]!!

                    if (areaId != null) {
                        // High priority: overwrites any set_floor_handler entry for this floor.
                        floorMappings[floorId] = FloorMapping(floorId, mapId, areaId, variantId, findEpisodeByMapId(mapId))
                    }
                }

                OP_BB_MAP_DESIGNATE -> {
                    val floorId = (inst.args[0] as IntArg).value  // floor id
                    val mapId = (inst.args[1] as IntArg).value   // map id
                    val variantId = (inst.args[2] as IntArg).value // variant (3rd parameter)

                    val areaId = getAreaIdByMapId(mapId)

                    if (areaId != null) {
                        // High priority: overwrites any set_floor_handler entry for this floor.
                        floorMappings[floorId] = FloorMapping(floorId, mapId, areaId, variantId, findEpisodeByMapId(mapId))
                    }
                }

                OP_SET_FLOOR_HANDLER -> {
                    try {
                        // set_floor_handler takes 2 stack args: floorId and label.
                        // If push normalization has been applied, args are inlined.
                        // Otherwise, we scan preceding arg_push* instructions.
                        val floorId: Int

                        if (inst.args.isNotEmpty()) {
                            // Normalized: args are inlined.
                            floorId = (inst.args[0] as IntArg).value
                        } else {
                            // Non-normalized: look for preceding arg_push* instructions.
                            // set_floor_handler pops 2 args; the first pushed is floorId.
                            floorId = findPrecedingArgPushValue(instructions, instIdx, 2)
                                ?: continue
                        }

                        val mapId = getMapId(episode, floorId) ?: continue

                        // Low priority: only fills in floors not yet mapped by map_designate / bb_map_designate.
                        if (!floorMappings.containsKey(floorId)) {
                            // Resolve areaId from mapId, consistent with the map_designate branches.
                            // Falls back to floorId if mapId isn't in the known area table.
                            val areaId = getAreaIdByMapId(mapId) ?: floorId
                            floorMappings[floorId] = FloorMapping(floorId, mapId, areaId, 0, findEpisodeByMapId(mapId))
                        }
                    } catch (e: Exception) {
                        logger.warn { "Error getting values for OP_SET_FLOOR_HANDLER: ${e.message}" }
                    }
                }
            }
        }
    }

    // Sort by floorId to make the result order deterministic regardless of instruction
    // traversal order. This prevents spurious list-equality mismatches in callers that
    // compare the result with a previously computed list.
    return floorMappings.values.sortedBy { it.floorId }
}

/**
 * Extract episode from function 0 segment's set_episode instruction.
 * Returns Episode.I if no set_episode instruction is found.
 */
internal fun getEpisode(func0Segment: InstructionSegment): Episode {
    val setEpisode = func0Segment.instructions.find { it.opcode == OP_SET_EPISODE }
        ?: return Episode.I

    if (setEpisode.args.isEmpty()) {
        logger.warn { "set_episode instruction has no arguments, defaulting to Episode I." }
        return Episode.I
    }

    val value = (setEpisode.args[0] as IntArg).value
    return Episode.fromBytecodeValue(value) ?: run {
        logger.warn { "Unknown set_episode value: $value, defaulting to Episode I." }
        Episode.I
    }
}

/**
 * Scans backwards from [targetIdx] in [instructions] for arg_push* instructions to extract
 * the value of the Nth argument (1-based from the bottom of the stack).
 *
 * For example, for `set_floor_handler` which pops 2 args, the first pushed is floorId (argCount=2
 * returns the value of the first push, i.e., the one pushed 2 positions before the target).
 *
 * @return The integer value of the first pushed argument, or null if not found.
 */
private fun findPrecedingArgPushValue(
    instructions: List<Instruction>,
    targetIdx: Int,
    argCount: Int,
): Int? {
    var found = 0
    var firstArgValue: Int? = null

    for (i in (targetIdx - 1) downTo 0) {
        val prev = instructions[i]
        when (prev.opcode) {
            OP_ARG_PUSHL, OP_ARG_PUSHB, OP_ARG_PUSHW, OP_ARG_PUSHA, OP_ARG_PUSHR, OP_ARG_PUSHO, OP_ARG_PUSHS -> {
                found++
                if (found == argCount) {
                    // This is the first pushed arg (bottom of stack)
                    firstArgValue = (prev.args[0] as IntArg).value
                }
                if (found >= argCount) break
            }
            else -> break // Non-push instruction means the pattern doesn't match
        }
    }

    return firstArgValue
}

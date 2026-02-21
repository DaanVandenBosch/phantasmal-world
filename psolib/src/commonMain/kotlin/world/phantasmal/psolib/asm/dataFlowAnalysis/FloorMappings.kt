package world.phantasmal.psolib.asm.dataFlowAnalysis

import mu.KotlinLogging
import world.phantasmal.psolib.asm.*
import world.phantasmal.psolib.fileFormats.quest.getAreaIdByMapId
import world.phantasmal.psolib.fileFormats.quest.getMapId

private val logger = KotlinLogging.logger {}

/**
 * Represents a mapping of a floor to a specific map and area.
 *
 * <p>This mapping now uses <code>areaId(1)</code> → <code>variant(N)</code>
 * instead of the previous <code>floor(1)</code> → <code>variant(N)</code> relationship,
 * in order to correctly handle multi-variant map parsing.</p>
 *
 * <p><b>Why switch from floor-variant to areaId-variant mapping?</b></p>
 *
 * <p>Let’s take <b>Phantasmal World #4</b> as an example. The label 0 of this quest looks like this:</p>
 *
 * <pre>{@code
 * 0:
 *     set_episode 1
 *     set_floor_handler 0, 300
 *     set_floor_handler 17, 310
 *     set_floor_handler 16, 320
 *     set_qt_success 308
 *     set_qt_failure 701
 *     get_difficulty_level2 r252
 *     get_slotnumber r250
 *     initial_floor 0
 *     bb_map_designate 0, 18, 0, 0
 *     bb_map_designate 17, 35, 0, 0
 *     bb_map_designate 16, 35, 1, 0
 *     gset 79
 * }</pre>
 *
 * <p>This is an <b>Episode 2</b> quest. The key instruction here is <code>bb_map_designate</code>:</p>
 *
 * <pre>{@code
 * Lab_Ep2        = 00000012
 * Seaside_Night  = 00000022
 * Tower          = 00000023
 * }</pre>
 *
 * <p>Let’s look at the first three <code>bb_map_designate</code> lines:</p>
 *
 * <ol>
 *   <li><b>Floor 0</b>: map = <b>Lab (0x12 = 18)</b>, variant = <b>0</b> (Lab/City maps each have only one variant).</li>
 *   <li><b>Floor 17</b>: map = <b>Tower (0x23 = 35)</b>, variant = <b>0</b> (Tower has five variants).</li>
 *   <li><b>Floor 16</b>: map = <b>Tower (0x23 = 35)</b>, variant = <b>1</b> (Tower has five variants).</li>
 * </ol>
 *
 * <p>Previously, the system used a <code>floor(1)</code> → <code>variant(N)</code> mapping.
 * This caused parsing issues for maps with multiple variants under the same <code>mapId</code>.
 * For example, in <b>Phantasmal World #4</b>, the old logic incorrectly produced:</p>
 *
 * <blockquote>Lab0, Tower0, and Seaside_Night1</blockquote>
 *
 * <p>The correct mapping should be:</p>
 *
 * <blockquote>Lab0, Tower1, and Tower2</blockquote>
 *
 * <p>By switching to an <code>areaId(1)</code> ↔ <code>variant(N)</code> mapping,
 * the <b>FloorMapping</b> design now correctly resolves multi-variant maps under the same <code>mapId</code>.</p>
 */
data class FloorMapping(
    /**
     * The ID of the floor where the map should be placed.
     *
     * Before multi-variant map support was introduced, most code used `areaId` to refer to what is now
     * the floor ID (at that time, `areaId` and floor ID were effectively the same concept).
     *
     * With multi-variant maps:
     * - `floorId` is the actual, unique ID of the floor/layer the map is assigned to.
     * - `areaId` is derived from `mapId` and represents the logical area the map belongs to (an area may span multiple floors).
     */
    val floorId: Int,
    // Map id to place on that floor.
    val mapId: Int,
    // Area id deduced from the mapId.
    val areaId: Int,
    // Map variation id on the floor.
    val variantId: Int
)

/**
 * Extract map designations from function 0 bytecode instructions.
 * Returns a map of area IDs to sets of variant IDs.
 */
fun getMapDesignations(
    func0Segment: InstructionSegment,
    createCfg: () -> ControlFlowGraph,
): MutableMap<Int, MutableSet<Int>> {
    val mapDesignations = mutableMapOf<Int, MutableSet<Int>>()
    var cfg: ControlFlowGraph? = null

    for (inst in func0Segment.instructions) {
        when (inst.opcode) {
            OP_MAP_DESIGNATE,
            OP_MAP_DESIGNATE_EX,
                -> {
                if (cfg == null) {
                    cfg = createCfg()
                }

                // These opcodes read consecutive registers starting from the given register
                val baseRegister = (inst.args[0] as IntArg).value

                // Get floor ID from base register
                val floorIdValues = getRegisterValue(cfg, inst, baseRegister)
                if (floorIdValues.size > 1) {
                    logger.warn { "Could not determine floor ID from register for ${inst.opcode.mnemonic}" }
                    continue
                }

                // Get map ID from base+1 register
                val mapIdValues = getRegisterValue(cfg, inst, baseRegister + 1)
                if (mapIdValues.size > 1) {
                    logger.warn { "Could not determine map ID from register for ${inst.opcode.mnemonic}" }
                    continue
                }

                // For OP_MAP_DESIGNATE, map ID is same as floor ID, it only changes the map variant for the default floor
                if (inst.opcode == OP_MAP_DESIGNATE) {
                    mapIdValues.setValue(floorIdValues[0]!!)
                }

                // map_designate R40 reads: R40, R41, R42, R43
                // is equivalent to R40, (R41), R42, R43, R44(map_designate_ex)
                // Get variant ID from base+2 or base+3 register depending on opcode
                val variantRegister = baseRegister + (if (inst.opcode == OP_MAP_DESIGNATE) 2 else 3)
                val variantIdValues = getRegisterValue(cfg, inst, variantRegister)
                if (variantIdValues.size > 1) {
                    logger.warn { "Could not determine variant ID from register for ${inst.opcode.mnemonic}" }
                    continue
                }

                val mapId = mapIdValues[0]!!
                val areaId = getAreaIdByMapId(mapId)
                val variantId = variantIdValues[0]!!

                if (areaId != null) {
                    mapDesignations.getOrPut(areaId) { mutableSetOf() }.add(variantId)
                }
            }

            OP_BB_MAP_DESIGNATE -> {
                val mapId = (inst.args[1] as IntArg).value   // map id
                val variantId = (inst.args[2] as IntArg).value // variant (3rd parameter)

                // Map game area ID to model area ID
                val areaId = getAreaIdByMapId(mapId)

                if (areaId != null) {
                    mapDesignations.getOrPut(areaId) { mutableSetOf() }.add(variantId)
                }
            }
        }
    }

    return mapDesignations
}

/**
 * Extract floor mappings from quest bytecode instructions.
 * Modified to search all instruction segments instead of just function 0 segment
 * to support multi-floor quests with bb_map_designate instructions in different segments.
 */
fun getFloorMappings(
    instructionSegments: List<InstructionSegment>,
    createCfg: () -> ControlFlowGraph,
): List<FloorMapping> {
    val floorMappings = mutableMapOf<Int, FloorMapping>()
    // Find label 0 segment for episode info
    val func0Segment = instructionSegments.find { 0 in it.labels }
    val episode = func0Segment?.let { getEpisode(it) } ?: 0

    var cfg: ControlFlowGraph? = null

    // Search all instruction segments for floor mapping instructions
    // This ensures we find bb_map_designate instructions in any segment, not just function 0
    for (segment in instructionSegments) {
        for (inst in segment.instructions) {
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
                    if (floorIdValues.size > 1) {
                        logger.warn { "Could not determine floor ID from register R$baseRegister for ${inst.opcode.mnemonic}" }
                        continue
                    }

                    // Get map ID from base+1 register
                    val mapIdValues = getRegisterValue(cfg, inst, baseRegister + 1)
                    if (mapIdValues.size > 1) {
                        logger.warn { "Could not determine map ID from register R${baseRegister + 1} for ${inst.opcode.mnemonic}" }
                        continue
                    }

                    // For OP_MAP_DESIGNATE, map ID is same as floor ID, it only changes the map variant for the default floor
                    if (inst.opcode == OP_MAP_DESIGNATE) {
                        mapIdValues.setValue(floorIdValues[0]!!)
                    }

                    // map_designate R40 reads: R40, R41, R42, R43
                    // is equivalent to R40, (R41), R42, R43, R44(map_designate_ex)
                    // Get variant ID from base+2 or base+3 register depending on opcode
                    val variantRegister = baseRegister + (if (inst.opcode == OP_MAP_DESIGNATE) 2 else 3)
                    val variantIdValues = getRegisterValue(cfg, inst, variantRegister)
                    if (variantIdValues.size > 1) {
                        logger.warn { "Could not determine variant ID from register R$variantRegister for ${inst.opcode.mnemonic}" }
                        continue
                    }

                    val floorId = floorIdValues[0]!!
                    val mapId = mapIdValues[0]!!
                    val areaId = getAreaIdByMapId(mapId)
                    val variantId = variantIdValues[0]!!

                    if (areaId != null) {
                        // map_designate/map_designate_ex have higher priority than set_floor_handler
                        floorMappings[floorId] = FloorMapping(floorId, mapId, areaId, variantId)
                    }
                }

                OP_BB_MAP_DESIGNATE -> {
                    val floorId = (inst.args[0] as IntArg).value  // floor id
                    val mapId = (inst.args[1] as IntArg).value   // map id
                    val variantId = (inst.args[2] as IntArg).value // variant (3rd parameter)

                    // Map map ID to model area ID
                    val areaId = getAreaIdByMapId(mapId)

                    if (areaId != null) {
                        // bb_map_designate has higher priority than set_floor_handler, can override existing mappings
                        floorMappings[floorId] = FloorMapping(floorId, mapId, areaId, variantId)
                    }
                }

                OP_SET_FLOOR_HANDLER -> {
                    if (cfg == null) {
                        cfg = createCfg()
                    }

                    try {
                        // Get floor number from stack (position 1, since stack is LIFO and floor is first param)
                        val (floorIdValue, _) = getStackValue(cfg, inst, 1)

                        if (floorIdValue.size == 1L) {
                            val floorId = floorIdValue.first()
                            val mapId = getMapId(episode, floorId)
                            val variantId = 0 // Default variant

                            // set_floor_handler has lower priority - only adds mapping if floor not already mapped
                            // This ensures bb_map_designate instructions take precedence
                            if (!floorMappings.containsKey(floorId)) {
                                // areaId is same as floorId for set_floor_handler
                                floorMappings[floorId] = FloorMapping(floorId, mapId, floorId, variantId)
                            }
                        }
                    } catch (e: Exception) {
                        logger.warn { "Error getting stack values for OP_SET_FLOOR_HANDLER: ${e.message}" }
                    }
                }
            }
        }
    }

    return floorMappings.values.toList()
}

/**
 * Extract episode number from function 0 segment.
 * Returns 0 if no set_episode instruction is found.
 */
fun getEpisode(func0Segment: InstructionSegment): Int {
    return when (val setEpisode = func0Segment.instructions.find {
        it.opcode == OP_SET_EPISODE
    }) {
        null -> {
            logger.debug { "Function 0 has no set_episode instruction." }
            0
        }

        else -> (setEpisode.args[0] as IntArg).value
    }
}

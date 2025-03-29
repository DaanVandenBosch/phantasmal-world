package world.phantasmal.psolib.asm.dataFlowAnalysis

import mu.KotlinLogging
import world.phantasmal.psolib.asm.*

private val logger = KotlinLogging.logger {}

data class FloorMapping(
    val floorId: Int,
    val areaId: Int,
    val variantId: Int
)

/**
 * Maps game-internal area IDs to AreaModel IDs.
 * Game uses hex area IDs while AreaModel uses sequential IDs per episode.
 */
fun mapGameAreaIdToModelAreaId(gameAreaId: Int): Int? {
    return when (gameAreaId) {
        // Episode I (0x00 - 0x11)
        0x00 -> 0   // Pioneer2_Ep1 -> Pioneer II
        0x01 -> 1   // Forest1 -> Forest 1
        0x02 -> 2   // Forest2 -> Forest 2
        0x03 -> 3   // Cave1 -> Cave 1
        0x04 -> 4   // Cave2 -> Cave 2
        0x05 -> 5   // Cave3 -> Cave 3
        0x06 -> 6   // Mines1 -> Mine 1
        0x07 -> 7   // Mines2 -> Mine 2
        0x08 -> 8   // Ruins1 -> Ruins 1
        0x09 -> 9   // Ruins2 -> Ruins 2
        0x0A -> 10  // Ruins3 -> Ruins 3
        0x0B -> 11  // Boss_Dragon -> Under the Dome
        0x0C -> 12  // Boss_Derolle -> Underground Channel
        0x0D -> 13  // Boss_Volopt -> Monitor Room
        0x0E -> 14  // Boss_Darkfalz -> Dark Falz
        0x0F -> 15  // Lobby -> Lobby
        0x10 -> 16  // Battle_Spaceship -> BA Spaceship
        0x11 -> 17  // Battle_Ruins -> BA Palace

        // Episode II (0x12 - 0x23)
        0x12 -> 0   // Pioneer2_Ep2 -> Lab
        0x13 -> 1   // Temple_A -> VR Temple Alpha
        0x14 -> 2   // Temple_B -> VR Temple Beta
        0x15 -> 3   // Spaceship_A -> VR Spaceship Alpha
        0x16 -> 4   // Spaceship_B -> VR Spaceship Beta
        0x17 -> 5   // CCA -> Central Control Area
        0x18 -> 6   // Jungle_East -> Jungle Area East
        0x19 -> 7   // Jungle_North -> Jungle Area North
        0x1A -> 8   // Mountain -> Mountain Area
        0x1B -> 9   // Seaside -> Seaside Area
        0x1C -> 10  // Seabed_Upper -> Seabed Upper Levels
        0x1D -> 11  // Seabed_Lower -> Seabed Lower Levels
        0x1E -> 12  // Boss_Galgryphon -> Cliffs of Gal Da Val
        0x1F -> 13  // Boss_Olgaflow -> Test Subject Disposal Area
        0x20 -> 14  // Boss_Barbaray -> VR Temple Final
        0x21 -> 15  // Bos_GolDragon -> VR Spaceship Final
        0x22 -> 16  // Seaside_Night -> Seaside Area at Night
        0x23 -> 17  // Tower -> Tower

        // Episode IV (0x24 - 0x2E)
        0x24 -> 1   // Wilds1 -> Crater Route 1
        0x25 -> 2   // Wilds2 -> Crater Route 2
        0x26 -> 3   // Wilds3 -> Crater Route 3
        0x27 -> 4   // Wilds4 -> Crater Route 4
        0x28 -> 5   // Crater -> Crater Interior
        0x29 -> 6   // Desert1 -> Subterranean Desert 1
        0x2A -> 7   // Desert2 -> Subterranean Desert 2
        0x2B -> 8   // Desert3 -> Subterranean Desert 3
        0x2C -> 9   // Boss_SaintMilion -> Meteor Impact Site
        0x2D -> 0   // Pioneer2_Ep4 -> Pioneer II
        0x2E -> null // Test_Area -> No mapping (test area)

        else -> null // Unknown area
    }
}

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

                // Get map ID from base+1 register
                val mapIdValues = getRegisterValue(cfg, inst, baseRegister + 1)
                if (mapIdValues.size > 1) {
                    logger.warn { "Could not determine map ID from register for ${inst.opcode.mnemonic}" }
                    continue
                }

                // Get variant ID from base+2 or base+3 register depending on opcode
                val variantRegister = baseRegister + (if (inst.opcode == OP_MAP_DESIGNATE) 2 else 3)
                val variantIdValues = getRegisterValue(cfg, inst, variantRegister)
                if (variantIdValues.size > 1) {
                    logger.warn { "Could not determine variant ID from register for ${inst.opcode.mnemonic}" }
                    continue
                }

                val gameAreaId = mapIdValues[0]!!
                val rawVariantId = variantIdValues[0]!!
                val modelAreaId = mapGameAreaIdToModelAreaId(gameAreaId)

                if (modelAreaId != null) {
                    mapDesignations.getOrPut(modelAreaId) { mutableSetOf() }.add(rawVariantId)
                }
            }

            OP_BB_MAP_DESIGNATE -> {
                (inst.args[0] as IntArg).value  // floor id
                val gameAreaId = (inst.args[1] as IntArg).value   // map id
                val rawVariantId = (inst.args[2] as IntArg).value // variant (3rd parameter)

                // Map game area ID to model area ID
                val modelAreaId = mapGameAreaIdToModelAreaId(gameAreaId)

                if (modelAreaId != null) {
                    mapDesignations.getOrPut(modelAreaId) { mutableSetOf() }.add(rawVariantId)
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
                    // map_designate_ex R40 reads: R40, R41, R42, R43, R44
                    // map_designate R40 reads: R40, R41, R42

                    val baseRegister = (inst.args[0] as IntArg).value

                    // Get floor ID from base register (R+0)
                    val floorIdValues = getRegisterValue(cfg, inst, baseRegister)
                    if (floorIdValues.size > 1) {
                        logger.warn { "Could not determine floor ID from register R$baseRegister for ${inst.opcode.mnemonic}" }
                        continue
                    }

                    // Get map ID from base+1 register (R+1)
                    val mapIdValues = getRegisterValue(cfg, inst, baseRegister + 1)
                    if (mapIdValues.size > 1) {
                        logger.warn { "Could not determine map ID from register R${baseRegister + 1} for ${inst.opcode.mnemonic}" }
                        continue
                    }

                    // Get variant ID from base+2 or base+3 register depending on opcode
                    val variantRegister = baseRegister + (if (inst.opcode == OP_MAP_DESIGNATE) 2 else 3)
                    val variantIdValues = getRegisterValue(cfg, inst, variantRegister)
                    if (variantIdValues.size > 1) {
                        logger.warn { "Could not determine variant ID from register R$variantRegister for ${inst.opcode.mnemonic}" }
                        continue
                    }

                    val floorNumber = floorIdValues[0]!!
                    val gameAreaId = mapIdValues[0]!!
                    val rawVariantId = variantIdValues[0]!!
                    val modelAreaId = mapGameAreaIdToModelAreaId(gameAreaId)

                    logger.info {
                        "${inst.opcode.mnemonic}: Floor=$floorNumber, GameAreaId=$gameAreaId (0x${
                            gameAreaId.toString(16).uppercase()
                        }), " +
                                "ModelAreaId=$modelAreaId, Variant=$rawVariantId, BaseReg=R$baseRegister"
                    }

                    if (modelAreaId != null) {
                        // map_designate/map_designate_ex have higher priority than set_floor_handler
                        floorMappings[floorNumber] = FloorMapping(floorNumber, modelAreaId, rawVariantId)
                        logger.info { "Added floor mapping: Floor $floorNumber -> Area $modelAreaId, Variant $rawVariantId" }
                    } else {
                        logger.warn {
                            "Could not map ${inst.opcode.mnemonic} gameAreaId 0x${
                                gameAreaId.toString(16).uppercase()
                            } to modelAreaId"
                        }
                    }
                }

                OP_BB_MAP_DESIGNATE -> {
                    val floorNumber = (inst.args[0] as IntArg).value  // floor id
                    val gameAreaId = (inst.args[1] as IntArg).value   // map id
                    val rawVariantId = (inst.args[2] as IntArg).value // variant (3rd parameter)

                    // Map game area ID to model area ID
                    val modelAreaId = mapGameAreaIdToModelAreaId(gameAreaId)

                    if (modelAreaId != null) {
                        // bb_map_designate has higher priority than set_floor_handler, can override existing mappings
                        floorMappings[floorNumber] = FloorMapping(floorNumber, modelAreaId, rawVariantId)
                    } else {
                        logger.warn {
                            "Could not map BB gameAreaId 0x${
                                gameAreaId.toString(16).uppercase()
                            } to modelAreaId"
                        }
                    }
                }

                OP_SET_FLOOR_HANDLER -> {
                    if (cfg == null) {
                        cfg = createCfg()
                    }

                    try {
                        // Get floor number from stack (position 1, since stack is LIFO and floor is first param)
                        val (floorNumberValues, _) = getStackValue(cfg, inst, 1)

                        if (floorNumberValues.size == 1L) {
                            val floorNumber = floorNumberValues.first()
                            val gameAreaId = when (episode) {
                                0 -> floorNumber
                                1 -> 0x12 + floorNumber
                                2 -> if (floorNumber == 0) 0x2D else 0x23 + floorNumber
                                else -> {
                                    logger.warn { "Unknown episode $episode, defaulting area ID to 0." }
                                    0
                                }
                            }
                            val rawVariantId = 0 // Default variant

                            // Map game area ID to model area ID
                            val modelAreaId = mapGameAreaIdToModelAreaId(gameAreaId)

                            if (modelAreaId != null) {
                                // set_floor_handler has lower priority - only adds mapping if floor not already mapped
                                // This ensures bb_map_designate instructions take precedence
                                if (!floorMappings.containsKey(floorNumber)) {
                                    floorMappings[floorNumber] = FloorMapping(floorNumber, modelAreaId, rawVariantId)
                                }
                            } else {
                                logger.warn {
                                    "Could not map SFH gameAreaId 0x${
                                        gameAreaId.toString(16).uppercase()
                                    } to modelAreaId"
                                }
                            }
                        } else {
                            logger.warn { "Could not determine floor number from stack: $floorNumberValues" }
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

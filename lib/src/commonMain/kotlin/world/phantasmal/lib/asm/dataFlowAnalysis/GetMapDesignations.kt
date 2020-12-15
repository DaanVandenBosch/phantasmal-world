package world.phantasmal.lib.asm.dataFlowAnalysis

import mu.KotlinLogging
import world.phantasmal.lib.asm.InstructionSegment
import world.phantasmal.lib.asm.OP_BB_MAP_DESIGNATE
import world.phantasmal.lib.asm.OP_MAP_DESIGNATE
import world.phantasmal.lib.asm.OP_MAP_DESIGNATE_EX

private val logger = KotlinLogging.logger {}

fun getMapDesignations(
    func0Segment: InstructionSegment,
    createCfg: () -> ControlFlowGraph,
): Map<Int, Int> {
    val mapDesignations = mutableMapOf<Int, Int>()
    var cfg: ControlFlowGraph? = null

    for (inst in func0Segment.instructions) {
        when (inst.opcode.code) {
            OP_MAP_DESIGNATE.code,
            OP_MAP_DESIGNATE_EX.code,
            -> {
                if (cfg == null) {
                    cfg = createCfg()
                }

                val areaId = getRegisterValue(cfg, inst, inst.args[0].value as Int)

                if (areaId.size > 1) {
                    logger.warn {
                        "Couldn't determine area ID for ${inst.opcode.mnemonic} instruction."
                    }
                    continue
                }

                val variantIdRegister =
                    inst.args[0].value as Int + (if (inst.opcode == OP_MAP_DESIGNATE) 2 else 3)
                val variantId = getRegisterValue(cfg, inst, variantIdRegister)

                if (variantId.size > 1) {
                    logger.warn {
                        "Couldn't determine area variant ID for ${inst.opcode.mnemonic} instruction."
                    }
                    continue
                }

                mapDesignations[areaId[0]!!] = variantId[0]!!
            }

            OP_BB_MAP_DESIGNATE.code -> {
                mapDesignations[inst.args[0].value as Int] = inst.args[2].value as Int
            }
        }
    }

    return mapDesignations
}

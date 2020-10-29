package world.phantasmal.lib.assembly.dataFlowAnalysis

import mu.KotlinLogging
import world.phantasmal.lib.assembly.*
import kotlin.math.max
import kotlin.math.min

private val logger = KotlinLogging.logger {}

/**
 * Computes the possible values of a register right before a specific instruction.
 */
fun getRegisterValue(cfg: ControlFlowGraph, instruction: Instruction, register: Int): ValueSet {
    require(register in 0..255) {
        "register should be between 0 and 255, inclusive but was $register."
    }

    val block = cfg.getBlockForInstruction(instruction)

    return RegisterValueFinder().find(
        mutableSetOf(),
        block,
        block.indexOfInstruction(instruction),
        register
    )
}

private class RegisterValueFinder {
    private var iterations = 0

    fun find(
        path: MutableSet<BasicBlock>,
        block: BasicBlock,
        end: Int,
        register: Int,
    ): ValueSet {
        if (++iterations > 100) {
            logger.warn { "Too many iterations." }
            return ValueSet.all()
        }

        for (i in end - 1 downTo block.start) {
            val instruction = block.segment.instructions[i]
            val args = instruction.args

            when (instruction.opcode.code) {
                OP_LET.code -> {
                    if (args[0].value == register) {
                        return find(LinkedHashSet(path), block, i, args[1].value as Int)
                    }
                }

                OP_LETI.code,
                OP_LETB.code,
                OP_LETW.code,
                OP_SYNC_LETI.code,
                -> {
                    if (args[0].value == register) {
                        return ValueSet.of(args[1].value as Int)
                    }
                }

                OP_SET.code -> {
                    if (args[0].value == register) {
                        return ValueSet.of(1)
                    }
                }

                OP_CLEAR.code -> {
                    if (args[0].value == register) {
                        return ValueSet.of(0)
                    }
                }

                OP_REV.code -> {
                    if (args[0].value == register) {
                        val prevVals = find(LinkedHashSet(path), block, i, register)

                        return if (prevVals.size == 1L && prevVals[0] == 0) {
                            ValueSet.of(1)
                        } else if (0 in prevVals) {
                            ValueSet.ofInterval(0, 1)
                        } else {
                            ValueSet.of(0)
                        }
                    }
                }

                OP_ADDI.code -> {
                    if (args[0].value == register) {
                        val prevVals = find(LinkedHashSet(path), block, i, register)
                        prevVals += args[1].value as Int
                        return prevVals
                    }
                }

                OP_SUBI.code -> {
                    if (args[0].value == register) {
                        val prevVals = find(LinkedHashSet(path), block, i, register)
                        prevVals -= args[1].value as Int
                        return prevVals
                    }
                }

                OP_MULI.code -> {
                    if (args[0].value == register) {
                        val prevVals = find(LinkedHashSet(path), block, i, register)
                        prevVals *= args[1].value as Int
                        return prevVals
                    }
                }

                OP_DIVI.code -> {
                    if (args[0].value == register) {
                        val prevVals = find(LinkedHashSet(path), block, i, register)
                        prevVals /= args[1].value as Int
                        return prevVals
                    }
                }

                OP_IF_ZONE_CLEAR.code -> {
                    if (args[0].value == register) {
                        return ValueSet.ofInterval(0, 1)
                    }
                }

                OP_GET_DIFFLVL.code -> {
                    if (args[0].value == register) {
                        return ValueSet.ofInterval(0, 2)
                    }
                }

                OP_GET_SLOTNUMBER.code -> {
                    if (args[0].value == register) {
                        return ValueSet.ofInterval(0, 3)
                    }
                }

                OP_GET_RANDOM.code -> {
                    if (args[1].value == register) {
                        // TODO: undefined values.
                        val min = find(
                            LinkedHashSet(path),
                            block,
                            i,
                            args[0].value as Int
                        ).minOrNull()!!

                        val max = max(
                            find(
                                LinkedHashSet(path),
                                block,
                                i,
                                args[0].value as Int + 1
                            ).maxOrNull()!!,
                            min + 1,
                        )

                        return ValueSet.ofInterval(min, max - 1)
                    }
                }

                OP_STACK_PUSHM.code,
                OP_STACK_POPM.code,
                -> {
                    val minReg = args[0].value as Int
                    val maxReg = args[0].value as Int + args[1].value as Int

                    if (register in minReg until maxReg) {
                        return ValueSet.all()
                    }
                }

                else -> {
                    // Assume any other opcodes that write to the register can produce any value.
                    val params = instruction.opcode.params
                    val argLen = min(args.size, params.size)

                    for (j in 0 until argLen) {
                        val param = params[j]

                        if (param.type is RegTupRefType) {
                            val regRef = args[j].value as Int

                            for ((k, reg_param) in param.type.registerTuple.withIndex()) {
                                if ((reg_param.access == ParamAccess.Write ||
                                            reg_param.access == ParamAccess.ReadWrite) &&
                                    regRef + k == register
                                ) {
                                    return ValueSet.all()
                                }
                            }
                        }
                    }
                }
            }
        }

        val values = ValueSet.empty()
        path.add(block)

        for (from in block.from) {
            // Bail out from loops.
            if (from in path) {
                return ValueSet.all()
            }

            values.union(find(LinkedHashSet(path), from, from.end, register))
        }

        // If values is empty at this point, we know nothing ever sets the register's value and it
        // still has its initial value of 0.
        if (values.isEmpty()) {
            values.setValue(0)
        }

        return values
    }
}

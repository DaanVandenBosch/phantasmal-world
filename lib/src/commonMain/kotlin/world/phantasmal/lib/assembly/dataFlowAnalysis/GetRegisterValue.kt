package world.phantasmal.lib.assembly.dataFlowAnalysis

import mu.KotlinLogging
import world.phantasmal.lib.assembly.*
import kotlin.math.max
import kotlin.math.min

private val logger = KotlinLogging.logger {}

const val MIN_REGISTER_VALUE = MIN_SIGNED_DWORD_VALUE
const val MAX_REGISTER_VALUE = MAX_SIGNED_DWORD_VALUE

/**
 * Computes the possible values of a register right before a specific instruction.
 */
fun getRegisterValue(cfg: ControlFlowGraph, instruction: Instruction, register: Int): ValueSet {
    val block = cfg.getBlockForInstruction(instruction)

    return findValues(
        Context(),
        mutableSetOf(),
        block,
        block.indexOfInstruction(instruction),
        register
    )
}

private class Context {
    var iterations = 0
}

private fun findValues(
    ctx: Context,
    path: MutableSet<BasicBlock>,
    block: BasicBlock,
    end: Int,
    register: Int,
): ValueSet {
    if (++ctx.iterations > 100) {
        logger.warn { "Too many iterations." }
        return ValueSet.ofInterval(MIN_REGISTER_VALUE, MAX_REGISTER_VALUE)
    }

    for (i in end - 1 downTo block.start) {
        val instruction = block.segment.instructions[i]
        val args = instruction.args

        when (instruction.opcode) {
            OP_LET -> {
                if (args[0].value == register) {
                    return findValues(ctx, LinkedHashSet(path), block, i, args[1].value as Int)
                }
            }

            OP_LETI,
            OP_LETB,
            OP_LETW,
            OP_SYNC_LETI,
            -> {
                if (args[0].value == register) {
                    return ValueSet.of(args[1].value as Int)
                }
            }

            OP_SET -> {
                if (args[0].value == register) {
                    return ValueSet.of(1)
                }
            }

            OP_CLEAR -> {
                if (args[0].value == register) {
                    return ValueSet.of(0)
                }
            }

            OP_REV -> {
                if (args[0].value == register) {
                    val prevVals = findValues(ctx, LinkedHashSet(path), block, i, register)

                    return if (prevVals.size == 1L && prevVals[0] == 0) {
                        ValueSet.of(1)
                    } else if (0 in prevVals) {
                        ValueSet.ofInterval(0, 1)
                    } else {
                        ValueSet.of(0)
                    }
                }
            }

            OP_ADDI -> {
                if (args[0].value == register) {
                    val prevVals = findValues(ctx, LinkedHashSet(path), block, i, register)
                    prevVals += args[1].value as Int
                    return prevVals
                }
            }

            OP_SUBI -> {
                if (args[0].value == register) {
                    val prevVals = findValues(ctx, LinkedHashSet(path), block, i, register)
                    prevVals -= args[1].value as Int
                    return prevVals
                }
            }

            OP_MULI -> {
                if (args[0].value == register) {
                    val prevVals = findValues(ctx, LinkedHashSet(path), block, i, register)
                    prevVals *= args[1].value as Int
                    return prevVals
                }
            }

            OP_DIVI -> {
                if (args[0].value == register) {
                    val prevVals = findValues(ctx, LinkedHashSet(path), block, i, register)
                    prevVals /= args[1].value as Int
                    return prevVals
                }
            }

            OP_IF_ZONE_CLEAR -> {
                if (args[0].value == register) {
                    return ValueSet.ofInterval(0, 1)
                }
            }

            OP_GET_DIFFLVL -> {
                if (args[0].value == register) {
                    return ValueSet.ofInterval(0, 2)
                }
            }

            OP_GET_SLOTNUMBER -> {
                if (args[0].value == register) {
                    return ValueSet.ofInterval(0, 3)
                }
            }

            OP_GET_RANDOM -> {
                if (args[1].value == register) {
                    // TODO: undefined values.
                    val min = findValues(
                        ctx,
                        LinkedHashSet(path),
                        block,
                        i,
                        args[0].value as Int
                    ).minOrNull()!!

                    val max = max(
                        findValues(
                            ctx,
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

            OP_STACK_PUSHM,
            OP_STACK_POPM,
            -> {
                val minReg = args[0].value as Int
                val maxReg = args[0].value as Int + args[1].value as Int

                if (register in minReg until maxReg) {
                    return ValueSet.ofInterval(MIN_REGISTER_VALUE, MAX_REGISTER_VALUE)
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

                        for ((k, reg_param) in param.type.registerTuples.withIndex()) {
                            if ((reg_param.access == ParamAccess.Write ||
                                        reg_param.access == ParamAccess.ReadWrite) &&
                                regRef + k == register
                            ) {
                                return ValueSet.ofInterval(
                                    MIN_REGISTER_VALUE,
                                    MAX_REGISTER_VALUE,
                                )
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
            values.setInterval(MIN_REGISTER_VALUE, MAX_REGISTER_VALUE)
            break
        }

        values.union(findValues(ctx, LinkedHashSet(path), from, from.end, register))
    }

    // If values is empty at this point, we know nothing ever sets the register's value and it still
    // has its initial value of 0.
    if (values.isEmpty()) {
        values.setValue(0)
    }

    return values
}

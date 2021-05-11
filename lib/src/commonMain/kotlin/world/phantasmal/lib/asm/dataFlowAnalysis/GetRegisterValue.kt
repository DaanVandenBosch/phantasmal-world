package world.phantasmal.lib.asm.dataFlowAnalysis

import mu.KotlinLogging
import world.phantasmal.lib.asm.*
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

    // TODO: Deal with incorrect argument types.
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
                OP_SYNC.code -> {
                    // After a sync call, concurrent code could have modified the register. We don't
                    // check whether concurrent code *ever* writes to the register to possibly
                    // continue the analysis.
                    return ValueSet.all()
                }

                OP_VA_CALL.code -> {
                    val value = vaCall(path, block, i, register)
                    if (value.isNotEmpty()) return value
                }

                OP_LET.code -> {
                    if (args[0].value == register) {
                        return find(LinkedHashSet(path), block, i, (args[1] as IntArg).value)
                    }
                }

                OP_LETI.code,
                OP_LETB.code,
                OP_LETW.code,
                OP_SYNC_LETI.code,
                -> {
                    if (args[0].value == register) {
                        return ValueSet.of((args[1] as IntArg).value)
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
                        prevVals += (args[1] as IntArg).value
                        return prevVals
                    }
                }

                OP_SUBI.code -> {
                    if (args[0].value == register) {
                        val prevVals = find(LinkedHashSet(path), block, i, register)
                        prevVals -= (args[1] as IntArg).value
                        return prevVals
                    }
                }

                OP_MULI.code -> {
                    if (args[0].value == register) {
                        val prevVals = find(LinkedHashSet(path), block, i, register)
                        prevVals *= (args[1] as IntArg).value
                        return prevVals
                    }
                }

                OP_DIVI.code -> {
                    if (args[0].value == register) {
                        val prevVals = find(LinkedHashSet(path), block, i, register)
                        prevVals /= (args[1] as IntArg).value
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
                            (args[0] as IntArg).value
                        ).minOrNull()!!

                        val max = max(
                            find(
                                LinkedHashSet(path),
                                block,
                                i,
                                (args[0] as IntArg).value + 1
                            ).maxOrNull()!!,
                            min + 1,
                        )

                        return ValueSet.ofInterval(min, max - 1)
                    }
                }

                OP_STACK_PUSHM.code,
                OP_STACK_POPM.code,
                -> {
                    val minReg = (args[0] as IntArg).value
                    val maxReg = (args[0] as IntArg).value + (args[1] as IntArg).value

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

                        if (param.type is RegType && param.type.registers != null) {
                            val regRef = (args[j] as IntArg).value

                            for ((k, regParam) in param.type.registers.withIndex()) {
                                if (regParam.write && regRef + k == register) {
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

        // If values is empty at this point, we know nothing ever sets the register's value from
        // this thread or handler. Concurrent code could have modified it, we don't yet try to
        // exclude this possibility so we just return all values.
        if (values.isEmpty()) {
            return ValueSet.all()
        }

        return values
    }

    /**
     * After a va_start instruction, 0 or more arg_push instructions can be used. When va_call is
     * executed the values on the stack will become the values of registers r1..r7 (inclusive) in
     * the order that they were pushed.
     *
     * E.g.:
     *
     * va_start
     * arg_pushl 10
     * arg_pushl 20
     * va_call 777
     * va_end
     *
     * This means call 777 with r1 = 10 and r2 = 20.
     */
    private fun vaCall(
        path: MutableSet<BasicBlock>,
        block: BasicBlock,
        vaCallIdx: Int,
        register: Int,
    ): ValueSet {
        if (register !in 1..7) return ValueSet.empty()

        var vaStartIdx = -1
        val stack = mutableListOf<Instruction>()

        for (i in block.start until vaCallIdx) {
            val instruction = block.segment.instructions[i]
            val opcode = instruction.opcode

            if (opcode.code == OP_VA_START.code) {
                vaStartIdx = i
            } else if (vaStartIdx != -1) {
                when (opcode.code) {
                    OP_ARG_PUSHR.code,
                    OP_ARG_PUSHL.code,
                    OP_ARG_PUSHB.code,
                    OP_ARG_PUSHW.code,
                    OP_ARG_PUSHA.code,
                    OP_ARG_PUSHO.code,
                    OP_ARG_PUSHS.code,
                    -> stack.add(instruction)
                }
            }
        }

        return if (register in 1..stack.size) {
            val instruction = stack[register - 1]
            val arg = instruction.args.first()

            when (instruction.opcode.code) {
                OP_ARG_PUSHR.code ->
                    find(LinkedHashSet(path), block, vaStartIdx, (arg as IntArg).value)

                OP_ARG_PUSHL.code,
                OP_ARG_PUSHB.code,
                OP_ARG_PUSHW.code,
                -> ValueSet.of((arg as IntArg).value)

                // TODO: Deal with strings.
                else -> ValueSet.all() // String or pointer
            }
        } else {
            ValueSet.of(0)
        }
    }
}

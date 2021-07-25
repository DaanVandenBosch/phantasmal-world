package world.phantasmal.psolib.asm.dataFlowAnalysis

import mu.KotlinLogging
import world.phantasmal.psolib.asm.*

private val logger = KotlinLogging.logger {}

/**
 * Computes the possible values of a stack element at the nth position from the top, right before a
 * specific instruction. If the stack element's value can be traced back to a single push
 * instruction, that instruction is also returned.
 */
fun getStackValue(
    cfg: ControlFlowGraph,
    instruction: Instruction,
    position: Int,
): Pair<ValueSet, Instruction?> {
    val block = cfg.getBlockForInstruction(instruction)

    return StackValueFinder().find(
        mutableSetOf(),
        cfg,
        block,
        block.indexOfInstruction(instruction),
        position,
    )
}

private class StackValueFinder {
    private var iterations = 0

    fun find(
        path: MutableSet<BasicBlock>,
        cfg: ControlFlowGraph,
        block: BasicBlock,
        end: Int,
        position: Int,
    ): Pair<ValueSet, Instruction?> {
        if (++iterations > 100) {
            logger.warn { "Too many iterations." }
            return Pair(ValueSet.all(), null)
        }

        var pos = position

        for (i in end - 1 downTo block.start) {
            val instruction = block.segment.instructions[i]

            if (instruction.opcode.stack == StackInteraction.Pop) {
                pos += instruction.opcode.params.size
                continue
            }

            val args = instruction.args

            when (instruction.opcode.code) {
                OP_ARG_PUSHR.code -> {
                    if (pos == 0) {
                        val arg = args[0]

                        return if (arg is IntArg) {
                            Pair(getRegisterValue(cfg, instruction, arg.value), instruction)
                        } else {
                            Pair(ValueSet.all(), instruction)
                        }
                    } else {
                        pos--
                    }
                }

                OP_ARG_PUSHL.code,
                OP_ARG_PUSHB.code,
                OP_ARG_PUSHW.code,
                -> {
                    if (pos == 0) {
                        val arg = args[0]

                        return if (arg is IntArg) {
                            Pair(ValueSet.of(arg.value), instruction)
                        } else {
                            Pair(ValueSet.all(), instruction)
                        }
                    } else {
                        pos--
                    }
                }

                OP_ARG_PUSHA.code,
                OP_ARG_PUSHO.code,
                OP_ARG_PUSHS.code,
                -> {
                    if (pos == 0) {
                        return Pair(ValueSet.all(), instruction)
                    } else {
                        pos--
                    }
                }
            }
        }

        val values = ValueSet.empty()
        var instruction: Instruction? = null
        var multipleInstructions = false
        path.add(block)

        for (from in block.from) {
            // Bail out from loops.
            if (from in path) {
                return Pair(ValueSet.all(), null)
            }

            val (fromValues, fromInstruction) = find(LinkedHashSet(path), cfg, from, from.end, pos)
            values.union(fromValues)

            if (!multipleInstructions) {
                if (instruction == null) {
                    instruction = fromInstruction
                } else if (instruction != fromInstruction) {
                    instruction = null
                    multipleInstructions = true
                }
            }
        }

        return Pair(values, instruction)
    }
}

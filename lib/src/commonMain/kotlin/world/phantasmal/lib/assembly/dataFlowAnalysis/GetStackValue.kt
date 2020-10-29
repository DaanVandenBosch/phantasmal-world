package world.phantasmal.lib.assembly.dataFlowAnalysis

import mu.KotlinLogging
import world.phantasmal.lib.assembly.*

private val logger = KotlinLogging.logger {}

/**
 * Computes the possible values of a stack element at the nth position from the top, right before a
 * specific instruction.
 */
fun getStackValue(cfg: ControlFlowGraph, instruction: Instruction, position: Int): ValueSet {
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
    ): ValueSet {
        if (++iterations > 100) {
            logger.warn { "Too many iterations." }
            return ValueSet.all()
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
                        return getRegisterValue(cfg, instruction, args[0].value as Int)
                    } else {
                        pos--
                    }
                }

                OP_ARG_PUSHL.code,
                OP_ARG_PUSHB.code,
                OP_ARG_PUSHW.code,
                -> {
                    if (pos == 0) {
                        return ValueSet.of(args[0].value as Int)
                    } else {
                        pos--
                    }
                }

                OP_ARG_PUSHA.code,
                OP_ARG_PUSHO.code,
                OP_ARG_PUSHS.code,
                -> {
                    if (pos == 0) {
                        return ValueSet.all()
                    } else {
                        pos--
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

            values.union(find(LinkedHashSet(path), cfg, from, from.end, pos))
        }

        return values
    }
}

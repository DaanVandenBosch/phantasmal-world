package world.phantasmal.psolib.asm.dataFlowAnalysis

import world.phantasmal.psolib.asm.*

// See https://en.wikipedia.org/wiki/Control-flow_graph.

enum class BranchType {
    /**
     * Only encountered when the last segment of a script has no jump or return.
     */
    None,

    /**
     * ret.
     */
    Return,

    /**
     * jmp.
     */
    Jump,

    /**
     * Every other jump instruction.
     */
    ConditionalJump,

    /**
     * call, switch_call or va_call.
     */
    Call,
}

/**
 * Instruction sequence into which control flow only enters at the start and only leaves at the end.
 * No code jumps/returns/calls into the middle of a basic block or branches out of a basic block
 * from the middle.
 */
interface BasicBlock {
    /**
     * The instruction segment that this block is a part of.
     */
    val segment: InstructionSegment

    /**
     * Index of this block's first instruction.
     */
    val start: Int

    /**
     * Index of the instruction right after this block's last instruction.
     */
    val end: Int

    /**
     * The way control flow leaves this block.
     */
    val branchType: BranchType

    /**
     * Either jumps or calls when non-empty, depending on [branchType].
     */
    val branchLabels: List<Int>

    /**
     * The blocks which branch to this block.
     */
    val from: List<BasicBlock>

    /**
     * The blocks this block branches to.
     */
    val to: List<BasicBlock>

    fun indexOfInstruction(instruction: Instruction): Int
}

/**
 * Graph representing the flow of control through the [BasicBlock]s of a script.
 */
class ControlFlowGraph internal constructor(
    val blocks: List<BasicBlock>,
    private val instructionToBlock: Map<Instruction, BasicBlock>,
) {
    fun getBlockForInstruction(instruction: Instruction): BasicBlock {
        val block = instructionToBlock[instruction]
        requireNotNull(block) { "Instruction is not part of the control-flow graph." }
        return block
    }

    companion object {
        fun create(bytecodeIr: BytecodeIr): ControlFlowGraph =
            create(bytecodeIr.instructionSegments())

        fun create(segments: List<InstructionSegment>): ControlFlowGraph {
            val cfg = ControlFlowGraphBuilder()

            // Mapping of labels to basic blocks.
            for (segment in segments) {
                createBasicBlocks(cfg, segment)
            }

            linkBlocks(cfg)
            return cfg.build()
        }
    }
}

private class ControlFlowGraphBuilder {
    val blocks: MutableList<BasicBlockImpl> = mutableListOf()
    val instructionsToBlock: MutableMap<Instruction, BasicBlockImpl> = mutableMapOf()
    val labelsToBlock: MutableMap<Int, BasicBlockImpl> = mutableMapOf()

    fun build(): ControlFlowGraph =
        ControlFlowGraph(blocks, instructionsToBlock)
}

private class BasicBlockImpl(
    override val segment: InstructionSegment,
    override val start: Int,
    override val end: Int,
    override val branchType: BranchType,
    override val branchLabels: List<Int>,
) : BasicBlock {
    override val from: MutableList<BasicBlockImpl> = mutableListOf()
    override val to: MutableList<BasicBlockImpl> = mutableListOf()

    override fun indexOfInstruction(instruction: Instruction): Int {
        var index = -1

        for (i in start until end) {
            if (instruction == segment.instructions[i]) {
                index = i
                break
            }
        }

        return index
    }

    fun linkTo(other: BasicBlockImpl) {
        if (other !in to) {
            to.add(other)
            other.from.add(this)
        }
    }
}

private fun createBasicBlocks(cfg: ControlFlowGraphBuilder, segment: InstructionSegment) {
    val len = segment.instructions.size
    var start = 0
    var firstBlock = true

    for (i in 0 until len) {
        val inst = segment.instructions[i]

        var branchType: BranchType
        var branchLabels: List<Int>

        when (inst.opcode.code) {
            // Return.
            OP_RET.code -> {
                branchType = BranchType.Return
                branchLabels = emptyList()
            }

            // Unconditional jump.
            OP_JMP.code -> {
                branchType = BranchType.Jump
                branchLabels = listOfNotNull((inst.args[0] as? IntArg)?.value)
            }

            // Conditional jumps.
            OP_JMP_ON.code,
            OP_JMP_OFF.code,
            -> {
                branchType = BranchType.ConditionalJump
                branchLabels = listOfNotNull((inst.args[0] as? IntArg)?.value)
            }
            OP_JMP_E.code,
            OP_JMPI_E.code,
            OP_JMP_NE.code,
            OP_JMPI_NE.code,
            OP_UJMP_G.code,
            OP_UJMPI_G.code,
            OP_JMP_G.code,
            OP_JMPI_G.code,
            OP_UJMP_L.code,
            OP_UJMPI_L.code,
            OP_JMP_L.code,
            OP_JMPI_L.code,
            OP_UJMP_GE.code,
            OP_UJMPI_GE.code,
            OP_JMP_GE.code,
            OP_JMPI_GE.code,
            OP_UJMP_LE.code,
            OP_UJMPI_LE.code,
            OP_JMP_LE.code,
            OP_JMPI_LE.code,
            -> {
                branchType = BranchType.ConditionalJump
                branchLabels = listOfNotNull((inst.args[2] as? IntArg)?.value)
            }
            OP_SWITCH_JMP.code -> {
                branchType = BranchType.ConditionalJump
                branchLabels = inst.args.drop(1).mapNotNull { (it as? IntArg)?.value }
            }

            // Calls.
            OP_CALL.code,
            OP_VA_CALL.code,
            -> {
                branchType = BranchType.Call
                branchLabels = listOfNotNull((inst.args[0] as? IntArg)?.value)
            }
            OP_SWITCH_CALL.code -> {
                branchType = BranchType.Call
                branchLabels = inst.args.drop(1).mapNotNull { (it as? IntArg)?.value }
            }

            // All other opcodes.
            else -> {
                if (i == len - 1) {
                    // This is the last block of the segment.
                    branchType = BranchType.None
                    branchLabels = emptyList()
                } else {
                    // Non-branching instruction, part of the current block.
                    continue
                }
            }
        }

        val block = BasicBlockImpl(segment, start, i + 1, branchType, branchLabels)

        for (j in block.start until block.end) {
            cfg.instructionsToBlock[block.segment.instructions[j]] = block
        }

        cfg.blocks.add(block)

        if (firstBlock) {
            for (label in segment.labels) {
                cfg.labelsToBlock[label] = block
            }

            firstBlock = false
        }

        start = i + 1
    }
}

private fun linkBlocks(cfg: ControlFlowGraphBuilder) {
    // Pairs of calling block and block to which callees should return to.
    val callers = mutableListOf<Pair<BasicBlockImpl, BasicBlockImpl>>()

    for ((i, block) in cfg.blocks.withIndex()) {
        val nextBlock = cfg.blocks.getOrNull(i + 1)

        when (block.branchType) {
            BranchType.Return ->
                continue

            BranchType.Call ->
                nextBlock?.let { callers.add(block to nextBlock) }

            BranchType.None,
            BranchType.ConditionalJump,
            -> nextBlock?.let(block::linkTo)

            BranchType.Jump -> {
                // Ignore.
            }
        }

        for (label in block.branchLabels) {
            cfg.labelsToBlock[label]?.let { toBlock ->
                block.linkTo(toBlock)
            }
        }
    }

    for ((caller, ret) in callers) {
        linkReturningBlocks(cfg.labelsToBlock, ret, caller)
    }
}

/**
 * Links returning blocks to their callers.
 *
 * @param labelBlocks Mapping of labels to basic blocks.
 * @param ret Basic block the caller should return to.
 * @param caller Calling basic block.
 */
private fun linkReturningBlocks(
    labelBlocks: Map<Int, BasicBlockImpl>,
    ret: BasicBlockImpl,
    caller: BasicBlockImpl,
) {
    for (label in caller.branchLabels) {
        labelBlocks[label]?.let { callee ->
            if (callee.branchType === BranchType.Return) {
                callee.linkTo(ret)
            } else {
                linkReturningBlocksRecurse(mutableSetOf(), ret, callee)
            }
        }
    }
}

/**
 * @param encountered For avoiding infinite loops.
 * @param ret
 * @param block
 */
private fun linkReturningBlocksRecurse(
    encountered: MutableSet<BasicBlockImpl>,
    ret: BasicBlockImpl,
    block: BasicBlockImpl,
) {
    if (block in encountered) {
        return
    } else {
        encountered.add(block)
    }

    for (toBlock in block.to) {
        if (toBlock.branchType === BranchType.Return) {
            toBlock.linkTo(ret)
        } else {
            linkReturningBlocksRecurse(encountered, ret, toBlock)
        }
    }
}

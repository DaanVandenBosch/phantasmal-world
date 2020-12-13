package world.phantasmal.lib.asm

import world.phantasmal.lib.buffer.Buffer

/**
 * Intermediate representation of PSO bytecode. Used by most ASM/bytecode analysis code.
 */
class BytecodeIr(
    val segments: List<Segment>,
) {
    fun instructionSegments(): List<InstructionSegment> =
        segments.filterIsInstance<InstructionSegment>()
}

enum class SegmentType {
    Instructions,
    Data,
    String,
}

/**
 * Segment of bytecode. A segment starts with an instruction, data block or string that is
 * referenced by one or more labels. The segment ends right before the next instruction, data block
 * or string that is referenced by a label.
 */
sealed class Segment(
    val type: SegmentType,
    val labels: MutableList<Int>,
    val srcLoc: SegmentSrcLoc,
)

class InstructionSegment(
    labels: MutableList<Int>,
    val instructions: MutableList<Instruction>,
    srcLoc: SegmentSrcLoc,
) : Segment(SegmentType.Instructions, labels, srcLoc)

class DataSegment(
    labels: MutableList<Int>,
    val data: Buffer,
    srcLoc: SegmentSrcLoc,
) : Segment(SegmentType.Data, labels, srcLoc)

class StringSegment(
    labels: MutableList<Int>,
    var value: String,
    srcLoc: SegmentSrcLoc,
) : Segment(SegmentType.String, labels, srcLoc)

/**
 * Opcode invocation.
 */
class Instruction(
    val opcode: Opcode,
    /**
     * Immediate arguments for the opcode.
     */
    val args: List<Arg>,
    val srcLoc: InstructionSrcLoc?,
) {
    /**
     * Maps each parameter by index to its immediate arguments.
     */
    private val paramToArgs: List<List<Arg>>

    init {
        val paramToArgs: MutableList<MutableList<Arg>> = mutableListOf()
        this.paramToArgs = paramToArgs

        if (opcode.stack != StackInteraction.Pop) {
            for (i in opcode.params.indices) {
                val type = opcode.params[i].type
                val pArgs = mutableListOf<Arg>()
                paramToArgs.add(pArgs)

                // Variable length arguments are always last, so we can just gobble up all arguments
                // from this point.
                if (type is ILabelVarType || type is RegRefVarType) {
                    check(i == opcode.params.lastIndex)

                    for (j in i until args.size) {
                        pArgs.add(args[j])
                    }
                } else {
                    pArgs.add(args[i])
                }
            }
        }
    }

    /**
     * Returns the immediate arguments for the parameter at the given index.
     */
    fun getArgs(paramIndex: Int): List<Arg> = paramToArgs[paramIndex]

    /**
     * Returns the source locations of the immediate arguments for the parameter at the given index.
     */
    fun getArgSrcLocs(paramIndex: Int): List<SrcLoc> {
        val argSrcLocs = srcLoc?.args
            ?: return emptyList()

        val type = opcode.params[paramIndex].type

        // Variable length arguments are always last, so we can just gobble up all SrcLocs from
        // paramIndex onward.
        return if (type is ILabelVarType || type is RegRefVarType) {
            argSrcLocs.drop(paramIndex)
        } else {
            listOf(argSrcLocs[paramIndex])
        }
    }

    /**
     * Returns the source locations of the stack arguments for the parameter at the given index.
     */
    fun getStackArgSrcLocs(paramIndex: Int): List<StackArgSrcLoc> {
        val argSrcLocs = srcLoc?.stackArgs

        if (argSrcLocs == null || paramIndex > argSrcLocs.lastIndex) {
            return emptyList()
        }

        val type = opcode.params[paramIndex].type

        // Variable length arguments are always last, so we can just gobble up all SrcLocs from
        // paramIndex onward.
        return if (type is ILabelVarType || type is RegRefVarType) {
            argSrcLocs.drop(paramIndex)
        } else {
            listOf(argSrcLocs[paramIndex])
        }
    }

    /**
     * Returns the byte size of the entire instruction, i.e. the sum of the opcode size and all
     * argument sizes.
     */
    fun getSize(dcGcFormat: Boolean): Int {
        var size = opcode.size

        if (opcode.stack == StackInteraction.Pop) return size

        for (i in opcode.params.indices) {
            val type = opcode.params[i].type
            val args = getArgs(i)

            size += when (type) {
                is ByteType,
                is RegRefType,
                is RegTupRefType,
                -> 1

                // Ensure this case is before the LabelType case because ILabelVarType extends
                // LabelType.
                is ILabelVarType -> 1 + 2 * args.size

                is ShortType,
                is LabelType,
                -> 2

                is IntType,
                is FloatType,
                -> 4

                is StringType -> {
                    if (dcGcFormat) {
                        (args[0].value as String).length + 1
                    } else {
                        2 * (args[0].value as String).length + 2
                    }
                }

                is RegRefVarType -> 1 + args.size

                else -> error("Parameter type ${type::class} not implemented.")
            }
        }

        return size
    }
}

/**
 * Instruction argument.
 */
data class Arg(val value: Any)

/**
 * Position and length of related source assembly code.
 */
open class SrcLoc(
    val lineNo: Int,
    val col: Int,
    val len: Int,
)

/**
 * Locations of the instruction parts in the source assembly code.
 */
class InstructionSrcLoc(
    val mnemonic: SrcLoc?,
    val args: List<SrcLoc>,
    val stackArgs: List<StackArgSrcLoc>,
)

/**
 * Locations of an instruction's stack arguments in the source assembly code.
 */
class StackArgSrcLoc(lineNo: Int, col: Int, len: Int, val value: Any) : SrcLoc(lineNo, col, len)

/**
 * Locations of a segment's labels in the source assembly code.
 */
class SegmentSrcLoc(val labels: MutableList<SrcLoc> = mutableListOf())

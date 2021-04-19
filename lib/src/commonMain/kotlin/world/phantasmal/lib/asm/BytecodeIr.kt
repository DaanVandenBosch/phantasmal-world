package world.phantasmal.lib.asm

import world.phantasmal.core.unsafe.unsafeAssertNotNull
import world.phantasmal.lib.buffer.Buffer
import kotlin.math.ceil

/**
 * Intermediate representation of PSO bytecode. Used by most ASM/bytecode analysis code.
 */
class BytecodeIr(
    val segments: List<Segment>,
) {
    fun instructionSegments(): List<InstructionSegment> =
        segments.filterIsInstance<InstructionSegment>()

    fun copy(): BytecodeIr =
        BytecodeIr(segments.map { it.copy() })
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
) {
    abstract fun size(dcGcFormat: Boolean): Int
    abstract fun copy(): Segment
}

class InstructionSegment(
    labels: MutableList<Int>,
    val instructions: MutableList<Instruction>,
    srcLoc: SegmentSrcLoc = SegmentSrcLoc(mutableListOf()),
) : Segment(SegmentType.Instructions, labels, srcLoc) {
    override fun size(dcGcFormat: Boolean): Int =
        instructions.sumBy { it.getSize(dcGcFormat) }

    override fun copy(): InstructionSegment =
        InstructionSegment(
            ArrayList(labels),
            instructions.mapTo(ArrayList(instructions.size)) { it.copy() },
            srcLoc.copy(),
        )
}

class DataSegment(
    labels: MutableList<Int>,
    val data: Buffer,
    srcLoc: SegmentSrcLoc = SegmentSrcLoc(mutableListOf()),
) : Segment(SegmentType.Data, labels, srcLoc) {
    override fun size(dcGcFormat: Boolean): Int =
        data.size

    override fun copy(): DataSegment =
        DataSegment(ArrayList(labels), data.copy(), srcLoc.copy())
}

class StringSegment(
    labels: MutableList<Int>,
    value: String,
    /**
     * Normally string segments have a byte length that is a multiple of 4, but some bytecode is
     * malformed so we store the initial size in the bytecode.
     */
    private var bytecodeSize: Int?,
    srcLoc: SegmentSrcLoc = SegmentSrcLoc(mutableListOf()),
) : Segment(SegmentType.String, labels, srcLoc) {
    var value: String = value
        set(value) {
            bytecodeSize = null
            field = value
        }

    override fun size(dcGcFormat: Boolean): Int =
        // String segments should be multiples of 4 bytes.
        bytecodeSize
            ?: if (dcGcFormat) {
                4 * ceil((value.length + 1) / 4.0).toInt()
            } else {
                4 * ceil((value.length + 1) / 2.0).toInt()
            }

    override fun copy(): StringSegment =
        StringSegment(ArrayList(labels), value, bytecodeSize, srcLoc.copy())
}

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
    // Avoid using lazy to keep GC pressure low.
    private var paramToArgs: List<List<Arg>>? = null

    /**
     * Returns the immediate arguments for the parameter at the given index.
     */
    fun getArgs(paramIndex: Int): List<Arg> {
        if (paramToArgs == null) {
            val paramToArgs: MutableList<MutableList<Arg>> = mutableListOf()
            this.paramToArgs = paramToArgs

            if (opcode.stack !== StackInteraction.Pop) {
                for (i in opcode.params.indices) {
                    val type = opcode.params[i].type
                    val pArgs = mutableListOf<Arg>()
                    paramToArgs.add(pArgs)

                    // Variable length arguments are always last, so we can just gobble up all arguments
                    // from this point.
                    if (type === ILabelVarType || type === RegRefVarType) {
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

        return paramToArgs.unsafeAssertNotNull()[paramIndex]
    }

    /**
     * Returns the source locations of the immediate arguments for the parameter at the given index.
     */
    fun getArgSrcLocs(paramIndex: Int): List<SrcLoc> {
        val argSrcLocs = srcLoc?.args
            ?: return emptyList()

        val type = opcode.params[paramIndex].type

        // Variable length arguments are always last, so we can just gobble up all SrcLocs from
        // paramIndex onward.
        return if (type === ILabelVarType || type === RegRefVarType) {
            argSrcLocs.drop(paramIndex)
        } else {
            listOf(argSrcLocs[paramIndex])
        }
    }

    /**
     * Returns the source locations of the stack arguments for the parameter at the given index.
     */
    fun getStackArgSrcLocs(paramIndex: Int): List<SrcLoc> {
        val argSrcLocs = srcLoc?.stackArgs

        if (argSrcLocs == null || paramIndex > argSrcLocs.lastIndex) {
            return emptyList()
        }

        val type = opcode.params[paramIndex].type

        // Variable length arguments are always last, so we can just gobble up all SrcLocs from
        // paramIndex onward.
        return if (type === ILabelVarType || type === RegRefVarType) {
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

        if (opcode.stack === StackInteraction.Pop) return size

        for (i in opcode.params.indices) {
            val type = opcode.params[i].type
            val args = getArgs(i)

            size += when (type) {
                ByteType,
                RegRefType,
                -> 1

                // Ensure this case is before the LabelType case because ILabelVarType extends
                // LabelType.
                ILabelVarType -> 1 + 2 * args.size

                ShortType -> 2

                IntType,
                FloatType,
                -> 4

                StringType -> {
                    if (dcGcFormat) {
                        (args[0].value as String).length + 1
                    } else {
                        2 * (args[0].value as String).length + 2
                    }
                }

                RegRefVarType -> 1 + args.size

                // Check RegTupRefType and LabelType last, because "is" checks are very slow in JS.
                is RegTupRefType -> 1

                is LabelType -> 2

                else -> error("Parameter type ${type::class} not implemented.")
            }
        }

        return size
    }

    fun copy(): Instruction =
        Instruction(opcode, args, srcLoc)
}

/**
 * Instruction argument.
 */
data class Arg(val value: Any)

/**
 * Position and length of related source assembly code.
 */
class SrcLoc(
    val lineNo: Int,
    val col: Int,
    val len: Int,
)

/**
 * Locations of the instruction parts in the source assembly code.
 */
class InstructionSrcLoc(
    val mnemonic: SrcLoc?,
    val args: List<SrcLoc> = emptyList(),
    val stackArgs: List<SrcLoc> = emptyList(),
)

/**
 * Locations of a segment's labels in the source assembly code.
 */
class SegmentSrcLoc(val labels: MutableList<SrcLoc> = mutableListOf()) {
    fun copy(): SegmentSrcLoc =
        SegmentSrcLoc(ArrayList(labels))
}

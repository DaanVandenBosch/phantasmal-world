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
    val valid: Boolean,
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
            val paramToArgs: MutableList<List<Arg>> = mutableListOf()
            this.paramToArgs = paramToArgs

            if (opcode.stack !== StackInteraction.Pop) {
                for (i in opcode.params.indices) {
                    val param = opcode.params[i]

                    // Variable length arguments are always last, so we can just gobble up all
                    // arguments from this point.
                    val pArgs = if (param.varargs) {
                        check(i == opcode.params.lastIndex)
                        args.drop(i)
                    } else {
                        listOfNotNull(args.getOrNull(i))
                    }

                    paramToArgs.add(pArgs)
                }
            }
        }

        return unsafeAssertNotNull(paramToArgs)[paramIndex]
    }

    /**
     * Returns the source locations of the (immediate or stack) arguments for the parameter at the
     * given index.
     */
    fun getArgSrcLocs(paramIndex: Int): List<ArgSrcLoc> {
        val argSrcLocs = srcLoc?.args
            ?: return emptyList()

        return if (opcode.params[paramIndex].varargs) {
            // Variadic parameters are always last, so we can just gobble up all SrcLocs from
            // paramIndex onward.
            argSrcLocs.drop(paramIndex)
        } else {
            listOfNotNull(argSrcLocs.getOrNull(paramIndex))
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
                ByteType -> 1

                // Ensure this case is before the LabelType case because ILabelVarType extends
                // LabelType.
                ILabelVarType -> 1 + 2 * args.size

                ShortType -> 2

                IntType,
                FloatType,
                -> 4

                StringType -> {
                    if (dcGcFormat) {
                        (args[0] as StringArg).value.length + 1
                    } else {
                        2 * (args[0] as StringArg).value.length + 2
                    }
                }

                RegVarType -> 1 + args.size

                // Check RegRefType and LabelType last, because "is" checks are very slow in JS.

                is RegType -> 1

                is LabelType -> 2

                else -> error("Parameter type ${type::class} not implemented.")
            }
        }

        return size
    }

    fun copy(): Instruction =
        Instruction(opcode, args, valid, srcLoc).also { it.paramToArgs = paramToArgs }
}

/**
 * Instruction argument.
 */
sealed class Arg {
    abstract val value: Any?

    abstract fun coerceInt(): Int
    abstract fun coerceFloat(): Float
    abstract fun coerceString(): String
}

data class IntArg(override val value: Int) : Arg() {
    override fun coerceInt(): Int = value
    override fun coerceFloat(): Float = Float.fromBits(value)
    override fun coerceString(): String = value.toString()
}

data class FloatArg(override val value: Float) : Arg() {
    override fun coerceInt(): Int = value.toRawBits()
    override fun coerceFloat(): Float = value
    override fun coerceString(): String = value.toString()
}

data class StringArg(override val value: String) : Arg() {
    override fun coerceInt(): Int = 0
    override fun coerceFloat(): Float = 0f
    override fun coerceString(): String = value
}

data class UnknownArg(override val value: Any?) : Arg() {
    override fun coerceInt(): Int = 0
    override fun coerceFloat(): Float = 0f
    override fun coerceString(): String = ""
}

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
    /**
     * Immediate or stack argument locations.
     */
    val args: List<ArgSrcLoc> = emptyList(),
    /**
     * Does the instruction end with a comma? This can be the case when a user has partially typed
     * an instruction.
     */
    val trailingArgSeparator: Boolean,
)

/**
 * Location of an instruction argument in the source assembly code.
 */
class ArgSrcLoc(
    /**
     * The precise location of this argument.
     */
    val precise: SrcLoc,
    /**
     * The location of this argument, its surrounding whitespace and the following comma if there is
     * one.
     */
    val coarse: SrcLoc,
)

/**
 * Locations of a segment's labels in the source assembly code.
 */
class SegmentSrcLoc(val labels: MutableList<SrcLoc> = mutableListOf()) {
    fun copy(): SegmentSrcLoc =
        SegmentSrcLoc(ArrayList(labels))
}

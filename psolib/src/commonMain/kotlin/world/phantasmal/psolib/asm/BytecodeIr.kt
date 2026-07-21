package world.phantasmal.psolib.asm

import world.phantasmal.core.unsafe.unsafeAssertNotNull
import world.phantasmal.psolib.buffer.Buffer
import world.phantasmal.psolib.encoding.encodeShiftJis
import kotlin.math.ceil

/**
 * Describes how strings are encoded in the bytecode binary format.
 *
 * This is independent of the push/pop calling convention, which is the same across all PSO
 * versions.
 */
enum class BytecodeStringEncoding {
    /** DC/GC non-Japanese: 1-byte-per-char ASCII. */
    ASCII,

    /** DC/GC Japanese: 1- or 2-byte-per-char Shift-JIS. */
    SHIFT_JIS,

    /** PC/BB: 2-bytes-per-char UTF-16LE. */
    UTF16,
}

/**
 * Byte length a string occupies in the bytecode, including its null terminator(s).
 * For Shift-JIS the size depends on the actual code points, so we re-encode to count.
 */
fun BytecodeStringEncoding.bytecodeByteLength(value: String): Int =
    when (this) {
        BytecodeStringEncoding.ASCII -> value.length + 1
        BytecodeStringEncoding.SHIFT_JIS -> encodeShiftJis(value).size + 1
        BytecodeStringEncoding.UTF16 -> 2 * value.length + 2
    }

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
    abstract fun size(stringEncoding: BytecodeStringEncoding): Int
    abstract fun copy(): Segment
}

class InstructionSegment(
    labels: MutableList<Int>,
    val instructions: MutableList<Instruction>,
    srcLoc: SegmentSrcLoc = SegmentSrcLoc(mutableListOf()),
) : Segment(SegmentType.Instructions, labels, srcLoc) {
    override fun size(stringEncoding: BytecodeStringEncoding): Int =
        instructions.sumOf { it.getSize(stringEncoding) }

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
    override fun size(stringEncoding: BytecodeStringEncoding): Int =
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

    override fun size(stringEncoding: BytecodeStringEncoding): Int =
        // String segments should be multiples of 4 bytes.
        bytecodeSize
            ?: when (stringEncoding) {
                BytecodeStringEncoding.ASCII ->
                    4 * ceil((value.length + 1) / 4.0).toInt()

                BytecodeStringEncoding.SHIFT_JIS ->
                    4 * ceil((encodeShiftJis(value).size + 1) / 4.0).toInt()

                BytecodeStringEncoding.UTF16 ->
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
    fun getSize(stringEncoding: BytecodeStringEncoding): Int {
        var size = opcode.size

        if (opcode.stack === StackInteraction.Pop) {
            // All known PSO versions use push instructions for Pop opcodes in binary format.
            size += pushInstructionsSize(stringEncoding)
            return size
        }

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
                    val str = (args[0] as StringArg).value
                    stringEncoding.bytecodeByteLength(str)
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

    private fun pushInstructionsSize(stringEncoding: BytecodeStringEncoding): Int {
        var totalSize = 0

        for (i in opcode.params.indices) {
            val type = opcode.params[i].type
            val args = getArgs(i)

            for (arg in args) {
                totalSize += pushInstructionSize(arg, type, stringEncoding)
            }
        }

        return totalSize
    }
}

/**
 * Returns the byte size of a single push instruction for the given argument and parameter type.
 */
fun pushInstructionSize(arg: Arg, paramType: AnyType, stringEncoding: BytecodeStringEncoding): Int {
    // Opcode byte (1) + argument bytes.
    if (arg is IntArg && arg.isRegRef) {
        return 2 // arg_pushr: 1 opcode + 1 byte register
    }

    return when (paramType) {
        ByteType, is RegType, RegVarType -> 2 // arg_pushb: 1 opcode + 1 byte
        ShortType, ILabelVarType, is LabelType -> 3 // arg_pushw: 1 opcode + 2 bytes
        IntType, FloatType -> 5 // arg_pushl: 1 opcode + 4 bytes
        StringType -> {
            val str = (arg as StringArg).value
            // arg_pushs: 1 opcode + null-terminated string
            1 + stringEncoding.bytecodeByteLength(str)
        }
        else -> error("Cannot compute push instruction size for type ${paramType::class.simpleName}.")
    }
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

data class IntArg(override val value: Int, val isRegRef: Boolean = false) : Arg() {
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
data class SrcLoc(
    val lineNo: Int,
    val col: Int,
    val len: Int,
)

/**
 * Locations of the instruction parts in the source assembly code.
 */
data class InstructionSrcLoc(
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
data class ArgSrcLoc(
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

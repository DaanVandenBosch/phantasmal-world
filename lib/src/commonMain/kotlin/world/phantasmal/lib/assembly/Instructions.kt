package world.phantasmal.lib.assembly

import world.phantasmal.lib.buffer.Buffer
import kotlin.math.min

/**
 * Opcode invocation.
 */
class Instruction(
    val opcode: Opcode,
    val args: List<Arg>,
    val srcLoc: InstructionSrcLoc?,
) {
    /**
     * Maps each parameter by index to its arguments.
     */
    val paramToArgs: List<List<Arg>>

    init {
        val len = min(opcode.params.size, args.size)
        val paramToArgs: MutableList<MutableList<Arg>> = mutableListOf()

        for (i in 0 until len) {
            val type = opcode.params[i].type
            val arg = args[i]
            paramToArgs[i] = mutableListOf()

            if (type is ILabelVarType || type is RegRefVarType) {
                for (j in i until args.size) {
                    paramToArgs[i].add(args[j])
                }
            } else {
                paramToArgs[i].add(arg)
            }
        }

        this.paramToArgs = paramToArgs
    }
}

/**
 * Returns the byte size of the entire instruction, i.e. the sum of the opcode size and all
 * argument sizes.
 */
fun instructionSize(instruction: Instruction, dcGcFormat: Boolean): Int {
    val opcode = instruction.opcode
    val pLen = min(opcode.params.size, instruction.paramToArgs.size)
    var argSize = 0

    for (i in 0 until pLen) {
        val type = opcode.params[i].type
        val args = instruction.paramToArgs[i]

        argSize += when (type) {
            is ByteType,
            is RegRefType,
            is RegTupRefType,
            -> 1

            is WordType,
            is LabelType,
            is ILabelType,
            is DLabelType,
            is SLabelType,
            -> 2

            is DWordType,
            is FloatType,
            -> 4

            is StringType -> {
                if (dcGcFormat) {
                    (args[0].value as String).length + 1
                } else {
                    2 * (args[0].value as String).length + 2
                }
            }

            is ILabelVarType -> 1 + 2 * args.size

            is RegRefVarType -> 1 + args.size

            else -> error("Parameter type ${type::class} not implemented.")
        }
    }

    return opcode.size + argSize
}

/**
 * Instruction argument.
 */
data class Arg(val value: Any)

enum class SegmentType {
    Instructions,
    Data,
    String,
}

/**
 * Segment of object code. A segment starts with an instruction, byte or string character that is
 * referenced by one or more labels. The segment ends right before the next instruction, byte or
 * string character that is referenced by a label.
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

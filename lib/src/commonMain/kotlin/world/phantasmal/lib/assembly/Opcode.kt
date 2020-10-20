package world.phantasmal.lib.assembly

/**
 * Abstract super type of all types.
 */
open class AnyType

/**
 * Purely abstract super type of all value types.
 */
sealed class ValueType : AnyType()

/**
 * 8-Bit integer.
 */
object ByteType : ValueType()

/**
 * 16-Bit integer.
 */
object WordType : ValueType()

/**
 * 32-Bit integer.
 */
object DWordType : ValueType()

/**
 * 32-Bit floating point number.
 */
object FloatType : ValueType()

/**
 * Abstract super type of all label types.
 */
open class LabelType : ValueType()

/**
 * Named reference to an instruction.
 */
object ILabelType : LabelType()

/**
 * Named reference to a data segment.
 */
object DLabelType : LabelType()

/**
 * Named reference to a string segment.
 */
object SLabelType : LabelType()

/**
 * String of arbitrary size.
 */
object StringType : LabelType()

/**
 * Arbitrary amount of instruction labels.
 */
object ILabelVarType : LabelType()

/**
 * Purely abstract super type of all reference types.
 */
sealed class RefType : AnyType()

/**
 * Reference to one or more registers.
 */
object RegRefType : RefType()

/**
 * Reference to a fixed amount of consecutive registers of specific types.
 * The only parameterized type.
 */
class RegTupRefType(val registerTuples: List<Param>) : RefType()

/**
 * Arbitrary amount of register references.
 */
object RegRefVarType : RefType()

/**
 * Raw memory pointer.
 */
object PointerType : AnyType()

const val MIN_SIGNED_DWORD_VALUE = Int.MIN_VALUE
const val MAX_SIGNED_DWORD_VALUE = Int.MAX_VALUE
const val MIN_UNSIGNED_DWORD_VALUE = UInt.MIN_VALUE
const val MAX_UNSIGNED_DWORD_VALUE = UInt.MAX_VALUE
const val MIN_DWORD_VALUE = MIN_SIGNED_DWORD_VALUE
const val MAX_DWORD_VALUE = MAX_UNSIGNED_DWORD_VALUE

enum class ParamAccess {
    Read,
    Write,
    ReadWrite,
}

class Param(
    val type: AnyType,
    /**
     * Documentation string.
     */
    val doc: String?,
    /**
     * The way referenced registers are accessed by the instruction. Only set when type is a
     * register reference.
     */
    val access: ParamAccess?,
)

enum class StackInteraction {
    Push,
    Pop,
}

/**
 * Opcode for script object code. Invoked by instructions.
 */
class Opcode(
    /**
     * 1- Or 2-byte big-endian representation of this opcode as used in object code.
     */
    val code: Int,
    /**
     * String representation of this opcode as used in assembly.
     */
    val mnemonic: String,
    /**
     * Documentation string.
     */
    val doc: String?,
    /**
     * Parameters passed in directly or via the stack, depending on the value of [stack].
     */
    val params: List<Param>,
    /**
     * Stack interaction.
     */
    val stack: StackInteraction?,
) {
    /**
     * Byte size of the opcode, either 1 or 2.
     */
    val size: Int = if (code < 0xFF) 1 else 2

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        other as Opcode
        return code == other.code
    }

    override fun hashCode(): Int = code
}

fun codeToOpcode(code: Int): Opcode =
    when {
        code <= 0xFF -> getOpcode(code, code, OPCODES)
        code <= 0xF8FF -> getOpcode(code, code and 0xFF, OPCODES_F8)
        else -> getOpcode(code, code and 0xFF, OPCODES_F9)
    }

private fun getOpcode(code: Int, index: Int, opcodes: Array<Opcode?>): Opcode {
    var opcode = opcodes[index]

    if (opcode == null) {
        opcode = Opcode(code, "unknown_${code.toString(16)}", null, emptyList(), null)
        opcodes[index] = opcode
    }

    return opcode
}

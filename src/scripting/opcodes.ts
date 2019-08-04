/**
 * Abstract super type of all types.
 */
class AnyType {}

/**
 * Abstract super type of all value types.
 */
class ValueType extends AnyType {}

/**
 * 8-Bit integer.
 */
class ByteType extends ValueType {}

/**
 * 16-Bit integer.
 */
class WordType extends ValueType {}

/**
 * 32-Bit integer.
 */
class DWordType extends ValueType {}

/**
 * 32-Bit floating point number.
 */
class FloatType extends ValueType {}

/**
 * Abstract super type of all label types.
 */
class LabelType extends ValueType {}

/**
 * Named reference to an instruction.
 */
class ILabelType extends LabelType {}

/**
 * Named reference to a data segment.
 */
class DLabelType extends LabelType {}

/**
 * Named reference to a string segment.
 */
class SLabelType extends LabelType {}

/**
 * String of arbitrary size.
 */
class StringType extends ValueType {}

/**
 * Arbitrary amount of instruction labels.
 */
class ILabelVarType extends ValueType {}

/**
 * Abstract super type of all reference types.
 */
class RefType extends AnyType {}

/**
 * Reference to one or more registers.
 */
class RegRefType extends RefType {}

/**
 * Reference to a fixed amount of consecutive registers of specific types.
 * The only parameterized type.
 */
export class RegTupRefType extends RefType {
    readonly registers: Param[];

    constructor(...registers: Param[]) {
        super();
        this.registers = registers;
    }
}

/**
 * Arbitrary amount of register references.
 */
class RegRefVarType extends RefType {}

/**
 * Raw memory pointer.
 */
class PointerType extends AnyType {}

// Singleton type constants.
// All types except `RegTupRefType` have a single instance.
export const TYPE_ANY = new AnyType();
export const TYPE_VALUE = new ValueType();
export const TYPE_BYTE = new ByteType();
export const TYPE_WORD = new WordType();
export const TYPE_DWORD = new DWordType();
export const TYPE_FLOAT = new FloatType();
export const TYPE_LABEL = new LabelType();
export const TYPE_I_LABEL = new ILabelType();
export const TYPE_D_LABEL = new DLabelType();
export const TYPE_S_LABEL = new SLabelType();
export const TYPE_STRING = new StringType();
export const TYPE_I_LABEL_VAR = new ILabelVarType();
export const TYPE_REF = new RefType();
export const TYPE_REG_REF = new RegRefType();
// No singleton constant for `RegTupRefType` because it is parameterized.
export const TYPE_REG_REF_VAR = new RegRefVarType();
export const TYPE_POINTER = new PointerType();

export const MIN_SIGNED_DWORD_VALUE = -Math.pow(2, 31);
export const MAX_SIGNED_DWORD_VALUE = Math.pow(2, 31) - 1;
export const MIN_UNSIGNED_DWORD_VALUE = 0;
export const MAX_UNSIGNED_DWORD_VALUE = Math.pow(2, 32) - 1;
export const MIN_DWORD_VALUE = MIN_SIGNED_DWORD_VALUE;
export const MAX_DWORD_VALUE = MAX_UNSIGNED_DWORD_VALUE;

export enum ParamAccess {
    Read,
    Write,
    ReadWrite,
}

export class Param {
    type: AnyType;
    /**
     * Documentation string.
     */
    doc?: string;
    /**
     * The way referenced registers are accessed by the instruction. Only set when type is a register reference.
     */
    access?: ParamAccess;

    constructor(type: AnyType, doc?: string, access?: ParamAccess) {
        this.type = type;
        this.doc = doc;
        this.access = access;
    }
}

export enum StackInteraction {
    Push,
    Pop,
}

export const OPCODES: Opcode[] = [];
export const OPCODES_BY_MNEMONIC = new Map<string, Opcode>();

/**
 * Opcode for script object code. Invoked by {@link ../bin/Instruction}s.
 */
export class Opcode {
    /**
     * 1- Or 2-byte big-endian representation of this opcode as used in object code.
     */
    readonly code: number;
    /**
     * String representation of this opcde as used in assembly.
     */
    readonly mnemonic: string;
    /**
     * Documentation string.
     */
    readonly doc?: string;
    /**
     * Byte size of the opcode, either 1 or 2.
     */
    readonly size: number;
    /**
     * Parameters passed in directly or via the stack, depending on the value of `stack`.
     */
    readonly params: Param[];
    /**
     * Stack interaction.
     */
    readonly stack?: StackInteraction;

    constructor(
        code: number,
        mnemonic: string,
        doc: string | undefined,
        params: Param[],
        stack: StackInteraction | undefined
    ) {
        this.code = code;
        this.mnemonic = mnemonic;
        this.doc = doc;
        this.size = this.code < 256 ? 1 : 2;
        this.params = params;
        this.stack = stack;
    }

    // !!! GENERATED_CODE_START !!!
    static readonly NOP = (OPCODES[0x00] = new Opcode(
        0x00,
        "nop",
        "No operation, does nothing.",
        [],
        undefined
    ));
    static readonly RET = (OPCODES[0x01] = new Opcode(
        0x01,
        "ret",
        "Returns control to caller.",
        [],
        undefined
    ));
    static readonly SYNC = (OPCODES[0x02] = new Opcode(
        0x02,
        "sync",
        "Yields control for the rest of the current frame. Execution will continue the following frame.",
        [],
        undefined
    ));
    static readonly EXIT = (OPCODES[0x03] = new Opcode(
        0x03,
        "exit",
        undefined,
        [new Param(TYPE_DWORD, undefined, undefined)],
        StackInteraction.Pop
    ));
    static readonly THREAD = (OPCODES[0x04] = new Opcode(
        0x04,
        "thread",
        "Starts a new thread. Thread execution will start at the given label.\nOften used to check a register every frame. Make sure to yield control with sync when looping.",
        [new Param(TYPE_I_LABEL, undefined, undefined)],
        undefined
    ));
    static readonly VA_START = (OPCODES[0x05] = new Opcode(
        0x05,
        "va_start",
        "Initializes a variable argument list.\nMake sure to call va_end after va_start and va_call.",
        [],
        undefined
    ));
    static readonly VA_END = (OPCODES[0x06] = new Opcode(
        0x06,
        "va_end",
        "Restores the registers overwritten by arg_push* instructions.\nCalled after va_call.",
        [],
        undefined
    ));
    static readonly VA_CALL = (OPCODES[0x07] = new Opcode(
        0x07,
        "va_call",
        "Calls the variable argument function at the given label.\nCalled after initializing the argument list with va_start and pushing arguments onto the stack with arg_push* instructions. Make sure to call va_end afterwards.",
        [new Param(TYPE_I_LABEL, undefined, undefined)],
        undefined
    ));
    static readonly LET = (OPCODES[0x08] = new Opcode(
        0x08,
        "let",
        "Sets the first register's value to second one's value.",
        [
            new Param(
                new RegTupRefType(new Param(TYPE_DWORD, undefined, ParamAccess.Write)),
                undefined,
                undefined
            ),
            new Param(
                new RegTupRefType(new Param(TYPE_DWORD, undefined, ParamAccess.Read)),
                undefined,
                undefined
            ),
        ],
        undefined
    ));
    static readonly LETI = (OPCODES[0x09] = new Opcode(
        0x09,
        "leti",
        "Sets a register to the given value.",
        [
            new Param(
                new RegTupRefType(new Param(TYPE_DWORD, undefined, ParamAccess.Write)),
                undefined,
                undefined
            ),
            new Param(TYPE_DWORD, undefined, undefined),
        ],
        undefined
    ));
    static readonly LETB = (OPCODES[0x0a] = new Opcode(
        0x0a,
        "letb",
        "Sets a register to the given value.",
        [
            new Param(
                new RegTupRefType(new Param(TYPE_BYTE, undefined, ParamAccess.Write)),
                undefined,
                undefined
            ),
            new Param(TYPE_BYTE, undefined, undefined),
        ],
        undefined
    ));
    static readonly LETW = (OPCODES[0x0b] = new Opcode(
        0x0b,
        "letw",
        "Sets a register to the given value.",
        [
            new Param(
                new RegTupRefType(new Param(TYPE_WORD, undefined, ParamAccess.Write)),
                undefined,
                undefined
            ),
            new Param(TYPE_WORD, undefined, undefined),
        ],
        undefined
    ));
    static readonly LETA = (OPCODES[0x0c] = new Opcode(
        0x0c,
        "leta",
        "Sets the first register to the memory address of the second register. Not used by Sega.",
        [
            new Param(
                new RegTupRefType(new Param(TYPE_POINTER, undefined, ParamAccess.Write)),
                undefined,
                undefined
            ),
            new Param(
                new RegTupRefType(new Param(TYPE_ANY, undefined, ParamAccess.Read)),
                undefined,
                undefined
            ),
        ],
        undefined
    ));
    static readonly LETO = (OPCODES[0x0d] = new Opcode(
        0x0d,
        "leto",
        "Sets a register to the memory address of the given label. Not used by Sega.",
        [
            new Param(
                new RegTupRefType(new Param(TYPE_POINTER, undefined, ParamAccess.Write)),
                undefined,
                undefined
            ),
            new Param(TYPE_LABEL, undefined, undefined),
        ],
        undefined
    ));
    static readonly UNKNOWN_0E = (OPCODES[0x0e] = new Opcode(
        0x0e,
        "unknown_0e",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_0F = (OPCODES[0x0f] = new Opcode(
        0x0f,
        "unknown_0f",
        undefined,
        [],
        undefined
    ));
    static readonly SET = (OPCODES[0x10] = new Opcode(
        0x10,
        "set",
        "Sets a register to 1.",
        [
            new Param(
                new RegTupRefType(new Param(TYPE_DWORD, undefined, ParamAccess.Write)),
                undefined,
                undefined
            ),
        ],
        undefined
    ));
    static readonly CLEAR = (OPCODES[0x11] = new Opcode(
        0x11,
        "clear",
        "Sets a register to 0.",
        [
            new Param(
                new RegTupRefType(new Param(TYPE_DWORD, undefined, ParamAccess.Write)),
                undefined,
                undefined
            ),
        ],
        undefined
    ));
    static readonly REV = (OPCODES[0x12] = new Opcode(
        0x12,
        "rev",
        "Sets a register to 1 if its current value is 0, otherwise sets it to 0.",
        [
            new Param(
                new RegTupRefType(new Param(TYPE_DWORD, undefined, ParamAccess.ReadWrite)),
                undefined,
                undefined
            ),
        ],
        undefined
    ));
    static readonly GSET = (OPCODES[0x13] = new Opcode(
        0x13,
        "gset",
        undefined,
        [new Param(TYPE_WORD, undefined, undefined)],
        undefined
    ));
    static readonly GCLEAR = (OPCODES[0x14] = new Opcode(
        0x14,
        "gclear",
        undefined,
        [new Param(TYPE_WORD, undefined, undefined)],
        undefined
    ));
    static readonly GREV = (OPCODES[0x15] = new Opcode(
        0x15,
        "grev",
        undefined,
        [new Param(TYPE_WORD, undefined, undefined)],
        undefined
    ));
    static readonly GLET = (OPCODES[0x16] = new Opcode(
        0x16,
        "glet",
        undefined,
        [new Param(TYPE_WORD, undefined, undefined)],
        undefined
    ));
    static readonly GGET = (OPCODES[0x17] = new Opcode(
        0x17,
        "gget",
        "Sets a register to value of the given flag.",
        [
            new Param(TYPE_WORD, undefined, undefined),
            new Param(
                new RegTupRefType(new Param(TYPE_WORD, undefined, ParamAccess.Write)),
                undefined,
                undefined
            ),
        ],
        undefined
    ));
    static readonly ADD = (OPCODES[0x18] = new Opcode(
        0x18,
        "add",
        undefined,
        [
            new Param(
                new RegTupRefType(new Param(TYPE_DWORD, undefined, ParamAccess.Write)),
                undefined,
                undefined
            ),
            new Param(
                new RegTupRefType(new Param(TYPE_DWORD, undefined, ParamAccess.Read)),
                undefined,
                undefined
            ),
        ],
        undefined
    ));
    static readonly ADDI = (OPCODES[0x19] = new Opcode(
        0x19,
        "addi",
        undefined,
        [
            new Param(
                new RegTupRefType(new Param(TYPE_DWORD, undefined, ParamAccess.Write)),
                undefined,
                undefined
            ),
            new Param(TYPE_DWORD, undefined, undefined),
        ],
        undefined
    ));
    static readonly SUB = (OPCODES[0x1a] = new Opcode(
        0x1a,
        "sub",
        undefined,
        [
            new Param(
                new RegTupRefType(new Param(TYPE_DWORD, undefined, ParamAccess.Write)),
                undefined,
                undefined
            ),
            new Param(
                new RegTupRefType(new Param(TYPE_DWORD, undefined, ParamAccess.Read)),
                undefined,
                undefined
            ),
        ],
        undefined
    ));
    static readonly SUBI = (OPCODES[0x1b] = new Opcode(
        0x1b,
        "subi",
        undefined,
        [
            new Param(
                new RegTupRefType(new Param(TYPE_DWORD, undefined, ParamAccess.Write)),
                undefined,
                undefined
            ),
            new Param(TYPE_DWORD, undefined, undefined),
        ],
        undefined
    ));
    static readonly MUL = (OPCODES[0x1c] = new Opcode(
        0x1c,
        "mul",
        undefined,
        [
            new Param(
                new RegTupRefType(new Param(TYPE_DWORD, undefined, ParamAccess.Write)),
                undefined,
                undefined
            ),
            new Param(
                new RegTupRefType(new Param(TYPE_DWORD, undefined, ParamAccess.Read)),
                undefined,
                undefined
            ),
        ],
        undefined
    ));
    static readonly MULI = (OPCODES[0x1d] = new Opcode(
        0x1d,
        "muli",
        undefined,
        [
            new Param(
                new RegTupRefType(new Param(TYPE_DWORD, undefined, ParamAccess.Write)),
                undefined,
                undefined
            ),
            new Param(TYPE_DWORD, undefined, undefined),
        ],
        undefined
    ));
    static readonly DIV = (OPCODES[0x1e] = new Opcode(
        0x1e,
        "div",
        undefined,
        [
            new Param(
                new RegTupRefType(new Param(TYPE_DWORD, undefined, ParamAccess.Write)),
                undefined,
                undefined
            ),
            new Param(
                new RegTupRefType(new Param(TYPE_DWORD, undefined, ParamAccess.Read)),
                undefined,
                undefined
            ),
        ],
        undefined
    ));
    static readonly DIVI = (OPCODES[0x1f] = new Opcode(
        0x1f,
        "divi",
        undefined,
        [
            new Param(
                new RegTupRefType(new Param(TYPE_DWORD, undefined, ParamAccess.Write)),
                undefined,
                undefined
            ),
            new Param(TYPE_DWORD, undefined, undefined),
        ],
        undefined
    ));
    static readonly AND = (OPCODES[0x20] = new Opcode(
        0x20,
        "and",
        undefined,
        [
            new Param(
                new RegTupRefType(new Param(TYPE_DWORD, undefined, ParamAccess.Write)),
                undefined,
                undefined
            ),
            new Param(
                new RegTupRefType(new Param(TYPE_DWORD, undefined, ParamAccess.Read)),
                undefined,
                undefined
            ),
        ],
        undefined
    ));
    static readonly ANDI = (OPCODES[0x21] = new Opcode(
        0x21,
        "andi",
        undefined,
        [
            new Param(
                new RegTupRefType(new Param(TYPE_DWORD, undefined, ParamAccess.Write)),
                undefined,
                undefined
            ),
            new Param(TYPE_DWORD, undefined, undefined),
        ],
        undefined
    ));
    static readonly OR = (OPCODES[0x22] = new Opcode(
        0x22,
        "or",
        undefined,
        [
            new Param(
                new RegTupRefType(new Param(TYPE_DWORD, undefined, ParamAccess.Write)),
                undefined,
                undefined
            ),
            new Param(
                new RegTupRefType(new Param(TYPE_DWORD, undefined, ParamAccess.Read)),
                undefined,
                undefined
            ),
        ],
        undefined
    ));
    static readonly ORI = (OPCODES[0x23] = new Opcode(
        0x23,
        "ori",
        undefined,
        [
            new Param(
                new RegTupRefType(new Param(TYPE_DWORD, undefined, ParamAccess.Write)),
                undefined,
                undefined
            ),
            new Param(TYPE_DWORD, undefined, undefined),
        ],
        undefined
    ));
    static readonly XOR = (OPCODES[0x24] = new Opcode(
        0x24,
        "xor",
        undefined,
        [
            new Param(
                new RegTupRefType(new Param(TYPE_DWORD, undefined, ParamAccess.Write)),
                undefined,
                undefined
            ),
            new Param(
                new RegTupRefType(new Param(TYPE_DWORD, undefined, ParamAccess.Read)),
                undefined,
                undefined
            ),
        ],
        undefined
    ));
    static readonly XORI = (OPCODES[0x25] = new Opcode(
        0x25,
        "xori",
        undefined,
        [
            new Param(
                new RegTupRefType(new Param(TYPE_DWORD, undefined, ParamAccess.Write)),
                undefined,
                undefined
            ),
            new Param(TYPE_DWORD, undefined, undefined),
        ],
        undefined
    ));
    static readonly MOD = (OPCODES[0x26] = new Opcode(
        0x26,
        "mod",
        undefined,
        [
            new Param(
                new RegTupRefType(new Param(TYPE_DWORD, undefined, ParamAccess.Write)),
                undefined,
                undefined
            ),
            new Param(
                new RegTupRefType(new Param(TYPE_DWORD, undefined, ParamAccess.Read)),
                undefined,
                undefined
            ),
        ],
        undefined
    ));
    static readonly MODI = (OPCODES[0x27] = new Opcode(
        0x27,
        "modi",
        undefined,
        [
            new Param(
                new RegTupRefType(new Param(TYPE_DWORD, undefined, ParamAccess.Write)),
                undefined,
                undefined
            ),
            new Param(TYPE_DWORD, undefined, undefined),
        ],
        undefined
    ));
    static readonly JMP = (OPCODES[0x28] = new Opcode(
        0x28,
        "jmp",
        undefined,
        [new Param(TYPE_I_LABEL, undefined, undefined)],
        undefined
    ));
    static readonly CALL = (OPCODES[0x29] = new Opcode(
        0x29,
        "call",
        undefined,
        [new Param(TYPE_I_LABEL, undefined, undefined)],
        undefined
    ));
    static readonly JMP_ON = (OPCODES[0x2a] = new Opcode(
        0x2a,
        "jmp_on",
        undefined,
        [
            new Param(TYPE_I_LABEL, undefined, undefined),
            new Param(TYPE_REG_REF_VAR, undefined, ParamAccess.Read),
        ],
        undefined
    ));
    static readonly JMP_OFF = (OPCODES[0x2b] = new Opcode(
        0x2b,
        "jmp_off",
        undefined,
        [
            new Param(TYPE_I_LABEL, undefined, undefined),
            new Param(TYPE_REG_REF_VAR, undefined, ParamAccess.Read),
        ],
        undefined
    ));
    static readonly JMP_E = (OPCODES[0x2c] = new Opcode(
        0x2c,
        "jmp_=",
        undefined,
        [
            new Param(
                new RegTupRefType(new Param(TYPE_ANY, undefined, ParamAccess.Read)),
                undefined,
                undefined
            ),
            new Param(
                new RegTupRefType(new Param(TYPE_ANY, undefined, ParamAccess.Read)),
                undefined,
                undefined
            ),
            new Param(TYPE_I_LABEL, undefined, undefined),
        ],
        undefined
    ));
    static readonly JMPI_E = (OPCODES[0x2d] = new Opcode(
        0x2d,
        "jmpi_=",
        undefined,
        [
            new Param(
                new RegTupRefType(new Param(TYPE_DWORD, undefined, ParamAccess.Read)),
                undefined,
                undefined
            ),
            new Param(TYPE_DWORD, undefined, undefined),
            new Param(TYPE_I_LABEL, undefined, undefined),
        ],
        undefined
    ));
    static readonly JMP_NE = (OPCODES[0x2e] = new Opcode(
        0x2e,
        "jmp_!=",
        undefined,
        [
            new Param(
                new RegTupRefType(new Param(TYPE_ANY, undefined, ParamAccess.Read)),
                undefined,
                undefined
            ),
            new Param(
                new RegTupRefType(new Param(TYPE_ANY, undefined, ParamAccess.Read)),
                undefined,
                undefined
            ),
            new Param(TYPE_I_LABEL, undefined, undefined),
        ],
        undefined
    ));
    static readonly JMPI_NE = (OPCODES[0x2f] = new Opcode(
        0x2f,
        "jmpi_!=",
        undefined,
        [
            new Param(
                new RegTupRefType(new Param(TYPE_DWORD, undefined, ParamAccess.Read)),
                undefined,
                undefined
            ),
            new Param(TYPE_DWORD, undefined, undefined),
            new Param(TYPE_I_LABEL, undefined, undefined),
        ],
        undefined
    ));
    static readonly UJMP_G = (OPCODES[0x30] = new Opcode(
        0x30,
        "ujmp_>",
        undefined,
        [
            new Param(
                new RegTupRefType(new Param(TYPE_DWORD, undefined, ParamAccess.Read)),
                undefined,
                undefined
            ),
            new Param(
                new RegTupRefType(new Param(TYPE_DWORD, undefined, ParamAccess.Read)),
                undefined,
                undefined
            ),
            new Param(TYPE_I_LABEL, undefined, undefined),
        ],
        undefined
    ));
    static readonly UJMPI_G = (OPCODES[0x31] = new Opcode(
        0x31,
        "ujmpi_>",
        undefined,
        [
            new Param(
                new RegTupRefType(new Param(TYPE_DWORD, undefined, ParamAccess.Read)),
                undefined,
                undefined
            ),
            new Param(TYPE_DWORD, undefined, undefined),
            new Param(TYPE_I_LABEL, undefined, undefined),
        ],
        undefined
    ));
    static readonly JMP_G = (OPCODES[0x32] = new Opcode(
        0x32,
        "jmp_>",
        undefined,
        [
            new Param(
                new RegTupRefType(new Param(TYPE_DWORD, undefined, ParamAccess.Read)),
                undefined,
                undefined
            ),
            new Param(
                new RegTupRefType(new Param(TYPE_DWORD, undefined, ParamAccess.Read)),
                undefined,
                undefined
            ),
            new Param(TYPE_I_LABEL, undefined, undefined),
        ],
        undefined
    ));
    static readonly JMPI_G = (OPCODES[0x33] = new Opcode(
        0x33,
        "jmpi_>",
        undefined,
        [
            new Param(
                new RegTupRefType(new Param(TYPE_DWORD, undefined, ParamAccess.Read)),
                undefined,
                undefined
            ),
            new Param(TYPE_DWORD, undefined, undefined),
            new Param(TYPE_I_LABEL, undefined, undefined),
        ],
        undefined
    ));
    static readonly UJMP_L = (OPCODES[0x34] = new Opcode(
        0x34,
        "ujmp_<",
        undefined,
        [
            new Param(
                new RegTupRefType(new Param(TYPE_DWORD, undefined, ParamAccess.Read)),
                undefined,
                undefined
            ),
            new Param(
                new RegTupRefType(new Param(TYPE_DWORD, undefined, ParamAccess.Read)),
                undefined,
                undefined
            ),
            new Param(TYPE_I_LABEL, undefined, undefined),
        ],
        undefined
    ));
    static readonly UJMPI_L = (OPCODES[0x35] = new Opcode(
        0x35,
        "ujmpi_<",
        undefined,
        [
            new Param(
                new RegTupRefType(new Param(TYPE_DWORD, undefined, ParamAccess.Read)),
                undefined,
                undefined
            ),
            new Param(TYPE_DWORD, undefined, undefined),
            new Param(TYPE_I_LABEL, undefined, undefined),
        ],
        undefined
    ));
    static readonly JMP_L = (OPCODES[0x36] = new Opcode(
        0x36,
        "jmp_<",
        undefined,
        [
            new Param(
                new RegTupRefType(new Param(TYPE_DWORD, undefined, ParamAccess.Read)),
                undefined,
                undefined
            ),
            new Param(
                new RegTupRefType(new Param(TYPE_DWORD, undefined, ParamAccess.Read)),
                undefined,
                undefined
            ),
            new Param(TYPE_I_LABEL, undefined, undefined),
        ],
        undefined
    ));
    static readonly JMPI_L = (OPCODES[0x37] = new Opcode(
        0x37,
        "jmpi_<",
        undefined,
        [
            new Param(
                new RegTupRefType(new Param(TYPE_DWORD, undefined, ParamAccess.Read)),
                undefined,
                undefined
            ),
            new Param(TYPE_DWORD, undefined, undefined),
            new Param(TYPE_I_LABEL, undefined, undefined),
        ],
        undefined
    ));
    static readonly UJMP_GE = (OPCODES[0x38] = new Opcode(
        0x38,
        "ujmp_>=",
        undefined,
        [
            new Param(
                new RegTupRefType(new Param(TYPE_DWORD, undefined, ParamAccess.Read)),
                undefined,
                undefined
            ),
            new Param(
                new RegTupRefType(new Param(TYPE_DWORD, undefined, ParamAccess.Read)),
                undefined,
                undefined
            ),
            new Param(TYPE_I_LABEL, undefined, undefined),
        ],
        undefined
    ));
    static readonly UJMPI_GE = (OPCODES[0x39] = new Opcode(
        0x39,
        "ujmpi_>=",
        undefined,
        [
            new Param(
                new RegTupRefType(new Param(TYPE_DWORD, undefined, ParamAccess.Read)),
                undefined,
                undefined
            ),
            new Param(TYPE_DWORD, undefined, undefined),
            new Param(TYPE_I_LABEL, undefined, undefined),
        ],
        undefined
    ));
    static readonly JMP_GE = (OPCODES[0x3a] = new Opcode(
        0x3a,
        "jmp_>=",
        undefined,
        [
            new Param(
                new RegTupRefType(new Param(TYPE_DWORD, undefined, ParamAccess.Read)),
                undefined,
                undefined
            ),
            new Param(
                new RegTupRefType(new Param(TYPE_DWORD, undefined, ParamAccess.Read)),
                undefined,
                undefined
            ),
            new Param(TYPE_I_LABEL, undefined, undefined),
        ],
        undefined
    ));
    static readonly JMPI_GE = (OPCODES[0x3b] = new Opcode(
        0x3b,
        "jmpi_>=",
        undefined,
        [
            new Param(
                new RegTupRefType(new Param(TYPE_DWORD, undefined, ParamAccess.Read)),
                undefined,
                undefined
            ),
            new Param(TYPE_DWORD, undefined, undefined),
            new Param(TYPE_I_LABEL, undefined, undefined),
        ],
        undefined
    ));
    static readonly UJMP_LE = (OPCODES[0x3c] = new Opcode(
        0x3c,
        "ujmp_<=",
        undefined,
        [
            new Param(
                new RegTupRefType(new Param(TYPE_DWORD, undefined, ParamAccess.Read)),
                undefined,
                undefined
            ),
            new Param(
                new RegTupRefType(new Param(TYPE_DWORD, undefined, ParamAccess.Read)),
                undefined,
                undefined
            ),
            new Param(TYPE_I_LABEL, undefined, undefined),
        ],
        undefined
    ));
    static readonly UJMPI_LE = (OPCODES[0x3d] = new Opcode(
        0x3d,
        "ujmpi_<=",
        undefined,
        [
            new Param(
                new RegTupRefType(new Param(TYPE_DWORD, undefined, ParamAccess.Read)),
                undefined,
                undefined
            ),
            new Param(TYPE_DWORD, undefined, undefined),
            new Param(TYPE_I_LABEL, undefined, undefined),
        ],
        undefined
    ));
    static readonly JMP_LE = (OPCODES[0x3e] = new Opcode(
        0x3e,
        "jmp_<=",
        undefined,
        [
            new Param(
                new RegTupRefType(new Param(TYPE_DWORD, undefined, ParamAccess.Read)),
                undefined,
                undefined
            ),
            new Param(
                new RegTupRefType(new Param(TYPE_DWORD, undefined, ParamAccess.Read)),
                undefined,
                undefined
            ),
            new Param(TYPE_I_LABEL, undefined, undefined),
        ],
        undefined
    ));
    static readonly JMPI_LE = (OPCODES[0x3f] = new Opcode(
        0x3f,
        "jmpi_<=",
        undefined,
        [
            new Param(
                new RegTupRefType(new Param(TYPE_DWORD, undefined, ParamAccess.Read)),
                undefined,
                undefined
            ),
            new Param(TYPE_DWORD, undefined, undefined),
            new Param(TYPE_I_LABEL, undefined, undefined),
        ],
        undefined
    ));
    static readonly SWITCH_JMP = (OPCODES[0x40] = new Opcode(
        0x40,
        "switch_jmp",
        undefined,
        [
            new Param(
                new RegTupRefType(new Param(TYPE_DWORD, undefined, ParamAccess.Read)),
                undefined,
                undefined
            ),
            new Param(TYPE_I_LABEL, undefined, undefined),
        ],
        undefined
    ));
    static readonly SWITCH_CALL = (OPCODES[0x41] = new Opcode(
        0x41,
        "switch_call",
        undefined,
        [
            new Param(
                new RegTupRefType(new Param(TYPE_DWORD, undefined, ParamAccess.Read)),
                undefined,
                undefined
            ),
            new Param(TYPE_I_LABEL, undefined, undefined),
        ],
        undefined
    ));
    static readonly STACK_PUSH = (OPCODES[0x42] = new Opcode(
        0x42,
        "stack_push",
        undefined,
        [
            new Param(
                new RegTupRefType(new Param(TYPE_ANY, undefined, ParamAccess.Read)),
                undefined,
                undefined
            ),
        ],
        undefined
    ));
    static readonly STACK_POP = (OPCODES[0x43] = new Opcode(
        0x43,
        "stack_pop",
        undefined,
        [
            new Param(
                new RegTupRefType(new Param(TYPE_ANY, undefined, ParamAccess.Write)),
                undefined,
                undefined
            ),
        ],
        undefined
    ));
    static readonly STACK_PUSHM = (OPCODES[0x44] = new Opcode(
        0x44,
        "stack_pushm",
        "Pushes the values of an arbitrary amount of registers onto the stack.",
        [
            new Param(TYPE_REG_REF, undefined, ParamAccess.Read),
            new Param(TYPE_DWORD, undefined, undefined),
        ],
        undefined
    ));
    static readonly STACK_POPM = (OPCODES[0x45] = new Opcode(
        0x45,
        "stack_popm",
        "Pops an arbitrary amount of values from the stack and writes them to registers.",
        [
            new Param(TYPE_REG_REF, undefined, ParamAccess.Write),
            new Param(TYPE_DWORD, undefined, undefined),
        ],
        undefined
    ));
    static readonly UNKNOWN_46 = (OPCODES[0x46] = new Opcode(
        0x46,
        "unknown_46",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_47 = (OPCODES[0x47] = new Opcode(
        0x47,
        "unknown_47",
        undefined,
        [],
        undefined
    ));
    static readonly ARG_PUSHR = (OPCODES[0x48] = new Opcode(
        0x48,
        "arg_pushr",
        "Pushes the value of the given register onto the stack.",
        [
            new Param(
                new RegTupRefType(new Param(TYPE_ANY, undefined, ParamAccess.Read)),
                undefined,
                undefined
            ),
        ],
        StackInteraction.Push
    ));
    static readonly ARG_PUSHL = (OPCODES[0x49] = new Opcode(
        0x49,
        "arg_pushl",
        "Pushes the given value onto the stack.",
        [new Param(TYPE_DWORD, undefined, undefined)],
        StackInteraction.Push
    ));
    static readonly ARG_PUSHB = (OPCODES[0x4a] = new Opcode(
        0x4a,
        "arg_pushb",
        "Pushes the given value onto the stack.",
        [new Param(TYPE_BYTE, undefined, undefined)],
        StackInteraction.Push
    ));
    static readonly ARG_PUSHW = (OPCODES[0x4b] = new Opcode(
        0x4b,
        "arg_pushw",
        "Pushes the given value onto the stack.",
        [new Param(TYPE_WORD, undefined, undefined)],
        StackInteraction.Push
    ));
    static readonly UNKNOWN_4C = (OPCODES[0x4c] = new Opcode(
        0x4c,
        "unknown_4c",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_4D = (OPCODES[0x4d] = new Opcode(
        0x4d,
        "unknown_4d",
        undefined,
        [],
        undefined
    ));
    static readonly ARG_PUSHS = (OPCODES[0x4e] = new Opcode(
        0x4e,
        "arg_pushs",
        "Pushes the given value onto the stack.",
        [new Param(TYPE_STRING, undefined, undefined)],
        StackInteraction.Push
    ));
    static readonly UNKNOWN_4F = (OPCODES[0x4f] = new Opcode(
        0x4f,
        "unknown_4f",
        undefined,
        [
            new Param(
                new RegTupRefType(new Param(TYPE_ANY, undefined, ParamAccess.Read)),
                undefined,
                undefined
            ),
            new Param(
                new RegTupRefType(new Param(TYPE_ANY, undefined, ParamAccess.Read)),
                undefined,
                undefined
            ),
        ],
        undefined
    ));
    static readonly MESSAGE = (OPCODES[0x50] = new Opcode(
        0x50,
        "message",
        undefined,
        [new Param(TYPE_DWORD, undefined, undefined), new Param(TYPE_STRING, undefined, undefined)],
        StackInteraction.Pop
    ));
    static readonly LIST = (OPCODES[0x51] = new Opcode(
        0x51,
        "list",
        "Used to display a list of items and retrieve the item selected by the player.\nList items should be seperated by newlines. The selected item's index will be written to the given register.",
        [
            new Param(
                new RegTupRefType(new Param(TYPE_BYTE, undefined, ParamAccess.Write)),
                undefined,
                undefined
            ),
            new Param(TYPE_STRING, undefined, undefined),
        ],
        StackInteraction.Pop
    ));
    static readonly FADEIN = (OPCODES[0x52] = new Opcode(0x52, "fadein", undefined, [], undefined));
    static readonly FADEOUT = (OPCODES[0x53] = new Opcode(
        0x53,
        "fadeout",
        undefined,
        [],
        undefined
    ));
    static readonly SE = (OPCODES[0x54] = new Opcode(
        0x54,
        "se",
        undefined,
        [new Param(TYPE_DWORD, undefined, undefined)],
        StackInteraction.Pop
    ));
    static readonly BGM = (OPCODES[0x55] = new Opcode(
        0x55,
        "bgm",
        undefined,
        [new Param(TYPE_DWORD, undefined, undefined)],
        StackInteraction.Pop
    ));
    static readonly UNKNOWN_56 = (OPCODES[0x56] = new Opcode(
        0x56,
        "unknown_56",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_57 = (OPCODES[0x57] = new Opcode(
        0x57,
        "unknown_57",
        undefined,
        [],
        undefined
    ));
    static readonly ENABLE = (OPCODES[0x58] = new Opcode(
        0x58,
        "enable",
        undefined,
        [new Param(TYPE_DWORD, undefined, undefined)],
        StackInteraction.Pop
    ));
    static readonly DISABLE = (OPCODES[0x59] = new Opcode(
        0x59,
        "disable",
        undefined,
        [new Param(TYPE_DWORD, undefined, undefined)],
        StackInteraction.Pop
    ));
    static readonly WINDOW_MSG = (OPCODES[0x5a] = new Opcode(
        0x5a,
        "window_msg",
        undefined,
        [new Param(TYPE_STRING, undefined, undefined)],
        StackInteraction.Pop
    ));
    static readonly ADD_MSG = (OPCODES[0x5b] = new Opcode(
        0x5b,
        "add_msg",
        undefined,
        [new Param(TYPE_STRING, undefined, undefined)],
        StackInteraction.Pop
    ));
    static readonly MESEND = (OPCODES[0x5c] = new Opcode(0x5c, "mesend", undefined, [], undefined));
    static readonly GETTIME = (OPCODES[0x5d] = new Opcode(
        0x5d,
        "gettime",
        undefined,
        [
            new Param(
                new RegTupRefType(new Param(TYPE_DWORD, undefined, ParamAccess.Write)),
                undefined,
                undefined
            ),
        ],
        undefined
    ));
    static readonly WINEND = (OPCODES[0x5e] = new Opcode(0x5e, "winend", undefined, [], undefined));
    static readonly UNKNOWN_5F = (OPCODES[0x5f] = new Opcode(
        0x5f,
        "unknown_5f",
        undefined,
        [],
        undefined
    ));
    static readonly NPC_CRT_V3 = (OPCODES[0x60] = new Opcode(
        0x60,
        "npc_crt_v3",
        undefined,
        [
            new Param(
                new RegTupRefType(new Param(TYPE_ANY, undefined, ParamAccess.Read)),
                undefined,
                undefined
            ),
        ],
        undefined
    ));
    static readonly NPC_STOP = (OPCODES[0x61] = new Opcode(
        0x61,
        "npc_stop",
        undefined,
        [new Param(TYPE_DWORD, undefined, undefined)],
        StackInteraction.Pop
    ));
    static readonly NPC_PLAY = (OPCODES[0x62] = new Opcode(
        0x62,
        "npc_play",
        undefined,
        [new Param(TYPE_DWORD, undefined, undefined)],
        StackInteraction.Pop
    ));
    static readonly NPC_KILL = (OPCODES[0x63] = new Opcode(
        0x63,
        "npc_kill",
        undefined,
        [new Param(TYPE_DWORD, undefined, undefined)],
        StackInteraction.Pop
    ));
    static readonly NPC_NONT = (OPCODES[0x64] = new Opcode(
        0x64,
        "npc_nont",
        undefined,
        [],
        undefined
    ));
    static readonly NPC_TALK = (OPCODES[0x65] = new Opcode(
        0x65,
        "npc_talk",
        undefined,
        [],
        undefined
    ));
    static readonly NPC_CRP_V3 = (OPCODES[0x66] = new Opcode(
        0x66,
        "npc_crp_v3",
        undefined,
        [
            new Param(
                new RegTupRefType(new Param(TYPE_ANY, undefined, ParamAccess.Read)),
                undefined,
                undefined
            ),
        ],
        undefined
    ));
    static readonly UNKNOWN_67 = (OPCODES[0x67] = new Opcode(
        0x67,
        "unknown_67",
        undefined,
        [],
        undefined
    ));
    static readonly CREATE_PIPE = (OPCODES[0x68] = new Opcode(
        0x68,
        "create_pipe",
        undefined,
        [new Param(TYPE_DWORD, undefined, undefined)],
        StackInteraction.Pop
    ));
    static readonly P_HPSTAT_V3 = (OPCODES[0x69] = new Opcode(
        0x69,
        "p_hpstat_v3",
        undefined,
        [
            new Param(
                new RegTupRefType(new Param(TYPE_ANY, undefined, ParamAccess.Read)),
                undefined,
                undefined
            ),
            new Param(TYPE_DWORD, undefined, undefined),
        ],
        StackInteraction.Pop
    ));
    static readonly P_DEAD_V3 = (OPCODES[0x6a] = new Opcode(
        0x6a,
        "p_dead_v3",
        undefined,
        [
            new Param(
                new RegTupRefType(new Param(TYPE_BYTE, undefined, ParamAccess.Write)),
                undefined,
                undefined
            ),
            new Param(TYPE_DWORD, undefined, undefined),
        ],
        undefined
    ));
    static readonly P_DISABLEWARP = (OPCODES[0x6b] = new Opcode(
        0x6b,
        "p_disablewarp",
        undefined,
        [],
        undefined
    ));
    static readonly P_ENABLEWARP = (OPCODES[0x6c] = new Opcode(
        0x6c,
        "p_enablewarp",
        undefined,
        [],
        undefined
    ));
    static readonly P_MOVE_V3 = (OPCODES[0x6d] = new Opcode(
        0x6d,
        "p_move_v3",
        undefined,
        [
            new Param(
                new RegTupRefType(new Param(TYPE_ANY, undefined, ParamAccess.Read)),
                undefined,
                undefined
            ),
        ],
        undefined
    ));
    static readonly P_LOOK = (OPCODES[0x6e] = new Opcode(
        0x6e,
        "p_look",
        undefined,
        [new Param(TYPE_DWORD, undefined, undefined)],
        StackInteraction.Pop
    ));
    static readonly UNKNOWN_6F = (OPCODES[0x6f] = new Opcode(
        0x6f,
        "unknown_6f",
        undefined,
        [],
        undefined
    ));
    static readonly P_ACTION_DISABLE = (OPCODES[0x70] = new Opcode(
        0x70,
        "p_action_disable",
        undefined,
        [],
        undefined
    ));
    static readonly P_ACTION_ENABLE = (OPCODES[0x71] = new Opcode(
        0x71,
        "p_action_enable",
        undefined,
        [],
        undefined
    ));
    static readonly DISABLE_MOVEMENT1 = (OPCODES[0x72] = new Opcode(
        0x72,
        "disable_movement1",
        undefined,
        [new Param(TYPE_DWORD, undefined, undefined)],
        StackInteraction.Pop
    ));
    static readonly ENABLE_MOVEMENT1 = (OPCODES[0x73] = new Opcode(
        0x73,
        "enable_movement1",
        undefined,
        [new Param(TYPE_DWORD, undefined, undefined)],
        StackInteraction.Pop
    ));
    static readonly P_NONCOL = (OPCODES[0x74] = new Opcode(
        0x74,
        "p_noncol",
        undefined,
        [],
        undefined
    ));
    static readonly P_COL = (OPCODES[0x75] = new Opcode(0x75, "p_col", undefined, [], undefined));
    static readonly P_SETPOS = (OPCODES[0x76] = new Opcode(
        0x76,
        "p_setpos",
        "Sets a player's position.",
        [
            new Param(TYPE_DWORD, "Player slot.", undefined),
            new Param(
                new RegTupRefType(
                    new Param(TYPE_DWORD, "X coordinate.", ParamAccess.Read),
                    new Param(TYPE_DWORD, "Y coordinate.", ParamAccess.Read),
                    new Param(TYPE_DWORD, "Z coordinate.", ParamAccess.Read),
                    new Param(TYPE_DWORD, "Y-axis rotation.", ParamAccess.Read)
                ),
                undefined,
                undefined
            ),
        ],
        StackInteraction.Pop
    ));
    static readonly P_RETURN_GUILD = (OPCODES[0x77] = new Opcode(
        0x77,
        "p_return_guild",
        undefined,
        [],
        undefined
    ));
    static readonly P_TALK_GUILD = (OPCODES[0x78] = new Opcode(
        0x78,
        "p_talk_guild",
        undefined,
        [new Param(TYPE_DWORD, undefined, undefined)],
        StackInteraction.Pop
    ));
    static readonly NPC_TALK_PL_V3 = (OPCODES[0x79] = new Opcode(
        0x79,
        "npc_talk_pl_v3",
        undefined,
        [
            new Param(
                new RegTupRefType(new Param(TYPE_ANY, undefined, ParamAccess.Read)),
                undefined,
                undefined
            ),
        ],
        undefined
    ));
    static readonly NPC_TALK_KILL = (OPCODES[0x7a] = new Opcode(
        0x7a,
        "npc_talk_kill",
        undefined,
        [new Param(TYPE_DWORD, undefined, undefined)],
        StackInteraction.Pop
    ));
    static readonly NPC_CRTPK_V3 = (OPCODES[0x7b] = new Opcode(
        0x7b,
        "npc_crtpk_v3",
        undefined,
        [
            new Param(
                new RegTupRefType(new Param(TYPE_ANY, undefined, ParamAccess.Read)),
                undefined,
                undefined
            ),
        ],
        undefined
    ));
    static readonly NPC_CRPPK_V3 = (OPCODES[0x7c] = new Opcode(
        0x7c,
        "npc_crppk_v3",
        undefined,
        [
            new Param(
                new RegTupRefType(new Param(TYPE_ANY, undefined, ParamAccess.Read)),
                undefined,
                undefined
            ),
        ],
        undefined
    ));
    static readonly NPC_CRPTALK_V3 = (OPCODES[0x7d] = new Opcode(
        0x7d,
        "npc_crptalk_v3",
        undefined,
        [
            new Param(
                new RegTupRefType(new Param(TYPE_ANY, undefined, ParamAccess.Read)),
                undefined,
                undefined
            ),
        ],
        undefined
    ));
    static readonly P_LOOK_AT_V1 = (OPCODES[0x7e] = new Opcode(
        0x7e,
        "p_look_at_v1",
        undefined,
        [new Param(TYPE_DWORD, undefined, undefined), new Param(TYPE_DWORD, undefined, undefined)],
        StackInteraction.Pop
    ));
    static readonly NPC_CRP_ID_V3 = (OPCODES[0x7f] = new Opcode(
        0x7f,
        "npc_crp_id_v3",
        undefined,
        [
            new Param(
                new RegTupRefType(new Param(TYPE_ANY, undefined, ParamAccess.Read)),
                undefined,
                undefined
            ),
        ],
        undefined
    ));
    static readonly CAM_QUAKE = (OPCODES[0x80] = new Opcode(
        0x80,
        "cam_quake",
        undefined,
        [],
        undefined
    ));
    static readonly CAM_ADJ = (OPCODES[0x81] = new Opcode(
        0x81,
        "cam_adj",
        undefined,
        [],
        undefined
    ));
    static readonly CAM_ZMIN = (OPCODES[0x82] = new Opcode(
        0x82,
        "cam_zmin",
        undefined,
        [],
        undefined
    ));
    static readonly CAM_ZMOUT = (OPCODES[0x83] = new Opcode(
        0x83,
        "cam_zmout",
        undefined,
        [],
        undefined
    ));
    static readonly CAM_PAN_V3 = (OPCODES[0x84] = new Opcode(
        0x84,
        "cam_pan_v3",
        undefined,
        [
            new Param(
                new RegTupRefType(
                    new Param(TYPE_DWORD, undefined, ParamAccess.Read),
                    new Param(TYPE_DWORD, undefined, ParamAccess.Read),
                    new Param(TYPE_DWORD, undefined, ParamAccess.Read),
                    new Param(TYPE_DWORD, undefined, ParamAccess.Read),
                    new Param(TYPE_DWORD, undefined, ParamAccess.Read)
                ),
                undefined,
                undefined
            ),
        ],
        undefined
    ));
    static readonly GAME_LEV_SUPER = (OPCODES[0x85] = new Opcode(
        0x85,
        "game_lev_super",
        undefined,
        [],
        undefined
    ));
    static readonly GAME_LEV_RESET = (OPCODES[0x86] = new Opcode(
        0x86,
        "game_lev_reset",
        undefined,
        [],
        undefined
    ));
    static readonly POS_PIPE_V3 = (OPCODES[0x87] = new Opcode(
        0x87,
        "pos_pipe_v3",
        "Create a telepipe at a specific position for the given player slot that takes players back to Pioneer 2 or the Lab.",
        [
            new Param(
                new RegTupRefType(
                    new Param(TYPE_DWORD, "X coordinate.", ParamAccess.Read),
                    new Param(TYPE_DWORD, "Y coordinate.", ParamAccess.Read),
                    new Param(TYPE_DWORD, "Z coordinate.", ParamAccess.Read),
                    new Param(TYPE_DWORD, "Player slot.", ParamAccess.Read)
                ),
                undefined,
                undefined
            ),
        ],
        undefined
    ));
    static readonly IF_ZONE_CLEAR = (OPCODES[0x88] = new Opcode(
        0x88,
        "if_zone_clear",
        undefined,
        [
            new Param(
                new RegTupRefType(new Param(TYPE_DWORD, undefined, ParamAccess.Write)),
                undefined,
                undefined
            ),
            new Param(
                new RegTupRefType(
                    new Param(TYPE_DWORD, undefined, ParamAccess.Read),
                    new Param(TYPE_DWORD, undefined, ParamAccess.Read)
                ),
                undefined,
                undefined
            ),
        ],
        undefined
    ));
    static readonly CHK_ENE_NUM = (OPCODES[0x89] = new Opcode(
        0x89,
        "chk_ene_num",
        "Retrieves the amount of enemies killed during the quest.",
        [
            new Param(
                new RegTupRefType(new Param(TYPE_DWORD, undefined, ParamAccess.Write)),
                undefined,
                undefined
            ),
        ],
        undefined
    ));
    static readonly UNHIDE_OBJ = (OPCODES[0x8a] = new Opcode(
        0x8a,
        "unhide_obj",
        undefined,
        [
            new Param(
                new RegTupRefType(
                    new Param(TYPE_DWORD, undefined, ParamAccess.Read),
                    new Param(TYPE_DWORD, undefined, ParamAccess.Read),
                    new Param(TYPE_DWORD, undefined, ParamAccess.Read)
                ),
                undefined,
                undefined
            ),
        ],
        undefined
    ));
    static readonly UNHIDE_ENE = (OPCODES[0x8b] = new Opcode(
        0x8b,
        "unhide_ene",
        undefined,
        [
            new Param(
                new RegTupRefType(
                    new Param(TYPE_DWORD, undefined, ParamAccess.Read),
                    new Param(TYPE_DWORD, undefined, ParamAccess.Read),
                    new Param(TYPE_DWORD, undefined, ParamAccess.Read),
                    new Param(TYPE_DWORD, undefined, ParamAccess.Read)
                ),
                undefined,
                undefined
            ),
        ],
        undefined
    ));
    static readonly AT_COORDS_CALL = (OPCODES[0x8c] = new Opcode(
        0x8c,
        "at_coords_call",
        undefined,
        [
            new Param(
                new RegTupRefType(
                    new Param(TYPE_DWORD, undefined, ParamAccess.Read),
                    new Param(TYPE_DWORD, undefined, ParamAccess.Read),
                    new Param(TYPE_DWORD, undefined, ParamAccess.Read),
                    new Param(TYPE_DWORD, undefined, ParamAccess.Read),
                    new Param(TYPE_I_LABEL, undefined, ParamAccess.Read)
                ),
                undefined,
                undefined
            ),
        ],
        undefined
    ));
    static readonly AT_COORDS_TALK = (OPCODES[0x8d] = new Opcode(
        0x8d,
        "at_coords_talk",
        undefined,
        [
            new Param(
                new RegTupRefType(
                    new Param(TYPE_DWORD, undefined, ParamAccess.Read),
                    new Param(TYPE_DWORD, undefined, ParamAccess.Read),
                    new Param(TYPE_DWORD, undefined, ParamAccess.Read),
                    new Param(TYPE_DWORD, undefined, ParamAccess.Read),
                    new Param(TYPE_I_LABEL, undefined, ParamAccess.Read)
                ),
                undefined,
                undefined
            ),
        ],
        undefined
    ));
    static readonly COL_NPCIN = (OPCODES[0x8e] = new Opcode(
        0x8e,
        "col_npcin",
        undefined,
        [
            new Param(
                new RegTupRefType(
                    new Param(TYPE_DWORD, undefined, ParamAccess.Read),
                    new Param(TYPE_DWORD, undefined, ParamAccess.Read),
                    new Param(TYPE_DWORD, undefined, ParamAccess.Read),
                    new Param(TYPE_DWORD, undefined, ParamAccess.Read),
                    new Param(TYPE_I_LABEL, undefined, ParamAccess.Read)
                ),
                undefined,
                undefined
            ),
        ],
        undefined
    ));
    static readonly COL_NPCINR = (OPCODES[0x8f] = new Opcode(
        0x8f,
        "col_npcinr",
        undefined,
        [
            new Param(
                new RegTupRefType(new Param(TYPE_ANY, undefined, ParamAccess.Read)),
                undefined,
                undefined
            ),
        ],
        undefined
    ));
    static readonly SWITCH_ON = (OPCODES[0x90] = new Opcode(
        0x90,
        "switch_on",
        undefined,
        [new Param(TYPE_DWORD, undefined, undefined)],
        StackInteraction.Pop
    ));
    static readonly SWITCH_OFF = (OPCODES[0x91] = new Opcode(
        0x91,
        "switch_off",
        undefined,
        [new Param(TYPE_DWORD, undefined, undefined)],
        StackInteraction.Pop
    ));
    static readonly PLAYBGM_EPI = (OPCODES[0x92] = new Opcode(
        0x92,
        "playbgm_epi",
        undefined,
        [new Param(TYPE_DWORD, undefined, undefined)],
        StackInteraction.Pop
    ));
    static readonly SET_MAINWARP = (OPCODES[0x93] = new Opcode(
        0x93,
        "set_mainwarp",
        undefined,
        [new Param(TYPE_DWORD, undefined, undefined)],
        StackInteraction.Pop
    ));
    static readonly SET_OBJ_PARAM = (OPCODES[0x94] = new Opcode(
        0x94,
        "set_obj_param",
        "Creates a targetable object.",
        [
            new Param(
                new RegTupRefType(
                    new Param(TYPE_DWORD, "X coordinate.", ParamAccess.Read),
                    new Param(TYPE_DWORD, "Y coordinate.", ParamAccess.Read),
                    new Param(TYPE_DWORD, "Z coordinate.", ParamAccess.Read),
                    new Param(TYPE_DWORD, "Collision radius.", ParamAccess.Read),
                    new Param(TYPE_I_LABEL, "Function label.", ParamAccess.Read),
                    new Param(TYPE_DWORD, "Vertical position of the cursor.", ParamAccess.Read)
                ),
                undefined,
                undefined
            ),
            new Param(
                new RegTupRefType(new Param(TYPE_ANY, undefined, ParamAccess.Write)),
                "Object handle.",
                undefined
            ),
        ],
        undefined
    ));
    static readonly SET_FLOOR_HANDLER = (OPCODES[0x95] = new Opcode(
        0x95,
        "set_floor_handler",
        undefined,
        [
            new Param(TYPE_DWORD, "Floor number.", undefined),
            new Param(TYPE_I_LABEL, "Handler function label.", undefined),
        ],
        StackInteraction.Pop
    ));
    static readonly CLR_FLOOR_HANDLER = (OPCODES[0x96] = new Opcode(
        0x96,
        "clr_floor_handler",
        undefined,
        [new Param(TYPE_DWORD, "Floor number.", undefined)],
        StackInteraction.Pop
    ));
    static readonly COL_PLINAW = (OPCODES[0x97] = new Opcode(
        0x97,
        "col_plinaw",
        undefined,
        [
            new Param(
                new RegTupRefType(new Param(TYPE_ANY, undefined, ParamAccess.Read)),
                undefined,
                undefined
            ),
        ],
        undefined
    ));
    static readonly HUD_HIDE = (OPCODES[0x98] = new Opcode(
        0x98,
        "hud_hide",
        undefined,
        [],
        undefined
    ));
    static readonly HUD_SHOW = (OPCODES[0x99] = new Opcode(
        0x99,
        "hud_show",
        undefined,
        [],
        undefined
    ));
    static readonly CINE_ENABLE = (OPCODES[0x9a] = new Opcode(
        0x9a,
        "cine_enable",
        undefined,
        [],
        undefined
    ));
    static readonly CINE_DISABLE = (OPCODES[0x9b] = new Opcode(
        0x9b,
        "cine_disable",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_9C = (OPCODES[0x9c] = new Opcode(
        0x9c,
        "unknown_9c",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_9D = (OPCODES[0x9d] = new Opcode(
        0x9d,
        "unknown_9d",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_9E = (OPCODES[0x9e] = new Opcode(
        0x9e,
        "unknown_9e",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_9F = (OPCODES[0x9f] = new Opcode(
        0x9f,
        "unknown_9f",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_A0 = (OPCODES[0xa0] = new Opcode(
        0xa0,
        "unknown_a0",
        undefined,
        [],
        undefined
    ));
    static readonly SET_QT_FAILURE = (OPCODES[0xa1] = new Opcode(
        0xa1,
        "set_qt_failure",
        undefined,
        [new Param(TYPE_I_LABEL, undefined, undefined)],
        undefined
    ));
    static readonly SET_QT_SUCCESS = (OPCODES[0xa2] = new Opcode(
        0xa2,
        "set_qt_success",
        undefined,
        [new Param(TYPE_I_LABEL, undefined, undefined)],
        undefined
    ));
    static readonly CLR_QT_FAILURE = (OPCODES[0xa3] = new Opcode(
        0xa3,
        "clr_qt_failure",
        undefined,
        [],
        undefined
    ));
    static readonly CLR_QT_SUCCESS = (OPCODES[0xa4] = new Opcode(
        0xa4,
        "clr_qt_success",
        undefined,
        [],
        undefined
    ));
    static readonly SET_QT_CANCEL = (OPCODES[0xa5] = new Opcode(
        0xa5,
        "set_qt_cancel",
        undefined,
        [new Param(TYPE_I_LABEL, undefined, undefined)],
        undefined
    ));
    static readonly CLR_QT_CANCEL = (OPCODES[0xa6] = new Opcode(
        0xa6,
        "clr_qt_cancel",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_A7 = (OPCODES[0xa7] = new Opcode(
        0xa7,
        "unknown_a7",
        undefined,
        [],
        undefined
    ));
    static readonly PL_WALK_V3 = (OPCODES[0xa8] = new Opcode(
        0xa8,
        "pl_walk_v3",
        undefined,
        [
            new Param(
                new RegTupRefType(new Param(TYPE_ANY, undefined, ParamAccess.Read)),
                undefined,
                undefined
            ),
        ],
        undefined
    ));
    static readonly UNKNOWN_A9 = (OPCODES[0xa9] = new Opcode(
        0xa9,
        "unknown_a9",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_AA = (OPCODES[0xaa] = new Opcode(
        0xaa,
        "unknown_aa",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_AB = (OPCODES[0xab] = new Opcode(
        0xab,
        "unknown_ab",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_AC = (OPCODES[0xac] = new Opcode(
        0xac,
        "unknown_ac",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_AD = (OPCODES[0xad] = new Opcode(
        0xad,
        "unknown_ad",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_AE = (OPCODES[0xae] = new Opcode(
        0xae,
        "unknown_ae",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_AF = (OPCODES[0xaf] = new Opcode(
        0xaf,
        "unknown_af",
        undefined,
        [],
        undefined
    ));
    static readonly PL_ADD_MESETA = (OPCODES[0xb0] = new Opcode(
        0xb0,
        "pl_add_meseta",
        undefined,
        [new Param(TYPE_DWORD, undefined, undefined), new Param(TYPE_DWORD, undefined, undefined)],
        StackInteraction.Pop
    ));
    static readonly THREAD_STG = (OPCODES[0xb1] = new Opcode(
        0xb1,
        "thread_stg",
        undefined,
        [new Param(TYPE_I_LABEL, undefined, undefined)],
        undefined
    ));
    static readonly DEL_OBJ_PARAM = (OPCODES[0xb2] = new Opcode(
        0xb2,
        "del_obj_param",
        undefined,
        [
            new Param(
                new RegTupRefType(new Param(TYPE_ANY, undefined, ParamAccess.Read)),
                "Object handle.",
                undefined
            ),
        ],
        undefined
    ));
    static readonly ITEM_CREATE = (OPCODES[0xb3] = new Opcode(
        0xb3,
        "item_create",
        undefined,
        [
            new Param(
                new RegTupRefType(new Param(TYPE_ANY, undefined, ParamAccess.Read)),
                undefined,
                undefined
            ),
            new Param(
                new RegTupRefType(new Param(TYPE_ANY, undefined, ParamAccess.Read)),
                undefined,
                undefined
            ),
        ],
        undefined
    ));
    static readonly ITEM_CREATE2 = (OPCODES[0xb4] = new Opcode(
        0xb4,
        "item_create2",
        undefined,
        [
            new Param(
                new RegTupRefType(new Param(TYPE_ANY, undefined, ParamAccess.Read)),
                undefined,
                undefined
            ),
            new Param(
                new RegTupRefType(new Param(TYPE_ANY, undefined, ParamAccess.Read)),
                undefined,
                undefined
            ),
        ],
        undefined
    ));
    static readonly ITEM_DELETE = (OPCODES[0xb5] = new Opcode(
        0xb5,
        "item_delete",
        undefined,
        [
            new Param(
                new RegTupRefType(new Param(TYPE_ANY, undefined, ParamAccess.Read)),
                undefined,
                undefined
            ),
            new Param(
                new RegTupRefType(new Param(TYPE_ANY, undefined, ParamAccess.Read)),
                undefined,
                undefined
            ),
        ],
        undefined
    ));
    static readonly ITEM_DELETE2 = (OPCODES[0xb6] = new Opcode(
        0xb6,
        "item_delete2",
        "Deletes an item from the player's inventory.",
        [
            new Param(
                new RegTupRefType(
                    new Param(TYPE_DWORD, undefined, ParamAccess.Read),
                    new Param(TYPE_DWORD, undefined, ParamAccess.Read),
                    new Param(TYPE_DWORD, undefined, ParamAccess.Read)
                ),
                undefined,
                undefined
            ),
            new Param(
                new RegTupRefType(new Param(TYPE_ANY, undefined, ParamAccess.Write)),
                undefined,
                undefined
            ),
        ],
        undefined
    ));
    static readonly ITEM_CHECK = (OPCODES[0xb7] = new Opcode(
        0xb7,
        "item_check",
        undefined,
        [
            new Param(
                new RegTupRefType(new Param(TYPE_ANY, undefined, ParamAccess.Read)),
                undefined,
                undefined
            ),
            new Param(
                new RegTupRefType(new Param(TYPE_ANY, undefined, ParamAccess.Read)),
                undefined,
                undefined
            ),
        ],
        undefined
    ));
    static readonly SETEVT = (OPCODES[0xb8] = new Opcode(
        0xb8,
        "setevt",
        undefined,
        [new Param(TYPE_DWORD, undefined, undefined)],
        StackInteraction.Pop
    ));
    static readonly GET_DIFFLVL = (OPCODES[0xb9] = new Opcode(
        0xb9,
        "get_difflvl",
        "Sets the given register to the current difficulty. 0 For normal, 1 for hard and 2 for both very hard and ultimate.\nUse get_difficulty_level2 if you want to differentiate between very hard and ultimate.",
        [
            new Param(
                new RegTupRefType(new Param(TYPE_DWORD, undefined, ParamAccess.Write)),
                undefined,
                undefined
            ),
        ],
        undefined
    ));
    static readonly SET_QT_EXIT = (OPCODES[0xba] = new Opcode(
        0xba,
        "set_qt_exit",
        undefined,
        [new Param(TYPE_I_LABEL, undefined, undefined)],
        undefined
    ));
    static readonly CLR_QT_EXIT = (OPCODES[0xbb] = new Opcode(
        0xbb,
        "clr_qt_exit",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_BC = (OPCODES[0xbc] = new Opcode(
        0xbc,
        "unknown_bc",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_BD = (OPCODES[0xbd] = new Opcode(
        0xbd,
        "unknown_bd",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_BE = (OPCODES[0xbe] = new Opcode(
        0xbe,
        "unknown_be",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_BF = (OPCODES[0xbf] = new Opcode(
        0xbf,
        "unknown_bf",
        undefined,
        [],
        undefined
    ));
    static readonly PARTICLE_V3 = (OPCODES[0xc0] = new Opcode(
        0xc0,
        "particle_v3",
        undefined,
        [
            new Param(
                new RegTupRefType(new Param(TYPE_ANY, undefined, ParamAccess.Read)),
                undefined,
                undefined
            ),
        ],
        undefined
    ));
    static readonly NPC_TEXT = (OPCODES[0xc1] = new Opcode(
        0xc1,
        "npc_text",
        undefined,
        [new Param(TYPE_DWORD, undefined, undefined), new Param(TYPE_STRING, undefined, undefined)],
        StackInteraction.Pop
    ));
    static readonly NPC_CHKWARP = (OPCODES[0xc2] = new Opcode(
        0xc2,
        "npc_chkwarp",
        undefined,
        [],
        undefined
    ));
    static readonly PL_PKOFF = (OPCODES[0xc3] = new Opcode(
        0xc3,
        "pl_pkoff",
        undefined,
        [],
        undefined
    ));
    static readonly MAP_DESIGNATE = (OPCODES[0xc4] = new Opcode(
        0xc4,
        "map_designate",
        undefined,
        [
            new Param(
                new RegTupRefType(new Param(TYPE_ANY, undefined, ParamAccess.Read)),
                undefined,
                undefined
            ),
        ],
        undefined
    ));
    static readonly MASTERKEY_ON = (OPCODES[0xc5] = new Opcode(
        0xc5,
        "masterkey_on",
        undefined,
        [],
        undefined
    ));
    static readonly MASTERKEY_OFF = (OPCODES[0xc6] = new Opcode(
        0xc6,
        "masterkey_off",
        undefined,
        [],
        undefined
    ));
    static readonly WINDOW_TIME = (OPCODES[0xc7] = new Opcode(
        0xc7,
        "window_time",
        undefined,
        [],
        undefined
    ));
    static readonly WINEND_TIME = (OPCODES[0xc8] = new Opcode(
        0xc8,
        "winend_time",
        undefined,
        [],
        undefined
    ));
    static readonly WINSET_TIME = (OPCODES[0xc9] = new Opcode(
        0xc9,
        "winset_time",
        undefined,
        [
            new Param(
                new RegTupRefType(new Param(TYPE_DWORD, undefined, ParamAccess.Write)),
                undefined,
                undefined
            ),
        ],
        undefined
    ));
    static readonly GETMTIME = (OPCODES[0xca] = new Opcode(
        0xca,
        "getmtime",
        undefined,
        [
            new Param(
                new RegTupRefType(new Param(TYPE_ANY, undefined, ParamAccess.Write)),
                undefined,
                undefined
            ),
        ],
        undefined
    ));
    static readonly SET_QUEST_BOARD_HANDLER = (OPCODES[0xcb] = new Opcode(
        0xcb,
        "set_quest_board_handler",
        undefined,
        [
            new Param(TYPE_DWORD, undefined, undefined),
            new Param(TYPE_I_LABEL, undefined, undefined),
            new Param(TYPE_STRING, undefined, undefined),
        ],
        StackInteraction.Pop
    ));
    static readonly CLEAR_QUEST_BOARD_HANDLER = (OPCODES[0xcc] = new Opcode(
        0xcc,
        "clear_quest_board_handler",
        undefined,
        [new Param(TYPE_DWORD, undefined, undefined)],
        StackInteraction.Pop
    ));
    static readonly PARTICLE_ID_V3 = (OPCODES[0xcd] = new Opcode(
        0xcd,
        "particle_id_v3",
        undefined,
        [
            new Param(
                new RegTupRefType(
                    new Param(TYPE_DWORD, undefined, ParamAccess.Read),
                    new Param(TYPE_DWORD, undefined, ParamAccess.Read),
                    new Param(TYPE_DWORD, undefined, ParamAccess.Read),
                    new Param(TYPE_DWORD, undefined, ParamAccess.Read)
                ),
                undefined,
                undefined
            ),
        ],
        undefined
    ));
    static readonly NPC_CRPTALK_ID_V3 = (OPCODES[0xce] = new Opcode(
        0xce,
        "npc_crptalk_id_v3",
        undefined,
        [
            new Param(
                new RegTupRefType(new Param(TYPE_ANY, undefined, ParamAccess.Read)),
                undefined,
                undefined
            ),
        ],
        undefined
    ));
    static readonly NPC_LANG_CLEAN = (OPCODES[0xcf] = new Opcode(
        0xcf,
        "npc_lang_clean",
        undefined,
        [],
        undefined
    ));
    static readonly PL_PKON = (OPCODES[0xd0] = new Opcode(
        0xd0,
        "pl_pkon",
        undefined,
        [],
        undefined
    ));
    static readonly PL_CHK_ITEM2 = (OPCODES[0xd1] = new Opcode(
        0xd1,
        "pl_chk_item2",
        undefined,
        [
            new Param(
                new RegTupRefType(new Param(TYPE_ANY, undefined, ParamAccess.Read)),
                undefined,
                undefined
            ),
            new Param(
                new RegTupRefType(new Param(TYPE_ANY, undefined, ParamAccess.Read)),
                undefined,
                undefined
            ),
        ],
        undefined
    ));
    static readonly ENABLE_MAINMENU = (OPCODES[0xd2] = new Opcode(
        0xd2,
        "enable_mainmenu",
        undefined,
        [],
        undefined
    ));
    static readonly DISABLE_MAINMENU = (OPCODES[0xd3] = new Opcode(
        0xd3,
        "disable_mainmenu",
        undefined,
        [],
        undefined
    ));
    static readonly START_BATTLEBGM = (OPCODES[0xd4] = new Opcode(
        0xd4,
        "start_battlebgm",
        undefined,
        [],
        undefined
    ));
    static readonly END_BATTLEBGM = (OPCODES[0xd5] = new Opcode(
        0xd5,
        "end_battlebgm",
        undefined,
        [],
        undefined
    ));
    static readonly DISP_MSG_QB = (OPCODES[0xd6] = new Opcode(
        0xd6,
        "disp_msg_qb",
        undefined,
        [new Param(TYPE_STRING, undefined, undefined)],
        StackInteraction.Pop
    ));
    static readonly CLOSE_MSG_QB = (OPCODES[0xd7] = new Opcode(
        0xd7,
        "close_msg_qb",
        undefined,
        [],
        undefined
    ));
    static readonly SET_EVENTFLAG_V3 = (OPCODES[0xd8] = new Opcode(
        0xd8,
        "set_eventflag_v3",
        undefined,
        [new Param(TYPE_DWORD, undefined, undefined), new Param(TYPE_DWORD, undefined, undefined)],
        StackInteraction.Pop
    ));
    static readonly SYNC_LETI = (OPCODES[0xd9] = new Opcode(
        0xd9,
        "sync_leti",
        undefined,
        [
            new Param(
                new RegTupRefType(new Param(TYPE_DWORD, undefined, ParamAccess.Write)),
                undefined,
                undefined
            ),
            new Param(TYPE_DWORD, undefined, undefined),
        ],
        undefined
    ));
    static readonly SET_RETURNHUNTER = (OPCODES[0xda] = new Opcode(
        0xda,
        "set_returnhunter",
        undefined,
        [],
        undefined
    ));
    static readonly SET_RETURNCITY = (OPCODES[0xdb] = new Opcode(
        0xdb,
        "set_returncity",
        undefined,
        [],
        undefined
    ));
    static readonly LOAD_PVR = (OPCODES[0xdc] = new Opcode(
        0xdc,
        "load_pvr",
        undefined,
        [],
        undefined
    ));
    static readonly LOAD_MIDI = (OPCODES[0xdd] = new Opcode(
        0xdd,
        "load_midi",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_DE = (OPCODES[0xde] = new Opcode(
        0xde,
        "unknown_de",
        undefined,
        [],
        undefined
    ));
    static readonly NPC_PARAM_V3 = (OPCODES[0xdf] = new Opcode(
        0xdf,
        "npc_param_v3",
        undefined,
        [
            new Param(
                new RegTupRefType(new Param(TYPE_ANY, undefined, ParamAccess.Read)),
                undefined,
                undefined
            ),
            new Param(TYPE_DWORD, undefined, undefined),
        ],
        StackInteraction.Pop
    ));
    static readonly PAD_DRAGON = (OPCODES[0xe0] = new Opcode(
        0xe0,
        "pad_dragon",
        undefined,
        [],
        undefined
    ));
    static readonly CLEAR_MAINWARP = (OPCODES[0xe1] = new Opcode(
        0xe1,
        "clear_mainwarp",
        undefined,
        [new Param(TYPE_DWORD, undefined, undefined)],
        StackInteraction.Pop
    ));
    static readonly PCAM_PARAM_V3 = (OPCODES[0xe2] = new Opcode(
        0xe2,
        "pcam_param_v3",
        undefined,
        [
            new Param(
                new RegTupRefType(new Param(TYPE_ANY, undefined, ParamAccess.Read)),
                undefined,
                undefined
            ),
        ],
        undefined
    ));
    static readonly START_SETEVT_V3 = (OPCODES[0xe3] = new Opcode(
        0xe3,
        "start_setevt_v3",
        undefined,
        [
            new Param(
                new RegTupRefType(new Param(TYPE_ANY, undefined, ParamAccess.Read)),
                undefined,
                undefined
            ),
            new Param(TYPE_DWORD, undefined, undefined),
        ],
        StackInteraction.Pop
    ));
    static readonly WARP_ON = (OPCODES[0xe4] = new Opcode(
        0xe4,
        "warp_on",
        undefined,
        [],
        undefined
    ));
    static readonly WARP_OFF = (OPCODES[0xe5] = new Opcode(
        0xe5,
        "warp_off",
        undefined,
        [],
        undefined
    ));
    static readonly GET_SLOTNUMBER = (OPCODES[0xe6] = new Opcode(
        0xe6,
        "get_slotnumber",
        undefined,
        [
            new Param(
                new RegTupRefType(new Param(TYPE_DWORD, undefined, ParamAccess.Write)),
                undefined,
                undefined
            ),
        ],
        undefined
    ));
    static readonly GET_SERVERNUMBER = (OPCODES[0xe7] = new Opcode(
        0xe7,
        "get_servernumber",
        undefined,
        [
            new Param(
                new RegTupRefType(new Param(TYPE_DWORD, undefined, ParamAccess.Write)),
                undefined,
                undefined
            ),
        ],
        undefined
    ));
    static readonly SET_EVENTFLAG2 = (OPCODES[0xe8] = new Opcode(
        0xe8,
        "set_eventflag2",
        undefined,
        [
            new Param(TYPE_DWORD, undefined, undefined),
            new Param(
                new RegTupRefType(new Param(TYPE_ANY, undefined, ParamAccess.Read)),
                undefined,
                undefined
            ),
        ],
        StackInteraction.Pop
    ));
    static readonly RES = (OPCODES[0xe9] = new Opcode(
        0xe9,
        "res",
        undefined,
        [
            new Param(
                new RegTupRefType(new Param(TYPE_ANY, undefined, ParamAccess.Read)),
                undefined,
                undefined
            ),
            new Param(
                new RegTupRefType(new Param(TYPE_ANY, undefined, ParamAccess.Read)),
                undefined,
                undefined
            ),
        ],
        undefined
    ));
    static readonly UNKNOWN_EA = (OPCODES[0xea] = new Opcode(
        0xea,
        "unknown_ea",
        undefined,
        [
            new Param(
                new RegTupRefType(new Param(TYPE_ANY, undefined, ParamAccess.Read)),
                undefined,
                undefined
            ),
            new Param(TYPE_DWORD, undefined, undefined),
        ],
        undefined
    ));
    static readonly ENABLE_BGMCTRL = (OPCODES[0xeb] = new Opcode(
        0xeb,
        "enable_bgmctrl",
        undefined,
        [new Param(TYPE_DWORD, undefined, undefined)],
        StackInteraction.Pop
    ));
    static readonly SW_SEND = (OPCODES[0xec] = new Opcode(
        0xec,
        "sw_send",
        undefined,
        [
            new Param(
                new RegTupRefType(new Param(TYPE_ANY, undefined, ParamAccess.Read)),
                undefined,
                undefined
            ),
        ],
        undefined
    ));
    static readonly CREATE_BGMCTRL = (OPCODES[0xed] = new Opcode(
        0xed,
        "create_bgmctrl",
        undefined,
        [],
        undefined
    ));
    static readonly PL_ADD_MESETA2 = (OPCODES[0xee] = new Opcode(
        0xee,
        "pl_add_meseta2",
        undefined,
        [new Param(TYPE_DWORD, undefined, undefined)],
        StackInteraction.Pop
    ));
    static readonly SYNC_REGISTER = (OPCODES[0xef] = new Opcode(
        0xef,
        "sync_register",
        undefined,
        [
            new Param(
                new RegTupRefType(new Param(TYPE_DWORD, undefined, ParamAccess.Write)),
                undefined,
                undefined
            ),
            new Param(TYPE_DWORD, undefined, undefined),
        ],
        StackInteraction.Pop
    ));
    static readonly SEND_REGWORK = (OPCODES[0xf0] = new Opcode(
        0xf0,
        "send_regwork",
        undefined,
        [],
        undefined
    ));
    static readonly LETI_FIXED_CAMERA_V3 = (OPCODES[0xf1] = new Opcode(
        0xf1,
        "leti_fixed_camera_v3",
        undefined,
        [
            new Param(
                new RegTupRefType(
                    new Param(TYPE_DWORD, undefined, ParamAccess.Read),
                    new Param(TYPE_DWORD, undefined, ParamAccess.Read),
                    new Param(TYPE_DWORD, undefined, ParamAccess.Read),
                    new Param(TYPE_DWORD, undefined, ParamAccess.Read),
                    new Param(TYPE_DWORD, undefined, ParamAccess.Read),
                    new Param(TYPE_DWORD, undefined, ParamAccess.Read)
                ),
                undefined,
                undefined
            ),
        ],
        undefined
    ));
    static readonly DEFAULT_CAMERA_POS1 = (OPCODES[0xf2] = new Opcode(
        0xf2,
        "default_camera_pos1",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F3 = (OPCODES[0xf3] = new Opcode(
        0xf3,
        "unknown_f3",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F4 = (OPCODES[0xf4] = new Opcode(
        0xf4,
        "unknown_f4",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F5 = (OPCODES[0xf5] = new Opcode(
        0xf5,
        "unknown_f5",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F6 = (OPCODES[0xf6] = new Opcode(
        0xf6,
        "unknown_f6",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F7 = (OPCODES[0xf7] = new Opcode(
        0xf7,
        "unknown_f7",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F8 = (OPCODES[0xf8] = new Opcode(
        0xf8,
        "unknown_f8",
        undefined,
        [
            new Param(
                new RegTupRefType(new Param(TYPE_ANY, undefined, ParamAccess.Read)),
                undefined,
                undefined
            ),
        ],
        undefined
    ));
    static readonly UNKNOWN_F9 = (OPCODES[0xf9] = new Opcode(
        0xf9,
        "unknown_f9",
        undefined,
        [],
        undefined
    ));
    static readonly GET_GC_NUMBER = (OPCODES[0xfa] = new Opcode(
        0xfa,
        "get_gc_number",
        undefined,
        [
            new Param(
                new RegTupRefType(new Param(TYPE_ANY, undefined, ParamAccess.Write)),
                undefined,
                undefined
            ),
        ],
        undefined
    ));
    static readonly UNKNOWN_FB = (OPCODES[0xfb] = new Opcode(
        0xfb,
        "unknown_fb",
        undefined,
        [new Param(TYPE_WORD, undefined, undefined)],
        undefined
    ));
    static readonly UNKNOWN_FC = (OPCODES[0xfc] = new Opcode(
        0xfc,
        "unknown_fc",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_FD = (OPCODES[0xfd] = new Opcode(
        0xfd,
        "unknown_fd",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_FE = (OPCODES[0xfe] = new Opcode(
        0xfe,
        "unknown_fe",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_FF = (OPCODES[0xff] = new Opcode(
        0xff,
        "unknown_ff",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F800 = (OPCODES[0xf800] = new Opcode(
        0xf800,
        "unknown_f800",
        undefined,
        [],
        undefined
    ));
    static readonly SET_CHAT_CALLBACK = (OPCODES[0xf801] = new Opcode(
        0xf801,
        "set_chat_callback",
        undefined,
        [
            new Param(
                new RegTupRefType(new Param(TYPE_ANY, undefined, ParamAccess.Read)),
                undefined,
                undefined
            ),
            new Param(TYPE_STRING, undefined, undefined),
        ],
        StackInteraction.Pop
    ));
    static readonly UNKNOWN_F802 = (OPCODES[0xf802] = new Opcode(
        0xf802,
        "unknown_f802",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F803 = (OPCODES[0xf803] = new Opcode(
        0xf803,
        "unknown_f803",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F804 = (OPCODES[0xf804] = new Opcode(
        0xf804,
        "unknown_f804",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F805 = (OPCODES[0xf805] = new Opcode(
        0xf805,
        "unknown_f805",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F806 = (OPCODES[0xf806] = new Opcode(
        0xf806,
        "unknown_f806",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F807 = (OPCODES[0xf807] = new Opcode(
        0xf807,
        "unknown_f807",
        undefined,
        [],
        undefined
    ));
    static readonly GET_DIFFICULTY_LEVEL2 = (OPCODES[0xf808] = new Opcode(
        0xf808,
        "get_difficulty_level2",
        "Sets the given register to the current difficulty. 0 For normal, 1 for hard, 2 for very hard and 3 for ultimate.",
        [
            new Param(
                new RegTupRefType(new Param(TYPE_DWORD, undefined, ParamAccess.Write)),
                undefined,
                undefined
            ),
        ],
        undefined
    ));
    static readonly GET_NUMBER_OF_PLAYER1 = (OPCODES[0xf809] = new Opcode(
        0xf809,
        "get_number_of_player1",
        "Set the given register to the current number of players. Either 1, 2, 3 or 4.",
        [
            new Param(
                new RegTupRefType(new Param(TYPE_DWORD, undefined, ParamAccess.Write)),
                undefined,
                undefined
            ),
        ],
        undefined
    ));
    static readonly GET_COORD_OF_PLAYER = (OPCODES[0xf80a] = new Opcode(
        0xf80a,
        "get_coord_of_player",
        "Retrieves a player's position.",
        [
            new Param(
                new RegTupRefType(
                    new Param(TYPE_DWORD, "X coordinate.", ParamAccess.Write),
                    new Param(TYPE_DWORD, "Y coordinate.", ParamAccess.Write),
                    new Param(TYPE_DWORD, "Z coordinate.", ParamAccess.Write)
                ),
                undefined,
                undefined
            ),
            new Param(
                new RegTupRefType(new Param(TYPE_DWORD, "Player slot.", ParamAccess.Read)),
                undefined,
                undefined
            ),
        ],
        undefined
    ));
    static readonly ENABLE_MAP = (OPCODES[0xf80b] = new Opcode(
        0xf80b,
        "enable_map",
        undefined,
        [],
        undefined
    ));
    static readonly DISABLE_MAP = (OPCODES[0xf80c] = new Opcode(
        0xf80c,
        "disable_map",
        undefined,
        [],
        undefined
    ));
    static readonly MAP_DESIGNATE_EX = (OPCODES[0xf80d] = new Opcode(
        0xf80d,
        "map_designate_ex",
        undefined,
        [
            new Param(
                new RegTupRefType(
                    new Param(TYPE_DWORD, undefined, ParamAccess.Read),
                    new Param(TYPE_DWORD, undefined, ParamAccess.Read),
                    new Param(TYPE_DWORD, undefined, ParamAccess.Read),
                    new Param(TYPE_DWORD, undefined, ParamAccess.Read),
                    new Param(TYPE_DWORD, undefined, ParamAccess.Read)
                ),
                undefined,
                undefined
            ),
        ],
        undefined
    ));
    static readonly UNKNOWN_F80E = (OPCODES[0xf80e] = new Opcode(
        0xf80e,
        "unknown_f80e",
        undefined,
        [new Param(TYPE_DWORD, undefined, undefined)],
        StackInteraction.Pop
    ));
    static readonly UNKNOWN_F80F = (OPCODES[0xf80f] = new Opcode(
        0xf80f,
        "unknown_f80f",
        undefined,
        [new Param(TYPE_DWORD, undefined, undefined)],
        StackInteraction.Pop
    ));
    static readonly BA_INITIAL_FLOOR = (OPCODES[0xf810] = new Opcode(
        0xf810,
        "ba_initial_floor",
        undefined,
        [new Param(TYPE_DWORD, undefined, undefined)],
        StackInteraction.Pop
    ));
    static readonly SET_BA_RULES = (OPCODES[0xf811] = new Opcode(
        0xf811,
        "set_ba_rules",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F812 = (OPCODES[0xf812] = new Opcode(
        0xf812,
        "unknown_f812",
        undefined,
        [new Param(TYPE_DWORD, undefined, undefined)],
        StackInteraction.Pop
    ));
    static readonly UNKNOWN_F813 = (OPCODES[0xf813] = new Opcode(
        0xf813,
        "unknown_f813",
        undefined,
        [new Param(TYPE_DWORD, undefined, undefined)],
        StackInteraction.Pop
    ));
    static readonly UNKNOWN_F814 = (OPCODES[0xf814] = new Opcode(
        0xf814,
        "unknown_f814",
        undefined,
        [new Param(TYPE_DWORD, undefined, undefined)],
        StackInteraction.Pop
    ));
    static readonly UNKNOWN_F815 = (OPCODES[0xf815] = new Opcode(
        0xf815,
        "unknown_f815",
        undefined,
        [new Param(TYPE_DWORD, undefined, undefined)],
        StackInteraction.Pop
    ));
    static readonly UNKNOWN_F816 = (OPCODES[0xf816] = new Opcode(
        0xf816,
        "unknown_f816",
        undefined,
        [new Param(TYPE_DWORD, undefined, undefined)],
        StackInteraction.Pop
    ));
    static readonly UNKNOWN_F817 = (OPCODES[0xf817] = new Opcode(
        0xf817,
        "unknown_f817",
        undefined,
        [new Param(TYPE_DWORD, undefined, undefined)],
        StackInteraction.Pop
    ));
    static readonly UNKNOWN_F818 = (OPCODES[0xf818] = new Opcode(
        0xf818,
        "unknown_f818",
        undefined,
        [new Param(TYPE_DWORD, undefined, undefined)],
        StackInteraction.Pop
    ));
    static readonly UNKNOWN_F819 = (OPCODES[0xf819] = new Opcode(
        0xf819,
        "unknown_f819",
        undefined,
        [new Param(TYPE_DWORD, undefined, undefined)],
        StackInteraction.Pop
    ));
    static readonly UNKNOWN_F81A = (OPCODES[0xf81a] = new Opcode(
        0xf81a,
        "unknown_f81a",
        undefined,
        [new Param(TYPE_DWORD, undefined, undefined)],
        StackInteraction.Pop
    ));
    static readonly UNKNOWN_F81B = (OPCODES[0xf81b] = new Opcode(
        0xf81b,
        "unknown_f81b",
        undefined,
        [new Param(TYPE_DWORD, undefined, undefined)],
        StackInteraction.Pop
    ));
    static readonly BA_DISP_MSG = (OPCODES[0xf81c] = new Opcode(
        0xf81c,
        "ba_disp_msg",
        undefined,
        [new Param(TYPE_STRING, undefined, undefined)],
        StackInteraction.Pop
    ));
    static readonly DEATH_LVL_UP = (OPCODES[0xf81d] = new Opcode(
        0xf81d,
        "death_lvl_up",
        undefined,
        [new Param(TYPE_DWORD, undefined, undefined)],
        StackInteraction.Pop
    ));
    static readonly DEATH_TECH_LVL_UP = (OPCODES[0xf81e] = new Opcode(
        0xf81e,
        "death_tech_lvl_up",
        undefined,
        [new Param(TYPE_DWORD, undefined, undefined)],
        StackInteraction.Pop
    ));
    static readonly UNKNOWN_F81F = (OPCODES[0xf81f] = new Opcode(
        0xf81f,
        "unknown_f81f",
        undefined,
        [],
        undefined
    ));
    static readonly CMODE_STAGE = (OPCODES[0xf820] = new Opcode(
        0xf820,
        "cmode_stage",
        undefined,
        [new Param(TYPE_DWORD, undefined, undefined)],
        StackInteraction.Pop
    ));
    static readonly UNKNOWN_F821 = (OPCODES[0xf821] = new Opcode(
        0xf821,
        "unknown_f821",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F822 = (OPCODES[0xf822] = new Opcode(
        0xf822,
        "unknown_f822",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F823 = (OPCODES[0xf823] = new Opcode(
        0xf823,
        "unknown_f823",
        undefined,
        [new Param(TYPE_DWORD, undefined, undefined)],
        StackInteraction.Pop
    ));
    static readonly UNKNOWN_F824 = (OPCODES[0xf824] = new Opcode(
        0xf824,
        "unknown_f824",
        undefined,
        [new Param(TYPE_DWORD, undefined, undefined)],
        StackInteraction.Pop
    ));
    static readonly EXP_MULTIPLICATION = (OPCODES[0xf825] = new Opcode(
        0xf825,
        "exp_multiplication",
        undefined,
        [
            new Param(
                new RegTupRefType(new Param(TYPE_ANY, undefined, ParamAccess.Write)),
                undefined,
                undefined
            ),
        ],
        undefined
    ));
    static readonly EXP_DIVISION = (OPCODES[0xf826] = new Opcode(
        0xf826,
        "exp_division",
        undefined,
        [
            new Param(
                new RegTupRefType(new Param(TYPE_ANY, undefined, ParamAccess.Write)),
                undefined,
                undefined
            ),
        ],
        undefined
    ));
    static readonly GET_USER_IS_DEAD = (OPCODES[0xf827] = new Opcode(
        0xf827,
        "get_user_is_dead",
        undefined,
        [
            new Param(
                new RegTupRefType(new Param(TYPE_ANY, undefined, ParamAccess.Write)),
                undefined,
                undefined
            ),
        ],
        undefined
    ));
    static readonly GO_FLOOR = (OPCODES[0xf828] = new Opcode(
        0xf828,
        "go_floor",
        "Sends a player to the given floor.",
        [
            new Param(
                new RegTupRefType(new Param(TYPE_DWORD, "Player slot.", ParamAccess.Read)),
                undefined,
                undefined
            ),
            new Param(
                new RegTupRefType(new Param(TYPE_DWORD, "Floor ID.", ParamAccess.Read)),
                undefined,
                undefined
            ),
        ],
        undefined
    ));
    static readonly UNKNOWN_F829 = (OPCODES[0xf829] = new Opcode(
        0xf829,
        "unknown_f829",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F82A = (OPCODES[0xf82a] = new Opcode(
        0xf82a,
        "unknown_f82a",
        undefined,
        [],
        undefined
    ));
    static readonly UNLOCK_DOOR2 = (OPCODES[0xf82b] = new Opcode(
        0xf82b,
        "unlock_door2",
        undefined,
        [new Param(TYPE_DWORD, undefined, undefined), new Param(TYPE_DWORD, undefined, undefined)],
        StackInteraction.Pop
    ));
    static readonly LOCK_DOOR2 = (OPCODES[0xf82c] = new Opcode(
        0xf82c,
        "lock_door2",
        undefined,
        [new Param(TYPE_DWORD, undefined, undefined), new Param(TYPE_DWORD, undefined, undefined)],
        StackInteraction.Pop
    ));
    static readonly IF_SWITCH_NOT_PRESSED = (OPCODES[0xf82d] = new Opcode(
        0xf82d,
        "if_switch_not_pressed",
        undefined,
        [
            new Param(
                new RegTupRefType(
                    new Param(TYPE_DWORD, undefined, ParamAccess.Read),
                    new Param(TYPE_DWORD, undefined, ParamAccess.Write)
                ),
                undefined,
                undefined
            ),
        ],
        undefined
    ));
    static readonly IF_SWITCH_PRESSED = (OPCODES[0xf82e] = new Opcode(
        0xf82e,
        "if_switch_pressed",
        undefined,
        [
            new Param(
                new RegTupRefType(
                    new Param(TYPE_DWORD, "Floor ID.", ParamAccess.Read),
                    new Param(TYPE_DWORD, "Switch ID.", ParamAccess.Read),
                    new Param(
                        TYPE_DWORD,
                        "Will be set to 1 if the switch is pressed, 0 otherwise.",
                        ParamAccess.Write
                    )
                ),
                undefined,
                undefined
            ),
        ],
        undefined
    ));
    static readonly UNKNOWN_F82F = (OPCODES[0xf82f] = new Opcode(
        0xf82f,
        "unknown_f82f",
        undefined,
        [new Param(TYPE_DWORD, undefined, undefined), new Param(TYPE_DWORD, undefined, undefined)],
        StackInteraction.Pop
    ));
    static readonly CONTROL_DRAGON = (OPCODES[0xf830] = new Opcode(
        0xf830,
        "control_dragon",
        undefined,
        [
            new Param(
                new RegTupRefType(new Param(TYPE_DWORD, undefined, ParamAccess.Read)),
                undefined,
                undefined
            ),
        ],
        undefined
    ));
    static readonly RELEASE_DRAGON = (OPCODES[0xf831] = new Opcode(
        0xf831,
        "release_dragon",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F832 = (OPCODES[0xf832] = new Opcode(
        0xf832,
        "unknown_f832",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F833 = (OPCODES[0xf833] = new Opcode(
        0xf833,
        "unknown_f833",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F834 = (OPCODES[0xf834] = new Opcode(
        0xf834,
        "unknown_f834",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F835 = (OPCODES[0xf835] = new Opcode(
        0xf835,
        "unknown_f835",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F836 = (OPCODES[0xf836] = new Opcode(
        0xf836,
        "unknown_f836",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F837 = (OPCODES[0xf837] = new Opcode(
        0xf837,
        "unknown_f837",
        undefined,
        [],
        undefined
    ));
    static readonly SHRINK = (OPCODES[0xf838] = new Opcode(
        0xf838,
        "shrink",
        undefined,
        [
            new Param(
                new RegTupRefType(new Param(TYPE_DWORD, "Player slot.", ParamAccess.Read)),
                undefined,
                undefined
            ),
        ],
        undefined
    ));
    static readonly UNSHRINK = (OPCODES[0xf839] = new Opcode(
        0xf839,
        "unshrink",
        undefined,
        [
            new Param(
                new RegTupRefType(new Param(TYPE_DWORD, "Player slot.", ParamAccess.Read)),
                undefined,
                undefined
            ),
        ],
        undefined
    ));
    static readonly UNKNOWN_F83A = (OPCODES[0xf83a] = new Opcode(
        0xf83a,
        "unknown_f83a",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F83B = (OPCODES[0xf83b] = new Opcode(
        0xf83b,
        "unknown_f83b",
        undefined,
        [],
        undefined
    ));
    static readonly DISPLAY_CLOCK2 = (OPCODES[0xf83c] = new Opcode(
        0xf83c,
        "display_clock2",
        undefined,
        [
            new Param(
                new RegTupRefType(new Param(TYPE_DWORD, undefined, ParamAccess.Read)),
                undefined,
                undefined
            ),
        ],
        undefined
    ));
    static readonly UNKNOWN_F83D = (OPCODES[0xf83d] = new Opcode(
        0xf83d,
        "unknown_f83d",
        undefined,
        [new Param(TYPE_DWORD, undefined, undefined)],
        StackInteraction.Pop
    ));
    static readonly DELETE_AREA_TITLE = (OPCODES[0xf83e] = new Opcode(
        0xf83e,
        "delete_area_title",
        undefined,
        [new Param(TYPE_DWORD, undefined, undefined)],
        StackInteraction.Pop
    ));
    static readonly UNKNOWN_F83F = (OPCODES[0xf83f] = new Opcode(
        0xf83f,
        "unknown_f83f",
        undefined,
        [],
        undefined
    ));
    static readonly LOAD_NPC_DATA = (OPCODES[0xf840] = new Opcode(
        0xf840,
        "load_npc_data",
        undefined,
        [],
        undefined
    ));
    static readonly GET_NPC_DATA = (OPCODES[0xf841] = new Opcode(
        0xf841,
        "get_npc_data",
        undefined,
        [new Param(TYPE_D_LABEL, undefined, undefined)],
        undefined
    ));
    static readonly UNKNOWN_F842 = (OPCODES[0xf842] = new Opcode(
        0xf842,
        "unknown_f842",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F843 = (OPCODES[0xf843] = new Opcode(
        0xf843,
        "unknown_f843",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F844 = (OPCODES[0xf844] = new Opcode(
        0xf844,
        "unknown_f844",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F845 = (OPCODES[0xf845] = new Opcode(
        0xf845,
        "unknown_f845",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F846 = (OPCODES[0xf846] = new Opcode(
        0xf846,
        "unknown_f846",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F847 = (OPCODES[0xf847] = new Opcode(
        0xf847,
        "unknown_f847",
        undefined,
        [],
        undefined
    ));
    static readonly GIVE_DAMAGE_SCORE = (OPCODES[0xf848] = new Opcode(
        0xf848,
        "give_damage_score",
        undefined,
        [
            new Param(
                new RegTupRefType(new Param(TYPE_DWORD, undefined, ParamAccess.Write)),
                undefined,
                undefined
            ),
        ],
        undefined
    ));
    static readonly TAKE_DAMAGE_SCORE = (OPCODES[0xf849] = new Opcode(
        0xf849,
        "take_damage_score",
        undefined,
        [
            new Param(
                new RegTupRefType(new Param(TYPE_DWORD, undefined, ParamAccess.Write)),
                undefined,
                undefined
            ),
        ],
        undefined
    ));
    static readonly UNK_SCORE_F84A = (OPCODES[0xf84a] = new Opcode(
        0xf84a,
        "unk_score_f84a",
        undefined,
        [
            new Param(
                new RegTupRefType(new Param(TYPE_DWORD, undefined, ParamAccess.Write)),
                undefined,
                undefined
            ),
        ],
        undefined
    ));
    static readonly UNK_SCORE_F84B = (OPCODES[0xf84b] = new Opcode(
        0xf84b,
        "unk_score_f84b",
        undefined,
        [
            new Param(
                new RegTupRefType(new Param(TYPE_DWORD, undefined, ParamAccess.Write)),
                undefined,
                undefined
            ),
        ],
        undefined
    ));
    static readonly KILL_SCORE = (OPCODES[0xf84c] = new Opcode(
        0xf84c,
        "kill_score",
        undefined,
        [
            new Param(
                new RegTupRefType(new Param(TYPE_DWORD, undefined, ParamAccess.Write)),
                undefined,
                undefined
            ),
        ],
        undefined
    ));
    static readonly DEATH_SCORE = (OPCODES[0xf84d] = new Opcode(
        0xf84d,
        "death_score",
        undefined,
        [
            new Param(
                new RegTupRefType(new Param(TYPE_DWORD, undefined, ParamAccess.Write)),
                undefined,
                undefined
            ),
        ],
        undefined
    ));
    static readonly UNK_SCORE_F84E = (OPCODES[0xf84e] = new Opcode(
        0xf84e,
        "unk_score_f84e",
        undefined,
        [
            new Param(
                new RegTupRefType(new Param(TYPE_DWORD, undefined, ParamAccess.Write)),
                undefined,
                undefined
            ),
        ],
        undefined
    ));
    static readonly ENEMY_DEATH_SCORE = (OPCODES[0xf84f] = new Opcode(
        0xf84f,
        "enemy_death_score",
        undefined,
        [
            new Param(
                new RegTupRefType(new Param(TYPE_DWORD, undefined, ParamAccess.Write)),
                undefined,
                undefined
            ),
        ],
        undefined
    ));
    static readonly MESETA_SCORE = (OPCODES[0xf850] = new Opcode(
        0xf850,
        "meseta_score",
        undefined,
        [
            new Param(
                new RegTupRefType(new Param(TYPE_DWORD, undefined, ParamAccess.Write)),
                undefined,
                undefined
            ),
        ],
        undefined
    ));
    static readonly UNKNOWN_F851 = (OPCODES[0xf851] = new Opcode(
        0xf851,
        "unknown_f851",
        undefined,
        [
            new Param(
                new RegTupRefType(new Param(TYPE_DWORD, undefined, ParamAccess.Write)),
                undefined,
                undefined
            ),
        ],
        undefined
    ));
    static readonly UNKNOWN_F852 = (OPCODES[0xf852] = new Opcode(
        0xf852,
        "unknown_f852",
        undefined,
        [new Param(TYPE_DWORD, undefined, undefined)],
        StackInteraction.Pop
    ));
    static readonly REVERSE_WARPS = (OPCODES[0xf853] = new Opcode(
        0xf853,
        "reverse_warps",
        undefined,
        [],
        undefined
    ));
    static readonly UNREVERSE_WARPS = (OPCODES[0xf854] = new Opcode(
        0xf854,
        "unreverse_warps",
        undefined,
        [],
        undefined
    ));
    static readonly SET_ULT_MAP = (OPCODES[0xf855] = new Opcode(
        0xf855,
        "set_ult_map",
        undefined,
        [],
        undefined
    ));
    static readonly UNSET_ULT_MAP = (OPCODES[0xf856] = new Opcode(
        0xf856,
        "unset_ult_map",
        undefined,
        [],
        undefined
    ));
    static readonly SET_AREA_TITLE = (OPCODES[0xf857] = new Opcode(
        0xf857,
        "set_area_title",
        undefined,
        [new Param(TYPE_STRING, undefined, undefined)],
        StackInteraction.Pop
    ));
    static readonly UNKNOWN_F858 = (OPCODES[0xf858] = new Opcode(
        0xf858,
        "unknown_f858",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F859 = (OPCODES[0xf859] = new Opcode(
        0xf859,
        "unknown_f859",
        undefined,
        [],
        undefined
    ));
    static readonly EQUIP_ITEM = (OPCODES[0xf85a] = new Opcode(
        0xf85a,
        "equip_item",
        undefined,
        [
            new Param(
                new RegTupRefType(new Param(TYPE_DWORD, undefined, ParamAccess.Read)),
                undefined,
                undefined
            ),
        ],
        undefined
    ));
    static readonly UNEQUIP_ITEM = (OPCODES[0xf85b] = new Opcode(
        0xf85b,
        "unequip_item",
        undefined,
        [new Param(TYPE_DWORD, undefined, undefined), new Param(TYPE_DWORD, undefined, undefined)],
        StackInteraction.Pop
    ));
    static readonly UNKNOWN_F85C = (OPCODES[0xf85c] = new Opcode(
        0xf85c,
        "unknown_f85c",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F85D = (OPCODES[0xf85d] = new Opcode(
        0xf85d,
        "unknown_f85d",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F85E = (OPCODES[0xf85e] = new Opcode(
        0xf85e,
        "unknown_f85e",
        undefined,
        [new Param(TYPE_DWORD, undefined, undefined)],
        StackInteraction.Pop
    ));
    static readonly UNKNOWN_F85F = (OPCODES[0xf85f] = new Opcode(
        0xf85f,
        "unknown_f85f",
        undefined,
        [new Param(TYPE_DWORD, undefined, undefined)],
        StackInteraction.Pop
    ));
    static readonly UNKNOWN_F860 = (OPCODES[0xf860] = new Opcode(
        0xf860,
        "unknown_f860",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F861 = (OPCODES[0xf861] = new Opcode(
        0xf861,
        "unknown_f861",
        undefined,
        [new Param(TYPE_DWORD, undefined, undefined)],
        StackInteraction.Pop
    ));
    static readonly UNKNOWN_F862 = (OPCODES[0xf862] = new Opcode(
        0xf862,
        "unknown_f862",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F863 = (OPCODES[0xf863] = new Opcode(
        0xf863,
        "unknown_f863",
        undefined,
        [],
        undefined
    ));
    static readonly CMODE_RANK = (OPCODES[0xf864] = new Opcode(
        0xf864,
        "cmode_rank",
        undefined,
        [new Param(TYPE_DWORD, undefined, undefined), new Param(TYPE_STRING, undefined, undefined)],
        StackInteraction.Pop
    ));
    static readonly AWARD_ITEM_NAME = (OPCODES[0xf865] = new Opcode(
        0xf865,
        "award_item_name",
        undefined,
        [],
        undefined
    ));
    static readonly AWARD_ITEM_SELECT = (OPCODES[0xf866] = new Opcode(
        0xf866,
        "award_item_select",
        undefined,
        [],
        undefined
    ));
    static readonly AWARD_ITEM_GIVE_TO = (OPCODES[0xf867] = new Opcode(
        0xf867,
        "award_item_give_to",
        undefined,
        [
            new Param(
                new RegTupRefType(new Param(TYPE_DWORD, undefined, ParamAccess.Read)),
                undefined,
                undefined
            ),
        ],
        undefined
    ));
    static readonly UNKNOWN_F868 = (OPCODES[0xf868] = new Opcode(
        0xf868,
        "unknown_f868",
        undefined,
        [
            new Param(
                new RegTupRefType(new Param(TYPE_ANY, undefined, ParamAccess.Write)),
                undefined,
                undefined
            ),
            new Param(
                new RegTupRefType(new Param(TYPE_ANY, undefined, ParamAccess.Write)),
                undefined,
                undefined
            ),
        ],
        undefined
    ));
    static readonly UNKNOWN_F869 = (OPCODES[0xf869] = new Opcode(
        0xf869,
        "unknown_f869",
        undefined,
        [
            new Param(
                new RegTupRefType(new Param(TYPE_ANY, undefined, ParamAccess.Write)),
                undefined,
                undefined
            ),
            new Param(
                new RegTupRefType(new Param(TYPE_ANY, undefined, ParamAccess.Write)),
                undefined,
                undefined
            ),
        ],
        undefined
    ));
    static readonly ITEM_CREATE_CMODE = (OPCODES[0xf86a] = new Opcode(
        0xf86a,
        "item_create_cmode",
        undefined,
        [
            new Param(
                new RegTupRefType(new Param(TYPE_ANY, undefined, ParamAccess.Write)),
                undefined,
                undefined
            ),
            new Param(
                new RegTupRefType(new Param(TYPE_ANY, undefined, ParamAccess.Write)),
                undefined,
                undefined
            ),
        ],
        undefined
    ));
    static readonly UNKNOWN_F86B = (OPCODES[0xf86b] = new Opcode(
        0xf86b,
        "unknown_f86b",
        undefined,
        [
            new Param(
                new RegTupRefType(new Param(TYPE_ANY, undefined, ParamAccess.Read)),
                undefined,
                undefined
            ),
        ],
        undefined
    ));
    static readonly AWARD_ITEM_OK = (OPCODES[0xf86c] = new Opcode(
        0xf86c,
        "award_item_ok",
        undefined,
        [
            new Param(
                new RegTupRefType(new Param(TYPE_ANY, undefined, ParamAccess.Write)),
                undefined,
                undefined
            ),
        ],
        undefined
    ));
    static readonly UNKNOWN_F86D = (OPCODES[0xf86d] = new Opcode(
        0xf86d,
        "unknown_f86d",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F86E = (OPCODES[0xf86e] = new Opcode(
        0xf86e,
        "unknown_f86e",
        undefined,
        [],
        undefined
    ));
    static readonly BA_SET_LIVES = (OPCODES[0xf86f] = new Opcode(
        0xf86f,
        "ba_set_lives",
        undefined,
        [new Param(TYPE_DWORD, undefined, undefined)],
        StackInteraction.Pop
    ));
    static readonly BA_SET_TECH_LVL = (OPCODES[0xf870] = new Opcode(
        0xf870,
        "ba_set_tech_lvl",
        undefined,
        [new Param(TYPE_DWORD, undefined, undefined)],
        StackInteraction.Pop
    ));
    static readonly BA_SET_LVL = (OPCODES[0xf871] = new Opcode(
        0xf871,
        "ba_set_lvl",
        undefined,
        [new Param(TYPE_DWORD, undefined, undefined)],
        StackInteraction.Pop
    ));
    static readonly BA_SET_TIME_LIMIT = (OPCODES[0xf872] = new Opcode(
        0xf872,
        "ba_set_time_limit",
        undefined,
        [new Param(TYPE_DWORD, undefined, undefined)],
        StackInteraction.Pop
    ));
    static readonly BOSS_IS_DEAD = (OPCODES[0xf873] = new Opcode(
        0xf873,
        "boss_is_dead",
        undefined,
        [
            new Param(
                new RegTupRefType(new Param(TYPE_ANY, undefined, ParamAccess.Write)),
                undefined,
                undefined
            ),
        ],
        undefined
    ));
    static readonly UNKNOWN_F874 = (OPCODES[0xf874] = new Opcode(
        0xf874,
        "unknown_f874",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F875 = (OPCODES[0xf875] = new Opcode(
        0xf875,
        "unknown_f875",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F876 = (OPCODES[0xf876] = new Opcode(
        0xf876,
        "unknown_f876",
        undefined,
        [],
        undefined
    ));
    static readonly ENABLE_TECHS = (OPCODES[0xf877] = new Opcode(
        0xf877,
        "enable_techs",
        "Enables technique use for the given player.",
        [
            new Param(
                new RegTupRefType(new Param(TYPE_DWORD, "Player slot.", ParamAccess.Read)),
                undefined,
                undefined
            ),
        ],
        undefined
    ));
    static readonly DISABLE_TECHS = (OPCODES[0xf878] = new Opcode(
        0xf878,
        "disable_techs",
        "Disables technique use for the given player.",
        [
            new Param(
                new RegTupRefType(new Param(TYPE_DWORD, "Player slot.", ParamAccess.Read)),
                undefined,
                undefined
            ),
        ],
        undefined
    ));
    static readonly GET_GENDER = (OPCODES[0xf879] = new Opcode(
        0xf879,
        "get_gender",
        "Retrieves the player's gender. 0 If male, 1 if female.",
        [
            new Param(
                new RegTupRefType(new Param(TYPE_DWORD, "Player slot.", ParamAccess.Read)),
                undefined,
                undefined
            ),
            new Param(
                new RegTupRefType(new Param(TYPE_DWORD, "Player gender.", ParamAccess.Write)),
                undefined,
                undefined
            ),
        ],
        undefined
    ));
    static readonly GET_CHARA_CLASS = (OPCODES[0xf87a] = new Opcode(
        0xf87a,
        "get_chara_class",
        "Retrieves the player's race and character class.",
        [
            new Param(
                new RegTupRefType(new Param(TYPE_DWORD, "Player slot.", ParamAccess.Read)),
                undefined,
                undefined
            ),
            new Param(
                new RegTupRefType(
                    new Param(
                        TYPE_DWORD,
                        "Player race. 0 If human, 1 if newman, 2 if cast.",
                        ParamAccess.Write
                    ),
                    new Param(
                        TYPE_DWORD,
                        "Player class. 0 If hunter, 1 if ranger, 2 if force.",
                        ParamAccess.Write
                    )
                ),
                undefined,
                undefined
            ),
        ],
        undefined
    ));
    static readonly TAKE_SLOT_MESETA = (OPCODES[0xf87b] = new Opcode(
        0xf87b,
        "take_slot_meseta",
        "Takes an amount of meseta from a player's inventory.",
        [
            new Param(
                new RegTupRefType(
                    new Param(TYPE_DWORD, "Player slot.", ParamAccess.Read),
                    new Param(TYPE_DWORD, "Amount of meseta to take.", ParamAccess.Read)
                ),
                undefined,
                undefined
            ),
            new Param(
                new RegTupRefType(
                    new Param(
                        TYPE_DWORD,
                        "Will be set to 1 if the meseta was taken, 0 otherwise.",
                        ParamAccess.Write
                    )
                ),
                undefined,
                undefined
            ),
        ],
        undefined
    ));
    static readonly UNKNOWN_F87C = (OPCODES[0xf87c] = new Opcode(
        0xf87c,
        "unknown_f87c",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F87D = (OPCODES[0xf87d] = new Opcode(
        0xf87d,
        "unknown_f87d",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F87E = (OPCODES[0xf87e] = new Opcode(
        0xf87e,
        "unknown_f87e",
        undefined,
        [],
        undefined
    ));
    static readonly READ_GUILDCARD_FLAG = (OPCODES[0xf87f] = new Opcode(
        0xf87f,
        "read_guildcard_flag",
        undefined,
        [
            new Param(
                new RegTupRefType(new Param(TYPE_ANY, undefined, ParamAccess.Write)),
                undefined,
                undefined
            ),
            new Param(
                new RegTupRefType(new Param(TYPE_ANY, undefined, ParamAccess.Write)),
                undefined,
                undefined
            ),
        ],
        undefined
    ));
    static readonly UNKNOWN_F880 = (OPCODES[0xf880] = new Opcode(
        0xf880,
        "unknown_f880",
        undefined,
        [
            new Param(
                new RegTupRefType(new Param(TYPE_ANY, undefined, ParamAccess.Write)),
                undefined,
                undefined
            ),
        ],
        undefined
    ));
    static readonly GET_PL_NAME = (OPCODES[0xf881] = new Opcode(
        0xf881,
        "get_pl_name",
        "Sets the value of <pl_name> to the given player's name.",
        [
            new Param(
                new RegTupRefType(new Param(TYPE_DWORD, "Player slot.", ParamAccess.Read)),
                undefined,
                undefined
            ),
        ],
        undefined
    ));
    static readonly UNKNOWN_F882 = (OPCODES[0xf882] = new Opcode(
        0xf882,
        "unknown_f882",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F883 = (OPCODES[0xf883] = new Opcode(
        0xf883,
        "unknown_f883",
        undefined,
        [
            new Param(
                new RegTupRefType(new Param(TYPE_ANY, undefined, ParamAccess.Write)),
                undefined,
                undefined
            ),
            new Param(
                new RegTupRefType(new Param(TYPE_ANY, undefined, ParamAccess.Write)),
                undefined,
                undefined
            ),
        ],
        undefined
    ));
    static readonly UNKNOWN_F884 = (OPCODES[0xf884] = new Opcode(
        0xf884,
        "unknown_f884",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F885 = (OPCODES[0xf885] = new Opcode(
        0xf885,
        "unknown_f885",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F886 = (OPCODES[0xf886] = new Opcode(
        0xf886,
        "unknown_f886",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F887 = (OPCODES[0xf887] = new Opcode(
        0xf887,
        "unknown_f887",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F888 = (OPCODES[0xf888] = new Opcode(
        0xf888,
        "unknown_f888",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F889 = (OPCODES[0xf889] = new Opcode(
        0xf889,
        "unknown_f889",
        undefined,
        [],
        undefined
    ));
    static readonly GET_PLAYER_STATUS = (OPCODES[0xf88a] = new Opcode(
        0xf88a,
        "get_player_status",
        undefined,
        [
            new Param(
                new RegTupRefType(new Param(TYPE_ANY, undefined, ParamAccess.Write)),
                undefined,
                undefined
            ),
            new Param(
                new RegTupRefType(new Param(TYPE_ANY, undefined, ParamAccess.Write)),
                undefined,
                undefined
            ),
        ],
        undefined
    ));
    static readonly SEND_MAIL = (OPCODES[0xf88b] = new Opcode(
        0xf88b,
        "send_mail",
        undefined,
        [
            new Param(
                new RegTupRefType(new Param(TYPE_ANY, undefined, ParamAccess.Read)),
                undefined,
                undefined
            ),
            new Param(TYPE_STRING, undefined, undefined),
        ],
        StackInteraction.Pop
    ));
    static readonly ONLINE_CHECK = (OPCODES[0xf88c] = new Opcode(
        0xf88c,
        "online_check",
        undefined,
        [
            new Param(
                new RegTupRefType(new Param(TYPE_ANY, undefined, ParamAccess.Write)),
                undefined,
                undefined
            ),
        ],
        undefined
    ));
    static readonly CHL_SET_TIMERECORD = (OPCODES[0xf88d] = new Opcode(
        0xf88d,
        "chl_set_timerecord",
        undefined,
        [
            new Param(
                new RegTupRefType(new Param(TYPE_ANY, undefined, ParamAccess.Read)),
                undefined,
                undefined
            ),
        ],
        undefined
    ));
    static readonly CHL_GET_TIMERECORD = (OPCODES[0xf88e] = new Opcode(
        0xf88e,
        "chl_get_timerecord",
        undefined,
        [
            new Param(
                new RegTupRefType(new Param(TYPE_ANY, undefined, ParamAccess.Write)),
                undefined,
                undefined
            ),
        ],
        undefined
    ));
    static readonly UNKNOWN_F88F = (OPCODES[0xf88f] = new Opcode(
        0xf88f,
        "unknown_f88f",
        undefined,
        [
            new Param(
                new RegTupRefType(new Param(TYPE_ANY, undefined, ParamAccess.Write)),
                undefined,
                undefined
            ),
        ],
        undefined
    ));
    static readonly UNKNOWN_F890 = (OPCODES[0xf890] = new Opcode(
        0xf890,
        "unknown_f890",
        undefined,
        [],
        undefined
    ));
    static readonly LOAD_ENEMY_DATA = (OPCODES[0xf891] = new Opcode(
        0xf891,
        "load_enemy_data",
        undefined,
        [new Param(TYPE_DWORD, undefined, undefined)],
        StackInteraction.Pop
    ));
    static readonly GET_PHYSICAL_DATA = (OPCODES[0xf892] = new Opcode(
        0xf892,
        "get_physical_data",
        undefined,
        [new Param(TYPE_WORD, undefined, undefined)],
        undefined
    ));
    static readonly GET_ATTACK_DATA = (OPCODES[0xf893] = new Opcode(
        0xf893,
        "get_attack_data",
        undefined,
        [new Param(TYPE_WORD, undefined, undefined)],
        undefined
    ));
    static readonly GET_RESIST_DATA = (OPCODES[0xf894] = new Opcode(
        0xf894,
        "get_resist_data",
        undefined,
        [new Param(TYPE_WORD, undefined, undefined)],
        undefined
    ));
    static readonly GET_MOVEMENT_DATA = (OPCODES[0xf895] = new Opcode(
        0xf895,
        "get_movement_data",
        undefined,
        [new Param(TYPE_WORD, undefined, undefined)],
        undefined
    ));
    static readonly UNKNOWN_F896 = (OPCODES[0xf896] = new Opcode(
        0xf896,
        "unknown_f896",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F897 = (OPCODES[0xf897] = new Opcode(
        0xf897,
        "unknown_f897",
        undefined,
        [],
        undefined
    ));
    static readonly SHIFT_LEFT = (OPCODES[0xf898] = new Opcode(
        0xf898,
        "shift_left",
        undefined,
        [
            new Param(
                new RegTupRefType(new Param(TYPE_DWORD, undefined, ParamAccess.Write)),
                undefined,
                undefined
            ),
            new Param(
                new RegTupRefType(new Param(TYPE_DWORD, undefined, ParamAccess.Read)),
                undefined,
                undefined
            ),
        ],
        undefined
    ));
    static readonly SHIFT_RIGHT = (OPCODES[0xf899] = new Opcode(
        0xf899,
        "shift_right",
        undefined,
        [
            new Param(
                new RegTupRefType(new Param(TYPE_DWORD, undefined, ParamAccess.Write)),
                undefined,
                undefined
            ),
            new Param(
                new RegTupRefType(new Param(TYPE_DWORD, undefined, ParamAccess.Read)),
                undefined,
                undefined
            ),
        ],
        undefined
    ));
    static readonly GET_RANDOM = (OPCODES[0xf89a] = new Opcode(
        0xf89a,
        "get_random",
        undefined,
        [
            new Param(
                new RegTupRefType(
                    new Param(TYPE_DWORD, undefined, ParamAccess.Read),
                    new Param(TYPE_DWORD, undefined, ParamAccess.Read)
                ),
                undefined,
                undefined
            ),
            new Param(
                new RegTupRefType(new Param(TYPE_DWORD, undefined, ParamAccess.Write)),
                undefined,
                undefined
            ),
        ],
        undefined
    ));
    static readonly RESET_MAP = (OPCODES[0xf89b] = new Opcode(
        0xf89b,
        "reset_map",
        "Sets all registers to 0 and resets the quest.",
        [],
        undefined
    ));
    static readonly DISP_CHL_RETRY_MENU = (OPCODES[0xf89c] = new Opcode(
        0xf89c,
        "disp_chl_retry_menu",
        undefined,
        [
            new Param(
                new RegTupRefType(new Param(TYPE_ANY, undefined, ParamAccess.Write)),
                undefined,
                undefined
            ),
        ],
        undefined
    ));
    static readonly CHL_REVERSER = (OPCODES[0xf89d] = new Opcode(
        0xf89d,
        "chl_reverser",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F89E = (OPCODES[0xf89e] = new Opcode(
        0xf89e,
        "unknown_f89e",
        undefined,
        [new Param(TYPE_DWORD, undefined, undefined)],
        StackInteraction.Pop
    ));
    static readonly UNKNOWN_F89F = (OPCODES[0xf89f] = new Opcode(
        0xf89f,
        "unknown_f89f",
        undefined,
        [
            new Param(
                new RegTupRefType(new Param(TYPE_ANY, undefined, ParamAccess.Write)),
                undefined,
                undefined
            ),
        ],
        undefined
    ));
    static readonly UNKNOWN_F8A0 = (OPCODES[0xf8a0] = new Opcode(
        0xf8a0,
        "unknown_f8a0",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F8A1 = (OPCODES[0xf8a1] = new Opcode(
        0xf8a1,
        "unknown_f8a1",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F8A2 = (OPCODES[0xf8a2] = new Opcode(
        0xf8a2,
        "unknown_f8a2",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F8A3 = (OPCODES[0xf8a3] = new Opcode(
        0xf8a3,
        "unknown_f8a3",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F8A4 = (OPCODES[0xf8a4] = new Opcode(
        0xf8a4,
        "unknown_f8a4",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F8A5 = (OPCODES[0xf8a5] = new Opcode(
        0xf8a5,
        "unknown_f8a5",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F8A6 = (OPCODES[0xf8a6] = new Opcode(
        0xf8a6,
        "unknown_f8a6",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F8A7 = (OPCODES[0xf8a7] = new Opcode(
        0xf8a7,
        "unknown_f8a7",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F8A8 = (OPCODES[0xf8a8] = new Opcode(
        0xf8a8,
        "unknown_f8a8",
        undefined,
        [new Param(TYPE_DWORD, undefined, undefined)],
        StackInteraction.Pop
    ));
    static readonly UNKNOWN_F8A9 = (OPCODES[0xf8a9] = new Opcode(
        0xf8a9,
        "unknown_f8a9",
        undefined,
        [
            new Param(
                new RegTupRefType(new Param(TYPE_ANY, undefined, ParamAccess.Write)),
                undefined,
                undefined
            ),
        ],
        undefined
    ));
    static readonly UNKNOWN_F8AA = (OPCODES[0xf8aa] = new Opcode(
        0xf8aa,
        "unknown_f8aa",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F8AB = (OPCODES[0xf8ab] = new Opcode(
        0xf8ab,
        "unknown_f8ab",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F8AC = (OPCODES[0xf8ac] = new Opcode(
        0xf8ac,
        "unknown_f8ac",
        undefined,
        [],
        undefined
    ));
    static readonly GET_NUMBER_OF_PLAYER2 = (OPCODES[0xf8ad] = new Opcode(
        0xf8ad,
        "get_number_of_player2",
        undefined,
        [
            new Param(
                new RegTupRefType(new Param(TYPE_ANY, undefined, ParamAccess.Write)),
                undefined,
                undefined
            ),
        ],
        undefined
    ));
    static readonly UNKNOWN_F8AE = (OPCODES[0xf8ae] = new Opcode(
        0xf8ae,
        "unknown_f8ae",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F8AF = (OPCODES[0xf8af] = new Opcode(
        0xf8af,
        "unknown_f8af",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F8B0 = (OPCODES[0xf8b0] = new Opcode(
        0xf8b0,
        "unknown_f8b0",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F8B1 = (OPCODES[0xf8b1] = new Opcode(
        0xf8b1,
        "unknown_f8b1",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F8B2 = (OPCODES[0xf8b2] = new Opcode(
        0xf8b2,
        "unknown_f8b2",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F8B3 = (OPCODES[0xf8b3] = new Opcode(
        0xf8b3,
        "unknown_f8b3",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F8B4 = (OPCODES[0xf8b4] = new Opcode(
        0xf8b4,
        "unknown_f8b4",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F8B5 = (OPCODES[0xf8b5] = new Opcode(
        0xf8b5,
        "unknown_f8b5",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F8B6 = (OPCODES[0xf8b6] = new Opcode(
        0xf8b6,
        "unknown_f8b6",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F8B7 = (OPCODES[0xf8b7] = new Opcode(
        0xf8b7,
        "unknown_f8b7",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F8B8 = (OPCODES[0xf8b8] = new Opcode(
        0xf8b8,
        "unknown_f8b8",
        undefined,
        [],
        undefined
    ));
    static readonly CHL_RECOVERY = (OPCODES[0xf8b9] = new Opcode(
        0xf8b9,
        "chl_recovery",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F8BA = (OPCODES[0xf8ba] = new Opcode(
        0xf8ba,
        "unknown_f8ba",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F8BB = (OPCODES[0xf8bb] = new Opcode(
        0xf8bb,
        "unknown_f8bb",
        undefined,
        [],
        undefined
    ));
    static readonly SET_EPISODE = (OPCODES[0xf8bc] = new Opcode(
        0xf8bc,
        "set_episode",
        undefined,
        [new Param(TYPE_DWORD, undefined, undefined)],
        undefined
    ));
    static readonly UNKNOWN_F8BD = (OPCODES[0xf8bd] = new Opcode(
        0xf8bd,
        "unknown_f8bd",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F8BE = (OPCODES[0xf8be] = new Opcode(
        0xf8be,
        "unknown_f8be",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F8BF = (OPCODES[0xf8bf] = new Opcode(
        0xf8bf,
        "unknown_f8bf",
        undefined,
        [],
        undefined
    ));
    static readonly FILE_DL_REQ = (OPCODES[0xf8c0] = new Opcode(
        0xf8c0,
        "file_dl_req",
        undefined,
        [new Param(TYPE_DWORD, undefined, undefined), new Param(TYPE_STRING, undefined, undefined)],
        StackInteraction.Pop
    ));
    static readonly GET_DL_STATUS = (OPCODES[0xf8c1] = new Opcode(
        0xf8c1,
        "get_dl_status",
        undefined,
        [
            new Param(
                new RegTupRefType(new Param(TYPE_ANY, undefined, ParamAccess.Write)),
                undefined,
                undefined
            ),
        ],
        undefined
    ));
    static readonly GBA_UNKNOWN4 = (OPCODES[0xf8c2] = new Opcode(
        0xf8c2,
        "gba_unknown4",
        undefined,
        [],
        undefined
    ));
    static readonly GET_GBA_STATE = (OPCODES[0xf8c3] = new Opcode(
        0xf8c3,
        "get_gba_state",
        undefined,
        [
            new Param(
                new RegTupRefType(new Param(TYPE_ANY, undefined, ParamAccess.Write)),
                undefined,
                undefined
            ),
        ],
        undefined
    ));
    static readonly UNKNOWN_F8C4 = (OPCODES[0xf8c4] = new Opcode(
        0xf8c4,
        "unknown_f8c4",
        undefined,
        [
            new Param(
                new RegTupRefType(new Param(TYPE_ANY, undefined, ParamAccess.Write)),
                undefined,
                undefined
            ),
        ],
        undefined
    ));
    static readonly UNKNOWN_F8C5 = (OPCODES[0xf8c5] = new Opcode(
        0xf8c5,
        "unknown_f8c5",
        undefined,
        [
            new Param(
                new RegTupRefType(new Param(TYPE_ANY, undefined, ParamAccess.Write)),
                undefined,
                undefined
            ),
        ],
        undefined
    ));
    static readonly QEXIT = (OPCODES[0xf8c6] = new Opcode(
        0xf8c6,
        "qexit",
        undefined,
        [],
        undefined
    ));
    static readonly USE_ANIMATION = (OPCODES[0xf8c7] = new Opcode(
        0xf8c7,
        "use_animation",
        undefined,
        [
            new Param(
                new RegTupRefType(new Param(TYPE_DWORD, "Player slot.", ParamAccess.Read)),
                undefined,
                undefined
            ),
            new Param(
                new RegTupRefType(
                    new Param(TYPE_DWORD, "Animation ID.", ParamAccess.Read),
                    new Param(
                        TYPE_DWORD,
                        "Animation duration in number of frames.",
                        ParamAccess.Read
                    )
                ),
                undefined,
                undefined
            ),
        ],
        undefined
    ));
    static readonly STOP_ANIMATION = (OPCODES[0xf8c8] = new Opcode(
        0xf8c8,
        "stop_animation",
        undefined,
        [
            new Param(
                new RegTupRefType(new Param(TYPE_DWORD, "Player slot.", ParamAccess.Read)),
                undefined,
                undefined
            ),
        ],
        undefined
    ));
    static readonly RUN_TO_COORD = (OPCODES[0xf8c9] = new Opcode(
        0xf8c9,
        "run_to_coord",
        undefined,
        [
            new Param(
                new RegTupRefType(new Param(TYPE_ANY, undefined, ParamAccess.Read)),
                undefined,
                undefined
            ),
            new Param(
                new RegTupRefType(new Param(TYPE_ANY, undefined, ParamAccess.Read)),
                undefined,
                undefined
            ),
        ],
        undefined
    ));
    static readonly SET_SLOT_INVINCIBLE = (OPCODES[0xf8ca] = new Opcode(
        0xf8ca,
        "set_slot_invincible",
        undefined,
        [
            new Param(
                new RegTupRefType(new Param(TYPE_DWORD, "Player slot.", ParamAccess.Read)),
                undefined,
                undefined
            ),
            new Param(
                new RegTupRefType(new Param(TYPE_DWORD, undefined, ParamAccess.Read)),
                undefined,
                undefined
            ),
        ],
        undefined
    ));
    static readonly UNKNOWN_F8CB = (OPCODES[0xf8cb] = new Opcode(
        0xf8cb,
        "unknown_f8cb",
        undefined,
        [
            new Param(
                new RegTupRefType(new Param(TYPE_ANY, undefined, ParamAccess.Read)),
                undefined,
                undefined
            ),
        ],
        undefined
    ));
    static readonly SET_SLOT_POISON = (OPCODES[0xf8cc] = new Opcode(
        0xf8cc,
        "set_slot_poison",
        undefined,
        [
            new Param(
                new RegTupRefType(new Param(TYPE_DWORD, "Player slot.", ParamAccess.Read)),
                undefined,
                undefined
            ),
        ],
        undefined
    ));
    static readonly SET_SLOT_PARALYZE = (OPCODES[0xf8cd] = new Opcode(
        0xf8cd,
        "set_slot_paralyze",
        undefined,
        [
            new Param(
                new RegTupRefType(new Param(TYPE_DWORD, "Player slot.", ParamAccess.Read)),
                undefined,
                undefined
            ),
        ],
        undefined
    ));
    static readonly SET_SLOT_SHOCK = (OPCODES[0xf8ce] = new Opcode(
        0xf8ce,
        "set_slot_shock",
        undefined,
        [
            new Param(
                new RegTupRefType(new Param(TYPE_DWORD, "Player slot.", ParamAccess.Read)),
                undefined,
                undefined
            ),
        ],
        undefined
    ));
    static readonly SET_SLOT_FREEZE = (OPCODES[0xf8cf] = new Opcode(
        0xf8cf,
        "set_slot_freeze",
        undefined,
        [
            new Param(
                new RegTupRefType(new Param(TYPE_DWORD, "Player slot.", ParamAccess.Read)),
                undefined,
                undefined
            ),
        ],
        undefined
    ));
    static readonly SET_SLOT_SLOW = (OPCODES[0xf8d0] = new Opcode(
        0xf8d0,
        "set_slot_slow",
        undefined,
        [
            new Param(
                new RegTupRefType(new Param(TYPE_DWORD, "Player slot.", ParamAccess.Read)),
                undefined,
                undefined
            ),
        ],
        undefined
    ));
    static readonly SET_SLOT_CONFUSE = (OPCODES[0xf8d1] = new Opcode(
        0xf8d1,
        "set_slot_confuse",
        undefined,
        [
            new Param(
                new RegTupRefType(new Param(TYPE_DWORD, "Player slot.", ParamAccess.Read)),
                undefined,
                undefined
            ),
        ],
        undefined
    ));
    static readonly SET_SLOT_SHIFTA = (OPCODES[0xf8d2] = new Opcode(
        0xf8d2,
        "set_slot_shifta",
        undefined,
        [
            new Param(
                new RegTupRefType(new Param(TYPE_DWORD, "Player slot.", ParamAccess.Read)),
                undefined,
                undefined
            ),
        ],
        undefined
    ));
    static readonly SET_SLOT_DEBAND = (OPCODES[0xf8d3] = new Opcode(
        0xf8d3,
        "set_slot_deband",
        undefined,
        [
            new Param(
                new RegTupRefType(new Param(TYPE_DWORD, "Player slot.", ParamAccess.Read)),
                undefined,
                undefined
            ),
        ],
        undefined
    ));
    static readonly SET_SLOT_JELLEN = (OPCODES[0xf8d4] = new Opcode(
        0xf8d4,
        "set_slot_jellen",
        undefined,
        [
            new Param(
                new RegTupRefType(new Param(TYPE_DWORD, "Player slot.", ParamAccess.Read)),
                undefined,
                undefined
            ),
        ],
        undefined
    ));
    static readonly SET_SLOT_ZALURE = (OPCODES[0xf8d5] = new Opcode(
        0xf8d5,
        "set_slot_zalure",
        undefined,
        [
            new Param(
                new RegTupRefType(new Param(TYPE_DWORD, "Player slot.", ParamAccess.Read)),
                undefined,
                undefined
            ),
        ],
        undefined
    ));
    static readonly FLETI_FIXED_CAMERA = (OPCODES[0xf8d6] = new Opcode(
        0xf8d6,
        "fleti_fixed_camera",
        undefined,
        [
            new Param(
                new RegTupRefType(new Param(TYPE_ANY, undefined, ParamAccess.Read)),
                undefined,
                undefined
            ),
        ],
        StackInteraction.Pop
    ));
    static readonly FLETI_LOCKED_CAMERA = (OPCODES[0xf8d7] = new Opcode(
        0xf8d7,
        "fleti_locked_camera",
        undefined,
        [
            new Param(TYPE_DWORD, undefined, undefined),
            new Param(
                new RegTupRefType(new Param(TYPE_ANY, undefined, ParamAccess.Read)),
                undefined,
                undefined
            ),
        ],
        StackInteraction.Pop
    ));
    static readonly DEFAULT_CAMERA_POS2 = (OPCODES[0xf8d8] = new Opcode(
        0xf8d8,
        "default_camera_pos2",
        undefined,
        [],
        undefined
    ));
    static readonly SET_MOTION_BLUR = (OPCODES[0xf8d9] = new Opcode(
        0xf8d9,
        "set_motion_blur",
        undefined,
        [],
        undefined
    ));
    static readonly SET_SCREEN_BW = (OPCODES[0xf8da] = new Opcode(
        0xf8da,
        "set_screen_bw",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F8DB = (OPCODES[0xf8db] = new Opcode(
        0xf8db,
        "unknown_f8db",
        undefined,
        [
            new Param(TYPE_DWORD, undefined, undefined),
            new Param(TYPE_DWORD, undefined, undefined),
            new Param(TYPE_DWORD, undefined, undefined),
            new Param(TYPE_DWORD, undefined, undefined),
            new Param(
                new RegTupRefType(new Param(TYPE_ANY, undefined, ParamAccess.Read)),
                undefined,
                undefined
            ),
            new Param(TYPE_WORD, undefined, undefined),
        ],
        StackInteraction.Pop
    ));
    static readonly NPC_ACTION_STRING = (OPCODES[0xf8dc] = new Opcode(
        0xf8dc,
        "npc_action_string",
        undefined,
        [
            new Param(
                new RegTupRefType(new Param(TYPE_ANY, undefined, ParamAccess.Read)),
                undefined,
                undefined
            ),
            new Param(
                new RegTupRefType(new Param(TYPE_ANY, undefined, ParamAccess.Read)),
                undefined,
                undefined
            ),
            new Param(TYPE_S_LABEL, undefined, undefined),
        ],
        undefined
    ));
    static readonly GET_PAD_COND = (OPCODES[0xf8dd] = new Opcode(
        0xf8dd,
        "get_pad_cond",
        undefined,
        [
            new Param(
                new RegTupRefType(new Param(TYPE_ANY, undefined, ParamAccess.Write)),
                undefined,
                undefined
            ),
            new Param(
                new RegTupRefType(new Param(TYPE_ANY, undefined, ParamAccess.Write)),
                undefined,
                undefined
            ),
        ],
        undefined
    ));
    static readonly GET_BUTTON_COND = (OPCODES[0xf8de] = new Opcode(
        0xf8de,
        "get_button_cond",
        undefined,
        [
            new Param(
                new RegTupRefType(new Param(TYPE_ANY, undefined, ParamAccess.Write)),
                undefined,
                undefined
            ),
            new Param(
                new RegTupRefType(new Param(TYPE_ANY, undefined, ParamAccess.Write)),
                undefined,
                undefined
            ),
        ],
        undefined
    ));
    static readonly FREEZE_ENEMIES = (OPCODES[0xf8df] = new Opcode(
        0xf8df,
        "freeze_enemies",
        undefined,
        [],
        undefined
    ));
    static readonly UNFREEZE_ENEMIES = (OPCODES[0xf8e0] = new Opcode(
        0xf8e0,
        "unfreeze_enemies",
        undefined,
        [],
        undefined
    ));
    static readonly FREEZE_EVERYTHING = (OPCODES[0xf8e1] = new Opcode(
        0xf8e1,
        "freeze_everything",
        undefined,
        [],
        undefined
    ));
    static readonly UNFREEZE_EVERYTHING = (OPCODES[0xf8e2] = new Opcode(
        0xf8e2,
        "unfreeze_everything",
        undefined,
        [],
        undefined
    ));
    static readonly RESTORE_HP = (OPCODES[0xf8e3] = new Opcode(
        0xf8e3,
        "restore_hp",
        undefined,
        [
            new Param(
                new RegTupRefType(new Param(TYPE_ANY, undefined, ParamAccess.Read)),
                undefined,
                undefined
            ),
        ],
        undefined
    ));
    static readonly RESTORE_TP = (OPCODES[0xf8e4] = new Opcode(
        0xf8e4,
        "restore_tp",
        undefined,
        [
            new Param(
                new RegTupRefType(new Param(TYPE_ANY, undefined, ParamAccess.Read)),
                undefined,
                undefined
            ),
        ],
        undefined
    ));
    static readonly CLOSE_CHAT_BUBBLE = (OPCODES[0xf8e5] = new Opcode(
        0xf8e5,
        "close_chat_bubble",
        undefined,
        [
            new Param(
                new RegTupRefType(new Param(TYPE_ANY, undefined, ParamAccess.Read)),
                undefined,
                undefined
            ),
        ],
        undefined
    ));
    static readonly MOVE_COORDS_OBJECT = (OPCODES[0xf8e6] = new Opcode(
        0xf8e6,
        "move_coords_object",
        undefined,
        [
            new Param(
                new RegTupRefType(new Param(TYPE_ANY, undefined, ParamAccess.Read)),
                undefined,
                undefined
            ),
            new Param(
                new RegTupRefType(new Param(TYPE_ANY, undefined, ParamAccess.Read)),
                undefined,
                undefined
            ),
        ],
        undefined
    ));
    static readonly AT_COORDS_CALL_EX = (OPCODES[0xf8e7] = new Opcode(
        0xf8e7,
        "at_coords_call_ex",
        undefined,
        [
            new Param(
                new RegTupRefType(new Param(TYPE_ANY, undefined, ParamAccess.Read)),
                undefined,
                undefined
            ),
            new Param(
                new RegTupRefType(new Param(TYPE_ANY, undefined, ParamAccess.Read)),
                undefined,
                undefined
            ),
        ],
        undefined
    ));
    static readonly UNKNOWN_F8E8 = (OPCODES[0xf8e8] = new Opcode(
        0xf8e8,
        "unknown_f8e8",
        undefined,
        [
            new Param(
                new RegTupRefType(new Param(TYPE_ANY, undefined, ParamAccess.Read)),
                undefined,
                undefined
            ),
            new Param(
                new RegTupRefType(new Param(TYPE_ANY, undefined, ParamAccess.Read)),
                undefined,
                undefined
            ),
        ],
        undefined
    ));
    static readonly UNKNOWN_F8E9 = (OPCODES[0xf8e9] = new Opcode(
        0xf8e9,
        "unknown_f8e9",
        undefined,
        [
            new Param(
                new RegTupRefType(new Param(TYPE_ANY, undefined, ParamAccess.Read)),
                undefined,
                undefined
            ),
            new Param(
                new RegTupRefType(new Param(TYPE_ANY, undefined, ParamAccess.Read)),
                undefined,
                undefined
            ),
        ],
        undefined
    ));
    static readonly UNKNOWN_F8EA = (OPCODES[0xf8ea] = new Opcode(
        0xf8ea,
        "unknown_f8ea",
        undefined,
        [
            new Param(
                new RegTupRefType(new Param(TYPE_ANY, undefined, ParamAccess.Read)),
                undefined,
                undefined
            ),
            new Param(
                new RegTupRefType(new Param(TYPE_ANY, undefined, ParamAccess.Read)),
                undefined,
                undefined
            ),
        ],
        undefined
    ));
    static readonly UNKNOWN_F8EB = (OPCODES[0xf8eb] = new Opcode(
        0xf8eb,
        "unknown_f8eb",
        undefined,
        [
            new Param(
                new RegTupRefType(new Param(TYPE_ANY, undefined, ParamAccess.Read)),
                undefined,
                undefined
            ),
            new Param(
                new RegTupRefType(new Param(TYPE_ANY, undefined, ParamAccess.Read)),
                undefined,
                undefined
            ),
        ],
        undefined
    ));
    static readonly UNKNOWN_F8EC = (OPCODES[0xf8ec] = new Opcode(
        0xf8ec,
        "unknown_f8ec",
        undefined,
        [
            new Param(
                new RegTupRefType(new Param(TYPE_ANY, undefined, ParamAccess.Read)),
                undefined,
                undefined
            ),
            new Param(
                new RegTupRefType(new Param(TYPE_ANY, undefined, ParamAccess.Read)),
                undefined,
                undefined
            ),
        ],
        undefined
    ));
    static readonly ANIMATION_CHECK = (OPCODES[0xf8ed] = new Opcode(
        0xf8ed,
        "animation_check",
        undefined,
        [
            new Param(
                new RegTupRefType(new Param(TYPE_ANY, undefined, ParamAccess.Read)),
                undefined,
                undefined
            ),
            new Param(
                new RegTupRefType(new Param(TYPE_ANY, undefined, ParamAccess.Read)),
                undefined,
                undefined
            ),
        ],
        undefined
    ));
    static readonly CALL_IMAGE_DATA = (OPCODES[0xf8ee] = new Opcode(
        0xf8ee,
        "call_image_data",
        undefined,
        [new Param(TYPE_DWORD, undefined, undefined), new Param(TYPE_WORD, undefined, undefined)],
        StackInteraction.Pop
    ));
    static readonly UNKNOWN_F8EF = (OPCODES[0xf8ef] = new Opcode(
        0xf8ef,
        "unknown_f8ef",
        undefined,
        [],
        undefined
    ));
    static readonly TURN_OFF_BGM_P2 = (OPCODES[0xf8f0] = new Opcode(
        0xf8f0,
        "turn_off_bgm_p2",
        undefined,
        [],
        undefined
    ));
    static readonly TURN_ON_BGM_P2 = (OPCODES[0xf8f1] = new Opcode(
        0xf8f1,
        "turn_on_bgm_p2",
        undefined,
        [],
        undefined
    ));
    static readonly LOAD_UNK_DATA = (OPCODES[0xf8f2] = new Opcode(
        0xf8f2,
        "load_unk_data",
        undefined,
        [
            new Param(TYPE_DWORD, undefined, undefined),
            new Param(TYPE_DWORD, undefined, undefined),
            new Param(TYPE_DWORD, undefined, undefined),
            new Param(TYPE_DWORD, undefined, undefined),
            new Param(
                new RegTupRefType(new Param(TYPE_ANY, undefined, ParamAccess.Read)),
                undefined,
                undefined
            ),
            new Param(TYPE_D_LABEL, undefined, undefined),
        ],
        StackInteraction.Pop
    ));
    static readonly PARTICLE2 = (OPCODES[0xf8f3] = new Opcode(
        0xf8f3,
        "particle2",
        undefined,
        [
            new Param(
                new RegTupRefType(new Param(TYPE_ANY, undefined, ParamAccess.Read)),
                undefined,
                undefined
            ),
            new Param(TYPE_DWORD, undefined, undefined),
            new Param(TYPE_FLOAT, undefined, undefined),
        ],
        StackInteraction.Pop
    ));
    static readonly UNKNOWN_F8F4 = (OPCODES[0xf8f4] = new Opcode(
        0xf8f4,
        "unknown_f8f4",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F8F5 = (OPCODES[0xf8f5] = new Opcode(
        0xf8f5,
        "unknown_f8f5",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F8F6 = (OPCODES[0xf8f6] = new Opcode(
        0xf8f6,
        "unknown_f8f6",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F8F7 = (OPCODES[0xf8f7] = new Opcode(
        0xf8f7,
        "unknown_f8f7",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F8F8 = (OPCODES[0xf8f8] = new Opcode(
        0xf8f8,
        "unknown_f8f8",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F8F9 = (OPCODES[0xf8f9] = new Opcode(
        0xf8f9,
        "unknown_f8f9",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F8FA = (OPCODES[0xf8fa] = new Opcode(
        0xf8fa,
        "unknown_f8fa",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F8FB = (OPCODES[0xf8fb] = new Opcode(
        0xf8fb,
        "unknown_f8fb",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F8FC = (OPCODES[0xf8fc] = new Opcode(
        0xf8fc,
        "unknown_f8fc",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F8FD = (OPCODES[0xf8fd] = new Opcode(
        0xf8fd,
        "unknown_f8fd",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F8FE = (OPCODES[0xf8fe] = new Opcode(
        0xf8fe,
        "unknown_f8fe",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F8FF = (OPCODES[0xf8ff] = new Opcode(
        0xf8ff,
        "unknown_f8ff",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F900 = (OPCODES[0xf900] = new Opcode(
        0xf900,
        "unknown_f900",
        undefined,
        [],
        undefined
    ));
    static readonly DEC2FLOAT = (OPCODES[0xf901] = new Opcode(
        0xf901,
        "dec2float",
        undefined,
        [
            new Param(
                new RegTupRefType(new Param(TYPE_FLOAT, undefined, ParamAccess.Write)),
                undefined,
                undefined
            ),
            new Param(
                new RegTupRefType(new Param(TYPE_DWORD, undefined, ParamAccess.Read)),
                undefined,
                undefined
            ),
        ],
        undefined
    ));
    static readonly FLOAT2DEC = (OPCODES[0xf902] = new Opcode(
        0xf902,
        "float2dec",
        undefined,
        [
            new Param(
                new RegTupRefType(new Param(TYPE_DWORD, undefined, ParamAccess.Write)),
                undefined,
                undefined
            ),
            new Param(
                new RegTupRefType(new Param(TYPE_FLOAT, undefined, ParamAccess.Read)),
                undefined,
                undefined
            ),
        ],
        undefined
    ));
    static readonly FLET = (OPCODES[0xf903] = new Opcode(
        0xf903,
        "flet",
        undefined,
        [
            new Param(
                new RegTupRefType(new Param(TYPE_FLOAT, undefined, ParamAccess.Write)),
                undefined,
                undefined
            ),
            new Param(
                new RegTupRefType(new Param(TYPE_FLOAT, undefined, ParamAccess.Read)),
                undefined,
                undefined
            ),
        ],
        undefined
    ));
    static readonly FLETI = (OPCODES[0xf904] = new Opcode(
        0xf904,
        "fleti",
        undefined,
        [
            new Param(
                new RegTupRefType(new Param(TYPE_FLOAT, undefined, ParamAccess.Write)),
                undefined,
                undefined
            ),
            new Param(TYPE_FLOAT, undefined, undefined),
        ],
        undefined
    ));
    static readonly UNKNOWN_F905 = (OPCODES[0xf905] = new Opcode(
        0xf905,
        "unknown_f905",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F906 = (OPCODES[0xf906] = new Opcode(
        0xf906,
        "unknown_f906",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F907 = (OPCODES[0xf907] = new Opcode(
        0xf907,
        "unknown_f907",
        undefined,
        [],
        undefined
    ));
    static readonly FADD = (OPCODES[0xf908] = new Opcode(
        0xf908,
        "fadd",
        undefined,
        [
            new Param(
                new RegTupRefType(new Param(TYPE_FLOAT, undefined, ParamAccess.Write)),
                undefined,
                undefined
            ),
            new Param(
                new RegTupRefType(new Param(TYPE_FLOAT, undefined, ParamAccess.Read)),
                undefined,
                undefined
            ),
        ],
        undefined
    ));
    static readonly FADDI = (OPCODES[0xf909] = new Opcode(
        0xf909,
        "faddi",
        undefined,
        [
            new Param(
                new RegTupRefType(new Param(TYPE_FLOAT, undefined, ParamAccess.Write)),
                undefined,
                undefined
            ),
            new Param(TYPE_FLOAT, undefined, undefined),
        ],
        undefined
    ));
    static readonly FSUB = (OPCODES[0xf90a] = new Opcode(
        0xf90a,
        "fsub",
        undefined,
        [
            new Param(
                new RegTupRefType(new Param(TYPE_FLOAT, undefined, ParamAccess.Write)),
                undefined,
                undefined
            ),
            new Param(
                new RegTupRefType(new Param(TYPE_FLOAT, undefined, ParamAccess.Read)),
                undefined,
                undefined
            ),
        ],
        undefined
    ));
    static readonly FSUBI = (OPCODES[0xf90b] = new Opcode(
        0xf90b,
        "fsubi",
        undefined,
        [
            new Param(
                new RegTupRefType(new Param(TYPE_FLOAT, undefined, ParamAccess.Write)),
                undefined,
                undefined
            ),
            new Param(TYPE_FLOAT, undefined, undefined),
        ],
        undefined
    ));
    static readonly FMUL = (OPCODES[0xf90c] = new Opcode(
        0xf90c,
        "fmul",
        undefined,
        [
            new Param(
                new RegTupRefType(new Param(TYPE_FLOAT, undefined, ParamAccess.Write)),
                undefined,
                undefined
            ),
            new Param(
                new RegTupRefType(new Param(TYPE_FLOAT, undefined, ParamAccess.Read)),
                undefined,
                undefined
            ),
        ],
        undefined
    ));
    static readonly FMULI = (OPCODES[0xf90d] = new Opcode(
        0xf90d,
        "fmuli",
        undefined,
        [
            new Param(
                new RegTupRefType(new Param(TYPE_FLOAT, undefined, ParamAccess.Write)),
                undefined,
                undefined
            ),
            new Param(TYPE_FLOAT, undefined, undefined),
        ],
        undefined
    ));
    static readonly FDIV = (OPCODES[0xf90e] = new Opcode(
        0xf90e,
        "fdiv",
        undefined,
        [
            new Param(
                new RegTupRefType(new Param(TYPE_FLOAT, undefined, ParamAccess.Write)),
                undefined,
                undefined
            ),
            new Param(
                new RegTupRefType(new Param(TYPE_FLOAT, undefined, ParamAccess.Read)),
                undefined,
                undefined
            ),
        ],
        undefined
    ));
    static readonly FDIVI = (OPCODES[0xf90f] = new Opcode(
        0xf90f,
        "fdivi",
        undefined,
        [
            new Param(
                new RegTupRefType(new Param(TYPE_FLOAT, undefined, ParamAccess.Write)),
                undefined,
                undefined
            ),
            new Param(TYPE_FLOAT, undefined, undefined),
        ],
        undefined
    ));
    static readonly GET_UNKNOWN_COUNT = (OPCODES[0xf910] = new Opcode(
        0xf910,
        "get_unknown_count",
        undefined,
        [
            new Param(TYPE_DWORD, undefined, undefined),
            new Param(
                new RegTupRefType(new Param(TYPE_DWORD, undefined, ParamAccess.Write)),
                undefined,
                undefined
            ),
        ],
        StackInteraction.Pop
    ));
    static readonly GET_STACKABLE_ITEM_COUNT = (OPCODES[0xf911] = new Opcode(
        0xf911,
        "get_stackable_item_count",
        undefined,
        [
            new Param(
                new RegTupRefType(
                    new Param(TYPE_DWORD, "Player slot.", ParamAccess.Read),
                    new Param(TYPE_DWORD, undefined, ParamAccess.Read),
                    new Param(TYPE_DWORD, undefined, ParamAccess.Read),
                    new Param(TYPE_DWORD, undefined, ParamAccess.Read)
                ),
                undefined,
                undefined
            ),
            new Param(
                new RegTupRefType(new Param(TYPE_DWORD, undefined, ParamAccess.Write)),
                undefined,
                undefined
            ),
        ],
        undefined
    ));
    static readonly FREEZE_AND_HIDE_EQUIP = (OPCODES[0xf912] = new Opcode(
        0xf912,
        "freeze_and_hide_equip",
        undefined,
        [],
        undefined
    ));
    static readonly THAW_AND_SHOW_EQUIP = (OPCODES[0xf913] = new Opcode(
        0xf913,
        "thaw_and_show_equip",
        undefined,
        [],
        undefined
    ));
    static readonly SET_PALETTEX_CALLBACK = (OPCODES[0xf914] = new Opcode(
        0xf914,
        "set_palettex_callback",
        undefined,
        [
            new Param(TYPE_DWORD, "Player slot.", undefined),
            new Param(TYPE_I_LABEL, undefined, undefined),
        ],
        StackInteraction.Pop
    ));
    static readonly ACTIVATE_PALETTEX = (OPCODES[0xf915] = new Opcode(
        0xf915,
        "activate_palettex",
        undefined,
        [new Param(TYPE_DWORD, "Player slot.", undefined)],
        StackInteraction.Pop
    ));
    static readonly ENABLE_PALETTEX = (OPCODES[0xf916] = new Opcode(
        0xf916,
        "enable_palettex",
        undefined,
        [new Param(TYPE_DWORD, "Player slot.", undefined)],
        StackInteraction.Pop
    ));
    static readonly RESTORE_PALETTEX = (OPCODES[0xf917] = new Opcode(
        0xf917,
        "restore_palettex",
        undefined,
        [new Param(TYPE_DWORD, "Player slot.", undefined)],
        StackInteraction.Pop
    ));
    static readonly DISABLE_PALETTEX = (OPCODES[0xf918] = new Opcode(
        0xf918,
        "disable_palettex",
        undefined,
        [new Param(TYPE_DWORD, "Player slot.", undefined)],
        StackInteraction.Pop
    ));
    static readonly GET_PALETTEX_ACTIVATED = (OPCODES[0xf919] = new Opcode(
        0xf919,
        "get_palettex_activated",
        undefined,
        [
            new Param(TYPE_DWORD, "Player slot.", undefined),
            new Param(
                new RegTupRefType(new Param(TYPE_DWORD, undefined, ParamAccess.Write)),
                undefined,
                undefined
            ),
        ],
        StackInteraction.Pop
    ));
    static readonly GET_UNKNOWN_PALETTEX_STATUS = (OPCODES[0xf91a] = new Opcode(
        0xf91a,
        "get_unknown_palettex_status",
        undefined,
        [
            new Param(TYPE_DWORD, undefined, undefined),
            new Param(
                new RegTupRefType(new Param(TYPE_DWORD, undefined, ParamAccess.Write)),
                undefined,
                undefined
            ),
        ],
        StackInteraction.Pop
    ));
    static readonly DISABLE_MOVEMENT2 = (OPCODES[0xf91b] = new Opcode(
        0xf91b,
        "disable_movement2",
        undefined,
        [new Param(TYPE_DWORD, "Player slot.", undefined)],
        StackInteraction.Pop
    ));
    static readonly ENABLE_MOVEMENT2 = (OPCODES[0xf91c] = new Opcode(
        0xf91c,
        "enable_movement2",
        undefined,
        [new Param(TYPE_DWORD, "Player slot.", undefined)],
        StackInteraction.Pop
    ));
    static readonly GET_TIME_PLAYED = (OPCODES[0xf91d] = new Opcode(
        0xf91d,
        "get_time_played",
        undefined,
        [
            new Param(
                new RegTupRefType(new Param(TYPE_DWORD, undefined, ParamAccess.Write)),
                undefined,
                undefined
            ),
        ],
        undefined
    ));
    static readonly GET_GUILDCARD_TOTAL = (OPCODES[0xf91e] = new Opcode(
        0xf91e,
        "get_guildcard_total",
        undefined,
        [
            new Param(
                new RegTupRefType(new Param(TYPE_DWORD, undefined, ParamAccess.Write)),
                undefined,
                undefined
            ),
        ],
        undefined
    ));
    static readonly GET_SLOT_MESETA = (OPCODES[0xf91f] = new Opcode(
        0xf91f,
        "get_slot_meseta",
        undefined,
        [
            new Param(
                new RegTupRefType(new Param(TYPE_DWORD, undefined, ParamAccess.Write)),
                undefined,
                undefined
            ),
        ],
        undefined
    ));
    static readonly GET_PLAYER_LEVEL = (OPCODES[0xf920] = new Opcode(
        0xf920,
        "get_player_level",
        undefined,
        [
            new Param(TYPE_DWORD, "Player slot.", undefined),
            new Param(
                new RegTupRefType(new Param(TYPE_DWORD, undefined, ParamAccess.Write)),
                undefined,
                undefined
            ),
        ],
        StackInteraction.Pop
    ));
    static readonly GET_SECTION_ID = (OPCODES[0xf921] = new Opcode(
        0xf921,
        "get_section_id",
        undefined,
        [
            new Param(TYPE_DWORD, "Player slot.", undefined),
            new Param(
                new RegTupRefType(new Param(TYPE_DWORD, undefined, ParamAccess.Write)),
                undefined,
                undefined
            ),
        ],
        StackInteraction.Pop
    ));
    static readonly GET_PLAYER_HP = (OPCODES[0xf922] = new Opcode(
        0xf922,
        "get_player_hp",
        undefined,
        [
            new Param(TYPE_DWORD, "Player slot.", undefined),
            new Param(
                new RegTupRefType(
                    new Param(TYPE_DWORD, "Maximum HP.", ParamAccess.Write),
                    new Param(TYPE_DWORD, "Current HP.", ParamAccess.Write),
                    new Param(TYPE_DWORD, "Maximum TP.", ParamAccess.Write),
                    new Param(TYPE_DWORD, "Current TP.", ParamAccess.Write)
                ),
                undefined,
                undefined
            ),
        ],
        StackInteraction.Pop
    ));
    static readonly GET_FLOOR_NUMBER = (OPCODES[0xf923] = new Opcode(
        0xf923,
        "get_floor_number",
        undefined,
        [
            new Param(TYPE_DWORD, "Player slot.", undefined),
            new Param(
                new RegTupRefType(new Param(TYPE_DWORD, undefined, ParamAccess.Write)),
                undefined,
                undefined
            ),
        ],
        StackInteraction.Pop
    ));
    static readonly GET_COORD_PLAYER_DETECT = (OPCODES[0xf924] = new Opcode(
        0xf924,
        "get_coord_player_detect",
        undefined,
        [
            new Param(TYPE_DWORD, "Player slot.", undefined),
            new Param(
                new RegTupRefType(new Param(TYPE_DWORD, undefined, ParamAccess.Write)),
                undefined,
                undefined
            ),
        ],
        undefined
    ));
    static readonly READ_GLOBAL_FLAG = (OPCODES[0xf925] = new Opcode(
        0xf925,
        "read_global_flag",
        undefined,
        [
            new Param(TYPE_DWORD, undefined, undefined),
            new Param(
                new RegTupRefType(new Param(TYPE_DWORD, undefined, ParamAccess.Write)),
                undefined,
                undefined
            ),
        ],
        StackInteraction.Pop
    ));
    static readonly WRITE_GLOBAL_FLAG = (OPCODES[0xf926] = new Opcode(
        0xf926,
        "write_global_flag",
        undefined,
        [new Param(TYPE_DWORD, undefined, undefined), new Param(TYPE_DWORD, undefined, undefined)],
        StackInteraction.Pop
    ));
    static readonly UNKNOWN_F927 = (OPCODES[0xf927] = new Opcode(
        0xf927,
        "unknown_f927",
        undefined,
        [
            new Param(
                new RegTupRefType(new Param(TYPE_ANY, undefined, ParamAccess.Write)),
                undefined,
                undefined
            ),
            new Param(
                new RegTupRefType(new Param(TYPE_ANY, undefined, ParamAccess.Write)),
                undefined,
                undefined
            ),
        ],
        undefined
    ));
    static readonly FLOOR_PLAYER_DETECT = (OPCODES[0xf928] = new Opcode(
        0xf928,
        "floor_player_detect",
        undefined,
        [
            new Param(
                new RegTupRefType(
                    new Param(TYPE_DWORD, undefined, ParamAccess.Write),
                    new Param(TYPE_DWORD, undefined, ParamAccess.Write),
                    new Param(TYPE_DWORD, undefined, ParamAccess.Write),
                    new Param(TYPE_DWORD, undefined, ParamAccess.Write)
                ),
                undefined,
                undefined
            ),
        ],
        undefined
    ));
    static readonly READ_DISK_FILE = (OPCODES[0xf929] = new Opcode(
        0xf929,
        "read_disk_file",
        undefined,
        [new Param(TYPE_STRING, undefined, undefined)],
        StackInteraction.Pop
    ));
    static readonly OPEN_PACK_SELECT = (OPCODES[0xf92a] = new Opcode(
        0xf92a,
        "open_pack_select",
        undefined,
        [],
        undefined
    ));
    static readonly ITEM_SELECT = (OPCODES[0xf92b] = new Opcode(
        0xf92b,
        "item_select",
        undefined,
        [
            new Param(
                new RegTupRefType(new Param(TYPE_ANY, undefined, ParamAccess.Write)),
                undefined,
                undefined
            ),
        ],
        undefined
    ));
    static readonly GET_ITEM_ID = (OPCODES[0xf92c] = new Opcode(
        0xf92c,
        "get_item_id",
        undefined,
        [
            new Param(
                new RegTupRefType(new Param(TYPE_ANY, undefined, ParamAccess.Write)),
                undefined,
                undefined
            ),
        ],
        undefined
    ));
    static readonly COLOR_CHANGE = (OPCODES[0xf92d] = new Opcode(
        0xf92d,
        "color_change",
        undefined,
        [
            new Param(TYPE_DWORD, undefined, undefined),
            new Param(TYPE_DWORD, undefined, undefined),
            new Param(TYPE_DWORD, undefined, undefined),
            new Param(TYPE_DWORD, undefined, undefined),
            new Param(TYPE_DWORD, undefined, undefined),
        ],
        StackInteraction.Pop
    ));
    static readonly SEND_STATISTIC = (OPCODES[0xf92e] = new Opcode(
        0xf92e,
        "send_statistic",
        undefined,
        [
            new Param(TYPE_DWORD, undefined, undefined),
            new Param(TYPE_DWORD, undefined, undefined),
            new Param(TYPE_DWORD, undefined, undefined),
            new Param(TYPE_DWORD, undefined, undefined),
            new Param(TYPE_DWORD, undefined, undefined),
            new Param(TYPE_DWORD, undefined, undefined),
            new Param(TYPE_DWORD, undefined, undefined),
            new Param(TYPE_DWORD, undefined, undefined),
        ],
        StackInteraction.Pop
    ));
    static readonly UNKNOWN_F92F = (OPCODES[0xf92f] = new Opcode(
        0xf92f,
        "unknown_f92f",
        undefined,
        [new Param(TYPE_DWORD, undefined, undefined), new Param(TYPE_DWORD, undefined, undefined)],
        StackInteraction.Pop
    ));
    static readonly CHAT_BOX = (OPCODES[0xf930] = new Opcode(
        0xf930,
        "chat_box",
        undefined,
        [
            new Param(TYPE_DWORD, undefined, undefined),
            new Param(TYPE_DWORD, undefined, undefined),
            new Param(TYPE_DWORD, undefined, undefined),
            new Param(TYPE_DWORD, undefined, undefined),
            new Param(TYPE_DWORD, undefined, undefined),
            new Param(TYPE_STRING, undefined, undefined),
        ],
        StackInteraction.Pop
    ));
    static readonly CHAT_BUBBLE = (OPCODES[0xf931] = new Opcode(
        0xf931,
        "chat_bubble",
        undefined,
        [new Param(TYPE_DWORD, undefined, undefined), new Param(TYPE_STRING, undefined, undefined)],
        StackInteraction.Pop
    ));
    static readonly UNKNOWN_F932 = (OPCODES[0xf932] = new Opcode(
        0xf932,
        "unknown_f932",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F933 = (OPCODES[0xf933] = new Opcode(
        0xf933,
        "unknown_f933",
        undefined,
        [
            new Param(
                new RegTupRefType(new Param(TYPE_ANY, undefined, ParamAccess.Write)),
                undefined,
                undefined
            ),
        ],
        undefined
    ));
    static readonly SCROLL_TEXT = (OPCODES[0xf934] = new Opcode(
        0xf934,
        "scroll_text",
        undefined,
        [
            new Param(TYPE_DWORD, undefined, undefined),
            new Param(TYPE_DWORD, undefined, undefined),
            new Param(TYPE_DWORD, undefined, undefined),
            new Param(TYPE_DWORD, undefined, undefined),
            new Param(TYPE_DWORD, undefined, undefined),
            new Param(TYPE_FLOAT, undefined, undefined),
            new Param(
                new RegTupRefType(new Param(TYPE_DWORD, undefined, ParamAccess.Write)),
                undefined,
                undefined
            ),
            new Param(TYPE_STRING, undefined, undefined),
        ],
        StackInteraction.Pop
    ));
    static readonly GBA_UNKNOWN1 = (OPCODES[0xf935] = new Opcode(
        0xf935,
        "gba_unknown1",
        undefined,
        [],
        undefined
    ));
    static readonly GBA_UNKNOWN2 = (OPCODES[0xf936] = new Opcode(
        0xf936,
        "gba_unknown2",
        undefined,
        [],
        undefined
    ));
    static readonly GBA_UNKNOWN3 = (OPCODES[0xf937] = new Opcode(
        0xf937,
        "gba_unknown3",
        undefined,
        [],
        undefined
    ));
    static readonly ADD_DAMAGE_TO = (OPCODES[0xf938] = new Opcode(
        0xf938,
        "add_damage_to",
        undefined,
        [new Param(TYPE_DWORD, undefined, undefined), new Param(TYPE_DWORD, undefined, undefined)],
        StackInteraction.Pop
    ));
    static readonly ITEM_DELETE3 = (OPCODES[0xf939] = new Opcode(
        0xf939,
        "item_delete3",
        undefined,
        [new Param(TYPE_DWORD, undefined, undefined)],
        StackInteraction.Pop
    ));
    static readonly GET_ITEM_INFO = (OPCODES[0xf93a] = new Opcode(
        0xf93a,
        "get_item_info",
        undefined,
        [
            new Param(TYPE_DWORD, undefined, undefined),
            new Param(
                new RegTupRefType(new Param(TYPE_ANY, undefined, ParamAccess.Write)),
                undefined,
                undefined
            ),
        ],
        StackInteraction.Pop
    ));
    static readonly ITEM_PACKING1 = (OPCODES[0xf93b] = new Opcode(
        0xf93b,
        "item_packing1",
        undefined,
        [new Param(TYPE_DWORD, undefined, undefined)],
        StackInteraction.Pop
    ));
    static readonly ITEM_PACKING2 = (OPCODES[0xf93c] = new Opcode(
        0xf93c,
        "item_packing2",
        undefined,
        [new Param(TYPE_DWORD, undefined, undefined), new Param(TYPE_DWORD, undefined, undefined)],
        StackInteraction.Pop
    ));
    static readonly GET_LANG_SETTING = (OPCODES[0xf93d] = new Opcode(
        0xf93d,
        "get_lang_setting",
        undefined,
        [
            new Param(
                new RegTupRefType(new Param(TYPE_ANY, undefined, ParamAccess.Write)),
                undefined,
                undefined
            ),
        ],
        StackInteraction.Pop
    ));
    static readonly PREPARE_STATISTIC = (OPCODES[0xf93e] = new Opcode(
        0xf93e,
        "prepare_statistic",
        undefined,
        [
            new Param(TYPE_DWORD, undefined, undefined),
            new Param(TYPE_I_LABEL, undefined, undefined),
            new Param(TYPE_I_LABEL, undefined, undefined),
        ],
        StackInteraction.Pop
    ));
    static readonly KEYWORD_DETECT = (OPCODES[0xf93f] = new Opcode(
        0xf93f,
        "keyword_detect",
        undefined,
        [],
        undefined
    ));
    static readonly KEYWORD = (OPCODES[0xf940] = new Opcode(
        0xf940,
        "keyword",
        undefined,
        [
            new Param(
                new RegTupRefType(new Param(TYPE_DWORD, undefined, ParamAccess.Write)),
                undefined,
                undefined
            ),
            new Param(TYPE_DWORD, "Player slot.", undefined),
            new Param(TYPE_STRING, undefined, undefined),
        ],
        StackInteraction.Pop
    ));
    static readonly GET_GUILDCARD_NUM = (OPCODES[0xf941] = new Opcode(
        0xf941,
        "get_guildcard_num",
        undefined,
        [
            new Param(TYPE_DWORD, "Player slot.", undefined),
            new Param(
                new RegTupRefType(new Param(TYPE_DWORD, undefined, ParamAccess.Write)),
                undefined,
                undefined
            ),
        ],
        StackInteraction.Pop
    ));
    static readonly UNKNOWN_F942 = (OPCODES[0xf942] = new Opcode(
        0xf942,
        "unknown_f942",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F943 = (OPCODES[0xf943] = new Opcode(
        0xf943,
        "unknown_f943",
        undefined,
        [],
        undefined
    ));
    static readonly GET_WRAP_STATUS = (OPCODES[0xf944] = new Opcode(
        0xf944,
        "get_wrap_status",
        undefined,
        [
            new Param(TYPE_DWORD, "Player slot.", undefined),
            new Param(
                new RegTupRefType(new Param(TYPE_DWORD, undefined, ParamAccess.Write)),
                undefined,
                undefined
            ),
        ],
        StackInteraction.Pop
    ));
    static readonly INITIAL_FLOOR = (OPCODES[0xf945] = new Opcode(
        0xf945,
        "initial_floor",
        undefined,
        [new Param(TYPE_DWORD, undefined, undefined)],
        StackInteraction.Pop
    ));
    static readonly SIN = (OPCODES[0xf946] = new Opcode(
        0xf946,
        "sin",
        undefined,
        [
            new Param(
                new RegTupRefType(new Param(TYPE_DWORD, undefined, ParamAccess.Write)),
                undefined,
                undefined
            ),
            new Param(TYPE_DWORD, undefined, undefined),
        ],
        StackInteraction.Pop
    ));
    static readonly COS = (OPCODES[0xf947] = new Opcode(
        0xf947,
        "cos",
        undefined,
        [
            new Param(
                new RegTupRefType(new Param(TYPE_DWORD, undefined, ParamAccess.Write)),
                undefined,
                undefined
            ),
            new Param(TYPE_DWORD, undefined, undefined),
        ],
        StackInteraction.Pop
    ));
    static readonly UNKNOWN_F948 = (OPCODES[0xf948] = new Opcode(
        0xf948,
        "unknown_f948",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F949 = (OPCODES[0xf949] = new Opcode(
        0xf949,
        "unknown_f949",
        undefined,
        [],
        undefined
    ));
    static readonly BOSS_IS_DEAD2 = (OPCODES[0xf94a] = new Opcode(
        0xf94a,
        "boss_is_dead2",
        undefined,
        [
            new Param(
                new RegTupRefType(new Param(TYPE_DWORD, undefined, ParamAccess.Write)),
                undefined,
                undefined
            ),
        ],
        undefined
    ));
    static readonly UNKNOWN_F94B = (OPCODES[0xf94b] = new Opcode(
        0xf94b,
        "unknown_f94b",
        undefined,
        [
            new Param(
                new RegTupRefType(new Param(TYPE_DWORD, undefined, ParamAccess.Write)),
                undefined,
                undefined
            ),
        ],
        undefined
    ));
    static readonly UNKNOWN_F94C = (OPCODES[0xf94c] = new Opcode(
        0xf94c,
        "unknown_f94c",
        undefined,
        [
            new Param(
                new RegTupRefType(new Param(TYPE_DWORD, undefined, ParamAccess.Write)),
                undefined,
                undefined
            ),
        ],
        undefined
    ));
    static readonly IS_THERE_CARDBATTLE = (OPCODES[0xf94d] = new Opcode(
        0xf94d,
        "is_there_cardbattle",
        undefined,
        [
            new Param(
                new RegTupRefType(new Param(TYPE_DWORD, undefined, ParamAccess.Write)),
                undefined,
                undefined
            ),
        ],
        undefined
    ));
    static readonly UNKNOWN_F94E = (OPCODES[0xf94e] = new Opcode(
        0xf94e,
        "unknown_f94e",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F94F = (OPCODES[0xf94f] = new Opcode(
        0xf94f,
        "unknown_f94f",
        undefined,
        [],
        undefined
    ));
    static readonly BB_P2_MENU = (OPCODES[0xf950] = new Opcode(
        0xf950,
        "bb_p2_menu",
        undefined,
        [new Param(TYPE_DWORD, undefined, undefined)],
        StackInteraction.Pop
    ));
    static readonly BB_MAP_DESIGNATE = (OPCODES[0xf951] = new Opcode(
        0xf951,
        "bb_map_designate",
        undefined,
        [
            new Param(TYPE_BYTE, undefined, undefined),
            new Param(TYPE_WORD, undefined, undefined),
            new Param(TYPE_BYTE, undefined, undefined),
            new Param(TYPE_BYTE, undefined, undefined),
        ],
        undefined
    ));
    static readonly BB_GET_NUMBER_IN_PACK = (OPCODES[0xf952] = new Opcode(
        0xf952,
        "bb_get_number_in_pack",
        undefined,
        [
            new Param(
                new RegTupRefType(new Param(TYPE_DWORD, undefined, ParamAccess.Write)),
                undefined,
                undefined
            ),
        ],
        undefined
    ));
    static readonly BB_SWAP_ITEM = (OPCODES[0xf953] = new Opcode(
        0xf953,
        "bb_swap_item",
        undefined,
        [
            new Param(TYPE_DWORD, undefined, undefined),
            new Param(TYPE_DWORD, undefined, undefined),
            new Param(TYPE_DWORD, undefined, undefined),
            new Param(TYPE_DWORD, undefined, undefined),
            new Param(TYPE_DWORD, undefined, undefined),
            new Param(TYPE_DWORD, undefined, undefined),
            new Param(TYPE_I_LABEL, undefined, undefined),
            new Param(TYPE_I_LABEL, undefined, undefined),
        ],
        StackInteraction.Pop
    ));
    static readonly BB_CHECK_WRAP = (OPCODES[0xf954] = new Opcode(
        0xf954,
        "bb_check_wrap",
        undefined,
        [
            new Param(TYPE_DWORD, undefined, undefined),
            new Param(
                new RegTupRefType(new Param(TYPE_DWORD, undefined, ParamAccess.Write)),
                undefined,
                undefined
            ),
        ],
        StackInteraction.Pop
    ));
    static readonly BB_EXCHANGE_PD_ITEM = (OPCODES[0xf955] = new Opcode(
        0xf955,
        "bb_exchange_pd_item",
        undefined,
        [
            new Param(TYPE_DWORD, undefined, undefined),
            new Param(TYPE_DWORD, undefined, undefined),
            new Param(TYPE_DWORD, undefined, undefined),
            new Param(TYPE_I_LABEL, undefined, undefined),
            new Param(TYPE_I_LABEL, undefined, undefined),
        ],
        StackInteraction.Pop
    ));
    static readonly BB_EXCHANGE_PD_SRANK = (OPCODES[0xf956] = new Opcode(
        0xf956,
        "bb_exchange_pd_srank",
        undefined,
        [
            new Param(TYPE_DWORD, undefined, undefined),
            new Param(TYPE_DWORD, undefined, undefined),
            new Param(TYPE_DWORD, undefined, undefined),
            new Param(TYPE_DWORD, undefined, undefined),
            new Param(TYPE_DWORD, undefined, undefined),
            new Param(TYPE_I_LABEL, undefined, undefined),
            new Param(TYPE_I_LABEL, undefined, undefined),
        ],
        StackInteraction.Pop
    ));
    static readonly BB_EXCHANGE_PD_SPECIAL = (OPCODES[0xf957] = new Opcode(
        0xf957,
        "bb_exchange_pd_special",
        undefined,
        [
            new Param(TYPE_DWORD, undefined, undefined),
            new Param(TYPE_DWORD, undefined, undefined),
            new Param(TYPE_DWORD, undefined, undefined),
            new Param(TYPE_DWORD, undefined, undefined),
            new Param(TYPE_DWORD, undefined, undefined),
            new Param(TYPE_DWORD, undefined, undefined),
            new Param(TYPE_I_LABEL, undefined, undefined),
            new Param(TYPE_I_LABEL, undefined, undefined),
        ],
        StackInteraction.Pop
    ));
    static readonly BB_EXCHANGE_PD_PERCENT = (OPCODES[0xf958] = new Opcode(
        0xf958,
        "bb_exchange_pd_percent",
        undefined,
        [
            new Param(TYPE_DWORD, undefined, undefined),
            new Param(TYPE_DWORD, undefined, undefined),
            new Param(TYPE_DWORD, undefined, undefined),
            new Param(TYPE_DWORD, undefined, undefined),
            new Param(TYPE_DWORD, undefined, undefined),
            new Param(TYPE_DWORD, undefined, undefined),
            new Param(TYPE_I_LABEL, undefined, undefined),
            new Param(TYPE_I_LABEL, undefined, undefined),
        ],
        StackInteraction.Pop
    ));
    static readonly UNKNOWN_F959 = (OPCODES[0xf959] = new Opcode(
        0xf959,
        "unknown_f959",
        undefined,
        [new Param(TYPE_DWORD, undefined, undefined)],
        StackInteraction.Pop
    ));
    static readonly UNKNOWN_F95A = (OPCODES[0xf95a] = new Opcode(
        0xf95a,
        "unknown_f95a",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F95B = (OPCODES[0xf95b] = new Opcode(
        0xf95b,
        "unknown_f95b",
        undefined,
        [],
        undefined
    ));
    static readonly BB_EXCHANGE_SLT = (OPCODES[0xf95c] = new Opcode(
        0xf95c,
        "bb_exchange_slt",
        undefined,
        [
            new Param(TYPE_DWORD, undefined, undefined),
            new Param(
                new RegTupRefType(new Param(TYPE_DWORD, undefined, ParamAccess.Write)),
                undefined,
                undefined
            ),
            new Param(TYPE_I_LABEL, undefined, undefined),
            new Param(TYPE_I_LABEL, undefined, undefined),
        ],
        StackInteraction.Pop
    ));
    static readonly BB_EXCHANGE_PC = (OPCODES[0xf95d] = new Opcode(
        0xf95d,
        "bb_exchange_pc",
        undefined,
        [],
        undefined
    ));
    static readonly BB_BOX_CREATE_BP = (OPCODES[0xf95e] = new Opcode(
        0xf95e,
        "bb_box_create_bp",
        undefined,
        [
            new Param(TYPE_DWORD, undefined, undefined),
            new Param(TYPE_FLOAT, undefined, undefined),
            new Param(TYPE_FLOAT, undefined, undefined),
        ],
        StackInteraction.Pop
    ));
    static readonly BB_EXCHANGE_PT = (OPCODES[0xf95f] = new Opcode(
        0xf95f,
        "bb_exchange_pt",
        undefined,
        [
            new Param(
                new RegTupRefType(new Param(TYPE_DWORD, undefined, ParamAccess.Write)),
                undefined,
                undefined
            ),
            new Param(
                new RegTupRefType(new Param(TYPE_DWORD, undefined, ParamAccess.Write)),
                undefined,
                undefined
            ),
            new Param(TYPE_DWORD, undefined, undefined),
            new Param(TYPE_I_LABEL, undefined, undefined),
            new Param(TYPE_I_LABEL, undefined, undefined),
        ],
        StackInteraction.Pop
    ));
    static readonly UNKNOWN_F960 = (OPCODES[0xf960] = new Opcode(
        0xf960,
        "unknown_f960",
        undefined,
        [new Param(TYPE_DWORD, undefined, undefined)],
        StackInteraction.Pop
    ));
    static readonly UNKNOWN_F961 = (OPCODES[0xf961] = new Opcode(
        0xf961,
        "unknown_f961",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F962 = (OPCODES[0xf962] = new Opcode(
        0xf962,
        "unknown_f962",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F963 = (OPCODES[0xf963] = new Opcode(
        0xf963,
        "unknown_f963",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F964 = (OPCODES[0xf964] = new Opcode(
        0xf964,
        "unknown_f964",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F965 = (OPCODES[0xf965] = new Opcode(
        0xf965,
        "unknown_f965",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F966 = (OPCODES[0xf966] = new Opcode(
        0xf966,
        "unknown_f966",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F967 = (OPCODES[0xf967] = new Opcode(
        0xf967,
        "unknown_f967",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F968 = (OPCODES[0xf968] = new Opcode(
        0xf968,
        "unknown_f968",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F969 = (OPCODES[0xf969] = new Opcode(
        0xf969,
        "unknown_f969",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F96A = (OPCODES[0xf96a] = new Opcode(
        0xf96a,
        "unknown_f96a",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F96B = (OPCODES[0xf96b] = new Opcode(
        0xf96b,
        "unknown_f96b",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F96C = (OPCODES[0xf96c] = new Opcode(
        0xf96c,
        "unknown_f96c",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F96D = (OPCODES[0xf96d] = new Opcode(
        0xf96d,
        "unknown_f96d",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F96E = (OPCODES[0xf96e] = new Opcode(
        0xf96e,
        "unknown_f96e",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F96F = (OPCODES[0xf96f] = new Opcode(
        0xf96f,
        "unknown_f96f",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F970 = (OPCODES[0xf970] = new Opcode(
        0xf970,
        "unknown_f970",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F971 = (OPCODES[0xf971] = new Opcode(
        0xf971,
        "unknown_f971",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F972 = (OPCODES[0xf972] = new Opcode(
        0xf972,
        "unknown_f972",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F973 = (OPCODES[0xf973] = new Opcode(
        0xf973,
        "unknown_f973",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F974 = (OPCODES[0xf974] = new Opcode(
        0xf974,
        "unknown_f974",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F975 = (OPCODES[0xf975] = new Opcode(
        0xf975,
        "unknown_f975",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F976 = (OPCODES[0xf976] = new Opcode(
        0xf976,
        "unknown_f976",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F977 = (OPCODES[0xf977] = new Opcode(
        0xf977,
        "unknown_f977",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F978 = (OPCODES[0xf978] = new Opcode(
        0xf978,
        "unknown_f978",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F979 = (OPCODES[0xf979] = new Opcode(
        0xf979,
        "unknown_f979",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F97A = (OPCODES[0xf97a] = new Opcode(
        0xf97a,
        "unknown_f97a",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F97B = (OPCODES[0xf97b] = new Opcode(
        0xf97b,
        "unknown_f97b",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F97C = (OPCODES[0xf97c] = new Opcode(
        0xf97c,
        "unknown_f97c",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F97D = (OPCODES[0xf97d] = new Opcode(
        0xf97d,
        "unknown_f97d",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F97E = (OPCODES[0xf97e] = new Opcode(
        0xf97e,
        "unknown_f97e",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F97F = (OPCODES[0xf97f] = new Opcode(
        0xf97f,
        "unknown_f97f",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F980 = (OPCODES[0xf980] = new Opcode(
        0xf980,
        "unknown_f980",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F981 = (OPCODES[0xf981] = new Opcode(
        0xf981,
        "unknown_f981",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F982 = (OPCODES[0xf982] = new Opcode(
        0xf982,
        "unknown_f982",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F983 = (OPCODES[0xf983] = new Opcode(
        0xf983,
        "unknown_f983",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F984 = (OPCODES[0xf984] = new Opcode(
        0xf984,
        "unknown_f984",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F985 = (OPCODES[0xf985] = new Opcode(
        0xf985,
        "unknown_f985",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F986 = (OPCODES[0xf986] = new Opcode(
        0xf986,
        "unknown_f986",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F987 = (OPCODES[0xf987] = new Opcode(
        0xf987,
        "unknown_f987",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F988 = (OPCODES[0xf988] = new Opcode(
        0xf988,
        "unknown_f988",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F989 = (OPCODES[0xf989] = new Opcode(
        0xf989,
        "unknown_f989",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F98A = (OPCODES[0xf98a] = new Opcode(
        0xf98a,
        "unknown_f98a",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F98B = (OPCODES[0xf98b] = new Opcode(
        0xf98b,
        "unknown_f98b",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F98C = (OPCODES[0xf98c] = new Opcode(
        0xf98c,
        "unknown_f98c",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F98D = (OPCODES[0xf98d] = new Opcode(
        0xf98d,
        "unknown_f98d",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F98E = (OPCODES[0xf98e] = new Opcode(
        0xf98e,
        "unknown_f98e",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F98F = (OPCODES[0xf98f] = new Opcode(
        0xf98f,
        "unknown_f98f",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F990 = (OPCODES[0xf990] = new Opcode(
        0xf990,
        "unknown_f990",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F991 = (OPCODES[0xf991] = new Opcode(
        0xf991,
        "unknown_f991",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F992 = (OPCODES[0xf992] = new Opcode(
        0xf992,
        "unknown_f992",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F993 = (OPCODES[0xf993] = new Opcode(
        0xf993,
        "unknown_f993",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F994 = (OPCODES[0xf994] = new Opcode(
        0xf994,
        "unknown_f994",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F995 = (OPCODES[0xf995] = new Opcode(
        0xf995,
        "unknown_f995",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F996 = (OPCODES[0xf996] = new Opcode(
        0xf996,
        "unknown_f996",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F997 = (OPCODES[0xf997] = new Opcode(
        0xf997,
        "unknown_f997",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F998 = (OPCODES[0xf998] = new Opcode(
        0xf998,
        "unknown_f998",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F999 = (OPCODES[0xf999] = new Opcode(
        0xf999,
        "unknown_f999",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F99A = (OPCODES[0xf99a] = new Opcode(
        0xf99a,
        "unknown_f99a",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F99B = (OPCODES[0xf99b] = new Opcode(
        0xf99b,
        "unknown_f99b",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F99C = (OPCODES[0xf99c] = new Opcode(
        0xf99c,
        "unknown_f99c",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F99D = (OPCODES[0xf99d] = new Opcode(
        0xf99d,
        "unknown_f99d",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F99E = (OPCODES[0xf99e] = new Opcode(
        0xf99e,
        "unknown_f99e",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F99F = (OPCODES[0xf99f] = new Opcode(
        0xf99f,
        "unknown_f99f",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F9A0 = (OPCODES[0xf9a0] = new Opcode(
        0xf9a0,
        "unknown_f9a0",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F9A1 = (OPCODES[0xf9a1] = new Opcode(
        0xf9a1,
        "unknown_f9a1",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F9A2 = (OPCODES[0xf9a2] = new Opcode(
        0xf9a2,
        "unknown_f9a2",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F9A3 = (OPCODES[0xf9a3] = new Opcode(
        0xf9a3,
        "unknown_f9a3",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F9A4 = (OPCODES[0xf9a4] = new Opcode(
        0xf9a4,
        "unknown_f9a4",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F9A5 = (OPCODES[0xf9a5] = new Opcode(
        0xf9a5,
        "unknown_f9a5",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F9A6 = (OPCODES[0xf9a6] = new Opcode(
        0xf9a6,
        "unknown_f9a6",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F9A7 = (OPCODES[0xf9a7] = new Opcode(
        0xf9a7,
        "unknown_f9a7",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F9A8 = (OPCODES[0xf9a8] = new Opcode(
        0xf9a8,
        "unknown_f9a8",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F9A9 = (OPCODES[0xf9a9] = new Opcode(
        0xf9a9,
        "unknown_f9a9",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F9AA = (OPCODES[0xf9aa] = new Opcode(
        0xf9aa,
        "unknown_f9aa",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F9AB = (OPCODES[0xf9ab] = new Opcode(
        0xf9ab,
        "unknown_f9ab",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F9AC = (OPCODES[0xf9ac] = new Opcode(
        0xf9ac,
        "unknown_f9ac",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F9AD = (OPCODES[0xf9ad] = new Opcode(
        0xf9ad,
        "unknown_f9ad",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F9AE = (OPCODES[0xf9ae] = new Opcode(
        0xf9ae,
        "unknown_f9ae",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F9AF = (OPCODES[0xf9af] = new Opcode(
        0xf9af,
        "unknown_f9af",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F9B0 = (OPCODES[0xf9b0] = new Opcode(
        0xf9b0,
        "unknown_f9b0",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F9B1 = (OPCODES[0xf9b1] = new Opcode(
        0xf9b1,
        "unknown_f9b1",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F9B2 = (OPCODES[0xf9b2] = new Opcode(
        0xf9b2,
        "unknown_f9b2",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F9B3 = (OPCODES[0xf9b3] = new Opcode(
        0xf9b3,
        "unknown_f9b3",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F9B4 = (OPCODES[0xf9b4] = new Opcode(
        0xf9b4,
        "unknown_f9b4",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F9B5 = (OPCODES[0xf9b5] = new Opcode(
        0xf9b5,
        "unknown_f9b5",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F9B6 = (OPCODES[0xf9b6] = new Opcode(
        0xf9b6,
        "unknown_f9b6",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F9B7 = (OPCODES[0xf9b7] = new Opcode(
        0xf9b7,
        "unknown_f9b7",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F9B8 = (OPCODES[0xf9b8] = new Opcode(
        0xf9b8,
        "unknown_f9b8",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F9B9 = (OPCODES[0xf9b9] = new Opcode(
        0xf9b9,
        "unknown_f9b9",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F9BA = (OPCODES[0xf9ba] = new Opcode(
        0xf9ba,
        "unknown_f9ba",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F9BB = (OPCODES[0xf9bb] = new Opcode(
        0xf9bb,
        "unknown_f9bb",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F9BC = (OPCODES[0xf9bc] = new Opcode(
        0xf9bc,
        "unknown_f9bc",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F9BD = (OPCODES[0xf9bd] = new Opcode(
        0xf9bd,
        "unknown_f9bd",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F9BE = (OPCODES[0xf9be] = new Opcode(
        0xf9be,
        "unknown_f9be",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F9BF = (OPCODES[0xf9bf] = new Opcode(
        0xf9bf,
        "unknown_f9bf",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F9C0 = (OPCODES[0xf9c0] = new Opcode(
        0xf9c0,
        "unknown_f9c0",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F9C1 = (OPCODES[0xf9c1] = new Opcode(
        0xf9c1,
        "unknown_f9c1",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F9C2 = (OPCODES[0xf9c2] = new Opcode(
        0xf9c2,
        "unknown_f9c2",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F9C3 = (OPCODES[0xf9c3] = new Opcode(
        0xf9c3,
        "unknown_f9c3",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F9C4 = (OPCODES[0xf9c4] = new Opcode(
        0xf9c4,
        "unknown_f9c4",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F9C5 = (OPCODES[0xf9c5] = new Opcode(
        0xf9c5,
        "unknown_f9c5",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F9C6 = (OPCODES[0xf9c6] = new Opcode(
        0xf9c6,
        "unknown_f9c6",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F9C7 = (OPCODES[0xf9c7] = new Opcode(
        0xf9c7,
        "unknown_f9c7",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F9C8 = (OPCODES[0xf9c8] = new Opcode(
        0xf9c8,
        "unknown_f9c8",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F9C9 = (OPCODES[0xf9c9] = new Opcode(
        0xf9c9,
        "unknown_f9c9",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F9CA = (OPCODES[0xf9ca] = new Opcode(
        0xf9ca,
        "unknown_f9ca",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F9CB = (OPCODES[0xf9cb] = new Opcode(
        0xf9cb,
        "unknown_f9cb",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F9CC = (OPCODES[0xf9cc] = new Opcode(
        0xf9cc,
        "unknown_f9cc",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F9CD = (OPCODES[0xf9cd] = new Opcode(
        0xf9cd,
        "unknown_f9cd",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F9CE = (OPCODES[0xf9ce] = new Opcode(
        0xf9ce,
        "unknown_f9ce",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F9CF = (OPCODES[0xf9cf] = new Opcode(
        0xf9cf,
        "unknown_f9cf",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F9D0 = (OPCODES[0xf9d0] = new Opcode(
        0xf9d0,
        "unknown_f9d0",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F9D1 = (OPCODES[0xf9d1] = new Opcode(
        0xf9d1,
        "unknown_f9d1",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F9D2 = (OPCODES[0xf9d2] = new Opcode(
        0xf9d2,
        "unknown_f9d2",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F9D3 = (OPCODES[0xf9d3] = new Opcode(
        0xf9d3,
        "unknown_f9d3",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F9D4 = (OPCODES[0xf9d4] = new Opcode(
        0xf9d4,
        "unknown_f9d4",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F9D5 = (OPCODES[0xf9d5] = new Opcode(
        0xf9d5,
        "unknown_f9d5",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F9D6 = (OPCODES[0xf9d6] = new Opcode(
        0xf9d6,
        "unknown_f9d6",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F9D7 = (OPCODES[0xf9d7] = new Opcode(
        0xf9d7,
        "unknown_f9d7",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F9D8 = (OPCODES[0xf9d8] = new Opcode(
        0xf9d8,
        "unknown_f9d8",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F9D9 = (OPCODES[0xf9d9] = new Opcode(
        0xf9d9,
        "unknown_f9d9",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F9DA = (OPCODES[0xf9da] = new Opcode(
        0xf9da,
        "unknown_f9da",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F9DB = (OPCODES[0xf9db] = new Opcode(
        0xf9db,
        "unknown_f9db",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F9DC = (OPCODES[0xf9dc] = new Opcode(
        0xf9dc,
        "unknown_f9dc",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F9DD = (OPCODES[0xf9dd] = new Opcode(
        0xf9dd,
        "unknown_f9dd",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F9DE = (OPCODES[0xf9de] = new Opcode(
        0xf9de,
        "unknown_f9de",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F9DF = (OPCODES[0xf9df] = new Opcode(
        0xf9df,
        "unknown_f9df",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F9E0 = (OPCODES[0xf9e0] = new Opcode(
        0xf9e0,
        "unknown_f9e0",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F9E1 = (OPCODES[0xf9e1] = new Opcode(
        0xf9e1,
        "unknown_f9e1",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F9E2 = (OPCODES[0xf9e2] = new Opcode(
        0xf9e2,
        "unknown_f9e2",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F9E3 = (OPCODES[0xf9e3] = new Opcode(
        0xf9e3,
        "unknown_f9e3",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F9E4 = (OPCODES[0xf9e4] = new Opcode(
        0xf9e4,
        "unknown_f9e4",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F9E5 = (OPCODES[0xf9e5] = new Opcode(
        0xf9e5,
        "unknown_f9e5",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F9E6 = (OPCODES[0xf9e6] = new Opcode(
        0xf9e6,
        "unknown_f9e6",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F9E7 = (OPCODES[0xf9e7] = new Opcode(
        0xf9e7,
        "unknown_f9e7",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F9E8 = (OPCODES[0xf9e8] = new Opcode(
        0xf9e8,
        "unknown_f9e8",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F9E9 = (OPCODES[0xf9e9] = new Opcode(
        0xf9e9,
        "unknown_f9e9",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F9EA = (OPCODES[0xf9ea] = new Opcode(
        0xf9ea,
        "unknown_f9ea",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F9EB = (OPCODES[0xf9eb] = new Opcode(
        0xf9eb,
        "unknown_f9eb",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F9EC = (OPCODES[0xf9ec] = new Opcode(
        0xf9ec,
        "unknown_f9ec",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F9ED = (OPCODES[0xf9ed] = new Opcode(
        0xf9ed,
        "unknown_f9ed",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F9EE = (OPCODES[0xf9ee] = new Opcode(
        0xf9ee,
        "unknown_f9ee",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F9EF = (OPCODES[0xf9ef] = new Opcode(
        0xf9ef,
        "unknown_f9ef",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F9F0 = (OPCODES[0xf9f0] = new Opcode(
        0xf9f0,
        "unknown_f9f0",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F9F1 = (OPCODES[0xf9f1] = new Opcode(
        0xf9f1,
        "unknown_f9f1",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F9F2 = (OPCODES[0xf9f2] = new Opcode(
        0xf9f2,
        "unknown_f9f2",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F9F3 = (OPCODES[0xf9f3] = new Opcode(
        0xf9f3,
        "unknown_f9f3",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F9F4 = (OPCODES[0xf9f4] = new Opcode(
        0xf9f4,
        "unknown_f9f4",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F9F5 = (OPCODES[0xf9f5] = new Opcode(
        0xf9f5,
        "unknown_f9f5",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F9F6 = (OPCODES[0xf9f6] = new Opcode(
        0xf9f6,
        "unknown_f9f6",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F9F7 = (OPCODES[0xf9f7] = new Opcode(
        0xf9f7,
        "unknown_f9f7",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F9F8 = (OPCODES[0xf9f8] = new Opcode(
        0xf9f8,
        "unknown_f9f8",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F9F9 = (OPCODES[0xf9f9] = new Opcode(
        0xf9f9,
        "unknown_f9f9",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F9FA = (OPCODES[0xf9fa] = new Opcode(
        0xf9fa,
        "unknown_f9fa",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F9FB = (OPCODES[0xf9fb] = new Opcode(
        0xf9fb,
        "unknown_f9fb",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F9FC = (OPCODES[0xf9fc] = new Opcode(
        0xf9fc,
        "unknown_f9fc",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F9FD = (OPCODES[0xf9fd] = new Opcode(
        0xf9fd,
        "unknown_f9fd",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F9FE = (OPCODES[0xf9fe] = new Opcode(
        0xf9fe,
        "unknown_f9fe",
        undefined,
        [],
        undefined
    ));
    static readonly UNKNOWN_F9FF = (OPCODES[0xf9ff] = new Opcode(
        0xf9ff,
        "unknown_f9ff",
        undefined,
        [],
        undefined
    ));
    // !!! GENERATED_CODE_END !!!
}

OPCODES.forEach(opcode => {
    OPCODES_BY_MNEMONIC.set(opcode.mnemonic, opcode);
});

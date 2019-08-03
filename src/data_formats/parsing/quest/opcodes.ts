/**
 * Opcode parameter type.
 */
export class Type {}
export const TYPE = new Type();

export class ValueType extends Type {}
export const TYPE_VALUE = new ValueType();

export class RefType extends Type {}
export const TYPE_REF = new RefType();

class PointerType extends Type {}
export const TYPE_POINTER = new PointerType();

/**
 * Unsigned 8-bit integer.
 */
class U8Type extends ValueType {}
export const TYPE_U8 = new U8Type();

/**
 * Unsigned 16-bit integer.
 */
class U16Type extends ValueType {}
export const TYPE_U16 = new U16Type();

/**
 * Unsigned 32-bit integer.
 */
class U32Type extends ValueType {}
export const TYPE_U32 = new U32Type();

/**
 * Signed 32-bit integer.
 */
class I32Type extends ValueType {}
export const TYPE_I32 = new I32Type();

/**
 * 32-Bit floating point number.
 */
class F32Type extends ValueType {}
export const TYPE_F32 = new F32Type();

/**
 * Named reference to an instruction.
 */
class ILabelType extends ValueType {}
export const TYPE_I_LABEL = new ILabelType();

/**
 * Named reference to a data segment.
 */
class DLabelType extends ValueType {}
export const TYPE_D_LABEL = new DLabelType();

/**
 * String of arbitrary size.
 */
class StringType extends ValueType {}
export const TYPE_STRING = new StringType();

/**
 * Arbitrary amount of instruction labels.
 */
class ILabelVarType extends ValueType {}
export const TYPE_I_LABEL_VAR = new ILabelVarType();

/**
 * Reference to one or more registers.
 */
class RegRefType extends RefType {}
export const TYPE_REG_REF = new RegRefType();

/**
 * Reference to a fixed amount of consecutive registers of specific types.
 */
export class RegTupRefType extends RefType {
    readonly types: ValueType[];

    constructor(...types: ValueType[]) {
        super();
        this.types = types;
    }
}

/**
 * Arbitrary amount of register references.
 */
class RegRefVarType extends RefType {}
export const TYPE_REG_REF_VAR = new RegRefVarType();

export type Param = {
    type: Type;
    /**
     * True if the registers referenced are read by this parameter's opcode. Only set when type is a register reference.
     */
    read?: boolean;
    /**
     * True if the registers referenced are written to by this parameter's opcode. Only set when type is a register reference.
     */
    write?: boolean;
    /**
     * Documentation string.
     */
    doc?: string;
};

export const OPCODES: Opcode[] = [];

/**
 * Opcode for script object code. Invoked by {@link ../bin/Instruction}s.
 */
export class Opcode {
    /**
     * 1- Or 2-byte representation of this opcode as used in object code.
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
     * Directly passed in arguments.
     */
    readonly params: Param[];
    /**
     * If true, this opcode pushes arguments onto the stack.
     */
    readonly push_stack: boolean;
    /**
     * Arguments passed in via the stack.
     * These arguments are popped from the stack when the opcode is executed.
     */
    readonly stack_params: Param[];

    constructor({
        code,
        mnemonic,
        doc,
        params,
        push_stack,
        stack_params,
    }: {
        code: number;
        mnemonic: string;
        doc?: string;
        params: Param[];
        push_stack: boolean;
        stack_params: Param[];
    }) {
        this.code = code;
        this.mnemonic = mnemonic;
        this.doc = doc;
        this.size = this.code < 256 ? 1 : 2;
        this.params = params;
        this.push_stack = push_stack;
        this.stack_params = stack_params;
    }

    static readonly nop = (OPCODES[0x00] = new Opcode({
        code: 0x00,
        mnemonic: "nop",
        doc: "No operation, does nothing.",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly ret = (OPCODES[0x01] = new Opcode({
        code: 0x01,
        mnemonic: "ret",
        doc: "Returns control to caller.",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly sync = (OPCODES[0x02] = new Opcode({
        code: 0x02,
        mnemonic: "sync",
        doc: "Yields control for this frame. Execution will continue in the next frame.",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly exit = (OPCODES[0x03] = new Opcode({
        code: 0x03,
        mnemonic: "exit",
        params: [],
        push_stack: false,
        stack_params: [{ type: TYPE_U32 }],
    }));
    static readonly thread = (OPCODES[0x04] = new Opcode({
        code: 0x04,
        mnemonic: "thread",
        doc:
            "Starts a new thread. Thread execution will start at the given label.\nOften used to check a register every frame. Make sure to yield control with sync when looping.",
        params: [{ type: TYPE_I_LABEL }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly va_start = (OPCODES[0x05] = new Opcode({
        code: 0x05,
        mnemonic: "va_start",
        doc:
            "Initializes a variable argument list.\nMake sure to call va_end after va_start and va_call.",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly va_end = (OPCODES[0x06] = new Opcode({
        code: 0x06,
        mnemonic: "va_end",
        doc:
            "Restores the registers overwritten by arg_push* instructions.\nCalled after va_start and va_call",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly va_call = (OPCODES[0x07] = new Opcode({
        code: 0x07,
        mnemonic: "va_call",
        doc:
            "Calls the variable argument function at the given label.\nCalled after initializing the argument list with va_start and pushing arguments onto the stack with arg_push* instructions. Make sure to call va_end afterwards.",
        params: [{ type: TYPE_I_LABEL }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly let = (OPCODES[0x08] = new Opcode({
        code: 0x08,
        mnemonic: "let",
        doc: "Sets the first register's value to second one's value.",
        params: [
            { type: new RegTupRefType(TYPE), write: true },
            { type: new RegTupRefType(TYPE), read: true },
        ],
        push_stack: false,
        stack_params: [],
    }));
    static readonly leti = (OPCODES[0x09] = new Opcode({
        code: 0x09,
        mnemonic: "leti",
        doc: "Sets a register to the given value.",
        params: [{ type: new RegTupRefType(TYPE_I32), write: true }, { type: TYPE_I32 }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly letb = (OPCODES[0x0a] = new Opcode({
        code: 0x0a,
        mnemonic: "letb",
        doc: "Sets a register to the given value.",
        params: [{ type: new RegTupRefType(TYPE_U8), write: true }, { type: TYPE_U8 }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly letw = (OPCODES[0x0b] = new Opcode({
        code: 0x0b,
        mnemonic: "letw",
        doc: "Sets a register to the given value.",
        params: [{ type: new RegTupRefType(TYPE_U16), write: true }, { type: TYPE_U16 }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly leta = (OPCODES[0x0c] = new Opcode({
        code: 0x0c,
        mnemonic: "leta",
        doc:
            "Sets the first register to the memory address of the second register. Not used by Sega.",
        params: [
            { type: new RegTupRefType(TYPE_POINTER), write: true },
            { type: new RegTupRefType(TYPE), read: true },
        ],
        push_stack: false,
        stack_params: [],
    }));
    static readonly leto = (OPCODES[0x0d] = new Opcode({
        code: 0x0d,
        mnemonic: "leto",
        doc: "Sets a register to the memory address of the given label. Not used by Sega.",
        params: [
            { type: new RegTupRefType(TYPE_POINTER), write: true },
            { type: TYPE_U16 /* ILabel or DLabel */ },
        ],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_0e = (OPCODES[0x0e] = new Opcode({
        code: 0x0e,
        mnemonic: "unknown_0e",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_0f = (OPCODES[0x0f] = new Opcode({
        code: 0x0f,
        mnemonic: "unknown_0f",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly set = (OPCODES[0x10] = new Opcode({
        code: 0x10,
        mnemonic: "set",
        doc: "Sets a register to 1.",
        params: [{ type: new RegTupRefType(TYPE_I32), write: true }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly clear = (OPCODES[0x11] = new Opcode({
        code: 0x11,
        mnemonic: "clear",
        doc: "Sets a register to 0.",
        params: [{ type: new RegTupRefType(TYPE_I32), write: true }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly rev = (OPCODES[0x12] = new Opcode({
        code: 0x12,
        mnemonic: "rev",
        doc: "Sets a register to 1 if its current value is 0, otherwise sets it to 0.",
        params: [{ type: new RegTupRefType(TYPE_I32), read: true, write: true }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly gset = (OPCODES[0x13] = new Opcode({
        code: 0x13,
        mnemonic: "gset",
        params: [{ type: TYPE_U16 }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly gclear = (OPCODES[0x14] = new Opcode({
        code: 0x14,
        mnemonic: "gclear",
        params: [{ type: TYPE_U16 }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly grev = (OPCODES[0x15] = new Opcode({
        code: 0x15,
        mnemonic: "grev",
        params: [{ type: TYPE_U16 }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly glet = (OPCODES[0x16] = new Opcode({
        code: 0x16,
        mnemonic: "glet",
        params: [{ type: TYPE_U16 }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly gget = (OPCODES[0x17] = new Opcode({
        code: 0x17,
        mnemonic: "gget",
        doc: "Sets a register to value of the given flag.",
        params: [{ type: TYPE_U16 }, { type: new RegTupRefType(TYPE_U16), write: true }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly add = (OPCODES[0x18] = new Opcode({
        code: 0x18,
        mnemonic: "add",
        params: [
            { type: new RegTupRefType(TYPE_I32), write: true },
            { type: new RegTupRefType(TYPE_I32), read: true },
        ],
        push_stack: false,
        stack_params: [],
    }));
    static readonly addi = (OPCODES[0x19] = new Opcode({
        code: 0x19,
        mnemonic: "addi",
        params: [{ type: new RegTupRefType(TYPE_I32), write: true }, { type: TYPE_I32 }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly sub = (OPCODES[0x1a] = new Opcode({
        code: 0x1a,
        mnemonic: "sub",
        params: [
            { type: new RegTupRefType(TYPE_I32), write: true },
            { type: new RegTupRefType(TYPE_I32), read: true },
        ],
        push_stack: false,
        stack_params: [],
    }));
    static readonly subi = (OPCODES[0x1b] = new Opcode({
        code: 0x1b,
        mnemonic: "subi",
        params: [{ type: new RegTupRefType(TYPE_I32), write: true }, { type: TYPE_I32 }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly mul = (OPCODES[0x1c] = new Opcode({
        code: 0x1c,
        mnemonic: "mul",
        params: [
            { type: new RegTupRefType(TYPE_I32), write: true },
            { type: new RegTupRefType(TYPE_I32), read: true },
        ],
        push_stack: false,
        stack_params: [],
    }));
    static readonly muli = (OPCODES[0x1d] = new Opcode({
        code: 0x1d,
        mnemonic: "muli",
        params: [{ type: new RegTupRefType(TYPE_I32), write: true }, { type: TYPE_I32 }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly div = (OPCODES[0x1e] = new Opcode({
        code: 0x1e,
        mnemonic: "div",
        params: [
            { type: new RegTupRefType(TYPE_I32), write: true },
            { type: new RegTupRefType(TYPE_I32), read: true },
        ],
        push_stack: false,
        stack_params: [],
    }));
    static readonly divi = (OPCODES[0x1f] = new Opcode({
        code: 0x1f,
        mnemonic: "divi",
        params: [{ type: new RegTupRefType(TYPE_I32), write: true }, { type: TYPE_I32 }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly and = (OPCODES[0x20] = new Opcode({
        code: 0x20,
        mnemonic: "and",
        params: [
            { type: new RegTupRefType(TYPE_I32), write: true },
            { type: new RegTupRefType(TYPE_I32), read: true },
        ],
        push_stack: false,
        stack_params: [],
    }));
    static readonly andi = (OPCODES[0x21] = new Opcode({
        code: 0x21,
        mnemonic: "andi",
        params: [{ type: new RegTupRefType(TYPE_I32), write: true }, { type: TYPE_I32 }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly or = (OPCODES[0x22] = new Opcode({
        code: 0x22,
        mnemonic: "or",
        params: [
            { type: new RegTupRefType(TYPE_I32), write: true },
            { type: new RegTupRefType(TYPE_I32), read: true },
        ],
        push_stack: false,
        stack_params: [],
    }));
    static readonly ori = (OPCODES[0x23] = new Opcode({
        code: 0x23,
        mnemonic: "ori",
        params: [{ type: new RegTupRefType(TYPE_I32), write: true }, { type: TYPE_I32 }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly xor = (OPCODES[0x24] = new Opcode({
        code: 0x24,
        mnemonic: "xor",
        params: [
            { type: new RegTupRefType(TYPE_I32), write: true },
            { type: new RegTupRefType(TYPE_I32), read: true },
        ],
        push_stack: false,
        stack_params: [],
    }));
    static readonly xori = (OPCODES[0x25] = new Opcode({
        code: 0x25,
        mnemonic: "xori",
        params: [{ type: new RegTupRefType(TYPE_I32), write: true }, { type: TYPE_I32 }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly mod = (OPCODES[0x26] = new Opcode({
        code: 0x26,
        mnemonic: "mod",
        params: [
            { type: new RegTupRefType(TYPE_I32), write: true },
            { type: new RegTupRefType(TYPE_I32), read: true },
        ],
        push_stack: false,
        stack_params: [],
    }));
    static readonly modi = (OPCODES[0x27] = new Opcode({
        code: 0x27,
        mnemonic: "modi",
        params: [{ type: new RegTupRefType(TYPE_I32), write: true }, { type: TYPE_I32 }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly jmp = (OPCODES[0x28] = new Opcode({
        code: 0x28,
        mnemonic: "jmp",
        params: [{ type: TYPE_I_LABEL }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly call = (OPCODES[0x29] = new Opcode({
        code: 0x29,
        mnemonic: "call",
        params: [{ type: TYPE_I_LABEL }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly jmp_on = (OPCODES[0x2a] = new Opcode({
        code: 0x2a,
        mnemonic: "jmp_on",
        params: [{ type: TYPE_I_LABEL }, { type: TYPE_REG_REF_VAR, read: true }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly jmp_off = (OPCODES[0x2b] = new Opcode({
        code: 0x2b,
        mnemonic: "jmp_off",
        params: [{ type: TYPE_I_LABEL }, { type: TYPE_REG_REF_VAR, read: true }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly jmp_e = (OPCODES[0x2c] = new Opcode({
        code: 0x2c,
        mnemonic: "jmp_=",
        params: [
            { type: new RegTupRefType(TYPE), read: true },
            { type: new RegTupRefType(TYPE), read: true },
            { type: TYPE_I_LABEL },
        ],
        push_stack: false,
        stack_params: [],
    }));
    static readonly jmpi_e = (OPCODES[0x2d] = new Opcode({
        code: 0x2d,
        mnemonic: "jmpi_=",
        params: [
            { type: new RegTupRefType(TYPE_I32), read: true },
            { type: TYPE_I32 },
            { type: TYPE_I_LABEL },
        ],
        push_stack: false,
        stack_params: [],
    }));
    static readonly jmp_ne = (OPCODES[0x2e] = new Opcode({
        code: 0x2e,
        mnemonic: "jmp_!=",
        params: [
            { type: new RegTupRefType(TYPE), read: true },
            { type: new RegTupRefType(TYPE), read: true },
            { type: TYPE_I_LABEL },
        ],
        push_stack: false,
        stack_params: [],
    }));
    static readonly jmpi_ne = (OPCODES[0x2f] = new Opcode({
        code: 0x2f,
        mnemonic: "jmpi_!=",
        params: [
            { type: new RegTupRefType(TYPE_I32), read: true },
            { type: TYPE_I32 },
            { type: TYPE_I_LABEL },
        ],
        push_stack: false,
        stack_params: [],
    }));
    static readonly ujmp_g = (OPCODES[0x30] = new Opcode({
        code: 0x30,
        mnemonic: "ujmp_>",
        params: [
            { type: new RegTupRefType(TYPE_U32), read: true },
            { type: new RegTupRefType(TYPE_U32), read: true },
            { type: TYPE_I_LABEL },
        ],
        push_stack: false,
        stack_params: [],
    }));
    static readonly ujmpi_g = (OPCODES[0x31] = new Opcode({
        code: 0x31,
        mnemonic: "ujmpi_>",
        params: [
            { type: new RegTupRefType(TYPE_U32), read: true },
            { type: TYPE_U32 },
            { type: TYPE_I_LABEL },
        ],
        push_stack: false,
        stack_params: [],
    }));
    static readonly jmp_g = (OPCODES[0x32] = new Opcode({
        code: 0x32,
        mnemonic: "jmp_>",
        params: [
            { type: new RegTupRefType(TYPE_I32), read: true },
            { type: new RegTupRefType(TYPE_I32), read: true },
            { type: TYPE_I_LABEL },
        ],
        push_stack: false,
        stack_params: [],
    }));
    static readonly jmpi_g = (OPCODES[0x33] = new Opcode({
        code: 0x33,
        mnemonic: "jmpi_>",
        params: [
            { type: new RegTupRefType(TYPE_I32), read: true },
            { type: TYPE_I32 },
            { type: TYPE_I_LABEL },
        ],
        push_stack: false,
        stack_params: [],
    }));
    static readonly ujmp_l = (OPCODES[0x34] = new Opcode({
        code: 0x34,
        mnemonic: "ujmp_<",
        params: [
            { type: new RegTupRefType(TYPE_U32), read: true },
            { type: new RegTupRefType(TYPE_U32), read: true },
            { type: TYPE_I_LABEL },
        ],
        push_stack: false,
        stack_params: [],
    }));
    static readonly ujmpi_l = (OPCODES[0x35] = new Opcode({
        code: 0x35,
        mnemonic: "ujmpi_<",
        params: [
            { type: new RegTupRefType(TYPE_U32), read: true },
            { type: TYPE_U32 },
            { type: TYPE_I_LABEL },
        ],
        push_stack: false,
        stack_params: [],
    }));
    static readonly jmp_l = (OPCODES[0x36] = new Opcode({
        code: 0x36,
        mnemonic: "jmp_<",
        params: [
            { type: new RegTupRefType(TYPE_I32), read: true },
            { type: new RegTupRefType(TYPE_I32), read: true },
            { type: TYPE_I_LABEL },
        ],
        push_stack: false,
        stack_params: [],
    }));
    static readonly jmpi_l = (OPCODES[0x37] = new Opcode({
        code: 0x37,
        mnemonic: "jmpi_<",
        params: [
            { type: new RegTupRefType(TYPE_I32), read: true },
            { type: TYPE_I32 },
            { type: TYPE_I_LABEL },
        ],
        push_stack: false,
        stack_params: [],
    }));
    static readonly ujmp_ge = (OPCODES[0x38] = new Opcode({
        code: 0x38,
        mnemonic: "ujmp_>=",
        params: [
            { type: new RegTupRefType(TYPE_U32), read: true },
            { type: new RegTupRefType(TYPE_U32), read: true },
            { type: TYPE_I_LABEL },
        ],
        push_stack: false,
        stack_params: [],
    }));
    static readonly ujmpi_ge = (OPCODES[0x39] = new Opcode({
        code: 0x39,
        mnemonic: "ujmpi_>=",
        params: [
            { type: new RegTupRefType(TYPE_U32), read: true },
            { type: TYPE_U32 },
            { type: TYPE_I_LABEL },
        ],
        push_stack: false,
        stack_params: [],
    }));
    static readonly jmp_ge = (OPCODES[0x3a] = new Opcode({
        code: 0x3a,
        mnemonic: "jmp_>=",
        params: [
            { type: new RegTupRefType(TYPE_I32), read: true },
            { type: new RegTupRefType(TYPE_I32), read: true },
            { type: TYPE_I_LABEL },
        ],
        push_stack: false,
        stack_params: [],
    }));
    static readonly jmpi_ge = (OPCODES[0x3b] = new Opcode({
        code: 0x3b,
        mnemonic: "jmpi_>=",
        params: [
            { type: new RegTupRefType(TYPE_I32), read: true },
            { type: TYPE_I32 },
            { type: TYPE_I_LABEL },
        ],
        push_stack: false,
        stack_params: [],
    }));
    static readonly ujmp_le = (OPCODES[0x3c] = new Opcode({
        code: 0x3c,
        mnemonic: "ujmp_<=",
        params: [
            { type: new RegTupRefType(TYPE_U32), read: true },
            { type: new RegTupRefType(TYPE_U32), read: true },
            { type: TYPE_I_LABEL },
        ],
        push_stack: false,
        stack_params: [],
    }));
    static readonly ujmpi_le = (OPCODES[0x3d] = new Opcode({
        code: 0x3d,
        mnemonic: "ujmpi_<=",
        params: [
            { type: new RegTupRefType(TYPE_U32), read: true },
            { type: TYPE_U32 },
            { type: TYPE_I_LABEL },
        ],
        push_stack: false,
        stack_params: [],
    }));
    static readonly jmp_le = (OPCODES[0x3e] = new Opcode({
        code: 0x3e,
        mnemonic: "jmp_<=",
        params: [
            { type: new RegTupRefType(TYPE_I32), read: true },
            { type: new RegTupRefType(TYPE_I32), read: true },
            { type: TYPE_I_LABEL },
        ],
        push_stack: false,
        stack_params: [],
    }));
    static readonly jmpi_le = (OPCODES[0x3f] = new Opcode({
        code: 0x3f,
        mnemonic: "jmpi_<=",
        params: [
            { type: new RegTupRefType(TYPE_I32), read: true },
            { type: TYPE_I32 },
            { type: TYPE_I_LABEL },
        ],
        push_stack: false,
        stack_params: [],
    }));
    static readonly switch_jmp = (OPCODES[0x40] = new Opcode({
        code: 0x40,
        mnemonic: "switch_jmp",
        params: [{ type: new RegTupRefType(TYPE_I32), read: true }, { type: TYPE_I_LABEL }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly switch_call = (OPCODES[0x41] = new Opcode({
        code: 0x41,
        mnemonic: "switch_call",
        params: [{ type: new RegTupRefType(TYPE_I32), read: true }, { type: TYPE_I_LABEL }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly stack_push = (OPCODES[0x42] = new Opcode({
        code: 0x42,
        mnemonic: "stack_push",
        params: [{ type: new RegTupRefType(TYPE), read: true }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly stack_pop = (OPCODES[0x43] = new Opcode({
        code: 0x43,
        mnemonic: "stack_pop",
        params: [{ type: new RegTupRefType(TYPE), write: true }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly stack_pushm = (OPCODES[0x44] = new Opcode({
        code: 0x44,
        mnemonic: "stack_pushm",
        doc: "Pushes a variable amount of registers onto the stack",
        params: [{ type: TYPE_REG_REF }, { type: TYPE_U32 }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly stack_popm = (OPCODES[0x45] = new Opcode({
        code: 0x45,
        mnemonic: "stack_popm",
        doc: "Pops a variable amount of registers from the stack",
        params: [{ type: TYPE_REG_REF }, { type: TYPE_U32 }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_46 = (OPCODES[0x46] = new Opcode({
        code: 0x46,
        mnemonic: "unknown_46",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_47 = (OPCODES[0x47] = new Opcode({
        code: 0x47,
        mnemonic: "unknown_47",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly arg_pushr = (OPCODES[0x48] = new Opcode({
        code: 0x48,
        mnemonic: "arg_pushr",
        doc: "Pushes the value of the given register onto the stack.",
        params: [{ type: new RegTupRefType(TYPE), read: true }],
        push_stack: true,
        stack_params: [],
    }));
    static readonly arg_pushl = (OPCODES[0x49] = new Opcode({
        code: 0x49,
        mnemonic: "arg_pushl",
        doc: "Pushes the given value onto the stack.",
        params: [{ type: TYPE_I32 }],
        push_stack: true,
        stack_params: [],
    }));
    static readonly arg_pushb = (OPCODES[0x4a] = new Opcode({
        code: 0x4a,
        mnemonic: "arg_pushb",
        doc: "Pushes the given value onto the stack.",
        params: [{ type: TYPE_U8 }],
        push_stack: true,
        stack_params: [],
    }));
    static readonly arg_pushw = (OPCODES[0x4b] = new Opcode({
        code: 0x4b,
        mnemonic: "arg_pushw",
        doc: "Pushes the given value onto the stack.",
        params: [{ type: TYPE_U16 }],
        push_stack: true,
        stack_params: [],
    }));
    static readonly unknown_4c = (OPCODES[0x4c] = new Opcode({
        code: 0x4c,
        mnemonic: "unknown_4c",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_4d = (OPCODES[0x4d] = new Opcode({
        code: 0x4d,
        mnemonic: "unknown_4d",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly arg_pushs = (OPCODES[0x4e] = new Opcode({
        code: 0x4e,
        mnemonic: "arg_pushs",
        doc: "Pushes the given value onto the stack.",
        params: [{ type: TYPE_STRING }],
        push_stack: true,
        stack_params: [],
    }));
    static readonly unknown_4f = (OPCODES[0x4f] = new Opcode({
        code: 0x4f,
        mnemonic: "unknown_4f",
        params: [{ type: new RegTupRefType(TYPE) }, { type: new RegTupRefType(TYPE) }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly message = (OPCODES[0x50] = new Opcode({
        code: 0x50,
        mnemonic: "message",
        params: [],
        push_stack: false,
        stack_params: [{ type: TYPE_U32 }, { type: TYPE_STRING }],
    }));
    static readonly list = (OPCODES[0x51] = new Opcode({
        code: 0x51,
        mnemonic: "list",
        doc:
            "Used to display a list of items and retrieve the item selected by the player.\nList items should be seperated by newlines. The selected item will be written to the given register.",
        params: [],
        push_stack: false,
        stack_params: [{ type: new RegTupRefType(TYPE_U8), write: true }, { type: TYPE_STRING }],
    }));
    static readonly fadein = (OPCODES[0x52] = new Opcode({
        code: 0x52,
        mnemonic: "fadein",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly fadeout = (OPCODES[0x53] = new Opcode({
        code: 0x53,
        mnemonic: "fadeout",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly se = (OPCODES[0x54] = new Opcode({
        code: 0x54,
        mnemonic: "se",
        params: [],
        push_stack: false,
        stack_params: [{ type: TYPE_U32 }],
    }));
    static readonly bgm = (OPCODES[0x55] = new Opcode({
        code: 0x55,
        mnemonic: "bgm",
        params: [],
        push_stack: false,
        stack_params: [{ type: TYPE_U32 }],
    }));
    static readonly unknown_56 = (OPCODES[0x56] = new Opcode({
        code: 0x56,
        mnemonic: "unknown_56",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_57 = (OPCODES[0x57] = new Opcode({
        code: 0x57,
        mnemonic: "unknown_57",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly enable = (OPCODES[0x58] = new Opcode({
        code: 0x58,
        mnemonic: "enable",
        params: [],
        push_stack: false,
        stack_params: [{ type: TYPE_U32 }],
    }));
    static readonly disable = (OPCODES[0x59] = new Opcode({
        code: 0x59,
        mnemonic: "disable",
        params: [],
        push_stack: false,
        stack_params: [{ type: TYPE_U32 }],
    }));
    static readonly window_msg = (OPCODES[0x5a] = new Opcode({
        code: 0x5a,
        mnemonic: "window_msg",
        params: [],
        push_stack: false,
        stack_params: [{ type: TYPE_STRING }],
    }));
    static readonly add_msg = (OPCODES[0x5b] = new Opcode({
        code: 0x5b,
        mnemonic: "add_msg",
        params: [],
        push_stack: false,
        stack_params: [{ type: TYPE_STRING }],
    }));
    static readonly mesend = (OPCODES[0x5c] = new Opcode({
        code: 0x5c,
        mnemonic: "mesend",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly gettime = (OPCODES[0x5d] = new Opcode({
        code: 0x5d,
        mnemonic: "gettime",
        params: [{ type: new RegTupRefType(TYPE_U32), write: true }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly winend = (OPCODES[0x5e] = new Opcode({
        code: 0x5e,
        mnemonic: "winend",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_5f = (OPCODES[0x5f] = new Opcode({
        code: 0x5f,
        mnemonic: "unknown_5f",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly npc_crt_v3 = (OPCODES[0x60] = new Opcode({
        code: 0x60,
        mnemonic: "npc_crt_v3",
        params: [{ type: new RegTupRefType(TYPE) }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly npc_stop = (OPCODES[0x61] = new Opcode({
        code: 0x61,
        mnemonic: "npc_stop",
        params: [],
        push_stack: false,
        stack_params: [{ type: TYPE_U32 }],
    }));
    static readonly npc_play = (OPCODES[0x62] = new Opcode({
        code: 0x62,
        mnemonic: "npc_play",
        params: [],
        push_stack: false,
        stack_params: [{ type: TYPE_U32 }],
    }));
    static readonly npc_kill = (OPCODES[0x63] = new Opcode({
        code: 0x63,
        mnemonic: "npc_kill",
        params: [],
        push_stack: false,
        stack_params: [{ type: TYPE_U32 }],
    }));
    static readonly npc_nont = (OPCODES[0x64] = new Opcode({
        code: 0x64,
        mnemonic: "npc_nont",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly npc_talk = (OPCODES[0x65] = new Opcode({
        code: 0x65,
        mnemonic: "npc_talk",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly npc_crp_v3 = (OPCODES[0x66] = new Opcode({
        code: 0x66,
        mnemonic: "npc_crp_v3",
        params: [{ type: new RegTupRefType(TYPE) }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_67 = (OPCODES[0x67] = new Opcode({
        code: 0x67,
        mnemonic: "unknown_67",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly create_pipe = (OPCODES[0x68] = new Opcode({
        code: 0x68,
        mnemonic: "create_pipe",
        params: [],
        push_stack: false,
        stack_params: [{ type: TYPE_U32 }],
    }));
    static readonly p_hpstat_v3 = (OPCODES[0x69] = new Opcode({
        code: 0x69,
        mnemonic: "p_hpstat_v3",
        params: [],
        push_stack: false,
        stack_params: [{ type: Type.RegRef2, write: true }, { type: TYPE_U32 }],
    }));
    static readonly p_dead_v3 = (OPCODES[0x6a] = new Opcode({
        code: 0x6a,
        mnemonic: "p_dead_v3",
        params: [],
        push_stack: false,
        stack_params: [{ type: new RegTupRefType(TYPE_U8), write: true }, { type: TYPE_U32 }],
    }));
    static readonly p_disablewarp = (OPCODES[0x6b] = new Opcode({
        code: 0x6b,
        mnemonic: "p_disablewarp",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly p_enablewarp = (OPCODES[0x6c] = new Opcode({
        code: 0x6c,
        mnemonic: "p_enablewarp",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly p_move_v3 = (OPCODES[0x6d] = new Opcode({
        code: 0x6d,
        mnemonic: "p_move_v3",
        params: [{ type: Type.RegRef }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly p_look = (OPCODES[0x6e] = new Opcode({
        code: 0x6e,
        mnemonic: "p_look",
        params: [],
        push_stack: false,
        stack_params: [{ type: TYPE_U32 }],
    }));
    static readonly unknown_6f = (OPCODES[0x6f] = new Opcode({
        code: 0x6f,
        mnemonic: "unknown_6f",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly p_action_disable = (OPCODES[0x70] = new Opcode({
        code: 0x70,
        mnemonic: "p_action_disable",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly p_action_enable = (OPCODES[0x71] = new Opcode({
        code: 0x71,
        mnemonic: "p_action_enable",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly disable_movement1 = (OPCODES[0x72] = new Opcode({
        code: 0x72,
        mnemonic: "disable_movement1",
        params: [],
        push_stack: false,
        stack_params: [{ type: TYPE_U32 }],
    }));
    static readonly enable_movement1 = (OPCODES[0x73] = new Opcode({
        code: 0x73,
        mnemonic: "enable_movement1",
        params: [],
        push_stack: false,
        stack_params: [{ type: TYPE_U32 }],
    }));
    static readonly p_noncol = (OPCODES[0x74] = new Opcode({
        code: 0x74,
        mnemonic: "p_noncol",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly p_col = (OPCODES[0x75] = new Opcode({
        code: 0x75,
        mnemonic: "p_col",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly p_setpos = (OPCODES[0x76] = new Opcode({
        code: 0x76,
        mnemonic: "p_setpos",
        params: [],
        push_stack: false,
        stack_params: [
            { type: TYPE_U32 },
            { type: new RegTupRefType(TYPE_I32, TYPE_I32, TYPE_I32, TYPE_I32), read: true },
        ],
    }));
    static readonly p_return_guild = (OPCODES[0x77] = new Opcode({
        code: 0x77,
        mnemonic: "p_return_guild",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly p_talk_guild = (OPCODES[0x78] = new Opcode({
        code: 0x78,
        mnemonic: "p_talk_guild",
        params: [],
        push_stack: false,
        stack_params: [{ type: TYPE_U32 }],
    }));
    static readonly npc_talk_pl_v3 = (OPCODES[0x79] = new Opcode({
        code: 0x79,
        mnemonic: "npc_talk_pl_v3",
        // TODO: is parameter really RegRef?
        params: [{ type: Type.RegRef }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly npc_talk_kill = (OPCODES[0x7a] = new Opcode({
        code: 0x7a,
        mnemonic: "npc_talk_kill",
        params: [],
        push_stack: false,
        stack_params: [{ type: TYPE_U32 }],
    }));
    static readonly npc_crtpk_v3 = (OPCODES[0x7b] = new Opcode({
        code: 0x7b,
        mnemonic: "npc_crtpk_v3",
        // TODO: is parameter really RegRef?
        params: [{ type: Type.RegRef }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly npc_crppk_v3 = (OPCODES[0x7c] = new Opcode({
        code: 0x7c,
        mnemonic: "npc_crppk_v3",
        // TODO: is parameter really RegRef?
        params: [{ type: Type.RegRef }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly npc_crptalk_v3 = (OPCODES[0x7d] = new Opcode({
        code: 0x7d,
        mnemonic: "npc_crptalk_v3",
        // TODO: is parameter really RegRef?
        params: [{ type: Type.RegRef }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly p_look_at_v1 = (OPCODES[0x7e] = new Opcode({
        code: 0x7e,
        mnemonic: "p_look_at_v1",
        params: [],
        push_stack: false,
        stack_params: [{ type: TYPE_U32 }, { type: TYPE_U32 }],
    }));
    static readonly npc_crp_id_v3 = (OPCODES[0x7f] = new Opcode({
        code: 0x7f,
        mnemonic: "npc_crp_id_v3",
        // TODO: is parameter really RegRef?
        params: [{ type: Type.RegRef }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly cam_quake = (OPCODES[0x80] = new Opcode({
        code: 0x80,
        mnemonic: "cam_quake",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly cam_adj = (OPCODES[0x81] = new Opcode({
        code: 0x81,
        mnemonic: "cam_adj",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly cam_zmin = (OPCODES[0x82] = new Opcode({
        code: 0x82,
        mnemonic: "cam_zmin",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly cam_zmout = (OPCODES[0x83] = new Opcode({
        code: 0x83,
        mnemonic: "cam_zmout",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly cam_pan_v3 = (OPCODES[0x84] = new Opcode({
        code: 0x84,
        mnemonic: "cam_pan_v3",
        params: [{ type: new RegTupRefType(TYPE_I32, TYPE_I32, TYPE_I32, TYPE_I32, TYPE_I32) }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly game_lev_super = (OPCODES[0x85] = new Opcode({
        code: 0x85,
        mnemonic: "game_lev_super",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly game_lev_reset = (OPCODES[0x86] = new Opcode({
        code: 0x86,
        mnemonic: "game_lev_reset",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly pos_pipe_v3 = (OPCODES[0x87] = new Opcode({
        code: 0x87,
        mnemonic: "pos_pipe_v3",
        doc:
            "Create a telepipe for the given player slot that takes players back to Pioneer 2 or the Lab.",
        params: [
            {
                type: new RegTupRefType(TYPE_I32, TYPE_I32, TYPE_I32, TYPE_I32),
                read: true,
                doc: "X, y, z coordinates and the player slot for the pipe.",
            },
        ],
        push_stack: false,
        stack_params: [],
    }));
    static readonly if_zone_clear = (OPCODES[0x88] = new Opcode({
        code: 0x88,
        mnemonic: "if_zone_clear",
        params: [
            {
                type: new RegTupRefType(TYPE_I32),
                write: true,
            },
            { type: new RegTupRefType(TYPE_I32, TYPE_I32), read: true },
        ],
        push_stack: false,
        stack_params: [],
    }));
    static readonly chk_ene_num = (OPCODES[0x89] = new Opcode({
        code: 0x89,
        mnemonic: "chk_ene_num",
        doc: "Retrieves the amount of enemies killed during the quest.",
        params: [{ type: new RegTupRefType(TYPE_I32), write: true }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unhide_obj = (OPCODES[0x8a] = new Opcode({
        code: 0x8a,
        mnemonic: "unhide_obj",
        params: [{ type: new RegTupRefType(TYPE_I32, TYPE_I32, TYPE_I32), read: true }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unhide_ene = (OPCODES[0x8b] = new Opcode({
        code: 0x8b,
        mnemonic: "unhide_ene",
        params: [{ type: new RegTupRefType(TYPE_I32, TYPE_I32, TYPE_I32, TYPE_I32), read: true }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly at_coords_call = (OPCODES[0x8c] = new Opcode({
        code: 0x8c,
        mnemonic: "at_coords_call",
        params: [
            {
                type: new RegTupRefType(TYPE_I32, TYPE_I32, TYPE_I32, TYPE_I32, TYPE_I_LABEL),
                read: true,
            },
        ],
        push_stack: false,
        stack_params: [],
    }));
    static readonly at_coords_talk = (OPCODES[0x8d] = new Opcode({
        code: 0x8d,
        mnemonic: "at_coords_talk",
        params: [
            {
                type: new RegTupRefType(TYPE_I32, TYPE_I32, TYPE_I32, TYPE_I32, TYPE_I_LABEL),
                read: true,
            },
        ],
        push_stack: false,
        stack_params: [],
    }));
    static readonly col_npcin = (OPCODES[0x8e] = new Opcode({
        code: 0x8e,
        mnemonic: "col_npcin",
        params: [
            {
                type: new RegTupRefType(TYPE_I32, TYPE_I32, TYPE_I32, TYPE_I32, TYPE_I_LABEL),
                read: true,
            },
        ],
        push_stack: false,
        stack_params: [],
    }));
    static readonly col_npcinr = (OPCODES[0x8f] = new Opcode({
        code: 0x8f,
        mnemonic: "col_npcinr",
        params: [{ type: Type.RegRef }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly switch_on = (OPCODES[0x90] = new Opcode({
        code: 0x90,
        mnemonic: "switch_on",
        params: [],
        push_stack: false,
        stack_params: [{ type: TYPE_U32 }],
    }));
    static readonly switch_off = (OPCODES[0x91] = new Opcode({
        code: 0x91,
        mnemonic: "switch_off",
        params: [],
        push_stack: false,
        stack_params: [{ type: TYPE_U32 }],
    }));
    static readonly playbgm_epi = (OPCODES[0x92] = new Opcode({
        code: 0x92,
        mnemonic: "playbgm_epi",
        params: [],
        push_stack: false,
        stack_params: [{ type: TYPE_U32 }],
    }));
    static readonly set_mainwarp = (OPCODES[0x93] = new Opcode({
        code: 0x93,
        mnemonic: "set_mainwarp",
        params: [],
        push_stack: false,
        stack_params: [{ type: TYPE_U32 }],
    }));
    static readonly set_obj_param = (OPCODES[0x94] = new Opcode({
        code: 0x94,
        mnemonic: "set_obj_param",
        doc: "Creates a targetable object.",
        params: [
            {
                type: new RegTupRefType(
                    TYPE_I32,
                    TYPE_I32,
                    TYPE_I32,
                    TYPE_I32,
                    TYPE_I_LABEL,
                    TYPE_I32
                ),
                read: true,
                doc:
                    "X, y, z coordinates, collision radius, function label and the vertical position of the cursor.",
            },
            { type: new RegTupRefType(TYPE), write: true, doc: "Object handle." },
        ],
        push_stack: false,
        stack_params: [],
    }));
    static readonly set_floor_handler = (OPCODES[0x95] = new Opcode({
        code: 0x95,
        mnemonic: "set_floor_handler",
        params: [],
        push_stack: false,
        stack_params: [{ type: TYPE_U32 }, { type: TYPE_I_LABEL }],
    }));
    static readonly clr_floor_handler = (OPCODES[0x96] = new Opcode({
        code: 0x96,
        mnemonic: "clr_floor_handler",
        params: [],
        push_stack: false,
        stack_params: [{ type: TYPE_U32 }],
    }));
    static readonly col_plinaw = (OPCODES[0x97] = new Opcode({
        code: 0x97,
        mnemonic: "col_plinaw",
        params: [{ type: Type.RegRef }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly hud_hide = (OPCODES[0x98] = new Opcode({
        code: 0x98,
        mnemonic: "hud_hide",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly hud_show = (OPCODES[0x99] = new Opcode({
        code: 0x99,
        mnemonic: "hud_show",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly cine_enable = (OPCODES[0x9a] = new Opcode({
        code: 0x9a,
        mnemonic: "cine_enable",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly cine_disable = (OPCODES[0x9b] = new Opcode({
        code: 0x9b,
        mnemonic: "cine_disable",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_9c = (OPCODES[0x9c] = new Opcode({
        code: 0x9c,
        mnemonic: "unknown_9c",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_9d = (OPCODES[0x9d] = new Opcode({
        code: 0x9d,
        mnemonic: "unknown_9d",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_9e = (OPCODES[0x9e] = new Opcode({
        code: 0x9e,
        mnemonic: "unknown_9e",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_9f = (OPCODES[0x9f] = new Opcode({
        code: 0x9f,
        mnemonic: "unknown_9f",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_a0 = (OPCODES[0xa0] = new Opcode({
        code: 0xa0,
        mnemonic: "unknown_a0",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly set_qt_failure = (OPCODES[0xa1] = new Opcode({
        code: 0xa1,
        mnemonic: "set_qt_failure",
        params: [{ type: TYPE_I_LABEL }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly set_qt_success = (OPCODES[0xa2] = new Opcode({
        code: 0xa2,
        mnemonic: "set_qt_success",
        params: [{ type: TYPE_I_LABEL }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly clr_qt_failure = (OPCODES[0xa3] = new Opcode({
        code: 0xa3,
        mnemonic: "clr_qt_failure",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly clr_qt_success = (OPCODES[0xa4] = new Opcode({
        code: 0xa4,
        mnemonic: "clr_qt_success",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly set_qt_cancel = (OPCODES[0xa5] = new Opcode({
        code: 0xa5,
        mnemonic: "set_qt_cancel",
        params: [{ type: TYPE_I_LABEL }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly clr_qt_cancel = (OPCODES[0xa6] = new Opcode({
        code: 0xa6,
        mnemonic: "clr_qt_cancel",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_a7 = (OPCODES[0xa7] = new Opcode({
        code: 0xa7,
        mnemonic: "unknown_a7",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly pl_walk_v3 = (OPCODES[0xa8] = new Opcode({
        code: 0xa8,
        mnemonic: "pl_walk_v3",
        params: [{ type: Type.RegRef }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_a9 = (OPCODES[0xa9] = new Opcode({
        code: 0xa9,
        mnemonic: "unknown_a9",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_aa = (OPCODES[0xaa] = new Opcode({
        code: 0xaa,
        mnemonic: "unknown_aa",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_ab = (OPCODES[0xab] = new Opcode({
        code: 0xab,
        mnemonic: "unknown_ab",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_ac = (OPCODES[0xac] = new Opcode({
        code: 0xac,
        mnemonic: "unknown_ac",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_ad = (OPCODES[0xad] = new Opcode({
        code: 0xad,
        mnemonic: "unknown_ad",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_ae = (OPCODES[0xae] = new Opcode({
        code: 0xae,
        mnemonic: "unknown_ae",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_af = (OPCODES[0xaf] = new Opcode({
        code: 0xaf,
        mnemonic: "unknown_af",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly pl_add_meseta = (OPCODES[0xb0] = new Opcode({
        code: 0xb0,
        mnemonic: "pl_add_meseta",
        params: [],
        push_stack: false,
        stack_params: [{ type: TYPE_U32 }, { type: TYPE_U32 }],
    }));
    static readonly thread_stg = (OPCODES[0xb1] = new Opcode({
        code: 0xb1,
        mnemonic: "thread_stg",
        params: [{ type: TYPE_I_LABEL }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly del_obj_param = (OPCODES[0xb2] = new Opcode({
        code: 0xb2,
        mnemonic: "del_obj_param",
        params: [{ type: new RegTupRefType(TYPE), read: true, doc: "Object handle." }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly item_create = (OPCODES[0xb3] = new Opcode({
        code: 0xb3,
        mnemonic: "item_create",
        params: [{ type: Type.RegRef }, { type: Type.RegRef }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly item_create2 = (OPCODES[0xb4] = new Opcode({
        code: 0xb4,
        mnemonic: "item_create2",
        params: [{ type: Type.RegRef }, { type: Type.RegRef }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly item_delete = (OPCODES[0xb5] = new Opcode({
        code: 0xb5,
        mnemonic: "item_delete",
        params: [{ type: Type.RegRef }, { type: Type.RegRef }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly item_delete2 = (OPCODES[0xb6] = new Opcode({
        code: 0xb6,
        mnemonic: "item_delete2",
        doc: "Deletes an item from the character's inventory.",
        params: [
            { type: new RegTupRefType(TYPE_I32, TYPE_I32, TYPE_I32), read: true },
            { type: new RegTupRefType(TYPE), write: true },
        ],
        push_stack: false,
        stack_params: [],
    }));
    static readonly item_check = (OPCODES[0xb7] = new Opcode({
        code: 0xb7,
        mnemonic: "item_check",
        params: [{ type: Type.RegRef }, { type: Type.RegRef }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly setevt = (OPCODES[0xb8] = new Opcode({
        code: 0xb8,
        mnemonic: "setevt",
        params: [],
        push_stack: false,
        stack_params: [{ type: TYPE_U32 }],
    }));
    static readonly get_difflvl = (OPCODES[0xb9] = new Opcode({
        code: 0xb9,
        mnemonic: "get_difflvl",
        doc:
            "Sets the given register to the current difficulty. 0 For normal, 1 for hard and 2 for both very hard and ultimate.\nUse get_difficulty_level2 if you want to differentiate between very hard and ultimate.",
        params: [{ type: new RegTupRefType(TYPE_I32), write: true }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly set_qt_exit = (OPCODES[0xba] = new Opcode({
        code: 0xba,
        mnemonic: "set_qt_exit",
        params: [{ type: TYPE_I_LABEL }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly clr_qt_exit = (OPCODES[0xbb] = new Opcode({
        code: 0xbb,
        mnemonic: "clr_qt_exit",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_bc = (OPCODES[0xbc] = new Opcode({
        code: 0xbc,
        mnemonic: "unknown_bc",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_bd = (OPCODES[0xbd] = new Opcode({
        code: 0xbd,
        mnemonic: "unknown_bd",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_be = (OPCODES[0xbe] = new Opcode({
        code: 0xbe,
        mnemonic: "unknown_be",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_bf = (OPCODES[0xbf] = new Opcode({
        code: 0xbf,
        mnemonic: "unknown_bf",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly particle_v3 = (OPCODES[0xc0] = new Opcode({
        code: 0xc0,
        mnemonic: "particle_v3",
        params: [{ type: Type.RegRef }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly npc_text = (OPCODES[0xc1] = new Opcode({
        code: 0xc1,
        mnemonic: "npc_text",
        params: [],
        push_stack: false,
        stack_params: [{ type: TYPE_U32 }, { type: TYPE_STRING }],
    }));
    static readonly npc_chkwarp = (OPCODES[0xc2] = new Opcode({
        code: 0xc2,
        mnemonic: "npc_chkwarp",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly pl_pkoff = (OPCODES[0xc3] = new Opcode({
        code: 0xc3,
        mnemonic: "pl_pkoff",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly map_designate = (OPCODES[0xc4] = new Opcode({
        code: 0xc4,
        mnemonic: "map_designate",
        params: [{ type: Type.RegRef }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly masterkey_on = (OPCODES[0xc5] = new Opcode({
        code: 0xc5,
        mnemonic: "masterkey_on",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly masterkey_off = (OPCODES[0xc6] = new Opcode({
        code: 0xc6,
        mnemonic: "masterkey_off",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly window_time = (OPCODES[0xc7] = new Opcode({
        code: 0xc7,
        mnemonic: "window_time",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly winend_time = (OPCODES[0xc8] = new Opcode({
        code: 0xc8,
        mnemonic: "winend_time",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly winset_time = (OPCODES[0xc9] = new Opcode({
        code: 0xc9,
        mnemonic: "winset_time",
        params: [{ type: new RegTupRefType(TYPE_I32), write: true }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly getmtime = (OPCODES[0xca] = new Opcode({
        code: 0xca,
        mnemonic: "getmtime",
        params: [{ type: Type.RegRef }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly set_quest_board_handler = (OPCODES[0xcb] = new Opcode({
        code: 0xcb,
        mnemonic: "set_quest_board_handler",
        params: [],
        push_stack: false,
        stack_params: [{ type: TYPE_U32 }, { type: TYPE_I_LABEL }, { type: TYPE_STRING }],
    }));
    static readonly clear_quest_board_handler = (OPCODES[0xcc] = new Opcode({
        code: 0xcc,
        mnemonic: "clear_quest_board_handler",
        params: [],
        push_stack: false,
        stack_params: [{ type: TYPE_U32 }],
    }));
    static readonly particle_id_v3 = (OPCODES[0xcd] = new Opcode({
        code: 0xcd,
        mnemonic: "particle_id_v3",
        params: [{ type: new RegTupRefType(TYPE_I32, TYPE_I32, TYPE_I32, TYPE_I32), read: true }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly npc_crptalk_id_v3 = (OPCODES[0xce] = new Opcode({
        code: 0xce,
        mnemonic: "npc_crptalk_id_v3",
        params: [{ type: Type.RegRef }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly npc_lang_clean = (OPCODES[0xcf] = new Opcode({
        code: 0xcf,
        mnemonic: "npc_lang_clean",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly pl_pkon = (OPCODES[0xd0] = new Opcode({
        code: 0xd0,
        mnemonic: "pl_pkon",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly pl_chk_item2 = (OPCODES[0xd1] = new Opcode({
        code: 0xd1,
        mnemonic: "pl_chk_item2",
        params: [{ type: Type.RegRef }, { type: Type.RegRef }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly enable_mainmenu = (OPCODES[0xd2] = new Opcode({
        code: 0xd2,
        mnemonic: "enable_mainmenu",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly disable_mainmenu = (OPCODES[0xd3] = new Opcode({
        code: 0xd3,
        mnemonic: "disable_mainmenu",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly start_battlebgm = (OPCODES[0xd4] = new Opcode({
        code: 0xd4,
        mnemonic: "start_battlebgm",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly end_battlebgm = (OPCODES[0xd5] = new Opcode({
        code: 0xd5,
        mnemonic: "end_battlebgm",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly disp_msg_qb = (OPCODES[0xd6] = new Opcode({
        code: 0xd6,
        mnemonic: "disp_msg_qb",
        params: [],
        push_stack: false,
        stack_params: [{ type: TYPE_STRING }],
    }));
    static readonly close_msg_qb = (OPCODES[0xd7] = new Opcode({
        code: 0xd7,
        mnemonic: "close_msg_qb",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly set_eventflag_v3 = (OPCODES[0xd8] = new Opcode({
        code: 0xd8,
        mnemonic: "set_eventflag_v3",
        params: [],
        push_stack: false,
        stack_params: [{ type: TYPE_U32 }, { type: TYPE_U32 }],
    }));
    static readonly sync_leti = (OPCODES[0xd9] = new Opcode({
        code: 0xd9,
        mnemonic: "sync_leti",
        params: [{ type: new RegTupRefType(TYPE_I32) }, { type: TYPE_I32 }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly set_returnhunter = (OPCODES[0xda] = new Opcode({
        code: 0xda,
        mnemonic: "set_returnhunter",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly set_returncity = (OPCODES[0xdb] = new Opcode({
        code: 0xdb,
        mnemonic: "set_returncity",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly load_pvr = (OPCODES[0xdc] = new Opcode({
        code: 0xdc,
        mnemonic: "load_pvr",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly load_midi = (OPCODES[0xdd] = new Opcode({
        code: 0xdd,
        mnemonic: "load_midi",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_de = (OPCODES[0xde] = new Opcode({
        code: 0xde,
        mnemonic: "unknown_de",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly npc_param_v3 = (OPCODES[0xdf] = new Opcode({
        code: 0xdf,
        mnemonic: "npc_param_v3",
        params: [],
        push_stack: false,
        stack_params: [{ type: Type.RegRef }, { type: TYPE_U32 }],
    }));
    static readonly pad_dragon = (OPCODES[0xe0] = new Opcode({
        code: 0xe0,
        mnemonic: "pad_dragon",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly clear_mainwarp = (OPCODES[0xe1] = new Opcode({
        code: 0xe1,
        mnemonic: "clear_mainwarp",
        params: [],
        push_stack: false,
        stack_params: [{ type: TYPE_U32 }],
    }));
    static readonly pcam_param_v3 = (OPCODES[0xe2] = new Opcode({
        code: 0xe2,
        mnemonic: "pcam_param_v3",
        params: [{ type: Type.RegRef }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly start_setevt_v3 = (OPCODES[0xe3] = new Opcode({
        code: 0xe3,
        mnemonic: "start_setevt_v3",
        params: [],
        push_stack: false,
        stack_params: [{ type: Type.RegRef }, { type: TYPE_U32 }],
    }));
    static readonly warp_on = (OPCODES[0xe4] = new Opcode({
        code: 0xe4,
        mnemonic: "warp_on",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly warp_off = (OPCODES[0xe5] = new Opcode({
        code: 0xe5,
        mnemonic: "warp_off",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly get_slotnumber = (OPCODES[0xe6] = new Opcode({
        code: 0xe6,
        mnemonic: "get_slotnumber",
        params: [{ type: Type.RegRef }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly get_servernumber = (OPCODES[0xe7] = new Opcode({
        code: 0xe7,
        mnemonic: "get_servernumber",
        params: [{ type: Type.RegRef }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly set_eventflag2 = (OPCODES[0xe8] = new Opcode({
        code: 0xe8,
        mnemonic: "set_eventflag2",
        params: [],
        push_stack: false,
        stack_params: [{ type: TYPE_U32 }, { type: Type.RegRef }],
    }));
    static readonly res = (OPCODES[0xe9] = new Opcode({
        code: 0xe9,
        mnemonic: "res",
        params: [{ type: Type.RegRef }, { type: Type.RegRef }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_ea = (OPCODES[0xea] = new Opcode({
        code: 0xea,
        mnemonic: "unknown_ea",
        params: [{ type: Type.RegRef }, { type: TYPE_U32 }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly enable_bgmctrl = (OPCODES[0xeb] = new Opcode({
        code: 0xeb,
        mnemonic: "enable_bgmctrl",
        params: [],
        push_stack: false,
        stack_params: [{ type: TYPE_U32 }],
    }));
    static readonly sw_send = (OPCODES[0xec] = new Opcode({
        code: 0xec,
        mnemonic: "sw_send",
        params: [{ type: Type.RegRef }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly create_bgmctrl = (OPCODES[0xed] = new Opcode({
        code: 0xed,
        mnemonic: "create_bgmctrl",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly pl_add_meseta2 = (OPCODES[0xee] = new Opcode({
        code: 0xee,
        mnemonic: "pl_add_meseta2",
        params: [],
        push_stack: false,
        stack_params: [{ type: TYPE_U32 }],
    }));
    static readonly sync_register = (OPCODES[0xef] = new Opcode({
        code: 0xef,
        mnemonic: "sync_register",
        params: [],
        push_stack: false,
        stack_params: [
            { type: Type.RegRef },
            { type: TYPE_U32 /* TODO: Can be U32 or Register. */ },
        ],
    }));
    static readonly send_regwork = (OPCODES[0xf0] = new Opcode({
        code: 0xf0,
        mnemonic: "send_regwork",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly leti_fixed_camera_v3 = (OPCODES[0xf1] = new Opcode({
        code: 0xf1,
        mnemonic: "leti_fixed_camera_v3",
        params: [{ type: Type.RegRef }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly default_camera_pos1 = (OPCODES[0xf2] = new Opcode({
        code: 0xf2,
        mnemonic: "default_camera_pos1",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f3 = (OPCODES[0xf3] = new Opcode({
        code: 0xf3,
        mnemonic: "unknown_f3",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f4 = (OPCODES[0xf4] = new Opcode({
        code: 0xf4,
        mnemonic: "unknown_f4",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f5 = (OPCODES[0xf5] = new Opcode({
        code: 0xf5,
        mnemonic: "unknown_f5",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f6 = (OPCODES[0xf6] = new Opcode({
        code: 0xf6,
        mnemonic: "unknown_f6",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f7 = (OPCODES[0xf7] = new Opcode({
        code: 0xf7,
        mnemonic: "unknown_f7",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f8 = (OPCODES[0xf8] = new Opcode({
        code: 0xf8,
        mnemonic: "unknown_f8",
        params: [{ type: Type.RegRef }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f9 = (OPCODES[0xf9] = new Opcode({
        code: 0xf9,
        mnemonic: "unknown_f9",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly get_gc_number = (OPCODES[0xfa] = new Opcode({
        code: 0xfa,
        mnemonic: "get_gc_number",
        params: [{ type: Type.RegRef }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_fb = (OPCODES[0xfb] = new Opcode({
        code: 0xfb,
        mnemonic: "unknown_fb",
        params: [{ type: TYPE_U16 }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_fc = (OPCODES[0xfc] = new Opcode({
        code: 0xfc,
        mnemonic: "unknown_fc",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_fd = (OPCODES[0xfd] = new Opcode({
        code: 0xfd,
        mnemonic: "unknown_fd",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_fe = (OPCODES[0xfe] = new Opcode({
        code: 0xfe,
        mnemonic: "unknown_fe",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_ff = (OPCODES[0xff] = new Opcode({
        code: 0xff,
        mnemonic: "unknown_ff",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f800 = (OPCODES[0xf800] = new Opcode({
        code: 0xf800,
        mnemonic: "unknown_f800",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly set_chat_callback = (OPCODES[0xf801] = new Opcode({
        code: 0xf801,
        mnemonic: "set_chat_callback",
        params: [],
        push_stack: false,
        stack_params: [{ type: Type.RegRef }, { type: TYPE_STRING }],
    }));
    static readonly unknown_f802 = (OPCODES[0xf802] = new Opcode({
        code: 0xf802,
        mnemonic: "unknown_f802",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f803 = (OPCODES[0xf803] = new Opcode({
        code: 0xf803,
        mnemonic: "unknown_f803",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f804 = (OPCODES[0xf804] = new Opcode({
        code: 0xf804,
        mnemonic: "unknown_f804",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f805 = (OPCODES[0xf805] = new Opcode({
        code: 0xf805,
        mnemonic: "unknown_f805",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f806 = (OPCODES[0xf806] = new Opcode({
        code: 0xf806,
        mnemonic: "unknown_f806",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f807 = (OPCODES[0xf807] = new Opcode({
        code: 0xf807,
        mnemonic: "unknown_f807",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly get_difficulty_level2 = (OPCODES[0xf808] = new Opcode({
        code: 0xf808,
        mnemonic: "get_difficulty_level2",
        doc:
            "Sets the given register to the current difficulty. 0 For normal, 1 for hard, 2 for very hard and 3 for ultimate.",
        params: [{ type: Type.RegRef }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly get_number_of_player1 = (OPCODES[0xf809] = new Opcode({
        code: 0xf809,
        mnemonic: "get_number_of_player1",
        params: [{ type: Type.RegRef }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly get_coord_of_player = (OPCODES[0xf80a] = new Opcode({
        code: 0xf80a,
        mnemonic: "get_coord_of_player",
        params: [{ type: Type.RegRef }, { type: Type.RegRef }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f80b = (OPCODES[0xf80b] = new Opcode({
        code: 0xf80b,
        mnemonic: "unknown_f80b",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f80c = (OPCODES[0xf80c] = new Opcode({
        code: 0xf80c,
        mnemonic: "unknown_f80c",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly map_designate_ex = (OPCODES[0xf80d] = new Opcode({
        code: 0xf80d,
        mnemonic: "map_designate_ex",
        params: [{ type: Type.RegRef }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f80e = (OPCODES[0xf80e] = new Opcode({
        code: 0xf80e,
        mnemonic: "unknown_f80e",
        params: [],
        push_stack: false,
        stack_params: [{ type: TYPE_U32 }],
    }));
    static readonly unknown_f80f = (OPCODES[0xf80f] = new Opcode({
        code: 0xf80f,
        mnemonic: "unknown_f80f",
        params: [],
        push_stack: false,
        stack_params: [{ type: TYPE_U32 }],
    }));
    static readonly ba_initial_floor = (OPCODES[0xf810] = new Opcode({
        code: 0xf810,
        mnemonic: "ba_initial_floor",
        params: [],
        push_stack: false,
        stack_params: [{ type: TYPE_U32 }],
    }));
    static readonly set_ba_rules = (OPCODES[0xf811] = new Opcode({
        code: 0xf811,
        mnemonic: "set_ba_rules",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f812 = (OPCODES[0xf812] = new Opcode({
        code: 0xf812,
        mnemonic: "unknown_f812",
        params: [],
        push_stack: false,
        stack_params: [{ type: TYPE_U32 }],
    }));
    static readonly unknown_f813 = (OPCODES[0xf813] = new Opcode({
        code: 0xf813,
        mnemonic: "unknown_f813",
        params: [],
        push_stack: false,
        stack_params: [{ type: TYPE_U32 }],
    }));
    static readonly unknown_f814 = (OPCODES[0xf814] = new Opcode({
        code: 0xf814,
        mnemonic: "unknown_f814",
        params: [],
        push_stack: false,
        stack_params: [{ type: TYPE_U32 }],
    }));
    static readonly unknown_f815 = (OPCODES[0xf815] = new Opcode({
        code: 0xf815,
        mnemonic: "unknown_f815",
        params: [],
        push_stack: false,
        stack_params: [{ type: TYPE_U32 }],
    }));
    static readonly unknown_f816 = (OPCODES[0xf816] = new Opcode({
        code: 0xf816,
        mnemonic: "unknown_f816",
        params: [],
        push_stack: false,
        stack_params: [{ type: TYPE_U32 }],
    }));
    static readonly unknown_f817 = (OPCODES[0xf817] = new Opcode({
        code: 0xf817,
        mnemonic: "unknown_f817",
        params: [],
        push_stack: false,
        stack_params: [{ type: TYPE_U32 }],
    }));
    static readonly unknown_f818 = (OPCODES[0xf818] = new Opcode({
        code: 0xf818,
        mnemonic: "unknown_f818",
        params: [],
        push_stack: false,
        stack_params: [{ type: TYPE_U32 }],
    }));
    static readonly unknown_f819 = (OPCODES[0xf819] = new Opcode({
        code: 0xf819,
        mnemonic: "unknown_f819",
        params: [],
        push_stack: false,
        stack_params: [{ type: TYPE_U32 }],
    }));
    static readonly unknown_f81a = (OPCODES[0xf81a] = new Opcode({
        code: 0xf81a,
        mnemonic: "unknown_f81a",
        params: [],
        push_stack: false,
        stack_params: [{ type: TYPE_U32 }],
    }));
    static readonly unknown_f81b = (OPCODES[0xf81b] = new Opcode({
        code: 0xf81b,
        mnemonic: "unknown_f81b",
        params: [],
        push_stack: false,
        stack_params: [{ type: TYPE_U32 }],
    }));
    static readonly ba_disp_msg = (OPCODES[0xf81c] = new Opcode({
        code: 0xf81c,
        mnemonic: "ba_disp_msg",
        params: [],
        push_stack: false,
        stack_params: [{ type: TYPE_STRING }],
    }));
    static readonly death_lvl_up = (OPCODES[0xf81d] = new Opcode({
        code: 0xf81d,
        mnemonic: "death_lvl_up",
        params: [],
        push_stack: false,
        stack_params: [{ type: TYPE_U32 }],
    }));
    static readonly death_tech_lvl_up = (OPCODES[0xf81e] = new Opcode({
        code: 0xf81e,
        mnemonic: "death_tech_lvl_up",
        params: [],
        push_stack: false,
        stack_params: [{ type: TYPE_U32 }],
    }));
    static readonly unknown_f81f = (OPCODES[0xf81f] = new Opcode({
        code: 0xf81f,
        mnemonic: "unknown_f81f",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly cmode_stage = (OPCODES[0xf820] = new Opcode({
        code: 0xf820,
        mnemonic: "cmode_stage",
        params: [],
        push_stack: false,
        stack_params: [{ type: TYPE_U32 }],
    }));
    static readonly unknown_f821 = (OPCODES[0xf821] = new Opcode({
        code: 0xf821,
        mnemonic: "unknown_f821",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f822 = (OPCODES[0xf822] = new Opcode({
        code: 0xf822,
        mnemonic: "unknown_f822",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f823 = (OPCODES[0xf823] = new Opcode({
        code: 0xf823,
        mnemonic: "unknown_f823",
        params: [],
        push_stack: false,
        stack_params: [{ type: TYPE_U32 }],
    }));
    static readonly unknown_f824 = (OPCODES[0xf824] = new Opcode({
        code: 0xf824,
        mnemonic: "unknown_f824",
        params: [],
        push_stack: false,
        stack_params: [{ type: TYPE_U32 }],
    }));
    static readonly exp_multiplication = (OPCODES[0xf825] = new Opcode({
        code: 0xf825,
        mnemonic: "exp_multiplication",
        params: [{ type: Type.RegRef }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly exp_division = (OPCODES[0xf826] = new Opcode({
        code: 0xf826,
        mnemonic: "exp_division",
        params: [{ type: Type.RegRef }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly get_user_is_dead = (OPCODES[0xf827] = new Opcode({
        code: 0xf827,
        mnemonic: "get_user_is_dead",
        params: [{ type: Type.RegRef }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly go_floor = (OPCODES[0xf828] = new Opcode({
        code: 0xf828,
        mnemonic: "go_floor",
        params: [{ type: Type.RegRef }, { type: Type.RegRef }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f829 = (OPCODES[0xf829] = new Opcode({
        code: 0xf829,
        mnemonic: "unknown_f829",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f82a = (OPCODES[0xf82a] = new Opcode({
        code: 0xf82a,
        mnemonic: "unknown_f82a",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unlock_door2 = (OPCODES[0xf82b] = new Opcode({
        code: 0xf82b,
        mnemonic: "unlock_door2",
        params: [],
        push_stack: false,
        stack_params: [{ type: TYPE_U32 }, { type: TYPE_U32 }],
    }));
    static readonly lock_door2 = (OPCODES[0xf82c] = new Opcode({
        code: 0xf82c,
        mnemonic: "lock_door2",
        params: [],
        push_stack: false,
        stack_params: [{ type: TYPE_U32 }, { type: TYPE_U32 }],
    }));
    static readonly if_switch_not_pressed = (OPCODES[0xf82d] = new Opcode({
        code: 0xf82d,
        mnemonic: "if_switch_not_pressed",
        params: [{ type: Type.RegRef }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly if_switch_pressed = (OPCODES[0xf82e] = new Opcode({
        code: 0xf82e,
        mnemonic: "if_switch_pressed",
        params: [{ type: Type.RegRef }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f82f = (OPCODES[0xf82f] = new Opcode({
        code: 0xf82f,
        mnemonic: "unknown_f82f",
        params: [],
        push_stack: false,
        stack_params: [{ type: TYPE_U32 }, { type: TYPE_U32 }],
    }));
    static readonly control_dragon = (OPCODES[0xf830] = new Opcode({
        code: 0xf830,
        mnemonic: "control_dragon",
        params: [{ type: Type.RegRef }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly release_dragon = (OPCODES[0xf831] = new Opcode({
        code: 0xf831,
        mnemonic: "release_dragon",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f832 = (OPCODES[0xf832] = new Opcode({
        code: 0xf832,
        mnemonic: "unknown_f832",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f833 = (OPCODES[0xf833] = new Opcode({
        code: 0xf833,
        mnemonic: "unknown_f833",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f834 = (OPCODES[0xf834] = new Opcode({
        code: 0xf834,
        mnemonic: "unknown_f834",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f835 = (OPCODES[0xf835] = new Opcode({
        code: 0xf835,
        mnemonic: "unknown_f835",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f836 = (OPCODES[0xf836] = new Opcode({
        code: 0xf836,
        mnemonic: "unknown_f836",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f837 = (OPCODES[0xf837] = new Opcode({
        code: 0xf837,
        mnemonic: "unknown_f837",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly shrink = (OPCODES[0xf838] = new Opcode({
        code: 0xf838,
        mnemonic: "shrink",
        params: [{ type: Type.RegRef }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unshrink = (OPCODES[0xf839] = new Opcode({
        code: 0xf839,
        mnemonic: "unshrink",
        params: [{ type: Type.RegRef }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f83a = (OPCODES[0xf83a] = new Opcode({
        code: 0xf83a,
        mnemonic: "unknown_f83a",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f83b = (OPCODES[0xf83b] = new Opcode({
        code: 0xf83b,
        mnemonic: "unknown_f83b",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly display_clock2 = (OPCODES[0xf83c] = new Opcode({
        code: 0xf83c,
        mnemonic: "display_clock2",
        params: [{ type: Type.RegRef }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f83d = (OPCODES[0xf83d] = new Opcode({
        code: 0xf83d,
        mnemonic: "unknown_f83d",
        params: [],
        push_stack: false,
        stack_params: [{ type: TYPE_U32 }],
    }));
    static readonly delete_area_title = (OPCODES[0xf83e] = new Opcode({
        code: 0xf83e,
        mnemonic: "delete_area_title",
        params: [],
        push_stack: false,
        stack_params: [{ type: TYPE_U32 }],
    }));
    static readonly unknown_f83f = (OPCODES[0xf83f] = new Opcode({
        code: 0xf83f,
        mnemonic: "unknown_f83f",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly load_npc_data = (OPCODES[0xf840] = new Opcode({
        code: 0xf840,
        mnemonic: "load_npc_data",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly get_npc_data = (OPCODES[0xf841] = new Opcode({
        code: 0xf841,
        mnemonic: "get_npc_data",
        params: [{ type: TYPE_D_LABEL }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f842 = (OPCODES[0xf842] = new Opcode({
        code: 0xf842,
        mnemonic: "unknown_f842",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f843 = (OPCODES[0xf843] = new Opcode({
        code: 0xf843,
        mnemonic: "unknown_f843",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f844 = (OPCODES[0xf844] = new Opcode({
        code: 0xf844,
        mnemonic: "unknown_f844",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f845 = (OPCODES[0xf845] = new Opcode({
        code: 0xf845,
        mnemonic: "unknown_f845",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f846 = (OPCODES[0xf846] = new Opcode({
        code: 0xf846,
        mnemonic: "unknown_f846",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f847 = (OPCODES[0xf847] = new Opcode({
        code: 0xf847,
        mnemonic: "unknown_f847",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly give_damage_score = (OPCODES[0xf848] = new Opcode({
        code: 0xf848,
        mnemonic: "give_damage_score",
        params: [{ type: Type.RegRef }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly take_damage_score = (OPCODES[0xf849] = new Opcode({
        code: 0xf849,
        mnemonic: "take_damage_score",
        params: [{ type: Type.RegRef }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unk_score_f84a = (OPCODES[0xf84a] = new Opcode({
        code: 0xf84a,
        mnemonic: "unk_score_f84a",
        params: [{ type: Type.RegRef }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unk_score_f84b = (OPCODES[0xf84b] = new Opcode({
        code: 0xf84b,
        mnemonic: "unk_score_f84b",
        params: [{ type: Type.RegRef }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly kill_score = (OPCODES[0xf84c] = new Opcode({
        code: 0xf84c,
        mnemonic: "kill_score",
        params: [{ type: Type.RegRef }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly death_score = (OPCODES[0xf84d] = new Opcode({
        code: 0xf84d,
        mnemonic: "death_score",
        params: [{ type: Type.RegRef }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unk_score_f84e = (OPCODES[0xf84e] = new Opcode({
        code: 0xf84e,
        mnemonic: "unk_score_f84e",
        params: [{ type: Type.RegRef }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly enemy_death_score = (OPCODES[0xf84f] = new Opcode({
        code: 0xf84f,
        mnemonic: "enemy_death_score",
        params: [{ type: Type.RegRef }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly meseta_score = (OPCODES[0xf850] = new Opcode({
        code: 0xf850,
        mnemonic: "meseta_score",
        params: [{ type: Type.RegRef }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f851 = (OPCODES[0xf851] = new Opcode({
        code: 0xf851,
        mnemonic: "unknown_f851",
        params: [{ type: Type.RegRef }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f852 = (OPCODES[0xf852] = new Opcode({
        code: 0xf852,
        mnemonic: "unknown_f852",
        params: [],
        push_stack: false,
        stack_params: [{ type: TYPE_U32 }],
    }));
    static readonly reverse_warps = (OPCODES[0xf853] = new Opcode({
        code: 0xf853,
        mnemonic: "reverse_warps",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unreverse_warps = (OPCODES[0xf854] = new Opcode({
        code: 0xf854,
        mnemonic: "unreverse_warps",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly set_ult_map = (OPCODES[0xf855] = new Opcode({
        code: 0xf855,
        mnemonic: "set_ult_map",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unset_ult_map = (OPCODES[0xf856] = new Opcode({
        code: 0xf856,
        mnemonic: "unset_ult_map",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly set_area_title = (OPCODES[0xf857] = new Opcode({
        code: 0xf857,
        mnemonic: "set_area_title",
        params: [],
        push_stack: false,
        stack_params: [{ type: TYPE_STRING }],
    }));
    static readonly unknown_f858 = (OPCODES[0xf858] = new Opcode({
        code: 0xf858,
        mnemonic: "unknown_f858",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f859 = (OPCODES[0xf859] = new Opcode({
        code: 0xf859,
        mnemonic: "unknown_f859",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly equip_item = (OPCODES[0xf85a] = new Opcode({
        code: 0xf85a,
        mnemonic: "equip_item",
        params: [{ type: Type.RegRef }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unequip_item = (OPCODES[0xf85b] = new Opcode({
        code: 0xf85b,
        mnemonic: "unequip_item",
        params: [],
        push_stack: false,
        stack_params: [{ type: TYPE_U32 }, { type: TYPE_U32 }],
    }));
    static readonly unknown_f85c = (OPCODES[0xf85c] = new Opcode({
        code: 0xf85c,
        mnemonic: "unknown_f85c",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f85d = (OPCODES[0xf85d] = new Opcode({
        code: 0xf85d,
        mnemonic: "unknown_f85d",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f85e = (OPCODES[0xf85e] = new Opcode({
        code: 0xf85e,
        mnemonic: "unknown_f85e",
        params: [],
        push_stack: false,
        stack_params: [{ type: TYPE_U32 }],
    }));
    static readonly unknown_f85f = (OPCODES[0xf85f] = new Opcode({
        code: 0xf85f,
        mnemonic: "unknown_f85f",
        params: [],
        push_stack: false,
        stack_params: [{ type: TYPE_U32 }],
    }));
    static readonly unknown_f860 = (OPCODES[0xf860] = new Opcode({
        code: 0xf860,
        mnemonic: "unknown_f860",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f861 = (OPCODES[0xf861] = new Opcode({
        code: 0xf861,
        mnemonic: "unknown_f861",
        params: [],
        push_stack: false,
        stack_params: [{ type: TYPE_U32 }],
    }));
    static readonly unknown_f862 = (OPCODES[0xf862] = new Opcode({
        code: 0xf862,
        mnemonic: "unknown_f862",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f863 = (OPCODES[0xf863] = new Opcode({
        code: 0xf863,
        mnemonic: "unknown_f863",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly cmode_rank = (OPCODES[0xf864] = new Opcode({
        code: 0xf864,
        mnemonic: "cmode_rank",
        params: [],
        push_stack: false,
        stack_params: [{ type: TYPE_U32 }, { type: TYPE_STRING }],
    }));
    static readonly award_item_name = (OPCODES[0xf865] = new Opcode({
        code: 0xf865,
        mnemonic: "award_item_name",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly award_item_select = (OPCODES[0xf866] = new Opcode({
        code: 0xf866,
        mnemonic: "award_item_select",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly award_item_give_to = (OPCODES[0xf867] = new Opcode({
        code: 0xf867,
        mnemonic: "award_item_give_to",
        params: [{ type: Type.RegRef }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f868 = (OPCODES[0xf868] = new Opcode({
        code: 0xf868,
        mnemonic: "unknown_f868",
        params: [{ type: Type.RegRef }, { type: Type.RegRef }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f869 = (OPCODES[0xf869] = new Opcode({
        code: 0xf869,
        mnemonic: "unknown_f869",
        params: [{ type: Type.RegRef }, { type: Type.RegRef }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly item_create_cmode = (OPCODES[0xf86a] = new Opcode({
        code: 0xf86a,
        mnemonic: "item_create_cmode",
        params: [{ type: Type.RegRef }, { type: Type.RegRef }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f86b = (OPCODES[0xf86b] = new Opcode({
        code: 0xf86b,
        mnemonic: "unknown_f86b",
        params: [{ type: Type.RegRef }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly award_item_ok = (OPCODES[0xf86c] = new Opcode({
        code: 0xf86c,
        mnemonic: "award_item_ok",
        params: [{ type: Type.RegRef }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f86d = (OPCODES[0xf86d] = new Opcode({
        code: 0xf86d,
        mnemonic: "unknown_f86d",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f86e = (OPCODES[0xf86e] = new Opcode({
        code: 0xf86e,
        mnemonic: "unknown_f86e",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly ba_set_lives = (OPCODES[0xf86f] = new Opcode({
        code: 0xf86f,
        mnemonic: "ba_set_lives",
        params: [],
        push_stack: false,
        stack_params: [{ type: TYPE_U32 }],
    }));
    static readonly ba_set_tech_lvl = (OPCODES[0xf870] = new Opcode({
        code: 0xf870,
        mnemonic: "ba_set_tech_lvl",
        params: [],
        push_stack: false,
        stack_params: [{ type: TYPE_U32 }],
    }));
    static readonly ba_set_lvl = (OPCODES[0xf871] = new Opcode({
        code: 0xf871,
        mnemonic: "ba_set_lvl",
        params: [],
        push_stack: false,
        stack_params: [{ type: TYPE_U32 }],
    }));
    static readonly ba_set_time_limit = (OPCODES[0xf872] = new Opcode({
        code: 0xf872,
        mnemonic: "ba_set_time_limit",
        params: [],
        push_stack: false,
        stack_params: [{ type: TYPE_U32 }],
    }));
    static readonly boss_is_dead = (OPCODES[0xf873] = new Opcode({
        code: 0xf873,
        mnemonic: "boss_is_dead",
        params: [{ type: Type.RegRef }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f874 = (OPCODES[0xf874] = new Opcode({
        code: 0xf874,
        mnemonic: "unknown_f874",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f875 = (OPCODES[0xf875] = new Opcode({
        code: 0xf875,
        mnemonic: "unknown_f875",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f876 = (OPCODES[0xf876] = new Opcode({
        code: 0xf876,
        mnemonic: "unknown_f876",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly enable_techs = (OPCODES[0xf877] = new Opcode({
        code: 0xf877,
        mnemonic: "enable_techs",
        params: [{ type: Type.RegRef }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly disable_techs = (OPCODES[0xf878] = new Opcode({
        code: 0xf878,
        mnemonic: "disable_techs",
        params: [{ type: Type.RegRef }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly get_gender = (OPCODES[0xf879] = new Opcode({
        code: 0xf879,
        mnemonic: "get_gender",
        params: [{ type: Type.RegRef }, { type: Type.RegRef }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly get_chara_class = (OPCODES[0xf87a] = new Opcode({
        code: 0xf87a,
        mnemonic: "get_chara_class",
        params: [{ type: Type.RegRef }, { type: Type.RegRef }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly take_slot_meseta = (OPCODES[0xf87b] = new Opcode({
        code: 0xf87b,
        mnemonic: "take_slot_meseta",
        params: [{ type: Type.RegRef }, { type: Type.RegRef }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f87c = (OPCODES[0xf87c] = new Opcode({
        code: 0xf87c,
        mnemonic: "unknown_f87c",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f87d = (OPCODES[0xf87d] = new Opcode({
        code: 0xf87d,
        mnemonic: "unknown_f87d",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f87e = (OPCODES[0xf87e] = new Opcode({
        code: 0xf87e,
        mnemonic: "unknown_f87e",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly read_guildcard_flag = (OPCODES[0xf87f] = new Opcode({
        code: 0xf87f,
        mnemonic: "read_guildcard_flag",
        params: [{ type: Type.RegRef }, { type: Type.RegRef }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f880 = (OPCODES[0xf880] = new Opcode({
        code: 0xf880,
        mnemonic: "unknown_f880",
        params: [{ type: Type.RegRef }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly get_pl_name = (OPCODES[0xf881] = new Opcode({
        code: 0xf881,
        mnemonic: "get_pl_name",
        params: [{ type: Type.RegRef }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f882 = (OPCODES[0xf882] = new Opcode({
        code: 0xf882,
        mnemonic: "unknown_f882",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f883 = (OPCODES[0xf883] = new Opcode({
        code: 0xf883,
        mnemonic: "unknown_f883",
        params: [{ type: Type.RegRef }, { type: Type.RegRef }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f884 = (OPCODES[0xf884] = new Opcode({
        code: 0xf884,
        mnemonic: "unknown_f884",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f885 = (OPCODES[0xf885] = new Opcode({
        code: 0xf885,
        mnemonic: "unknown_f885",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f886 = (OPCODES[0xf886] = new Opcode({
        code: 0xf886,
        mnemonic: "unknown_f886",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f887 = (OPCODES[0xf887] = new Opcode({
        code: 0xf887,
        mnemonic: "unknown_f887",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly ba_close_msg = (OPCODES[0xf888] = new Opcode({
        code: 0xf888,
        mnemonic: "ba_close_msg",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f889 = (OPCODES[0xf889] = new Opcode({
        code: 0xf889,
        mnemonic: "unknown_f889",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly get_player_status = (OPCODES[0xf88a] = new Opcode({
        code: 0xf88a,
        mnemonic: "get_player_status",
        params: [{ type: Type.RegRef }, { type: Type.RegRef }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly send_mail = (OPCODES[0xf88b] = new Opcode({
        code: 0xf88b,
        mnemonic: "send_mail",
        params: [],
        push_stack: false,
        stack_params: [{ type: Type.RegRef }, { type: TYPE_STRING }],
    }));
    static readonly online_check = (OPCODES[0xf88c] = new Opcode({
        code: 0xf88c,
        mnemonic: "online_check",
        params: [{ type: Type.RegRef }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly chl_set_timerecord = (OPCODES[0xf88d] = new Opcode({
        code: 0xf88d,
        mnemonic: "chl_set_timerecord",
        params: [{ type: Type.RegRef }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly chl_get_timerecord = (OPCODES[0xf88e] = new Opcode({
        code: 0xf88e,
        mnemonic: "chl_get_timerecord",
        params: [{ type: Type.RegRef }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f88f = (OPCODES[0xf88f] = new Opcode({
        code: 0xf88f,
        mnemonic: "unknown_f88f",
        params: [{ type: Type.RegRef }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f890 = (OPCODES[0xf890] = new Opcode({
        code: 0xf890,
        mnemonic: "unknown_f890",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly load_enemy_data = (OPCODES[0xf891] = new Opcode({
        code: 0xf891,
        mnemonic: "load_enemy_data",
        params: [],
        push_stack: false,
        stack_params: [{ type: TYPE_U32 }],
    }));
    static readonly get_physical_data = (OPCODES[0xf892] = new Opcode({
        code: 0xf892,
        mnemonic: "get_physical_data",
        params: [{ type: TYPE_U16 }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly get_attack_data = (OPCODES[0xf893] = new Opcode({
        code: 0xf893,
        mnemonic: "get_attack_data",
        params: [{ type: TYPE_U16 }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly get_resist_data = (OPCODES[0xf894] = new Opcode({
        code: 0xf894,
        mnemonic: "get_resist_data",
        params: [{ type: TYPE_U16 }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly get_movement_data = (OPCODES[0xf895] = new Opcode({
        code: 0xf895,
        mnemonic: "get_movement_data",
        params: [{ type: TYPE_U16 }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f896 = (OPCODES[0xf896] = new Opcode({
        code: 0xf896,
        mnemonic: "unknown_f896",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f897 = (OPCODES[0xf897] = new Opcode({
        code: 0xf897,
        mnemonic: "unknown_f897",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly shift_left = (OPCODES[0xf898] = new Opcode({
        code: 0xf898,
        mnemonic: "shift_left",
        params: [{ type: Type.RegRef }, { type: Type.RegRef }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly shift_right = (OPCODES[0xf899] = new Opcode({
        code: 0xf899,
        mnemonic: "shift_right",
        params: [{ type: Type.RegRef }, { type: Type.RegRef }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly get_random = (OPCODES[0xf89a] = new Opcode({
        code: 0xf89a,
        mnemonic: "get_random",
        params: [{ type: Type.RegRef }, { type: Type.RegRef }],
        push_stack: false,
        stack_params: [],
    }));
    // Resets all registers to 0 (may have to change areas?).
    static readonly reset_map = (OPCODES[0xf89b] = new Opcode({
        code: 0xf89b,
        mnemonic: "reset_map",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly disp_chl_retry_menu = (OPCODES[0xf89c] = new Opcode({
        code: 0xf89c,
        mnemonic: "disp_chl_retry_menu",
        params: [{ type: Type.RegRef }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly chl_reverser = (OPCODES[0xf89d] = new Opcode({
        code: 0xf89d,
        mnemonic: "chl_reverser",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f89e = (OPCODES[0xf89e] = new Opcode({
        code: 0xf89e,
        mnemonic: "unknown_f89e",
        params: [],
        push_stack: false,
        stack_params: [{ type: TYPE_U32 }],
    }));
    static readonly unknown_f89f = (OPCODES[0xf89f] = new Opcode({
        code: 0xf89f,
        mnemonic: "unknown_f89f",
        params: [{ type: Type.RegRef }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f8a0 = (OPCODES[0xf8a0] = new Opcode({
        code: 0xf8a0,
        mnemonic: "unknown_f8a0",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f8a1 = (OPCODES[0xf8a1] = new Opcode({
        code: 0xf8a1,
        mnemonic: "unknown_f8a1",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f8a2 = (OPCODES[0xf8a2] = new Opcode({
        code: 0xf8a2,
        mnemonic: "unknown_f8a2",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f8a3 = (OPCODES[0xf8a3] = new Opcode({
        code: 0xf8a3,
        mnemonic: "unknown_f8a3",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f8a4 = (OPCODES[0xf8a4] = new Opcode({
        code: 0xf8a4,
        mnemonic: "unknown_f8a4",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f8a5 = (OPCODES[0xf8a5] = new Opcode({
        code: 0xf8a5,
        mnemonic: "unknown_f8a5",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f8a6 = (OPCODES[0xf8a6] = new Opcode({
        code: 0xf8a6,
        mnemonic: "unknown_f8a6",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f8a7 = (OPCODES[0xf8a7] = new Opcode({
        code: 0xf8a7,
        mnemonic: "unknown_f8a7",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f8a8 = (OPCODES[0xf8a8] = new Opcode({
        code: 0xf8a8,
        mnemonic: "unknown_f8a8",
        params: [],
        push_stack: false,
        stack_params: [{ type: TYPE_U32 }],
    }));
    static readonly unknown_f8a9 = (OPCODES[0xf8a9] = new Opcode({
        code: 0xf8a9,
        mnemonic: "unknown_f8a9",
        params: [{ type: Type.RegRef }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f8aa = (OPCODES[0xf8aa] = new Opcode({
        code: 0xf8aa,
        mnemonic: "unknown_f8aa",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f8ab = (OPCODES[0xf8ab] = new Opcode({
        code: 0xf8ab,
        mnemonic: "unknown_f8ab",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f8ac = (OPCODES[0xf8ac] = new Opcode({
        code: 0xf8ac,
        mnemonic: "unknown_f8ac",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly get_number_of_player2 = (OPCODES[0xf8ad] = new Opcode({
        code: 0xf8ad,
        mnemonic: "get_number_of_player2",
        params: [{ type: Type.RegRef }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f8ae = (OPCODES[0xf8ae] = new Opcode({
        code: 0xf8ae,
        mnemonic: "unknown_f8ae",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f8af = (OPCODES[0xf8af] = new Opcode({
        code: 0xf8af,
        mnemonic: "unknown_f8af",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f8b0 = (OPCODES[0xf8b0] = new Opcode({
        code: 0xf8b0,
        mnemonic: "unknown_f8b0",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f8b1 = (OPCODES[0xf8b1] = new Opcode({
        code: 0xf8b1,
        mnemonic: "unknown_f8b1",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f8b2 = (OPCODES[0xf8b2] = new Opcode({
        code: 0xf8b2,
        mnemonic: "unknown_f8b2",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f8b3 = (OPCODES[0xf8b3] = new Opcode({
        code: 0xf8b3,
        mnemonic: "unknown_f8b3",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f8b4 = (OPCODES[0xf8b4] = new Opcode({
        code: 0xf8b4,
        mnemonic: "unknown_f8b4",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f8b5 = (OPCODES[0xf8b5] = new Opcode({
        code: 0xf8b5,
        mnemonic: "unknown_f8b5",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f8b6 = (OPCODES[0xf8b6] = new Opcode({
        code: 0xf8b6,
        mnemonic: "unknown_f8b6",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f8b7 = (OPCODES[0xf8b7] = new Opcode({
        code: 0xf8b7,
        mnemonic: "unknown_f8b7",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f8b8 = (OPCODES[0xf8b8] = new Opcode({
        code: 0xf8b8,
        mnemonic: "unknown_f8b8",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly chl_recovery = (OPCODES[0xf8b9] = new Opcode({
        code: 0xf8b9,
        mnemonic: "chl_recovery",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f8ba = (OPCODES[0xf8ba] = new Opcode({
        code: 0xf8ba,
        mnemonic: "unknown_f8ba",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f8bb = (OPCODES[0xf8bb] = new Opcode({
        code: 0xf8bb,
        mnemonic: "unknown_f8bb",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly set_episode = (OPCODES[0xf8bc] = new Opcode({
        code: 0xf8bc,
        mnemonic: "set_episode",
        params: [{ type: TYPE_U32 }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f8bd = (OPCODES[0xf8bd] = new Opcode({
        code: 0xf8bd,
        mnemonic: "unknown_f8bd",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f8be = (OPCODES[0xf8be] = new Opcode({
        code: 0xf8be,
        mnemonic: "unknown_f8be",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f8bf = (OPCODES[0xf8bf] = new Opcode({
        code: 0xf8bf,
        mnemonic: "unknown_f8bf",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly file_dl_req = (OPCODES[0xf8c0] = new Opcode({
        code: 0xf8c0,
        mnemonic: "file_dl_req",
        params: [],
        push_stack: false,
        stack_params: [{ type: TYPE_U32 }, { type: TYPE_STRING }],
    }));
    static readonly get_dl_status = (OPCODES[0xf8c1] = new Opcode({
        code: 0xf8c1,
        mnemonic: "get_dl_status",
        params: [{ type: Type.RegRef }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly gba_unknown4 = (OPCODES[0xf8c2] = new Opcode({
        code: 0xf8c2,
        mnemonic: "gba_unknown4",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly get_gba_state = (OPCODES[0xf8c3] = new Opcode({
        code: 0xf8c3,
        mnemonic: "get_gba_state",
        params: [{ type: Type.RegRef }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f8c4 = (OPCODES[0xf8c4] = new Opcode({
        code: 0xf8c4,
        mnemonic: "unknown_f8c4",
        params: [{ type: Type.RegRef }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f8c5 = (OPCODES[0xf8c5] = new Opcode({
        code: 0xf8c5,
        mnemonic: "unknown_f8c5",
        params: [{ type: Type.RegRef }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly qexit = (OPCODES[0xf8c6] = new Opcode({
        code: 0xf8c6,
        mnemonic: "qexit",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly use_animation = (OPCODES[0xf8c7] = new Opcode({
        code: 0xf8c7,
        mnemonic: "use_animation",
        params: [{ type: Type.RegRef }, { type: Type.RegRef }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly stop_animation = (OPCODES[0xf8c8] = new Opcode({
        code: 0xf8c8,
        mnemonic: "stop_animation",
        params: [{ type: Type.RegRef }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly run_to_coord = (OPCODES[0xf8c9] = new Opcode({
        code: 0xf8c9,
        mnemonic: "run_to_coord",
        params: [{ type: Type.RegRef }, { type: Type.RegRef }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly set_slot_invincible = (OPCODES[0xf8ca] = new Opcode({
        code: 0xf8ca,
        mnemonic: "set_slot_invincible",
        params: [{ type: Type.RegRef }, { type: Type.RegRef }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f8cb = (OPCODES[0xf8cb] = new Opcode({
        code: 0xf8cb,
        mnemonic: "unknown_f8cb",
        params: [{ type: Type.RegRef }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly set_slot_poison = (OPCODES[0xf8cc] = new Opcode({
        code: 0xf8cc,
        mnemonic: "set_slot_poison",
        params: [{ type: Type.RegRef }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly set_slot_paralyze = (OPCODES[0xf8cd] = new Opcode({
        code: 0xf8cd,
        mnemonic: "set_slot_paralyze",
        params: [{ type: Type.RegRef }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly set_slot_shock = (OPCODES[0xf8ce] = new Opcode({
        code: 0xf8ce,
        mnemonic: "set_slot_shock",
        params: [{ type: Type.RegRef }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly set_slot_freeze = (OPCODES[0xf8cf] = new Opcode({
        code: 0xf8cf,
        mnemonic: "set_slot_freeze",
        params: [{ type: Type.RegRef }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly set_slot_slow = (OPCODES[0xf8d0] = new Opcode({
        code: 0xf8d0,
        mnemonic: "set_slot_slow",
        params: [{ type: Type.RegRef }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly set_slot_confuse = (OPCODES[0xf8d1] = new Opcode({
        code: 0xf8d1,
        mnemonic: "set_slot_confuse",
        params: [{ type: Type.RegRef }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly set_slot_shifta = (OPCODES[0xf8d2] = new Opcode({
        code: 0xf8d2,
        mnemonic: "set_slot_shifta",
        params: [{ type: Type.RegRef }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly set_slot_deband = (OPCODES[0xf8d3] = new Opcode({
        code: 0xf8d3,
        mnemonic: "set_slot_deband",
        params: [{ type: Type.RegRef }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly set_slot_jellen = (OPCODES[0xf8d4] = new Opcode({
        code: 0xf8d4,
        mnemonic: "set_slot_jellen",
        params: [{ type: Type.RegRef }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly set_slot_zalure = (OPCODES[0xf8d5] = new Opcode({
        code: 0xf8d5,
        mnemonic: "set_slot_zalure",
        params: [{ type: Type.RegRef }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly fleti_fixed_camera = (OPCODES[0xf8d6] = new Opcode({
        code: 0xf8d6,
        mnemonic: "fleti_fixed_camera",
        params: [],
        push_stack: false,
        stack_params: [{ type: Type.RegRef }],
    }));
    static readonly fleti_locked_camera = (OPCODES[0xf8d7] = new Opcode({
        code: 0xf8d7,
        mnemonic: "fleti_locked_camera",
        params: [],
        push_stack: false,
        stack_params: [{ type: TYPE_U32 }, { type: Type.RegRef }],
    }));
    static readonly default_camera_pos2 = (OPCODES[0xf8d8] = new Opcode({
        code: 0xf8d8,
        mnemonic: "default_camera_pos2",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly set_motion_blur = (OPCODES[0xf8d9] = new Opcode({
        code: 0xf8d9,
        mnemonic: "set_motion_blur",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly set_screen_bw = (OPCODES[0xf8da] = new Opcode({
        code: 0xf8da,
        mnemonic: "set_screen_bw",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f8db = (OPCODES[0xf8db] = new Opcode({
        code: 0xf8db,
        mnemonic: "unknown_f8db",
        params: [],
        push_stack: false,
        stack_params: [
            { type: TYPE_U32 },
            { type: TYPE_U32 },
            { type: TYPE_U32 },
            { type: TYPE_U32 },
            { type: Type.RegRef },
            { type: TYPE_U16 },
        ],
    }));
    // TODO: 3rd parameter is a string data reference.
    static readonly npc_action_string = (OPCODES[0xf8dc] = new Opcode({
        code: 0xf8dc,
        mnemonic: "npc_action_string",
        params: [{ type: Type.RegRef }, { type: Type.RegRef }, { type: TYPE_D_LABEL }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly get_pad_cond = (OPCODES[0xf8dd] = new Opcode({
        code: 0xf8dd,
        mnemonic: "get_pad_cond",
        params: [{ type: Type.RegRef }, { type: Type.RegRef }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly get_button_cond = (OPCODES[0xf8de] = new Opcode({
        code: 0xf8de,
        mnemonic: "get_button_cond",
        params: [{ type: Type.RegRef }, { type: Type.RegRef }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly freeze_enemies = (OPCODES[0xf8df] = new Opcode({
        code: 0xf8df,
        mnemonic: "freeze_enemies",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unfreeze_enemies = (OPCODES[0xf8e0] = new Opcode({
        code: 0xf8e0,
        mnemonic: "unfreeze_enemies",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly freeze_everything = (OPCODES[0xf8e1] = new Opcode({
        code: 0xf8e1,
        mnemonic: "freeze_everything",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unfreeze_everything = (OPCODES[0xf8e2] = new Opcode({
        code: 0xf8e2,
        mnemonic: "unfreeze_everything",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly restore_hp = (OPCODES[0xf8e3] = new Opcode({
        code: 0xf8e3,
        mnemonic: "restore_hp",
        params: [{ type: Type.RegRef }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly restore_tp = (OPCODES[0xf8e4] = new Opcode({
        code: 0xf8e4,
        mnemonic: "restore_tp",
        params: [{ type: Type.RegRef }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly close_chat_bubble = (OPCODES[0xf8e5] = new Opcode({
        code: 0xf8e5,
        mnemonic: "close_chat_bubble",
        params: [{ type: Type.RegRef }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly move_coords_object = (OPCODES[0xf8e6] = new Opcode({
        code: 0xf8e6,
        mnemonic: "move_coords_object ",
        params: [{ type: Type.RegRef }, { type: Type.RegRef }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly at_coords_call_ex = (OPCODES[0xf8e7] = new Opcode({
        code: 0xf8e7,
        mnemonic: "at_coords_call_ex",
        params: [{ type: Type.RegRef }, { type: Type.RegRef }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f8e8 = (OPCODES[0xf8e8] = new Opcode({
        code: 0xf8e8,
        mnemonic: "unknown_f8e8",
        params: [{ type: Type.RegRef }, { type: Type.RegRef }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f8e9 = (OPCODES[0xf8e9] = new Opcode({
        code: 0xf8e9,
        mnemonic: "unknown_f8e9",
        params: [{ type: Type.RegRef }, { type: Type.RegRef }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f8ea = (OPCODES[0xf8ea] = new Opcode({
        code: 0xf8ea,
        mnemonic: "unknown_f8ea",
        params: [{ type: Type.RegRef }, { type: Type.RegRef }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f8eb = (OPCODES[0xf8eb] = new Opcode({
        code: 0xf8eb,
        mnemonic: "unknown_f8eb",
        params: [{ type: Type.RegRef }, { type: Type.RegRef }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f8ec = (OPCODES[0xf8ec] = new Opcode({
        code: 0xf8ec,
        mnemonic: "unknown_f8ec",
        params: [{ type: Type.RegRef }, { type: Type.RegRef }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly animation_check = (OPCODES[0xf8ed] = new Opcode({
        code: 0xf8ed,
        mnemonic: "animation_check",
        params: [{ type: Type.RegRef }, { type: Type.RegRef }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly call_image_data = (OPCODES[0xf8ee] = new Opcode({
        code: 0xf8ee,
        mnemonic: "call_image_data",
        params: [],
        push_stack: false,
        stack_params: [{ type: TYPE_U32 }, { type: TYPE_U16 }],
    }));
    static readonly unknown_f8ef = (OPCODES[0xf8ef] = new Opcode({
        code: 0xf8ef,
        mnemonic: "unknown_f8ef",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly turn_off_bgm_p2 = (OPCODES[0xf8f0] = new Opcode({
        code: 0xf8f0,
        mnemonic: "turn_off_bgm_p2",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly turn_on_bgm_p2 = (OPCODES[0xf8f1] = new Opcode({
        code: 0xf8f1,
        mnemonic: "turn_on_bgm_p2",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly load_unk_data = (OPCODES[0xf8f2] = new Opcode({
        code: 0xf8f2,
        mnemonic: "load_unk_data",
        params: [],
        push_stack: false,
        stack_params: [
            { type: TYPE_U32 },
            { type: TYPE_U32 },
            { type: TYPE_U32 },
            { type: TYPE_U32 },
            { type: Type.RegRef },
            { type: TYPE_D_LABEL },
        ],
    }));
    static readonly particle2 = (OPCODES[0xf8f3] = new Opcode({
        code: 0xf8f3,
        mnemonic: "particle2",
        params: [],
        push_stack: false,
        stack_params: [{ type: Type.RegRef }, { type: TYPE_U32 }, { type: TYPE_F32 }],
    }));
    static readonly unknown_f8f4 = (OPCODES[0xf8f4] = new Opcode({
        code: 0xf8f4,
        mnemonic: "unknown_f8f4",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f8f5 = (OPCODES[0xf8f5] = new Opcode({
        code: 0xf8f5,
        mnemonic: "unknown_f8f5",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f8f6 = (OPCODES[0xf8f6] = new Opcode({
        code: 0xf8f6,
        mnemonic: "unknown_f8f6",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f8f7 = (OPCODES[0xf8f7] = new Opcode({
        code: 0xf8f7,
        mnemonic: "unknown_f8f7",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f8f8 = (OPCODES[0xf8f8] = new Opcode({
        code: 0xf8f8,
        mnemonic: "unknown_f8f8",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f8f9 = (OPCODES[0xf8f9] = new Opcode({
        code: 0xf8f9,
        mnemonic: "unknown_f8f9",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f8fa = (OPCODES[0xf8fa] = new Opcode({
        code: 0xf8fa,
        mnemonic: "unknown_f8fa",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f8fb = (OPCODES[0xf8fb] = new Opcode({
        code: 0xf8fb,
        mnemonic: "unknown_f8fb",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f8fc = (OPCODES[0xf8fc] = new Opcode({
        code: 0xf8fc,
        mnemonic: "unknown_f8fc",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f8fd = (OPCODES[0xf8fd] = new Opcode({
        code: 0xf8fd,
        mnemonic: "unknown_f8fd",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f8fe = (OPCODES[0xf8fe] = new Opcode({
        code: 0xf8fe,
        mnemonic: "unknown_f8fe",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f8ff = (OPCODES[0xf8ff] = new Opcode({
        code: 0xf8ff,
        mnemonic: "unknown_f8ff",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f900 = (OPCODES[0xf900] = new Opcode({
        code: 0xf900,
        mnemonic: "unknown_f900",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly dec2float = (OPCODES[0xf901] = new Opcode({
        code: 0xf901,
        mnemonic: "dec2float",
        params: [{ type: Type.RegRef }, { type: Type.RegRef }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly float2dec = (OPCODES[0xf902] = new Opcode({
        code: 0xf902,
        mnemonic: "float2dec",
        params: [{ type: Type.RegRef }, { type: Type.RegRef }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly flet = (OPCODES[0xf903] = new Opcode({
        code: 0xf903,
        mnemonic: "flet",
        params: [{ type: Type.RegRef }, { type: Type.RegRef }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly fleti = (OPCODES[0xf904] = new Opcode({
        code: 0xf904,
        mnemonic: "fleti",
        params: [{ type: Type.RegRef }, { type: TYPE_F32 }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f905 = (OPCODES[0xf905] = new Opcode({
        code: 0xf905,
        mnemonic: "unknown_f905",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f906 = (OPCODES[0xf906] = new Opcode({
        code: 0xf906,
        mnemonic: "unknown_f906",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f907 = (OPCODES[0xf907] = new Opcode({
        code: 0xf907,
        mnemonic: "unknown_f907",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly fadd = (OPCODES[0xf908] = new Opcode({
        code: 0xf908,
        mnemonic: "fadd",
        params: [{ type: Type.RegRef }, { type: Type.RegRef }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly faddi = (OPCODES[0xf909] = new Opcode({
        code: 0xf909,
        mnemonic: "faddi",
        params: [{ type: Type.RegRef }, { type: TYPE_F32 }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly fsub = (OPCODES[0xf90a] = new Opcode({
        code: 0xf90a,
        mnemonic: "fsub",
        params: [{ type: Type.RegRef }, { type: Type.RegRef }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly fsubi = (OPCODES[0xf90b] = new Opcode({
        code: 0xf90b,
        mnemonic: "fsubi",
        params: [{ type: Type.RegRef }, { type: TYPE_F32 }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly fmul = (OPCODES[0xf90c] = new Opcode({
        code: 0xf90c,
        mnemonic: "fmul",
        params: [{ type: Type.RegRef }, { type: Type.RegRef }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly fmuli = (OPCODES[0xf90d] = new Opcode({
        code: 0xf90d,
        mnemonic: "fmuli",
        params: [{ type: Type.RegRef }, { type: TYPE_F32 }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly fdiv = (OPCODES[0xf90e] = new Opcode({
        code: 0xf90e,
        mnemonic: "fdiv",
        params: [{ type: Type.RegRef }, { type: Type.RegRef }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly fdivi = (OPCODES[0xf90f] = new Opcode({
        code: 0xf90f,
        mnemonic: "fdivi",
        params: [{ type: Type.RegRef }, { type: TYPE_F32 }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly get_unknown_count = (OPCODES[0xf910] = new Opcode({
        code: 0xf910,
        mnemonic: "get_unknown_count",
        params: [],
        push_stack: false,
        stack_params: [{ type: TYPE_U32 }, { type: Type.RegRef }],
    }));
    static readonly get_stackable_item_count = (OPCODES[0xf911] = new Opcode({
        code: 0xf911,
        mnemonic: "get_stackable_item_count",
        params: [{ type: Type.RegRef }, { type: Type.RegRef }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly freeze_and_hide_equip = (OPCODES[0xf912] = new Opcode({
        code: 0xf912,
        mnemonic: "freeze_and_hide_equip",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly thaw_and_show_equip = (OPCODES[0xf913] = new Opcode({
        code: 0xf913,
        mnemonic: "thaw_and_show_equip",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly set_palettex_callback = (OPCODES[0xf914] = new Opcode({
        code: 0xf914,
        mnemonic: "set_palettex_callback",
        params: [],
        push_stack: false,
        stack_params: [{ type: Type.RegRef }, { type: TYPE_I_LABEL }],
    }));
    static readonly activate_palettex = (OPCODES[0xf915] = new Opcode({
        code: 0xf915,
        mnemonic: "activate_palettex",
        params: [],
        push_stack: false,
        stack_params: [{ type: Type.RegRef }],
    }));
    static readonly enable_palettex = (OPCODES[0xf916] = new Opcode({
        code: 0xf916,
        mnemonic: "enable_palettex",
        params: [],
        push_stack: false,
        stack_params: [{ type: Type.RegRef }],
    }));
    static readonly restore_palettex = (OPCODES[0xf917] = new Opcode({
        code: 0xf917,
        mnemonic: "restore_palettex",
        params: [],
        push_stack: false,
        stack_params: [{ type: TYPE_U32 }],
    }));
    static readonly disable_palettex = (OPCODES[0xf918] = new Opcode({
        code: 0xf918,
        mnemonic: "disable_palettex",
        params: [],
        push_stack: false,
        stack_params: [{ type: TYPE_U32 }],
    }));
    static readonly get_palettex_activated = (OPCODES[0xf919] = new Opcode({
        code: 0xf919,
        mnemonic: "get_palettex_activated",
        params: [],
        push_stack: false,
        stack_params: [{ type: TYPE_U32 }, { type: Type.RegRef }],
    }));
    static readonly get_unknown_palettex_status = (OPCODES[0xf91a] = new Opcode({
        code: 0xf91a,
        mnemonic: "get_unknown_palettex_status",
        params: [],
        push_stack: false,
        stack_params: [{ type: TYPE_U32 }, { type: Type.RegRef }],
    }));
    static readonly disable_movement2 = (OPCODES[0xf91b] = new Opcode({
        code: 0xf91b,
        mnemonic: "disable_movement2",
        params: [],
        push_stack: false,
        stack_params: [{ type: Type.RegRef }],
    }));
    static readonly enable_movement2 = (OPCODES[0xf91c] = new Opcode({
        code: 0xf91c,
        mnemonic: "enable_movement2",
        params: [],
        push_stack: false,
        stack_params: [{ type: Type.RegRef }],
    }));
    static readonly get_time_played = (OPCODES[0xf91d] = new Opcode({
        code: 0xf91d,
        mnemonic: "get_time_played",
        params: [{ type: Type.RegRef }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly get_guildcard_total = (OPCODES[0xf91e] = new Opcode({
        code: 0xf91e,
        mnemonic: "get_guildcard_total",
        params: [{ type: Type.RegRef }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly get_slot_meseta = (OPCODES[0xf91f] = new Opcode({
        code: 0xf91f,
        mnemonic: "get_slot_meseta",
        params: [{ type: Type.RegRef }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly get_player_level = (OPCODES[0xf920] = new Opcode({
        code: 0xf920,
        mnemonic: "get_player_level",
        params: [],
        push_stack: false,
        stack_params: [{ type: TYPE_U32 }, { type: Type.RegRef }],
    }));
    static readonly get_section_id = (OPCODES[0xf921] = new Opcode({
        code: 0xf921,
        mnemonic: "get_section_id",
        params: [],
        push_stack: false,
        stack_params: [{ type: TYPE_U32 }, { type: Type.RegRef }],
    }));
    static readonly get_player_hp = (OPCODES[0xf922] = new Opcode({
        code: 0xf922,
        mnemonic: "get_player_hp",
        params: [],
        push_stack: false,
        stack_params: [{ type: Type.RegRef }, { type: Type.RegRef }],
    }));
    static readonly get_floor_number = (OPCODES[0xf923] = new Opcode({
        code: 0xf923,
        mnemonic: "get_floor_number",
        params: [],
        push_stack: false,
        stack_params: [{ type: Type.RegRef }, { type: Type.RegRef }],
    }));
    static readonly get_coord_player_detect = (OPCODES[0xf924] = new Opcode({
        code: 0xf924,
        mnemonic: "get_coord_player_detect",
        params: [{ type: Type.RegRef }, { type: Type.RegRef }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly read_global_flag = (OPCODES[0xf925] = new Opcode({
        code: 0xf925,
        mnemonic: "read_global_flag",
        params: [],
        push_stack: false,
        stack_params: [{ type: TYPE_U8 }, { type: Type.RegRef }],
    }));
    static readonly write_global_flag = (OPCODES[0xf926] = new Opcode({
        code: 0xf926,
        mnemonic: "write_global_flag",
        params: [],
        push_stack: false,
        stack_params: [{ type: TYPE_U8 }, { type: Type.RegRef }],
    }));
    static readonly unknown_f927 = (OPCODES[0xf927] = new Opcode({
        code: 0xf927,
        mnemonic: "unknown_f927",
        params: [{ type: Type.RegRef }, { type: Type.RegRef }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly floor_player_detect = (OPCODES[0xf928] = new Opcode({
        code: 0xf928,
        mnemonic: "floor_player_detect",
        params: [{ type: Type.RegRef }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly read_disk_file = (OPCODES[0xf929] = new Opcode({
        code: 0xf929,
        mnemonic: "read_disk_file",
        params: [],
        push_stack: false,
        stack_params: [{ type: TYPE_STRING }],
    }));
    static readonly open_pack_select = (OPCODES[0xf92a] = new Opcode({
        code: 0xf92a,
        mnemonic: "open_pack_select",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly item_select = (OPCODES[0xf92b] = new Opcode({
        code: 0xf92b,
        mnemonic: "item_select",
        params: [{ type: Type.RegRef }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly get_item_id = (OPCODES[0xf92c] = new Opcode({
        code: 0xf92c,
        mnemonic: "get_item_id",
        params: [{ type: Type.RegRef }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly color_change = (OPCODES[0xf92d] = new Opcode({
        code: 0xf92d,
        mnemonic: "color_change",
        params: [],
        push_stack: false,
        stack_params: [
            { type: Type.RegRef },
            { type: Type.RegRef },
            { type: Type.RegRef },
            { type: Type.RegRef },
            { type: Type.RegRef },
        ],
    }));
    static readonly send_statistic = (OPCODES[0xf92e] = new Opcode({
        code: 0xf92e,
        mnemonic: "send_statistic",
        params: [],
        push_stack: false,
        stack_params: [
            { type: TYPE_U32 },
            { type: TYPE_U32 },
            { type: TYPE_U32 },
            { type: TYPE_U32 },
            { type: TYPE_U32 },
            { type: TYPE_U32 },
            { type: TYPE_U32 },
            { type: TYPE_U32 },
        ],
    }));
    static readonly unknown_f92f = (OPCODES[0xf92f] = new Opcode({
        code: 0xf92f,
        mnemonic: "unknown_f92f",
        params: [],
        push_stack: false,
        stack_params: [{ type: TYPE_U32 }, { type: TYPE_U32 }],
    }));
    static readonly chat_box = (OPCODES[0xf930] = new Opcode({
        code: 0xf930,
        mnemonic: "chat_box",
        params: [],
        push_stack: false,
        stack_params: [
            { type: TYPE_U32 },
            { type: TYPE_U32 },
            { type: TYPE_U32 },
            { type: TYPE_U32 },
            { type: TYPE_U32 },
            { type: TYPE_STRING },
        ],
    }));
    static readonly chat_bubble = (OPCODES[0xf931] = new Opcode({
        code: 0xf931,
        mnemonic: "chat_bubble",
        params: [],
        push_stack: false,
        stack_params: [{ type: TYPE_U32 }, { type: TYPE_STRING }],
    }));
    static readonly unknown_f932 = (OPCODES[0xf932] = new Opcode({
        code: 0xf932,
        mnemonic: "unknown_f932",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f933 = (OPCODES[0xf933] = new Opcode({
        code: 0xf933,
        mnemonic: "unknown_f933",
        params: [{ type: Type.RegRef }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly scroll_text = (OPCODES[0xf934] = new Opcode({
        code: 0xf934,
        mnemonic: "scroll_text",
        params: [],
        push_stack: false,
        stack_params: [
            { type: TYPE_U32 },
            { type: TYPE_U32 },
            { type: TYPE_U32 },
            { type: TYPE_U32 },
            { type: TYPE_U32 },
            { type: TYPE_F32 },
            { type: Type.RegRef },
            { type: TYPE_STRING },
        ],
    }));
    static readonly gba_unknown1 = (OPCODES[0xf935] = new Opcode({
        code: 0xf935,
        mnemonic: "gba_unknown1",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly gba_unknown2 = (OPCODES[0xf936] = new Opcode({
        code: 0xf936,
        mnemonic: "gba_unknown2",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly gba_unknown3 = (OPCODES[0xf937] = new Opcode({
        code: 0xf937,
        mnemonic: "gba_unknown3",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly add_damage_to = (OPCODES[0xf938] = new Opcode({
        code: 0xf938,
        mnemonic: "add_damage_to",
        params: [],
        push_stack: false,
        stack_params: [{ type: TYPE_U32 }, { type: TYPE_U32 }],
    }));
    static readonly item_delete3 = (OPCODES[0xf939] = new Opcode({
        code: 0xf939,
        mnemonic: "item_delete3",
        params: [],
        push_stack: false,
        stack_params: [{ type: TYPE_U32 }],
    }));
    static readonly get_item_info = (OPCODES[0xf93a] = new Opcode({
        code: 0xf93a,
        mnemonic: "get_item_info",
        params: [],
        push_stack: false,
        stack_params: [{ type: TYPE_U32 }, { type: Type.RegRef }],
    }));
    static readonly item_packing1 = (OPCODES[0xf93b] = new Opcode({
        code: 0xf93b,
        mnemonic: "item_packing1",
        params: [],
        push_stack: false,
        stack_params: [{ type: TYPE_U32 }],
    }));
    static readonly item_packing2 = (OPCODES[0xf93c] = new Opcode({
        code: 0xf93c,
        mnemonic: "item_packing2",
        params: [],
        push_stack: false,
        stack_params: [{ type: TYPE_U32 }, { type: TYPE_U32 }],
    }));
    static readonly get_lang_setting = (OPCODES[0xf93d] = new Opcode({
        code: 0xf93d,
        mnemonic: "get_lang_setting",
        params: [],
        push_stack: false,
        stack_params: [{ type: Type.RegRef }],
    }));
    static readonly prepare_statistic = (OPCODES[0xf93e] = new Opcode({
        code: 0xf93e,
        mnemonic: "prepare_statistic",
        params: [],
        push_stack: false,
        stack_params: [{ type: TYPE_U32 }, { type: TYPE_I_LABEL }, { type: TYPE_I_LABEL }],
    }));
    static readonly keyword_detect = (OPCODES[0xf93f] = new Opcode({
        code: 0xf93f,
        mnemonic: "keyword_detect",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly keyword = (OPCODES[0xf940] = new Opcode({
        code: 0xf940,
        mnemonic: "keyword",
        params: [],
        push_stack: false,
        stack_params: [{ type: Type.RegRef }, { type: TYPE_U32 }, { type: TYPE_STRING }],
    }));
    static readonly get_guildcard_num = (OPCODES[0xf941] = new Opcode({
        code: 0xf941,
        mnemonic: "get_guildcard_num",
        params: [],
        push_stack: false,
        stack_params: [{ type: TYPE_U32 }, { type: Type.RegRef }],
    }));
    static readonly unknown_f942 = (OPCODES[0xf942] = new Opcode({
        code: 0xf942,
        mnemonic: "unknown_f942",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f943 = (OPCODES[0xf943] = new Opcode({
        code: 0xf943,
        mnemonic: "unknown_f943",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly get_wrap_status = (OPCODES[0xf944] = new Opcode({
        code: 0xf944,
        mnemonic: "get_wrap_status",
        params: [],
        push_stack: false,
        stack_params: [{ type: TYPE_U32 }, { type: Type.RegRef }],
    }));
    static readonly initial_floor = (OPCODES[0xf945] = new Opcode({
        code: 0xf945,
        mnemonic: "initial_floor",
        params: [],
        push_stack: false,
        stack_params: [{ type: TYPE_U32 }],
    }));
    static readonly sin = (OPCODES[0xf946] = new Opcode({
        code: 0xf946,
        mnemonic: "sin",
        params: [],
        push_stack: false,
        stack_params: [{ type: Type.RegRef }, { type: TYPE_U32 }],
    }));
    static readonly cos = (OPCODES[0xf947] = new Opcode({
        code: 0xf947,
        mnemonic: "cos",
        params: [],
        push_stack: false,
        stack_params: [{ type: Type.RegRef }, { type: TYPE_U32 }],
    }));
    static readonly unknown_f948 = (OPCODES[0xf948] = new Opcode({
        code: 0xf948,
        mnemonic: "unknown_f948",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f949 = (OPCODES[0xf949] = new Opcode({
        code: 0xf949,
        mnemonic: "unknown_f949",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly boss_is_dead2 = (OPCODES[0xf94a] = new Opcode({
        code: 0xf94a,
        mnemonic: "boss_is_dead2",
        params: [{ type: Type.RegRef }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f94b = (OPCODES[0xf94b] = new Opcode({
        code: 0xf94b,
        mnemonic: "unknown_f94b",
        params: [{ type: Type.RegRef }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f94c = (OPCODES[0xf94c] = new Opcode({
        code: 0xf94c,
        mnemonic: "unknown_f94c",
        params: [{ type: Type.RegRef }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly is_there_cardbattle = (OPCODES[0xf94d] = new Opcode({
        code: 0xf94d,
        mnemonic: "is_there_cardbattle",
        params: [{ type: Type.RegRef }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f94e = (OPCODES[0xf94e] = new Opcode({
        code: 0xf94e,
        mnemonic: "unknown_f94e",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f94f = (OPCODES[0xf94f] = new Opcode({
        code: 0xf94f,
        mnemonic: "unknown_f94f",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly bb_p2_menu = (OPCODES[0xf950] = new Opcode({
        code: 0xf950,
        mnemonic: "bb_p2_menu",
        params: [],
        push_stack: false,
        stack_params: [{ type: TYPE_U32 }],
    }));
    static readonly bb_map_designate = (OPCODES[0xf951] = new Opcode({
        code: 0xf951,
        mnemonic: "bb_map_designate",
        params: [{ type: TYPE_U8 }, { type: TYPE_U16 }, { type: TYPE_U8 }, { type: TYPE_U8 }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly bb_get_number_in_pack = (OPCODES[0xf952] = new Opcode({
        code: 0xf952,
        mnemonic: "bb_get_number_in_pack",
        params: [{ type: Type.RegRef }],
        push_stack: false,
        stack_params: [],
    }));
    static readonly bb_swap_item = (OPCODES[0xf953] = new Opcode({
        code: 0xf953,
        mnemonic: "bb_swap_item",
        params: [],
        push_stack: false,
        stack_params: [
            { type: TYPE_U32 },
            { type: TYPE_U32 },
            { type: TYPE_U32 },
            { type: TYPE_U32 },
            { type: TYPE_U32 },
            { type: TYPE_U32 },
            { type: TYPE_I_LABEL },
            { type: TYPE_I_LABEL },
        ],
    }));
    static readonly bb_check_wrap = (OPCODES[0xf954] = new Opcode({
        code: 0xf954,
        mnemonic: "bb_check_wrap",
        params: [],
        push_stack: false,
        stack_params: [{ type: Type.RegRef }, { type: Type.RegRef }],
    }));
    static readonly bb_exchange_pd_item = (OPCODES[0xf955] = new Opcode({
        code: 0xf955,
        mnemonic: "bb_exchange_pd_item",
        params: [],
        push_stack: false,
        stack_params: [
            { type: Type.RegRef },
            { type: Type.RegRef },
            { type: Type.RegRef },
            { type: TYPE_I_LABEL },
            { type: TYPE_I_LABEL },
        ],
    }));
    static readonly bb_exchange_pd_srank = (OPCODES[0xf956] = new Opcode({
        code: 0xf956,
        mnemonic: "bb_exchange_pd_srank",
        params: [],
        push_stack: false,
        stack_params: [
            { type: Type.RegRef },
            { type: Type.RegRef },
            { type: Type.RegRef },
            { type: Type.RegRef },
            { type: Type.RegRef },
            { type: TYPE_I_LABEL },
            { type: TYPE_I_LABEL },
        ],
    }));
    static readonly bb_exchange_pd_special = (OPCODES[0xf957] = new Opcode({
        code: 0xf957,
        mnemonic: "bb_exchange_pd_special",
        params: [],
        push_stack: false,
        stack_params: [
            { type: Type.RegRef },
            { type: Type.RegRef },
            { type: Type.RegRef },
            { type: Type.RegRef },
            { type: Type.RegRef },
            { type: TYPE_U32 },
            { type: TYPE_I_LABEL },
            { type: TYPE_I_LABEL },
        ],
    }));
    static readonly bb_exchange_pd_percent = (OPCODES[0xf958] = new Opcode({
        code: 0xf958,
        mnemonic: "bb_exchange_pd_percent",
        params: [],
        push_stack: false,
        stack_params: [
            { type: Type.RegRef },
            { type: Type.RegRef },
            { type: Type.RegRef },
            { type: Type.RegRef },
            { type: Type.RegRef },
            { type: TYPE_U32 },
            { type: TYPE_I_LABEL },
            { type: TYPE_I_LABEL },
        ],
    }));
    static readonly unknown_f959 = (OPCODES[0xf959] = new Opcode({
        code: 0xf959,
        mnemonic: "unknown_f959",
        params: [],
        push_stack: false,
        stack_params: [{ type: TYPE_U32 }],
    }));
    static readonly unknown_f95a = (OPCODES[0xf95a] = new Opcode({
        code: 0xf95a,
        mnemonic: "unknown_f95a",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f95b = (OPCODES[0xf95b] = new Opcode({
        code: 0xf95b,
        mnemonic: "unknown_f95b",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly bb_exchange_slt = (OPCODES[0xf95c] = new Opcode({
        code: 0xf95c,
        mnemonic: "bb_exchange_slt",
        params: [],
        push_stack: false,
        stack_params: [
            { type: TYPE_U32 },
            { type: Type.RegRef },
            { type: TYPE_I_LABEL },
            { type: TYPE_I_LABEL },
        ],
    }));
    static readonly bb_exchange_pc = (OPCODES[0xf95d] = new Opcode({
        code: 0xf95d,
        mnemonic: "bb_exchange_pc",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly bb_box_create_bp = (OPCODES[0xf95e] = new Opcode({
        code: 0xf95e,
        mnemonic: "bb_box_create_bp",
        params: [],
        push_stack: false,
        stack_params: [{ type: TYPE_U32 }, { type: TYPE_F32 }, { type: TYPE_F32 }],
    }));
    static readonly bb_exchange_pt = (OPCODES[0xf95f] = new Opcode({
        code: 0xf95f,
        mnemonic: "bb_exchange_pt",
        params: [],
        push_stack: false,
        stack_params: [
            { type: Type.RegRef },
            { type: Type.RegRef },
            { type: TYPE_U32 },
            { type: TYPE_I_LABEL },
            { type: TYPE_I_LABEL },
        ],
    }));
    static readonly unknown_f960 = (OPCODES[0xf960] = new Opcode({
        code: 0xf960,
        mnemonic: "unknown_f960",
        params: [],
        push_stack: false,
        stack_params: [{ type: TYPE_U32 }],
    }));
    static readonly unknown_f961 = (OPCODES[0xf961] = new Opcode({
        code: 0xf961,
        mnemonic: "unknown_f961",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f962 = (OPCODES[0xf962] = new Opcode({
        code: 0xf962,
        mnemonic: "unknown_f962",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f963 = (OPCODES[0xf963] = new Opcode({
        code: 0xf963,
        mnemonic: "unknown_f963",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f964 = (OPCODES[0xf964] = new Opcode({
        code: 0xf964,
        mnemonic: "unknown_f964",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f965 = (OPCODES[0xf965] = new Opcode({
        code: 0xf965,
        mnemonic: "unknown_f965",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f966 = (OPCODES[0xf966] = new Opcode({
        code: 0xf966,
        mnemonic: "unknown_f966",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f967 = (OPCODES[0xf967] = new Opcode({
        code: 0xf967,
        mnemonic: "unknown_f967",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f968 = (OPCODES[0xf968] = new Opcode({
        code: 0xf968,
        mnemonic: "unknown_f968",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f969 = (OPCODES[0xf969] = new Opcode({
        code: 0xf969,
        mnemonic: "unknown_f969",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f96a = (OPCODES[0xf96a] = new Opcode({
        code: 0xf96a,
        mnemonic: "unknown_f96a",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f96b = (OPCODES[0xf96b] = new Opcode({
        code: 0xf96b,
        mnemonic: "unknown_f96b",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f96c = (OPCODES[0xf96c] = new Opcode({
        code: 0xf96c,
        mnemonic: "unknown_f96c",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f96d = (OPCODES[0xf96d] = new Opcode({
        code: 0xf96d,
        mnemonic: "unknown_f96d",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f96e = (OPCODES[0xf96e] = new Opcode({
        code: 0xf96e,
        mnemonic: "unknown_f96e",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f96f = (OPCODES[0xf96f] = new Opcode({
        code: 0xf96f,
        mnemonic: "unknown_f96f",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f970 = (OPCODES[0xf970] = new Opcode({
        code: 0xf970,
        mnemonic: "unknown_f970",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f971 = (OPCODES[0xf971] = new Opcode({
        code: 0xf971,
        mnemonic: "unknown_f971",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f972 = (OPCODES[0xf972] = new Opcode({
        code: 0xf972,
        mnemonic: "unknown_f972",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f973 = (OPCODES[0xf973] = new Opcode({
        code: 0xf973,
        mnemonic: "unknown_f973",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f974 = (OPCODES[0xf974] = new Opcode({
        code: 0xf974,
        mnemonic: "unknown_f974",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f975 = (OPCODES[0xf975] = new Opcode({
        code: 0xf975,
        mnemonic: "unknown_f975",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f976 = (OPCODES[0xf976] = new Opcode({
        code: 0xf976,
        mnemonic: "unknown_f976",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f977 = (OPCODES[0xf977] = new Opcode({
        code: 0xf977,
        mnemonic: "unknown_f977",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f978 = (OPCODES[0xf978] = new Opcode({
        code: 0xf978,
        mnemonic: "unknown_f978",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f979 = (OPCODES[0xf979] = new Opcode({
        code: 0xf979,
        mnemonic: "unknown_f979",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f97a = (OPCODES[0xf97a] = new Opcode({
        code: 0xf97a,
        mnemonic: "unknown_f97a",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f97b = (OPCODES[0xf97b] = new Opcode({
        code: 0xf97b,
        mnemonic: "unknown_f97b",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f97c = (OPCODES[0xf97c] = new Opcode({
        code: 0xf97c,
        mnemonic: "unknown_f97c",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f97d = (OPCODES[0xf97d] = new Opcode({
        code: 0xf97d,
        mnemonic: "unknown_f97d",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f97e = (OPCODES[0xf97e] = new Opcode({
        code: 0xf97e,
        mnemonic: "unknown_f97e",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f97f = (OPCODES[0xf97f] = new Opcode({
        code: 0xf97f,
        mnemonic: "unknown_f97f",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f980 = (OPCODES[0xf980] = new Opcode({
        code: 0xf980,
        mnemonic: "unknown_f980",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f981 = (OPCODES[0xf981] = new Opcode({
        code: 0xf981,
        mnemonic: "unknown_f981",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f982 = (OPCODES[0xf982] = new Opcode({
        code: 0xf982,
        mnemonic: "unknown_f982",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f983 = (OPCODES[0xf983] = new Opcode({
        code: 0xf983,
        mnemonic: "unknown_f983",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f984 = (OPCODES[0xf984] = new Opcode({
        code: 0xf984,
        mnemonic: "unknown_f984",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f985 = (OPCODES[0xf985] = new Opcode({
        code: 0xf985,
        mnemonic: "unknown_f985",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f986 = (OPCODES[0xf986] = new Opcode({
        code: 0xf986,
        mnemonic: "unknown_f986",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f987 = (OPCODES[0xf987] = new Opcode({
        code: 0xf987,
        mnemonic: "unknown_f987",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f988 = (OPCODES[0xf988] = new Opcode({
        code: 0xf988,
        mnemonic: "unknown_f988",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f989 = (OPCODES[0xf989] = new Opcode({
        code: 0xf989,
        mnemonic: "unknown_f989",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f98a = (OPCODES[0xf98a] = new Opcode({
        code: 0xf98a,
        mnemonic: "unknown_f98a",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f98b = (OPCODES[0xf98b] = new Opcode({
        code: 0xf98b,
        mnemonic: "unknown_f98b",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f98c = (OPCODES[0xf98c] = new Opcode({
        code: 0xf98c,
        mnemonic: "unknown_f98c",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f98d = (OPCODES[0xf98d] = new Opcode({
        code: 0xf98d,
        mnemonic: "unknown_f98d",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f98e = (OPCODES[0xf98e] = new Opcode({
        code: 0xf98e,
        mnemonic: "unknown_f98e",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f98f = (OPCODES[0xf98f] = new Opcode({
        code: 0xf98f,
        mnemonic: "unknown_f98f",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f990 = (OPCODES[0xf990] = new Opcode({
        code: 0xf990,
        mnemonic: "unknown_f990",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f991 = (OPCODES[0xf991] = new Opcode({
        code: 0xf991,
        mnemonic: "unknown_f991",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f992 = (OPCODES[0xf992] = new Opcode({
        code: 0xf992,
        mnemonic: "unknown_f992",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f993 = (OPCODES[0xf993] = new Opcode({
        code: 0xf993,
        mnemonic: "unknown_f993",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f994 = (OPCODES[0xf994] = new Opcode({
        code: 0xf994,
        mnemonic: "unknown_f994",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f995 = (OPCODES[0xf995] = new Opcode({
        code: 0xf995,
        mnemonic: "unknown_f995",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f996 = (OPCODES[0xf996] = new Opcode({
        code: 0xf996,
        mnemonic: "unknown_f996",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f997 = (OPCODES[0xf997] = new Opcode({
        code: 0xf997,
        mnemonic: "unknown_f997",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f998 = (OPCODES[0xf998] = new Opcode({
        code: 0xf998,
        mnemonic: "unknown_f998",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f999 = (OPCODES[0xf999] = new Opcode({
        code: 0xf999,
        mnemonic: "unknown_f999",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f99a = (OPCODES[0xf99a] = new Opcode({
        code: 0xf99a,
        mnemonic: "unknown_f99a",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f99b = (OPCODES[0xf99b] = new Opcode({
        code: 0xf99b,
        mnemonic: "unknown_f99b",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f99c = (OPCODES[0xf99c] = new Opcode({
        code: 0xf99c,
        mnemonic: "unknown_f99c",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f99d = (OPCODES[0xf99d] = new Opcode({
        code: 0xf99d,
        mnemonic: "unknown_f99d",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f99e = (OPCODES[0xf99e] = new Opcode({
        code: 0xf99e,
        mnemonic: "unknown_f99e",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f99f = (OPCODES[0xf99f] = new Opcode({
        code: 0xf99f,
        mnemonic: "unknown_f99f",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f9a0 = (OPCODES[0xf9a0] = new Opcode({
        code: 0xf9a0,
        mnemonic: "unknown_f9a0",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f9a1 = (OPCODES[0xf9a1] = new Opcode({
        code: 0xf9a1,
        mnemonic: "unknown_f9a1",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f9a2 = (OPCODES[0xf9a2] = new Opcode({
        code: 0xf9a2,
        mnemonic: "unknown_f9a2",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f9a3 = (OPCODES[0xf9a3] = new Opcode({
        code: 0xf9a3,
        mnemonic: "unknown_f9a3",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f9a4 = (OPCODES[0xf9a4] = new Opcode({
        code: 0xf9a4,
        mnemonic: "unknown_f9a4",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f9a5 = (OPCODES[0xf9a5] = new Opcode({
        code: 0xf9a5,
        mnemonic: "unknown_f9a5",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f9a6 = (OPCODES[0xf9a6] = new Opcode({
        code: 0xf9a6,
        mnemonic: "unknown_f9a6",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f9a7 = (OPCODES[0xf9a7] = new Opcode({
        code: 0xf9a7,
        mnemonic: "unknown_f9a7",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f9a8 = (OPCODES[0xf9a8] = new Opcode({
        code: 0xf9a8,
        mnemonic: "unknown_f9a8",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f9a9 = (OPCODES[0xf9a9] = new Opcode({
        code: 0xf9a9,
        mnemonic: "unknown_f9a9",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f9aa = (OPCODES[0xf9aa] = new Opcode({
        code: 0xf9aa,
        mnemonic: "unknown_f9aa",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f9ab = (OPCODES[0xf9ab] = new Opcode({
        code: 0xf9ab,
        mnemonic: "unknown_f9ab",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f9ac = (OPCODES[0xf9ac] = new Opcode({
        code: 0xf9ac,
        mnemonic: "unknown_f9ac",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f9ad = (OPCODES[0xf9ad] = new Opcode({
        code: 0xf9ad,
        mnemonic: "unknown_f9ad",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f9ae = (OPCODES[0xf9ae] = new Opcode({
        code: 0xf9ae,
        mnemonic: "unknown_f9ae",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f9af = (OPCODES[0xf9af] = new Opcode({
        code: 0xf9af,
        mnemonic: "unknown_f9af",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f9b0 = (OPCODES[0xf9b0] = new Opcode({
        code: 0xf9b0,
        mnemonic: "unknown_f9b0",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f9b1 = (OPCODES[0xf9b1] = new Opcode({
        code: 0xf9b1,
        mnemonic: "unknown_f9b1",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f9b2 = (OPCODES[0xf9b2] = new Opcode({
        code: 0xf9b2,
        mnemonic: "unknown_f9b2",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f9b3 = (OPCODES[0xf9b3] = new Opcode({
        code: 0xf9b3,
        mnemonic: "unknown_f9b3",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f9b4 = (OPCODES[0xf9b4] = new Opcode({
        code: 0xf9b4,
        mnemonic: "unknown_f9b4",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f9b5 = (OPCODES[0xf9b5] = new Opcode({
        code: 0xf9b5,
        mnemonic: "unknown_f9b5",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f9b6 = (OPCODES[0xf9b6] = new Opcode({
        code: 0xf9b6,
        mnemonic: "unknown_f9b6",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f9b7 = (OPCODES[0xf9b7] = new Opcode({
        code: 0xf9b7,
        mnemonic: "unknown_f9b7",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f9b8 = (OPCODES[0xf9b8] = new Opcode({
        code: 0xf9b8,
        mnemonic: "unknown_f9b8",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f9b9 = (OPCODES[0xf9b9] = new Opcode({
        code: 0xf9b9,
        mnemonic: "unknown_f9b9",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f9ba = (OPCODES[0xf9ba] = new Opcode({
        code: 0xf9ba,
        mnemonic: "unknown_f9ba",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f9bb = (OPCODES[0xf9bb] = new Opcode({
        code: 0xf9bb,
        mnemonic: "unknown_f9bb",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f9bc = (OPCODES[0xf9bc] = new Opcode({
        code: 0xf9bc,
        mnemonic: "unknown_f9bc",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f9bd = (OPCODES[0xf9bd] = new Opcode({
        code: 0xf9bd,
        mnemonic: "unknown_f9bd",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f9be = (OPCODES[0xf9be] = new Opcode({
        code: 0xf9be,
        mnemonic: "unknown_f9be",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f9bf = (OPCODES[0xf9bf] = new Opcode({
        code: 0xf9bf,
        mnemonic: "unknown_f9bf",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f9c0 = (OPCODES[0xf9c0] = new Opcode({
        code: 0xf9c0,
        mnemonic: "unknown_f9c0",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f9c1 = (OPCODES[0xf9c1] = new Opcode({
        code: 0xf9c1,
        mnemonic: "unknown_f9c1",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f9c2 = (OPCODES[0xf9c2] = new Opcode({
        code: 0xf9c2,
        mnemonic: "unknown_f9c2",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f9c3 = (OPCODES[0xf9c3] = new Opcode({
        code: 0xf9c3,
        mnemonic: "unknown_f9c3",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f9c4 = (OPCODES[0xf9c4] = new Opcode({
        code: 0xf9c4,
        mnemonic: "unknown_f9c4",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f9c5 = (OPCODES[0xf9c5] = new Opcode({
        code: 0xf9c5,
        mnemonic: "unknown_f9c5",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f9c6 = (OPCODES[0xf9c6] = new Opcode({
        code: 0xf9c6,
        mnemonic: "unknown_f9c6",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f9c7 = (OPCODES[0xf9c7] = new Opcode({
        code: 0xf9c7,
        mnemonic: "unknown_f9c7",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f9c8 = (OPCODES[0xf9c8] = new Opcode({
        code: 0xf9c8,
        mnemonic: "unknown_f9c8",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f9c9 = (OPCODES[0xf9c9] = new Opcode({
        code: 0xf9c9,
        mnemonic: "unknown_f9c9",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f9ca = (OPCODES[0xf9ca] = new Opcode({
        code: 0xf9ca,
        mnemonic: "unknown_f9ca",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f9cb = (OPCODES[0xf9cb] = new Opcode({
        code: 0xf9cb,
        mnemonic: "unknown_f9cb",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f9cc = (OPCODES[0xf9cc] = new Opcode({
        code: 0xf9cc,
        mnemonic: "unknown_f9cc",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f9cd = (OPCODES[0xf9cd] = new Opcode({
        code: 0xf9cd,
        mnemonic: "unknown_f9cd",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f9ce = (OPCODES[0xf9ce] = new Opcode({
        code: 0xf9ce,
        mnemonic: "unknown_f9ce",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f9cf = (OPCODES[0xf9cf] = new Opcode({
        code: 0xf9cf,
        mnemonic: "unknown_f9cf",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f9d0 = (OPCODES[0xf9d0] = new Opcode({
        code: 0xf9d0,
        mnemonic: "unknown_f9d0",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f9d1 = (OPCODES[0xf9d1] = new Opcode({
        code: 0xf9d1,
        mnemonic: "unknown_f9d1",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f9d2 = (OPCODES[0xf9d2] = new Opcode({
        code: 0xf9d2,
        mnemonic: "unknown_f9d2",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f9d3 = (OPCODES[0xf9d3] = new Opcode({
        code: 0xf9d3,
        mnemonic: "unknown_f9d3",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f9d4 = (OPCODES[0xf9d4] = new Opcode({
        code: 0xf9d4,
        mnemonic: "unknown_f9d4",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f9d5 = (OPCODES[0xf9d5] = new Opcode({
        code: 0xf9d5,
        mnemonic: "unknown_f9d5",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f9d6 = (OPCODES[0xf9d6] = new Opcode({
        code: 0xf9d6,
        mnemonic: "unknown_f9d6",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f9d7 = (OPCODES[0xf9d7] = new Opcode({
        code: 0xf9d7,
        mnemonic: "unknown_f9d7",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f9d8 = (OPCODES[0xf9d8] = new Opcode({
        code: 0xf9d8,
        mnemonic: "unknown_f9d8",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f9d9 = (OPCODES[0xf9d9] = new Opcode({
        code: 0xf9d9,
        mnemonic: "unknown_f9d9",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f9da = (OPCODES[0xf9da] = new Opcode({
        code: 0xf9da,
        mnemonic: "unknown_f9da",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f9db = (OPCODES[0xf9db] = new Opcode({
        code: 0xf9db,
        mnemonic: "unknown_f9db",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f9dc = (OPCODES[0xf9dc] = new Opcode({
        code: 0xf9dc,
        mnemonic: "unknown_f9dc",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f9dd = (OPCODES[0xf9dd] = new Opcode({
        code: 0xf9dd,
        mnemonic: "unknown_f9dd",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f9de = (OPCODES[0xf9de] = new Opcode({
        code: 0xf9de,
        mnemonic: "unknown_f9de",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f9df = (OPCODES[0xf9df] = new Opcode({
        code: 0xf9df,
        mnemonic: "unknown_f9df",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f9e0 = (OPCODES[0xf9e0] = new Opcode({
        code: 0xf9e0,
        mnemonic: "unknown_f9e0",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f9e1 = (OPCODES[0xf9e1] = new Opcode({
        code: 0xf9e1,
        mnemonic: "unknown_f9e1",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f9e2 = (OPCODES[0xf9e2] = new Opcode({
        code: 0xf9e2,
        mnemonic: "unknown_f9e2",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f9e3 = (OPCODES[0xf9e3] = new Opcode({
        code: 0xf9e3,
        mnemonic: "unknown_f9e3",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f9e4 = (OPCODES[0xf9e4] = new Opcode({
        code: 0xf9e4,
        mnemonic: "unknown_f9e4",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f9e5 = (OPCODES[0xf9e5] = new Opcode({
        code: 0xf9e5,
        mnemonic: "unknown_f9e5",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f9e6 = (OPCODES[0xf9e6] = new Opcode({
        code: 0xf9e6,
        mnemonic: "unknown_f9e6",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f9e7 = (OPCODES[0xf9e7] = new Opcode({
        code: 0xf9e7,
        mnemonic: "unknown_f9e7",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f9e8 = (OPCODES[0xf9e8] = new Opcode({
        code: 0xf9e8,
        mnemonic: "unknown_f9e8",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f9e9 = (OPCODES[0xf9e9] = new Opcode({
        code: 0xf9e9,
        mnemonic: "unknown_f9e9",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f9ea = (OPCODES[0xf9ea] = new Opcode({
        code: 0xf9ea,
        mnemonic: "unknown_f9ea",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f9eb = (OPCODES[0xf9eb] = new Opcode({
        code: 0xf9eb,
        mnemonic: "unknown_f9eb",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f9ec = (OPCODES[0xf9ec] = new Opcode({
        code: 0xf9ec,
        mnemonic: "unknown_f9ec",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f9ed = (OPCODES[0xf9ed] = new Opcode({
        code: 0xf9ed,
        mnemonic: "unknown_f9ed",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f9ee = (OPCODES[0xf9ee] = new Opcode({
        code: 0xf9ee,
        mnemonic: "unknown_f9ee",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f9ef = (OPCODES[0xf9ef] = new Opcode({
        code: 0xf9ef,
        mnemonic: "unknown_f9ef",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f9f0 = (OPCODES[0xf9f0] = new Opcode({
        code: 0xf9f0,
        mnemonic: "unknown_f9f0",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f9f1 = (OPCODES[0xf9f1] = new Opcode({
        code: 0xf9f1,
        mnemonic: "unknown_f9f1",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f9f2 = (OPCODES[0xf9f2] = new Opcode({
        code: 0xf9f2,
        mnemonic: "unknown_f9f2",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f9f3 = (OPCODES[0xf9f3] = new Opcode({
        code: 0xf9f3,
        mnemonic: "unknown_f9f3",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f9f4 = (OPCODES[0xf9f4] = new Opcode({
        code: 0xf9f4,
        mnemonic: "unknown_f9f4",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f9f5 = (OPCODES[0xf9f5] = new Opcode({
        code: 0xf9f5,
        mnemonic: "unknown_f9f5",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f9f6 = (OPCODES[0xf9f6] = new Opcode({
        code: 0xf9f6,
        mnemonic: "unknown_f9f6",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f9f7 = (OPCODES[0xf9f7] = new Opcode({
        code: 0xf9f7,
        mnemonic: "unknown_f9f7",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f9f8 = (OPCODES[0xf9f8] = new Opcode({
        code: 0xf9f8,
        mnemonic: "unknown_f9f8",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f9f9 = (OPCODES[0xf9f9] = new Opcode({
        code: 0xf9f9,
        mnemonic: "unknown_f9f9",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f9fa = (OPCODES[0xf9fa] = new Opcode({
        code: 0xf9fa,
        mnemonic: "unknown_f9fa",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f9fb = (OPCODES[0xf9fb] = new Opcode({
        code: 0xf9fb,
        mnemonic: "unknown_f9fb",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f9fc = (OPCODES[0xf9fc] = new Opcode({
        code: 0xf9fc,
        mnemonic: "unknown_f9fc",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f9fd = (OPCODES[0xf9fd] = new Opcode({
        code: 0xf9fd,
        mnemonic: "unknown_f9fd",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f9fe = (OPCODES[0xf9fe] = new Opcode({
        code: 0xf9fe,
        mnemonic: "unknown_f9fe",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
    static readonly unknown_f9ff = (OPCODES[0xf9ff] = new Opcode({
        code: 0xf9ff,
        mnemonic: "unknown_f9ff",
        params: [],
        push_stack: false,
        stack_params: [],
    }));
}

export const OPCODES_BY_MNEMONIC = new Map<string, Opcode>();

OPCODES.forEach(opcode => {
    OPCODES_BY_MNEMONIC.set(opcode.mnemonic, opcode);
});

/**
 * Enumerates all the types that are not purely abstract.
 */
export enum Kind {
    Any,
    Byte,
    Word,
    DWord,
    Float,
    Label,
    ILabel,
    DLabel,
    SLabel,
    String,
    ILabelVar,
    RegRef,
    RegTupRef,
    RegRefVar,
    Pointer,
}

/**
 * Abstract super type of all types.
 */
export type AnyType =
    | ValueType
    | RefType
    | PointerType
    | {
          readonly kind: Kind.Any;
      };

/**
 * Purely abstract super type of all value types.
 */
export type ValueType = ByteType | WordType | DWordType | FloatType | LabelType;

/**
 * 8-Bit integer.
 */
export type ByteType = {
    readonly kind: Kind.Byte;
};

/**
 * 16-Bit integer.
 */
export type WordType = {
    readonly kind: Kind.Word;
};

/**
 * 32-Bit integer.
 */
export type DWordType = {
    readonly kind: Kind.DWord;
};

/**
 * 32-Bit floating point number.
 */
export type FloatType = {
    readonly kind: Kind.Float;
};

/**
 * Abstract super type of all label types.
 */
export type LabelType =
    | ILabelType
    | DLabelType
    | SLabelType
    | StringType
    | ILabelVarType
    | {
          readonly kind: Kind.Label;
      };

/**
 * Named reference to an instruction.
 */
export type ILabelType = {
    readonly kind: Kind.ILabel;
};

/**
 * Named reference to a data segment.
 */
export type DLabelType = {
    readonly kind: Kind.DLabel;
};

/**
 * Named reference to a string segment.
 */
export type SLabelType = {
    readonly kind: Kind.SLabel;
};

/**
 * String of arbitrary size.
 */
export type StringType = {
    readonly kind: Kind.String;
};

/**
 * Arbitrary amount of instruction labels.
 */
export type ILabelVarType = {
    readonly kind: Kind.ILabelVar;
};

/**
 * Purely abstract super type of all reference types.
 */
export type RefType = RegRefType | RegTupRefType | RegRefVarType;

/**
 * Reference to one or more registers.
 */
export type RegRefType = {
    readonly kind: Kind.RegRef;
};

/**
 * Reference to a fixed amount of consecutive registers of specific types.
 * The only parameterized type.
 */
export type RegTupRefType = {
    readonly kind: Kind.RegTupRef;
    readonly register_tuples: Param[];
};

/**
 * Arbitrary amount of register references.
 */
export type RegRefVarType = {
    readonly kind: Kind.RegRefVar;
};

/**
 * Raw memory pointer.
 */
export type PointerType = {
    readonly kind: Kind.Pointer;
};

// Singleton type constants.
// No singleton constant for `RegTupRef` because it is parameterized.
export const TYPE_ANY: AnyType = { kind: Kind.Any };
export const TYPE_BYTE: ByteType = { kind: Kind.Byte };
export const TYPE_WORD: WordType = { kind: Kind.Word };
export const TYPE_DWORD: DWordType = { kind: Kind.DWord };
export const TYPE_FLOAT: FloatType = { kind: Kind.Float };
export const TYPE_LABEL: LabelType = { kind: Kind.Label };
export const TYPE_I_LABEL: ILabelType = { kind: Kind.ILabel };
export const TYPE_D_LABEL: DLabelType = { kind: Kind.DLabel };
export const TYPE_S_LABEL: SLabelType = { kind: Kind.SLabel };
export const TYPE_STRING: StringType = { kind: Kind.String };
export const TYPE_I_LABEL_VAR: ILabelVarType = { kind: Kind.ILabelVar };
export const TYPE_REG_REF: RegRefType = { kind: Kind.RegRef };
export const TYPE_REG_REF_VAR: RegRefVarType = { kind: Kind.RegRefVar };
export const TYPE_POINTER: PointerType = { kind: Kind.Pointer };

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

export type Param = {
    readonly type: AnyType;
    /**
     * Documentation string.
     */
    readonly doc?: string;
    /**
     * The way referenced registers are accessed by the instruction. Only set when type is a register reference.
     */
    readonly access?: ParamAccess;
};

function new_param(type: AnyType, doc?: string, access?: ParamAccess): Param {
    return {
        type,
        doc,
        access,
    };
}

export enum StackInteraction {
    Push,
    Pop,
}

export const OPCODES: Opcode[] = [];
export const OPCODES_BY_MNEMONIC = new Map<string, Opcode>();

/**
 * Opcode for script object code. Invoked by {@link Instruction}s.
 */
export type Opcode = {
    /**
     * 1- Or 2-byte big-endian representation of this opcode as used in object code.
     */
    readonly code: number;
    /**
     * String representation of this opcode as used in assembly.
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
    readonly params: readonly Param[];
    /**
     * Stack interaction.
     */
    readonly stack?: StackInteraction;
};

function new_opcode(
    code: number,
    mnemonic: string,
    doc: string | undefined,
    params: readonly Param[],
    stack: StackInteraction | undefined,
): Opcode {
    return {
        code,
        mnemonic,
        doc,
        size: code < 256 ? 1 : 2,
        params,
        stack,
    };
}

// !!! GENERATED_CODE_START !!!
export const OP_NOP = (OPCODES[0x00] = new_opcode(
    0x00,
    "nop",
    "No operation, does nothing.",
    [],
    undefined,
));
export const OP_RET = (OPCODES[0x01] = new_opcode(
    0x01,
    "ret",
    "Returns control to caller.",
    [],
    undefined,
));
export const OP_SYNC = (OPCODES[0x02] = new_opcode(
    0x02,
    "sync",
    "Yields control for the rest of the current frame. Execution will continue the following frame.",
    [],
    undefined,
));
export const OP_EXIT = (OPCODES[0x03] = new_opcode(
    0x03,
    "exit",
    undefined,
    [new_param(TYPE_DWORD, undefined, undefined)],
    StackInteraction.Pop,
));
export const OP_THREAD = (OPCODES[0x04] = new_opcode(
    0x04,
    "thread",
    "Starts a new thread. Thread execution will start at the given label.\nOften used to check a register every frame. Make sure to yield control with sync when looping.",
    [new_param(TYPE_I_LABEL, undefined, undefined)],
    undefined,
));
export const OP_VA_START = (OPCODES[0x05] = new_opcode(
    0x05,
    "va_start",
    "Initializes a variable argument list.\nMake sure to call va_end after va_start and va_call.",
    [],
    undefined,
));
export const OP_VA_END = (OPCODES[0x06] = new_opcode(
    0x06,
    "va_end",
    "Restores the registers overwritten by arg_push* instructions.\nCalled after va_call.",
    [],
    undefined,
));
export const OP_VA_CALL = (OPCODES[0x07] = new_opcode(
    0x07,
    "va_call",
    "Calls the variable argument function at the given label.\nCalled after initializing the argument list with va_start and pushing arguments onto the stack with arg_push* instructions. Make sure to call va_end afterwards.",
    [new_param(TYPE_I_LABEL, undefined, undefined)],
    undefined,
));
export const OP_LET = (OPCODES[0x08] = new_opcode(
    0x08,
    "let",
    "Sets the first register's value to second one's value.",
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_DWORD, undefined, ParamAccess.Write)],
            },
            undefined,
            undefined,
        ),
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_DWORD, undefined, ParamAccess.Read)],
            },
            undefined,
            undefined,
        ),
    ],
    undefined,
));
export const OP_LETI = (OPCODES[0x09] = new_opcode(
    0x09,
    "leti",
    "Sets a register to the given value.",
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_DWORD, undefined, ParamAccess.Write)],
            },
            undefined,
            undefined,
        ),
        new_param(TYPE_DWORD, undefined, undefined),
    ],
    undefined,
));
export const OP_LETB = (OPCODES[0x0a] = new_opcode(
    0x0a,
    "letb",
    "Sets a register to the given value.",
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_BYTE, undefined, ParamAccess.Write)],
            },
            undefined,
            undefined,
        ),
        new_param(TYPE_BYTE, undefined, undefined),
    ],
    undefined,
));
export const OP_LETW = (OPCODES[0x0b] = new_opcode(
    0x0b,
    "letw",
    "Sets a register to the given value.",
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_WORD, undefined, ParamAccess.Write)],
            },
            undefined,
            undefined,
        ),
        new_param(TYPE_WORD, undefined, undefined),
    ],
    undefined,
));
export const OP_LETA = (OPCODES[0x0c] = new_opcode(
    0x0c,
    "leta",
    "Sets the first register to the memory address of the second register. Not used by Sega.",
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_POINTER, undefined, ParamAccess.Write)],
            },
            undefined,
            undefined,
        ),
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_ANY, undefined, ParamAccess.Read)],
            },
            undefined,
            undefined,
        ),
    ],
    undefined,
));
export const OP_LETO = (OPCODES[0x0d] = new_opcode(
    0x0d,
    "leto",
    "Sets a register to the memory address of the given label. Not used by Sega.",
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_POINTER, undefined, ParamAccess.Write)],
            },
            undefined,
            undefined,
        ),
        new_param(TYPE_LABEL, undefined, undefined),
    ],
    undefined,
));
export const OP_UNKNOWN_0E = (OPCODES[0x0e] = new_opcode(
    0x0e,
    "unknown_0e",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_0F = (OPCODES[0x0f] = new_opcode(
    0x0f,
    "unknown_0f",
    undefined,
    [],
    undefined,
));
export const OP_SET = (OPCODES[0x10] = new_opcode(
    0x10,
    "set",
    "Sets a register to 1.",
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_DWORD, undefined, ParamAccess.Write)],
            },
            undefined,
            undefined,
        ),
    ],
    undefined,
));
export const OP_CLEAR = (OPCODES[0x11] = new_opcode(
    0x11,
    "clear",
    "Sets a register to 0.",
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_DWORD, undefined, ParamAccess.Write)],
            },
            undefined,
            undefined,
        ),
    ],
    undefined,
));
export const OP_REV = (OPCODES[0x12] = new_opcode(
    0x12,
    "rev",
    "Sets a register to 1 if its current value is 0, otherwise sets it to 0.",
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_DWORD, undefined, ParamAccess.ReadWrite)],
            },
            undefined,
            undefined,
        ),
    ],
    undefined,
));
export const OP_GSET = (OPCODES[0x13] = new_opcode(
    0x13,
    "gset",
    undefined,
    [new_param(TYPE_WORD, undefined, undefined)],
    undefined,
));
export const OP_GCLEAR = (OPCODES[0x14] = new_opcode(
    0x14,
    "gclear",
    undefined,
    [new_param(TYPE_WORD, undefined, undefined)],
    undefined,
));
export const OP_GREV = (OPCODES[0x15] = new_opcode(
    0x15,
    "grev",
    undefined,
    [new_param(TYPE_WORD, undefined, undefined)],
    undefined,
));
export const OP_GLET = (OPCODES[0x16] = new_opcode(
    0x16,
    "glet",
    undefined,
    [new_param(TYPE_WORD, undefined, undefined)],
    undefined,
));
export const OP_GGET = (OPCODES[0x17] = new_opcode(
    0x17,
    "gget",
    "Sets a register to value of the given flag.",
    [
        new_param(TYPE_WORD, undefined, undefined),
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_WORD, undefined, ParamAccess.Write)],
            },
            undefined,
            undefined,
        ),
    ],
    undefined,
));
export const OP_ADD = (OPCODES[0x18] = new_opcode(
    0x18,
    "add",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_DWORD, undefined, ParamAccess.Write)],
            },
            undefined,
            undefined,
        ),
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_DWORD, undefined, ParamAccess.Read)],
            },
            undefined,
            undefined,
        ),
    ],
    undefined,
));
export const OP_ADDI = (OPCODES[0x19] = new_opcode(
    0x19,
    "addi",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_DWORD, undefined, ParamAccess.Write)],
            },
            undefined,
            undefined,
        ),
        new_param(TYPE_DWORD, undefined, undefined),
    ],
    undefined,
));
export const OP_SUB = (OPCODES[0x1a] = new_opcode(
    0x1a,
    "sub",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_DWORD, undefined, ParamAccess.Write)],
            },
            undefined,
            undefined,
        ),
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_DWORD, undefined, ParamAccess.Read)],
            },
            undefined,
            undefined,
        ),
    ],
    undefined,
));
export const OP_SUBI = (OPCODES[0x1b] = new_opcode(
    0x1b,
    "subi",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_DWORD, undefined, ParamAccess.Write)],
            },
            undefined,
            undefined,
        ),
        new_param(TYPE_DWORD, undefined, undefined),
    ],
    undefined,
));
export const OP_MUL = (OPCODES[0x1c] = new_opcode(
    0x1c,
    "mul",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_DWORD, undefined, ParamAccess.Write)],
            },
            undefined,
            undefined,
        ),
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_DWORD, undefined, ParamAccess.Read)],
            },
            undefined,
            undefined,
        ),
    ],
    undefined,
));
export const OP_MULI = (OPCODES[0x1d] = new_opcode(
    0x1d,
    "muli",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_DWORD, undefined, ParamAccess.Write)],
            },
            undefined,
            undefined,
        ),
        new_param(TYPE_DWORD, undefined, undefined),
    ],
    undefined,
));
export const OP_DIV = (OPCODES[0x1e] = new_opcode(
    0x1e,
    "div",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_DWORD, undefined, ParamAccess.Write)],
            },
            undefined,
            undefined,
        ),
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_DWORD, undefined, ParamAccess.Read)],
            },
            undefined,
            undefined,
        ),
    ],
    undefined,
));
export const OP_DIVI = (OPCODES[0x1f] = new_opcode(
    0x1f,
    "divi",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_DWORD, undefined, ParamAccess.Write)],
            },
            undefined,
            undefined,
        ),
        new_param(TYPE_DWORD, undefined, undefined),
    ],
    undefined,
));
export const OP_AND = (OPCODES[0x20] = new_opcode(
    0x20,
    "and",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_DWORD, undefined, ParamAccess.Write)],
            },
            undefined,
            undefined,
        ),
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_DWORD, undefined, ParamAccess.Read)],
            },
            undefined,
            undefined,
        ),
    ],
    undefined,
));
export const OP_ANDI = (OPCODES[0x21] = new_opcode(
    0x21,
    "andi",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_DWORD, undefined, ParamAccess.Write)],
            },
            undefined,
            undefined,
        ),
        new_param(TYPE_DWORD, undefined, undefined),
    ],
    undefined,
));
export const OP_OR = (OPCODES[0x22] = new_opcode(
    0x22,
    "or",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_DWORD, undefined, ParamAccess.Write)],
            },
            undefined,
            undefined,
        ),
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_DWORD, undefined, ParamAccess.Read)],
            },
            undefined,
            undefined,
        ),
    ],
    undefined,
));
export const OP_ORI = (OPCODES[0x23] = new_opcode(
    0x23,
    "ori",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_DWORD, undefined, ParamAccess.Write)],
            },
            undefined,
            undefined,
        ),
        new_param(TYPE_DWORD, undefined, undefined),
    ],
    undefined,
));
export const OP_XOR = (OPCODES[0x24] = new_opcode(
    0x24,
    "xor",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_DWORD, undefined, ParamAccess.Write)],
            },
            undefined,
            undefined,
        ),
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_DWORD, undefined, ParamAccess.Read)],
            },
            undefined,
            undefined,
        ),
    ],
    undefined,
));
export const OP_XORI = (OPCODES[0x25] = new_opcode(
    0x25,
    "xori",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_DWORD, undefined, ParamAccess.Write)],
            },
            undefined,
            undefined,
        ),
        new_param(TYPE_DWORD, undefined, undefined),
    ],
    undefined,
));
export const OP_MOD = (OPCODES[0x26] = new_opcode(
    0x26,
    "mod",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_DWORD, undefined, ParamAccess.Write)],
            },
            undefined,
            undefined,
        ),
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_DWORD, undefined, ParamAccess.Read)],
            },
            undefined,
            undefined,
        ),
    ],
    undefined,
));
export const OP_MODI = (OPCODES[0x27] = new_opcode(
    0x27,
    "modi",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_DWORD, undefined, ParamAccess.Write)],
            },
            undefined,
            undefined,
        ),
        new_param(TYPE_DWORD, undefined, undefined),
    ],
    undefined,
));
export const OP_JMP = (OPCODES[0x28] = new_opcode(
    0x28,
    "jmp",
    undefined,
    [new_param(TYPE_I_LABEL, undefined, undefined)],
    undefined,
));
export const OP_CALL = (OPCODES[0x29] = new_opcode(
    0x29,
    "call",
    undefined,
    [new_param(TYPE_I_LABEL, undefined, undefined)],
    undefined,
));
export const OP_JMP_ON = (OPCODES[0x2a] = new_opcode(
    0x2a,
    "jmp_on",
    undefined,
    [
        new_param(TYPE_I_LABEL, undefined, undefined),
        new_param(TYPE_REG_REF_VAR, undefined, ParamAccess.Read),
    ],
    undefined,
));
export const OP_JMP_OFF = (OPCODES[0x2b] = new_opcode(
    0x2b,
    "jmp_off",
    undefined,
    [
        new_param(TYPE_I_LABEL, undefined, undefined),
        new_param(TYPE_REG_REF_VAR, undefined, ParamAccess.Read),
    ],
    undefined,
));
export const OP_JMP_E = (OPCODES[0x2c] = new_opcode(
    0x2c,
    "jmp_=",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_ANY, undefined, ParamAccess.Read)],
            },
            undefined,
            undefined,
        ),
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_ANY, undefined, ParamAccess.Read)],
            },
            undefined,
            undefined,
        ),
        new_param(TYPE_I_LABEL, undefined, undefined),
    ],
    undefined,
));
export const OP_JMPI_E = (OPCODES[0x2d] = new_opcode(
    0x2d,
    "jmpi_=",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_DWORD, undefined, ParamAccess.Read)],
            },
            undefined,
            undefined,
        ),
        new_param(TYPE_DWORD, undefined, undefined),
        new_param(TYPE_I_LABEL, undefined, undefined),
    ],
    undefined,
));
export const OP_JMP_NE = (OPCODES[0x2e] = new_opcode(
    0x2e,
    "jmp_!=",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_ANY, undefined, ParamAccess.Read)],
            },
            undefined,
            undefined,
        ),
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_ANY, undefined, ParamAccess.Read)],
            },
            undefined,
            undefined,
        ),
        new_param(TYPE_I_LABEL, undefined, undefined),
    ],
    undefined,
));
export const OP_JMPI_NE = (OPCODES[0x2f] = new_opcode(
    0x2f,
    "jmpi_!=",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_DWORD, undefined, ParamAccess.Read)],
            },
            undefined,
            undefined,
        ),
        new_param(TYPE_DWORD, undefined, undefined),
        new_param(TYPE_I_LABEL, undefined, undefined),
    ],
    undefined,
));
export const OP_UJMP_G = (OPCODES[0x30] = new_opcode(
    0x30,
    "ujmp_>",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_DWORD, undefined, ParamAccess.Read)],
            },
            undefined,
            undefined,
        ),
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_DWORD, undefined, ParamAccess.Read)],
            },
            undefined,
            undefined,
        ),
        new_param(TYPE_I_LABEL, undefined, undefined),
    ],
    undefined,
));
export const OP_UJMPI_G = (OPCODES[0x31] = new_opcode(
    0x31,
    "ujmpi_>",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_DWORD, undefined, ParamAccess.Read)],
            },
            undefined,
            undefined,
        ),
        new_param(TYPE_DWORD, undefined, undefined),
        new_param(TYPE_I_LABEL, undefined, undefined),
    ],
    undefined,
));
export const OP_JMP_G = (OPCODES[0x32] = new_opcode(
    0x32,
    "jmp_>",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_DWORD, undefined, ParamAccess.Read)],
            },
            undefined,
            undefined,
        ),
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_DWORD, undefined, ParamAccess.Read)],
            },
            undefined,
            undefined,
        ),
        new_param(TYPE_I_LABEL, undefined, undefined),
    ],
    undefined,
));
export const OP_JMPI_G = (OPCODES[0x33] = new_opcode(
    0x33,
    "jmpi_>",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_DWORD, undefined, ParamAccess.Read)],
            },
            undefined,
            undefined,
        ),
        new_param(TYPE_DWORD, undefined, undefined),
        new_param(TYPE_I_LABEL, undefined, undefined),
    ],
    undefined,
));
export const OP_UJMP_L = (OPCODES[0x34] = new_opcode(
    0x34,
    "ujmp_<",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_DWORD, undefined, ParamAccess.Read)],
            },
            undefined,
            undefined,
        ),
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_DWORD, undefined, ParamAccess.Read)],
            },
            undefined,
            undefined,
        ),
        new_param(TYPE_I_LABEL, undefined, undefined),
    ],
    undefined,
));
export const OP_UJMPI_L = (OPCODES[0x35] = new_opcode(
    0x35,
    "ujmpi_<",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_DWORD, undefined, ParamAccess.Read)],
            },
            undefined,
            undefined,
        ),
        new_param(TYPE_DWORD, undefined, undefined),
        new_param(TYPE_I_LABEL, undefined, undefined),
    ],
    undefined,
));
export const OP_JMP_L = (OPCODES[0x36] = new_opcode(
    0x36,
    "jmp_<",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_DWORD, undefined, ParamAccess.Read)],
            },
            undefined,
            undefined,
        ),
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_DWORD, undefined, ParamAccess.Read)],
            },
            undefined,
            undefined,
        ),
        new_param(TYPE_I_LABEL, undefined, undefined),
    ],
    undefined,
));
export const OP_JMPI_L = (OPCODES[0x37] = new_opcode(
    0x37,
    "jmpi_<",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_DWORD, undefined, ParamAccess.Read)],
            },
            undefined,
            undefined,
        ),
        new_param(TYPE_DWORD, undefined, undefined),
        new_param(TYPE_I_LABEL, undefined, undefined),
    ],
    undefined,
));
export const OP_UJMP_GE = (OPCODES[0x38] = new_opcode(
    0x38,
    "ujmp_>=",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_DWORD, undefined, ParamAccess.Read)],
            },
            undefined,
            undefined,
        ),
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_DWORD, undefined, ParamAccess.Read)],
            },
            undefined,
            undefined,
        ),
        new_param(TYPE_I_LABEL, undefined, undefined),
    ],
    undefined,
));
export const OP_UJMPI_GE = (OPCODES[0x39] = new_opcode(
    0x39,
    "ujmpi_>=",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_DWORD, undefined, ParamAccess.Read)],
            },
            undefined,
            undefined,
        ),
        new_param(TYPE_DWORD, undefined, undefined),
        new_param(TYPE_I_LABEL, undefined, undefined),
    ],
    undefined,
));
export const OP_JMP_GE = (OPCODES[0x3a] = new_opcode(
    0x3a,
    "jmp_>=",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_DWORD, undefined, ParamAccess.Read)],
            },
            undefined,
            undefined,
        ),
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_DWORD, undefined, ParamAccess.Read)],
            },
            undefined,
            undefined,
        ),
        new_param(TYPE_I_LABEL, undefined, undefined),
    ],
    undefined,
));
export const OP_JMPI_GE = (OPCODES[0x3b] = new_opcode(
    0x3b,
    "jmpi_>=",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_DWORD, undefined, ParamAccess.Read)],
            },
            undefined,
            undefined,
        ),
        new_param(TYPE_DWORD, undefined, undefined),
        new_param(TYPE_I_LABEL, undefined, undefined),
    ],
    undefined,
));
export const OP_UJMP_LE = (OPCODES[0x3c] = new_opcode(
    0x3c,
    "ujmp_<=",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_DWORD, undefined, ParamAccess.Read)],
            },
            undefined,
            undefined,
        ),
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_DWORD, undefined, ParamAccess.Read)],
            },
            undefined,
            undefined,
        ),
        new_param(TYPE_I_LABEL, undefined, undefined),
    ],
    undefined,
));
export const OP_UJMPI_LE = (OPCODES[0x3d] = new_opcode(
    0x3d,
    "ujmpi_<=",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_DWORD, undefined, ParamAccess.Read)],
            },
            undefined,
            undefined,
        ),
        new_param(TYPE_DWORD, undefined, undefined),
        new_param(TYPE_I_LABEL, undefined, undefined),
    ],
    undefined,
));
export const OP_JMP_LE = (OPCODES[0x3e] = new_opcode(
    0x3e,
    "jmp_<=",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_DWORD, undefined, ParamAccess.Read)],
            },
            undefined,
            undefined,
        ),
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_DWORD, undefined, ParamAccess.Read)],
            },
            undefined,
            undefined,
        ),
        new_param(TYPE_I_LABEL, undefined, undefined),
    ],
    undefined,
));
export const OP_JMPI_LE = (OPCODES[0x3f] = new_opcode(
    0x3f,
    "jmpi_<=",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_DWORD, undefined, ParamAccess.Read)],
            },
            undefined,
            undefined,
        ),
        new_param(TYPE_DWORD, undefined, undefined),
        new_param(TYPE_I_LABEL, undefined, undefined),
    ],
    undefined,
));
export const OP_SWITCH_JMP = (OPCODES[0x40] = new_opcode(
    0x40,
    "switch_jmp",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_DWORD, undefined, ParamAccess.Read)],
            },
            undefined,
            undefined,
        ),
        new_param(TYPE_I_LABEL_VAR, undefined, undefined),
    ],
    undefined,
));
export const OP_SWITCH_CALL = (OPCODES[0x41] = new_opcode(
    0x41,
    "switch_call",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_DWORD, undefined, ParamAccess.Read)],
            },
            undefined,
            undefined,
        ),
        new_param(TYPE_I_LABEL_VAR, undefined, undefined),
    ],
    undefined,
));
export const OP_STACK_PUSH = (OPCODES[0x42] = new_opcode(
    0x42,
    "stack_push",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_ANY, undefined, ParamAccess.Read)],
            },
            undefined,
            undefined,
        ),
    ],
    undefined,
));
export const OP_STACK_POP = (OPCODES[0x43] = new_opcode(
    0x43,
    "stack_pop",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_ANY, undefined, ParamAccess.Write)],
            },
            undefined,
            undefined,
        ),
    ],
    undefined,
));
export const OP_STACK_PUSHM = (OPCODES[0x44] = new_opcode(
    0x44,
    "stack_pushm",
    "Pushes the values of an arbitrary amount of registers onto the stack.",
    [
        new_param(TYPE_REG_REF, undefined, ParamAccess.Read),
        new_param(TYPE_DWORD, undefined, undefined),
    ],
    undefined,
));
export const OP_STACK_POPM = (OPCODES[0x45] = new_opcode(
    0x45,
    "stack_popm",
    "Pops an arbitrary amount of values from the stack and writes them to registers.",
    [
        new_param(TYPE_REG_REF, undefined, ParamAccess.Write),
        new_param(TYPE_DWORD, undefined, undefined),
    ],
    undefined,
));
export const OP_UNKNOWN_46 = (OPCODES[0x46] = new_opcode(
    0x46,
    "unknown_46",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_47 = (OPCODES[0x47] = new_opcode(
    0x47,
    "unknown_47",
    undefined,
    [],
    undefined,
));
export const OP_ARG_PUSHR = (OPCODES[0x48] = new_opcode(
    0x48,
    "arg_pushr",
    "Pushes the value of the given register onto the stack.",
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_ANY, undefined, ParamAccess.Read)],
            },
            undefined,
            undefined,
        ),
    ],
    StackInteraction.Push,
));
export const OP_ARG_PUSHL = (OPCODES[0x49] = new_opcode(
    0x49,
    "arg_pushl",
    "Pushes the given value onto the stack.",
    [new_param(TYPE_DWORD, undefined, undefined)],
    StackInteraction.Push,
));
export const OP_ARG_PUSHB = (OPCODES[0x4a] = new_opcode(
    0x4a,
    "arg_pushb",
    "Pushes the given value onto the stack.",
    [new_param(TYPE_BYTE, undefined, undefined)],
    StackInteraction.Push,
));
export const OP_ARG_PUSHW = (OPCODES[0x4b] = new_opcode(
    0x4b,
    "arg_pushw",
    "Pushes the given value onto the stack.",
    [new_param(TYPE_WORD, undefined, undefined)],
    StackInteraction.Push,
));
export const OP_ARG_PUSHA = (OPCODES[0x4c] = new_opcode(
    0x4c,
    "arg_pusha",
    "Pushes the memory address of the given register onto the stack. Not used by Sega.",
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_ANY, undefined, ParamAccess.Read)],
            },
            undefined,
            undefined,
        ),
    ],
    StackInteraction.Push,
));
export const OP_ARG_PUSHO = (OPCODES[0x4d] = new_opcode(
    0x4d,
    "arg_pusho",
    "Pushes the memory address of the given label onto the stack. Not used by Sega.",
    [new_param(TYPE_LABEL, undefined, undefined)],
    StackInteraction.Push,
));
export const OP_ARG_PUSHS = (OPCODES[0x4e] = new_opcode(
    0x4e,
    "arg_pushs",
    "Pushes the given value onto the stack.",
    [new_param(TYPE_STRING, undefined, undefined)],
    StackInteraction.Push,
));
export const OP_UNKNOWN_4F = (OPCODES[0x4f] = new_opcode(
    0x4f,
    "unknown_4f",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_ANY, undefined, ParamAccess.Read)],
            },
            undefined,
            undefined,
        ),
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_ANY, undefined, ParamAccess.Read)],
            },
            undefined,
            undefined,
        ),
    ],
    undefined,
));
export const OP_MESSAGE = (OPCODES[0x50] = new_opcode(
    0x50,
    "message",
    undefined,
    [new_param(TYPE_DWORD, undefined, undefined), new_param(TYPE_STRING, undefined, undefined)],
    StackInteraction.Pop,
));
export const OP_LIST = (OPCODES[0x51] = new_opcode(
    0x51,
    "list",
    "Used to display a list of items and retrieve the item selected by the player.\nList items should be seperated by newlines. The selected item's index will be written to the given register.",
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_BYTE, undefined, ParamAccess.Write)],
            },
            undefined,
            undefined,
        ),
        new_param(TYPE_STRING, undefined, undefined),
    ],
    StackInteraction.Pop,
));
export const OP_FADEIN = (OPCODES[0x52] = new_opcode(0x52, "fadein", undefined, [], undefined));
export const OP_FADEOUT = (OPCODES[0x53] = new_opcode(0x53, "fadeout", undefined, [], undefined));
export const OP_SE = (OPCODES[0x54] = new_opcode(
    0x54,
    "se",
    undefined,
    [new_param(TYPE_DWORD, undefined, undefined)],
    StackInteraction.Pop,
));
export const OP_BGM = (OPCODES[0x55] = new_opcode(
    0x55,
    "bgm",
    undefined,
    [new_param(TYPE_DWORD, undefined, undefined)],
    StackInteraction.Pop,
));
export const OP_UNKNOWN_56 = (OPCODES[0x56] = new_opcode(
    0x56,
    "unknown_56",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_57 = (OPCODES[0x57] = new_opcode(
    0x57,
    "unknown_57",
    undefined,
    [],
    undefined,
));
export const OP_ENABLE = (OPCODES[0x58] = new_opcode(
    0x58,
    "enable",
    undefined,
    [new_param(TYPE_DWORD, undefined, undefined)],
    StackInteraction.Pop,
));
export const OP_DISABLE = (OPCODES[0x59] = new_opcode(
    0x59,
    "disable",
    undefined,
    [new_param(TYPE_DWORD, undefined, undefined)],
    StackInteraction.Pop,
));
export const OP_WINDOW_MSG = (OPCODES[0x5a] = new_opcode(
    0x5a,
    "window_msg",
    undefined,
    [new_param(TYPE_STRING, undefined, undefined)],
    StackInteraction.Pop,
));
export const OP_ADD_MSG = (OPCODES[0x5b] = new_opcode(
    0x5b,
    "add_msg",
    undefined,
    [new_param(TYPE_STRING, undefined, undefined)],
    StackInteraction.Pop,
));
export const OP_MESEND = (OPCODES[0x5c] = new_opcode(0x5c, "mesend", undefined, [], undefined));
export const OP_GETTIME = (OPCODES[0x5d] = new_opcode(
    0x5d,
    "gettime",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_DWORD, undefined, ParamAccess.Write)],
            },
            undefined,
            undefined,
        ),
    ],
    undefined,
));
export const OP_WINEND = (OPCODES[0x5e] = new_opcode(0x5e, "winend", undefined, [], undefined));
export const OP_UNKNOWN_5F = (OPCODES[0x5f] = new_opcode(
    0x5f,
    "unknown_5f",
    undefined,
    [],
    undefined,
));
export const OP_NPC_CRT_V3 = (OPCODES[0x60] = new_opcode(
    0x60,
    "npc_crt_v3",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_ANY, undefined, ParamAccess.Read)],
            },
            undefined,
            undefined,
        ),
    ],
    undefined,
));
export const OP_NPC_STOP = (OPCODES[0x61] = new_opcode(
    0x61,
    "npc_stop",
    undefined,
    [new_param(TYPE_DWORD, undefined, undefined)],
    StackInteraction.Pop,
));
export const OP_NPC_PLAY = (OPCODES[0x62] = new_opcode(
    0x62,
    "npc_play",
    undefined,
    [new_param(TYPE_DWORD, undefined, undefined)],
    StackInteraction.Pop,
));
export const OP_NPC_KILL = (OPCODES[0x63] = new_opcode(
    0x63,
    "npc_kill",
    undefined,
    [new_param(TYPE_DWORD, undefined, undefined)],
    StackInteraction.Pop,
));
export const OP_NPC_NONT = (OPCODES[0x64] = new_opcode(0x64, "npc_nont", undefined, [], undefined));
export const OP_NPC_TALK = (OPCODES[0x65] = new_opcode(0x65, "npc_talk", undefined, [], undefined));
export const OP_NPC_CRP_V3 = (OPCODES[0x66] = new_opcode(
    0x66,
    "npc_crp_v3",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [
                    new_param(TYPE_ANY, undefined, ParamAccess.Read),
                    new_param(TYPE_ANY, undefined, ParamAccess.Read),
                    new_param(TYPE_ANY, undefined, ParamAccess.Read),
                    new_param(TYPE_I_LABEL, undefined, ParamAccess.Read),
                    new_param(TYPE_ANY, undefined, ParamAccess.Read),
                    new_param(TYPE_ANY, undefined, ParamAccess.Read),
                ],
            },
            undefined,
            undefined,
        ),
    ],
    undefined,
));
export const OP_UNKNOWN_67 = (OPCODES[0x67] = new_opcode(
    0x67,
    "unknown_67",
    undefined,
    [],
    undefined,
));
export const OP_CREATE_PIPE = (OPCODES[0x68] = new_opcode(
    0x68,
    "create_pipe",
    undefined,
    [new_param(TYPE_DWORD, undefined, undefined)],
    StackInteraction.Pop,
));
export const OP_P_HPSTAT_V3 = (OPCODES[0x69] = new_opcode(
    0x69,
    "p_hpstat_v3",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_ANY, undefined, ParamAccess.Read)],
            },
            undefined,
            undefined,
        ),
        new_param(TYPE_DWORD, undefined, undefined),
    ],
    StackInteraction.Pop,
));
export const OP_P_DEAD_V3 = (OPCODES[0x6a] = new_opcode(
    0x6a,
    "p_dead_v3",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_DWORD, undefined, ParamAccess.Write)],
            },
            undefined,
            undefined,
        ),
        new_param(TYPE_DWORD, "Player slot.", undefined),
    ],
    StackInteraction.Pop,
));
export const OP_P_DISABLEWARP = (OPCODES[0x6b] = new_opcode(
    0x6b,
    "p_disablewarp",
    undefined,
    [],
    undefined,
));
export const OP_P_ENABLEWARP = (OPCODES[0x6c] = new_opcode(
    0x6c,
    "p_enablewarp",
    undefined,
    [],
    undefined,
));
export const OP_P_MOVE_V3 = (OPCODES[0x6d] = new_opcode(
    0x6d,
    "p_move_v3",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_ANY, undefined, ParamAccess.Read)],
            },
            undefined,
            undefined,
        ),
    ],
    undefined,
));
export const OP_P_LOOK = (OPCODES[0x6e] = new_opcode(
    0x6e,
    "p_look",
    undefined,
    [new_param(TYPE_DWORD, undefined, undefined)],
    StackInteraction.Pop,
));
export const OP_UNKNOWN_6F = (OPCODES[0x6f] = new_opcode(
    0x6f,
    "unknown_6f",
    undefined,
    [],
    undefined,
));
export const OP_P_ACTION_DISABLE = (OPCODES[0x70] = new_opcode(
    0x70,
    "p_action_disable",
    undefined,
    [],
    undefined,
));
export const OP_P_ACTION_ENABLE = (OPCODES[0x71] = new_opcode(
    0x71,
    "p_action_enable",
    undefined,
    [],
    undefined,
));
export const OP_DISABLE_MOVEMENT1 = (OPCODES[0x72] = new_opcode(
    0x72,
    "disable_movement1",
    undefined,
    [new_param(TYPE_DWORD, undefined, undefined)],
    StackInteraction.Pop,
));
export const OP_ENABLE_MOVEMENT1 = (OPCODES[0x73] = new_opcode(
    0x73,
    "enable_movement1",
    undefined,
    [new_param(TYPE_DWORD, undefined, undefined)],
    StackInteraction.Pop,
));
export const OP_P_NONCOL = (OPCODES[0x74] = new_opcode(0x74, "p_noncol", undefined, [], undefined));
export const OP_P_COL = (OPCODES[0x75] = new_opcode(0x75, "p_col", undefined, [], undefined));
export const OP_P_SETPOS = (OPCODES[0x76] = new_opcode(
    0x76,
    "p_setpos",
    "Sets a player's position.",
    [
        new_param(TYPE_DWORD, "Player slot.", undefined),
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [
                    new_param(TYPE_DWORD, "X coordinate.", ParamAccess.Read),
                    new_param(TYPE_DWORD, "Y coordinate.", ParamAccess.Read),
                    new_param(TYPE_DWORD, "Z coordinate.", ParamAccess.Read),
                    new_param(TYPE_DWORD, "Y-axis rotation.", ParamAccess.Read),
                ],
            },
            undefined,
            undefined,
        ),
    ],
    StackInteraction.Pop,
));
export const OP_P_RETURN_GUILD = (OPCODES[0x77] = new_opcode(
    0x77,
    "p_return_guild",
    undefined,
    [],
    undefined,
));
export const OP_P_TALK_GUILD = (OPCODES[0x78] = new_opcode(
    0x78,
    "p_talk_guild",
    undefined,
    [new_param(TYPE_DWORD, undefined, undefined)],
    StackInteraction.Pop,
));
export const OP_NPC_TALK_PL_V3 = (OPCODES[0x79] = new_opcode(
    0x79,
    "npc_talk_pl_v3",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_ANY, undefined, ParamAccess.Read)],
            },
            undefined,
            undefined,
        ),
    ],
    undefined,
));
export const OP_NPC_TALK_KILL = (OPCODES[0x7a] = new_opcode(
    0x7a,
    "npc_talk_kill",
    undefined,
    [new_param(TYPE_DWORD, undefined, undefined)],
    StackInteraction.Pop,
));
export const OP_NPC_CRTPK_V3 = (OPCODES[0x7b] = new_opcode(
    0x7b,
    "npc_crtpk_v3",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_ANY, undefined, ParamAccess.Read)],
            },
            undefined,
            undefined,
        ),
    ],
    undefined,
));
export const OP_NPC_CRPPK_V3 = (OPCODES[0x7c] = new_opcode(
    0x7c,
    "npc_crppk_v3",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_ANY, undefined, ParamAccess.Read)],
            },
            undefined,
            undefined,
        ),
    ],
    undefined,
));
export const OP_NPC_CRPTALK_V3 = (OPCODES[0x7d] = new_opcode(
    0x7d,
    "npc_crptalk_v3",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_ANY, undefined, ParamAccess.Read)],
            },
            undefined,
            undefined,
        ),
    ],
    undefined,
));
export const OP_P_LOOK_AT_V1 = (OPCODES[0x7e] = new_opcode(
    0x7e,
    "p_look_at_v1",
    undefined,
    [new_param(TYPE_DWORD, undefined, undefined), new_param(TYPE_DWORD, undefined, undefined)],
    StackInteraction.Pop,
));
export const OP_NPC_CRP_ID_V3 = (OPCODES[0x7f] = new_opcode(
    0x7f,
    "npc_crp_id_v3",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_ANY, undefined, ParamAccess.Read)],
            },
            undefined,
            undefined,
        ),
    ],
    undefined,
));
export const OP_CAM_QUAKE = (OPCODES[0x80] = new_opcode(
    0x80,
    "cam_quake",
    undefined,
    [],
    undefined,
));
export const OP_CAM_ADJ = (OPCODES[0x81] = new_opcode(0x81, "cam_adj", undefined, [], undefined));
export const OP_CAM_ZMIN = (OPCODES[0x82] = new_opcode(0x82, "cam_zmin", undefined, [], undefined));
export const OP_CAM_ZMOUT = (OPCODES[0x83] = new_opcode(
    0x83,
    "cam_zmout",
    undefined,
    [],
    undefined,
));
export const OP_CAM_PAN_V3 = (OPCODES[0x84] = new_opcode(
    0x84,
    "cam_pan_v3",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [
                    new_param(TYPE_DWORD, undefined, ParamAccess.Read),
                    new_param(TYPE_DWORD, undefined, ParamAccess.Read),
                    new_param(TYPE_DWORD, undefined, ParamAccess.Read),
                    new_param(TYPE_DWORD, undefined, ParamAccess.Read),
                    new_param(TYPE_DWORD, undefined, ParamAccess.Read),
                ],
            },
            undefined,
            undefined,
        ),
    ],
    undefined,
));
export const OP_GAME_LEV_SUPER = (OPCODES[0x85] = new_opcode(
    0x85,
    "game_lev_super",
    undefined,
    [],
    undefined,
));
export const OP_GAME_LEV_RESET = (OPCODES[0x86] = new_opcode(
    0x86,
    "game_lev_reset",
    undefined,
    [],
    undefined,
));
export const OP_POS_PIPE_V3 = (OPCODES[0x87] = new_opcode(
    0x87,
    "pos_pipe_v3",
    "Create a telepipe at a specific position for the given player slot that takes players back to Pioneer 2 or the Lab.",
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [
                    new_param(TYPE_DWORD, "X coordinate.", ParamAccess.Read),
                    new_param(TYPE_DWORD, "Y coordinate.", ParamAccess.Read),
                    new_param(TYPE_DWORD, "Z coordinate.", ParamAccess.Read),
                    new_param(TYPE_DWORD, "Player slot.", ParamAccess.Read),
                ],
            },
            undefined,
            undefined,
        ),
    ],
    undefined,
));
export const OP_IF_ZONE_CLEAR = (OPCODES[0x88] = new_opcode(
    0x88,
    "if_zone_clear",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_DWORD, undefined, ParamAccess.Write)],
            },
            undefined,
            undefined,
        ),
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [
                    new_param(TYPE_DWORD, undefined, ParamAccess.Read),
                    new_param(TYPE_DWORD, undefined, ParamAccess.Read),
                ],
            },
            undefined,
            undefined,
        ),
    ],
    undefined,
));
export const OP_CHK_ENE_NUM = (OPCODES[0x89] = new_opcode(
    0x89,
    "chk_ene_num",
    "Retrieves the amount of enemies killed during the quest.",
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_DWORD, undefined, ParamAccess.Write)],
            },
            undefined,
            undefined,
        ),
    ],
    undefined,
));
export const OP_UNHIDE_OBJ = (OPCODES[0x8a] = new_opcode(
    0x8a,
    "unhide_obj",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [
                    new_param(TYPE_DWORD, undefined, ParamAccess.Read),
                    new_param(TYPE_DWORD, undefined, ParamAccess.Read),
                    new_param(TYPE_DWORD, undefined, ParamAccess.Read),
                ],
            },
            undefined,
            undefined,
        ),
    ],
    undefined,
));
export const OP_UNHIDE_ENE = (OPCODES[0x8b] = new_opcode(
    0x8b,
    "unhide_ene",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [
                    new_param(TYPE_DWORD, undefined, ParamAccess.Read),
                    new_param(TYPE_DWORD, undefined, ParamAccess.Read),
                    new_param(TYPE_DWORD, undefined, ParamAccess.Read),
                    new_param(TYPE_DWORD, undefined, ParamAccess.Read),
                ],
            },
            undefined,
            undefined,
        ),
    ],
    undefined,
));
export const OP_AT_COORDS_CALL = (OPCODES[0x8c] = new_opcode(
    0x8c,
    "at_coords_call",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [
                    new_param(TYPE_DWORD, undefined, ParamAccess.Read),
                    new_param(TYPE_DWORD, undefined, ParamAccess.Read),
                    new_param(TYPE_DWORD, undefined, ParamAccess.Read),
                    new_param(TYPE_DWORD, undefined, ParamAccess.Read),
                    new_param(TYPE_I_LABEL, undefined, ParamAccess.Read),
                ],
            },
            undefined,
            undefined,
        ),
    ],
    undefined,
));
export const OP_AT_COORDS_TALK = (OPCODES[0x8d] = new_opcode(
    0x8d,
    "at_coords_talk",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [
                    new_param(TYPE_DWORD, undefined, ParamAccess.Read),
                    new_param(TYPE_DWORD, undefined, ParamAccess.Read),
                    new_param(TYPE_DWORD, undefined, ParamAccess.Read),
                    new_param(TYPE_DWORD, undefined, ParamAccess.Read),
                    new_param(TYPE_I_LABEL, undefined, ParamAccess.Read),
                ],
            },
            undefined,
            undefined,
        ),
    ],
    undefined,
));
export const OP_COL_NPCIN = (OPCODES[0x8e] = new_opcode(
    0x8e,
    "col_npcin",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [
                    new_param(TYPE_DWORD, undefined, ParamAccess.Read),
                    new_param(TYPE_DWORD, undefined, ParamAccess.Read),
                    new_param(TYPE_DWORD, undefined, ParamAccess.Read),
                    new_param(TYPE_DWORD, undefined, ParamAccess.Read),
                    new_param(TYPE_I_LABEL, undefined, ParamAccess.Read),
                ],
            },
            undefined,
            undefined,
        ),
    ],
    undefined,
));
export const OP_COL_NPCINR = (OPCODES[0x8f] = new_opcode(
    0x8f,
    "col_npcinr",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_ANY, undefined, ParamAccess.Read)],
            },
            undefined,
            undefined,
        ),
    ],
    undefined,
));
export const OP_SWITCH_ON = (OPCODES[0x90] = new_opcode(
    0x90,
    "switch_on",
    undefined,
    [new_param(TYPE_DWORD, undefined, undefined)],
    StackInteraction.Pop,
));
export const OP_SWITCH_OFF = (OPCODES[0x91] = new_opcode(
    0x91,
    "switch_off",
    undefined,
    [new_param(TYPE_DWORD, undefined, undefined)],
    StackInteraction.Pop,
));
export const OP_PLAYBGM_EPI = (OPCODES[0x92] = new_opcode(
    0x92,
    "playbgm_epi",
    undefined,
    [new_param(TYPE_DWORD, undefined, undefined)],
    StackInteraction.Pop,
));
export const OP_SET_MAINWARP = (OPCODES[0x93] = new_opcode(
    0x93,
    "set_mainwarp",
    undefined,
    [new_param(TYPE_DWORD, undefined, undefined)],
    StackInteraction.Pop,
));
export const OP_SET_OBJ_PARAM = (OPCODES[0x94] = new_opcode(
    0x94,
    "set_obj_param",
    "Creates a targetable object.",
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [
                    new_param(TYPE_DWORD, "X coordinate.", ParamAccess.Read),
                    new_param(TYPE_DWORD, "Y coordinate.", ParamAccess.Read),
                    new_param(TYPE_DWORD, "Z coordinate.", ParamAccess.Read),
                    new_param(TYPE_DWORD, "Collision radius.", ParamAccess.Read),
                    new_param(TYPE_I_LABEL, "Function label.", ParamAccess.Read),
                    new_param(TYPE_DWORD, "Vertical position of the cursor.", ParamAccess.Read),
                ],
            },
            undefined,
            undefined,
        ),
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_ANY, undefined, ParamAccess.Write)],
            },
            "Object handle.",
            undefined,
        ),
    ],
    undefined,
));
export const OP_SET_FLOOR_HANDLER = (OPCODES[0x95] = new_opcode(
    0x95,
    "set_floor_handler",
    undefined,
    [
        new_param(TYPE_DWORD, "Floor number.", undefined),
        new_param(TYPE_I_LABEL, "Handler function label.", undefined),
    ],
    StackInteraction.Pop,
));
export const OP_CLR_FLOOR_HANDLER = (OPCODES[0x96] = new_opcode(
    0x96,
    "clr_floor_handler",
    undefined,
    [new_param(TYPE_DWORD, "Floor number.", undefined)],
    StackInteraction.Pop,
));
export const OP_COL_PLINAW = (OPCODES[0x97] = new_opcode(
    0x97,
    "col_plinaw",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_ANY, undefined, ParamAccess.Read)],
            },
            undefined,
            undefined,
        ),
    ],
    undefined,
));
export const OP_HUD_HIDE = (OPCODES[0x98] = new_opcode(0x98, "hud_hide", undefined, [], undefined));
export const OP_HUD_SHOW = (OPCODES[0x99] = new_opcode(0x99, "hud_show", undefined, [], undefined));
export const OP_CINE_ENABLE = (OPCODES[0x9a] = new_opcode(
    0x9a,
    "cine_enable",
    undefined,
    [],
    undefined,
));
export const OP_CINE_DISABLE = (OPCODES[0x9b] = new_opcode(
    0x9b,
    "cine_disable",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_9C = (OPCODES[0x9c] = new_opcode(
    0x9c,
    "unknown_9c",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_9D = (OPCODES[0x9d] = new_opcode(
    0x9d,
    "unknown_9d",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_9E = (OPCODES[0x9e] = new_opcode(
    0x9e,
    "unknown_9e",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_9F = (OPCODES[0x9f] = new_opcode(
    0x9f,
    "unknown_9f",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_A0 = (OPCODES[0xa0] = new_opcode(
    0xa0,
    "unknown_a0",
    undefined,
    [],
    undefined,
));
export const OP_SET_QT_FAILURE = (OPCODES[0xa1] = new_opcode(
    0xa1,
    "set_qt_failure",
    undefined,
    [new_param(TYPE_I_LABEL, undefined, undefined)],
    undefined,
));
export const OP_SET_QT_SUCCESS = (OPCODES[0xa2] = new_opcode(
    0xa2,
    "set_qt_success",
    undefined,
    [new_param(TYPE_I_LABEL, undefined, undefined)],
    undefined,
));
export const OP_CLR_QT_FAILURE = (OPCODES[0xa3] = new_opcode(
    0xa3,
    "clr_qt_failure",
    undefined,
    [],
    undefined,
));
export const OP_CLR_QT_SUCCESS = (OPCODES[0xa4] = new_opcode(
    0xa4,
    "clr_qt_success",
    undefined,
    [],
    undefined,
));
export const OP_SET_QT_CANCEL = (OPCODES[0xa5] = new_opcode(
    0xa5,
    "set_qt_cancel",
    undefined,
    [new_param(TYPE_I_LABEL, undefined, undefined)],
    undefined,
));
export const OP_CLR_QT_CANCEL = (OPCODES[0xa6] = new_opcode(
    0xa6,
    "clr_qt_cancel",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_A7 = (OPCODES[0xa7] = new_opcode(
    0xa7,
    "unknown_a7",
    undefined,
    [],
    undefined,
));
export const OP_PL_WALK_V3 = (OPCODES[0xa8] = new_opcode(
    0xa8,
    "pl_walk_v3",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_ANY, undefined, ParamAccess.Read)],
            },
            undefined,
            undefined,
        ),
    ],
    undefined,
));
export const OP_UNKNOWN_A9 = (OPCODES[0xa9] = new_opcode(
    0xa9,
    "unknown_a9",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_AA = (OPCODES[0xaa] = new_opcode(
    0xaa,
    "unknown_aa",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_AB = (OPCODES[0xab] = new_opcode(
    0xab,
    "unknown_ab",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_AC = (OPCODES[0xac] = new_opcode(
    0xac,
    "unknown_ac",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_AD = (OPCODES[0xad] = new_opcode(
    0xad,
    "unknown_ad",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_AE = (OPCODES[0xae] = new_opcode(
    0xae,
    "unknown_ae",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_AF = (OPCODES[0xaf] = new_opcode(
    0xaf,
    "unknown_af",
    undefined,
    [],
    undefined,
));
export const OP_PL_ADD_MESETA = (OPCODES[0xb0] = new_opcode(
    0xb0,
    "pl_add_meseta",
    undefined,
    [new_param(TYPE_DWORD, undefined, undefined), new_param(TYPE_DWORD, undefined, undefined)],
    StackInteraction.Pop,
));
export const OP_THREAD_STG = (OPCODES[0xb1] = new_opcode(
    0xb1,
    "thread_stg",
    undefined,
    [new_param(TYPE_I_LABEL, undefined, undefined)],
    undefined,
));
export const OP_DEL_OBJ_PARAM = (OPCODES[0xb2] = new_opcode(
    0xb2,
    "del_obj_param",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_ANY, undefined, ParamAccess.Read)],
            },
            "Object handle.",
            undefined,
        ),
    ],
    undefined,
));
export const OP_ITEM_CREATE = (OPCODES[0xb3] = new_opcode(
    0xb3,
    "item_create",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_ANY, undefined, ParamAccess.Read)],
            },
            undefined,
            undefined,
        ),
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_ANY, undefined, ParamAccess.Read)],
            },
            undefined,
            undefined,
        ),
    ],
    undefined,
));
export const OP_ITEM_CREATE2 = (OPCODES[0xb4] = new_opcode(
    0xb4,
    "item_create2",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_ANY, undefined, ParamAccess.Read)],
            },
            undefined,
            undefined,
        ),
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_ANY, undefined, ParamAccess.Read)],
            },
            undefined,
            undefined,
        ),
    ],
    undefined,
));
export const OP_ITEM_DELETE = (OPCODES[0xb5] = new_opcode(
    0xb5,
    "item_delete",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_ANY, undefined, ParamAccess.Read)],
            },
            undefined,
            undefined,
        ),
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_ANY, undefined, ParamAccess.Read)],
            },
            undefined,
            undefined,
        ),
    ],
    undefined,
));
export const OP_ITEM_DELETE2 = (OPCODES[0xb6] = new_opcode(
    0xb6,
    "item_delete2",
    "Deletes an item from the player's inventory.",
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [
                    new_param(TYPE_DWORD, undefined, ParamAccess.Read),
                    new_param(TYPE_DWORD, undefined, ParamAccess.Read),
                    new_param(TYPE_DWORD, undefined, ParamAccess.Read),
                ],
            },
            undefined,
            undefined,
        ),
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_ANY, undefined, ParamAccess.Write)],
            },
            undefined,
            undefined,
        ),
    ],
    undefined,
));
export const OP_ITEM_CHECK = (OPCODES[0xb7] = new_opcode(
    0xb7,
    "item_check",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_ANY, undefined, ParamAccess.Read)],
            },
            undefined,
            undefined,
        ),
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_ANY, undefined, ParamAccess.Read)],
            },
            undefined,
            undefined,
        ),
    ],
    undefined,
));
export const OP_SETEVT = (OPCODES[0xb8] = new_opcode(
    0xb8,
    "setevt",
    undefined,
    [new_param(TYPE_DWORD, undefined, undefined)],
    StackInteraction.Pop,
));
export const OP_GET_DIFFLVL = (OPCODES[0xb9] = new_opcode(
    0xb9,
    "get_difflvl",
    "Sets the given register to the current difficulty. 0 For normal, 1 for hard and 2 for both very hard and ultimate.\nUse get_difficulty_level2 if you want to differentiate between very hard and ultimate.",
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_DWORD, undefined, ParamAccess.Write)],
            },
            undefined,
            undefined,
        ),
    ],
    undefined,
));
export const OP_SET_QT_EXIT = (OPCODES[0xba] = new_opcode(
    0xba,
    "set_qt_exit",
    undefined,
    [new_param(TYPE_I_LABEL, undefined, undefined)],
    undefined,
));
export const OP_CLR_QT_EXIT = (OPCODES[0xbb] = new_opcode(
    0xbb,
    "clr_qt_exit",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_BC = (OPCODES[0xbc] = new_opcode(
    0xbc,
    "unknown_bc",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_BD = (OPCODES[0xbd] = new_opcode(
    0xbd,
    "unknown_bd",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_BE = (OPCODES[0xbe] = new_opcode(
    0xbe,
    "unknown_be",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_BF = (OPCODES[0xbf] = new_opcode(
    0xbf,
    "unknown_bf",
    undefined,
    [],
    undefined,
));
export const OP_PARTICLE_V3 = (OPCODES[0xc0] = new_opcode(
    0xc0,
    "particle_v3",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_ANY, undefined, ParamAccess.Read)],
            },
            undefined,
            undefined,
        ),
    ],
    undefined,
));
export const OP_NPC_TEXT = (OPCODES[0xc1] = new_opcode(
    0xc1,
    "npc_text",
    undefined,
    [new_param(TYPE_DWORD, undefined, undefined), new_param(TYPE_STRING, undefined, undefined)],
    StackInteraction.Pop,
));
export const OP_NPC_CHKWARP = (OPCODES[0xc2] = new_opcode(
    0xc2,
    "npc_chkwarp",
    undefined,
    [],
    undefined,
));
export const OP_PL_PKOFF = (OPCODES[0xc3] = new_opcode(0xc3, "pl_pkoff", undefined, [], undefined));
export const OP_MAP_DESIGNATE = (OPCODES[0xc4] = new_opcode(
    0xc4,
    "map_designate",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_ANY, undefined, ParamAccess.Read)],
            },
            undefined,
            undefined,
        ),
    ],
    undefined,
));
export const OP_MASTERKEY_ON = (OPCODES[0xc5] = new_opcode(
    0xc5,
    "masterkey_on",
    undefined,
    [],
    undefined,
));
export const OP_MASTERKEY_OFF = (OPCODES[0xc6] = new_opcode(
    0xc6,
    "masterkey_off",
    undefined,
    [],
    undefined,
));
export const OP_WINDOW_TIME = (OPCODES[0xc7] = new_opcode(
    0xc7,
    "window_time",
    undefined,
    [],
    undefined,
));
export const OP_WINEND_TIME = (OPCODES[0xc8] = new_opcode(
    0xc8,
    "winend_time",
    undefined,
    [],
    undefined,
));
export const OP_WINSET_TIME = (OPCODES[0xc9] = new_opcode(
    0xc9,
    "winset_time",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_DWORD, undefined, ParamAccess.Write)],
            },
            undefined,
            undefined,
        ),
    ],
    undefined,
));
export const OP_GETMTIME = (OPCODES[0xca] = new_opcode(
    0xca,
    "getmtime",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_ANY, undefined, ParamAccess.Write)],
            },
            undefined,
            undefined,
        ),
    ],
    undefined,
));
export const OP_SET_QUEST_BOARD_HANDLER = (OPCODES[0xcb] = new_opcode(
    0xcb,
    "set_quest_board_handler",
    undefined,
    [
        new_param(TYPE_DWORD, undefined, undefined),
        new_param(TYPE_I_LABEL, undefined, undefined),
        new_param(TYPE_STRING, undefined, undefined),
    ],
    StackInteraction.Pop,
));
export const OP_CLEAR_QUEST_BOARD_HANDLER = (OPCODES[0xcc] = new_opcode(
    0xcc,
    "clear_quest_board_handler",
    undefined,
    [new_param(TYPE_DWORD, undefined, undefined)],
    StackInteraction.Pop,
));
export const OP_PARTICLE_ID_V3 = (OPCODES[0xcd] = new_opcode(
    0xcd,
    "particle_id_v3",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [
                    new_param(TYPE_DWORD, undefined, ParamAccess.Read),
                    new_param(TYPE_DWORD, undefined, ParamAccess.Read),
                    new_param(TYPE_DWORD, undefined, ParamAccess.Read),
                    new_param(TYPE_DWORD, undefined, ParamAccess.Read),
                ],
            },
            undefined,
            undefined,
        ),
    ],
    undefined,
));
export const OP_NPC_CRPTALK_ID_V3 = (OPCODES[0xce] = new_opcode(
    0xce,
    "npc_crptalk_id_v3",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_ANY, undefined, ParamAccess.Read)],
            },
            undefined,
            undefined,
        ),
    ],
    undefined,
));
export const OP_NPC_LANG_CLEAN = (OPCODES[0xcf] = new_opcode(
    0xcf,
    "npc_lang_clean",
    undefined,
    [],
    undefined,
));
export const OP_PL_PKON = (OPCODES[0xd0] = new_opcode(0xd0, "pl_pkon", undefined, [], undefined));
export const OP_PL_CHK_ITEM2 = (OPCODES[0xd1] = new_opcode(
    0xd1,
    "pl_chk_item2",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_ANY, undefined, ParamAccess.Read)],
            },
            undefined,
            undefined,
        ),
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_ANY, undefined, ParamAccess.Read)],
            },
            undefined,
            undefined,
        ),
    ],
    undefined,
));
export const OP_ENABLE_MAINMENU = (OPCODES[0xd2] = new_opcode(
    0xd2,
    "enable_mainmenu",
    undefined,
    [],
    undefined,
));
export const OP_DISABLE_MAINMENU = (OPCODES[0xd3] = new_opcode(
    0xd3,
    "disable_mainmenu",
    undefined,
    [],
    undefined,
));
export const OP_START_BATTLEBGM = (OPCODES[0xd4] = new_opcode(
    0xd4,
    "start_battlebgm",
    undefined,
    [],
    undefined,
));
export const OP_END_BATTLEBGM = (OPCODES[0xd5] = new_opcode(
    0xd5,
    "end_battlebgm",
    undefined,
    [],
    undefined,
));
export const OP_DISP_MSG_QB = (OPCODES[0xd6] = new_opcode(
    0xd6,
    "disp_msg_qb",
    undefined,
    [new_param(TYPE_STRING, undefined, undefined)],
    StackInteraction.Pop,
));
export const OP_CLOSE_MSG_QB = (OPCODES[0xd7] = new_opcode(
    0xd7,
    "close_msg_qb",
    undefined,
    [],
    undefined,
));
export const OP_SET_EVENTFLAG_V3 = (OPCODES[0xd8] = new_opcode(
    0xd8,
    "set_eventflag_v3",
    undefined,
    [new_param(TYPE_DWORD, undefined, undefined), new_param(TYPE_DWORD, undefined, undefined)],
    StackInteraction.Pop,
));
export const OP_SYNC_LETI = (OPCODES[0xd9] = new_opcode(
    0xd9,
    "sync_leti",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_DWORD, undefined, ParamAccess.Write)],
            },
            undefined,
            undefined,
        ),
        new_param(TYPE_DWORD, undefined, undefined),
    ],
    undefined,
));
export const OP_SET_RETURNHUNTER = (OPCODES[0xda] = new_opcode(
    0xda,
    "set_returnhunter",
    undefined,
    [],
    undefined,
));
export const OP_SET_RETURNCITY = (OPCODES[0xdb] = new_opcode(
    0xdb,
    "set_returncity",
    undefined,
    [],
    undefined,
));
export const OP_LOAD_PVR = (OPCODES[0xdc] = new_opcode(0xdc, "load_pvr", undefined, [], undefined));
export const OP_LOAD_MIDI = (OPCODES[0xdd] = new_opcode(
    0xdd,
    "load_midi",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_DE = (OPCODES[0xde] = new_opcode(
    0xde,
    "unknown_de",
    undefined,
    [],
    undefined,
));
export const OP_NPC_PARAM_V3 = (OPCODES[0xdf] = new_opcode(
    0xdf,
    "npc_param_v3",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_ANY, undefined, ParamAccess.Read)],
            },
            undefined,
            undefined,
        ),
        new_param(TYPE_DWORD, undefined, undefined),
    ],
    StackInteraction.Pop,
));
export const OP_PAD_DRAGON = (OPCODES[0xe0] = new_opcode(
    0xe0,
    "pad_dragon",
    undefined,
    [],
    undefined,
));
export const OP_CLEAR_MAINWARP = (OPCODES[0xe1] = new_opcode(
    0xe1,
    "clear_mainwarp",
    undefined,
    [new_param(TYPE_DWORD, undefined, undefined)],
    StackInteraction.Pop,
));
export const OP_PCAM_PARAM_V3 = (OPCODES[0xe2] = new_opcode(
    0xe2,
    "pcam_param_v3",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_ANY, undefined, ParamAccess.Read)],
            },
            undefined,
            undefined,
        ),
    ],
    undefined,
));
export const OP_START_SETEVT_V3 = (OPCODES[0xe3] = new_opcode(
    0xe3,
    "start_setevt_v3",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_ANY, undefined, ParamAccess.Read)],
            },
            undefined,
            undefined,
        ),
        new_param(TYPE_DWORD, undefined, undefined),
    ],
    StackInteraction.Pop,
));
export const OP_WARP_ON = (OPCODES[0xe4] = new_opcode(0xe4, "warp_on", undefined, [], undefined));
export const OP_WARP_OFF = (OPCODES[0xe5] = new_opcode(0xe5, "warp_off", undefined, [], undefined));
export const OP_GET_SLOTNUMBER = (OPCODES[0xe6] = new_opcode(
    0xe6,
    "get_slotnumber",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_DWORD, undefined, ParamAccess.Write)],
            },
            undefined,
            undefined,
        ),
    ],
    undefined,
));
export const OP_GET_SERVERNUMBER = (OPCODES[0xe7] = new_opcode(
    0xe7,
    "get_servernumber",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_DWORD, undefined, ParamAccess.Write)],
            },
            undefined,
            undefined,
        ),
    ],
    undefined,
));
export const OP_SET_EVENTFLAG2 = (OPCODES[0xe8] = new_opcode(
    0xe8,
    "set_eventflag2",
    undefined,
    [
        new_param(TYPE_DWORD, undefined, undefined),
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_ANY, undefined, ParamAccess.Read)],
            },
            undefined,
            undefined,
        ),
    ],
    StackInteraction.Pop,
));
export const OP_RES = (OPCODES[0xe9] = new_opcode(
    0xe9,
    "res",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_ANY, undefined, ParamAccess.Read)],
            },
            undefined,
            undefined,
        ),
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_ANY, undefined, ParamAccess.Read)],
            },
            undefined,
            undefined,
        ),
    ],
    undefined,
));
export const OP_UNKNOWN_EA = (OPCODES[0xea] = new_opcode(
    0xea,
    "unknown_ea",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_ANY, undefined, ParamAccess.Read)],
            },
            undefined,
            undefined,
        ),
        new_param(TYPE_DWORD, undefined, undefined),
    ],
    undefined,
));
export const OP_ENABLE_BGMCTRL = (OPCODES[0xeb] = new_opcode(
    0xeb,
    "enable_bgmctrl",
    undefined,
    [new_param(TYPE_DWORD, undefined, undefined)],
    StackInteraction.Pop,
));
export const OP_SW_SEND = (OPCODES[0xec] = new_opcode(
    0xec,
    "sw_send",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_ANY, undefined, ParamAccess.Read)],
            },
            undefined,
            undefined,
        ),
    ],
    undefined,
));
export const OP_CREATE_BGMCTRL = (OPCODES[0xed] = new_opcode(
    0xed,
    "create_bgmctrl",
    undefined,
    [],
    undefined,
));
export const OP_PL_ADD_MESETA2 = (OPCODES[0xee] = new_opcode(
    0xee,
    "pl_add_meseta2",
    undefined,
    [new_param(TYPE_DWORD, undefined, undefined)],
    StackInteraction.Pop,
));
export const OP_SYNC_REGISTER = (OPCODES[0xef] = new_opcode(
    0xef,
    "sync_register",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_DWORD, undefined, ParamAccess.Write)],
            },
            undefined,
            undefined,
        ),
        new_param(TYPE_DWORD, undefined, undefined),
    ],
    StackInteraction.Pop,
));
export const OP_SEND_REGWORK = (OPCODES[0xf0] = new_opcode(
    0xf0,
    "send_regwork",
    undefined,
    [],
    undefined,
));
export const OP_LETI_FIXED_CAMERA_V3 = (OPCODES[0xf1] = new_opcode(
    0xf1,
    "leti_fixed_camera_v3",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [
                    new_param(TYPE_DWORD, undefined, ParamAccess.Read),
                    new_param(TYPE_DWORD, undefined, ParamAccess.Read),
                    new_param(TYPE_DWORD, undefined, ParamAccess.Read),
                    new_param(TYPE_DWORD, undefined, ParamAccess.Read),
                    new_param(TYPE_DWORD, undefined, ParamAccess.Read),
                    new_param(TYPE_DWORD, undefined, ParamAccess.Read),
                ],
            },
            undefined,
            undefined,
        ),
    ],
    undefined,
));
export const OP_DEFAULT_CAMERA_POS1 = (OPCODES[0xf2] = new_opcode(
    0xf2,
    "default_camera_pos1",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F3 = (OPCODES[0xf3] = new_opcode(
    0xf3,
    "unknown_f3",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F4 = (OPCODES[0xf4] = new_opcode(
    0xf4,
    "unknown_f4",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F5 = (OPCODES[0xf5] = new_opcode(
    0xf5,
    "unknown_f5",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F6 = (OPCODES[0xf6] = new_opcode(
    0xf6,
    "unknown_f6",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F7 = (OPCODES[0xf7] = new_opcode(
    0xf7,
    "unknown_f7",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F8 = (OPCODES[0xf8] = new_opcode(
    0xf8,
    "unknown_f8",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_ANY, undefined, ParamAccess.Read)],
            },
            undefined,
            undefined,
        ),
    ],
    undefined,
));
export const OP_UNKNOWN_F9 = (OPCODES[0xf9] = new_opcode(
    0xf9,
    "unknown_f9",
    undefined,
    [],
    undefined,
));
export const OP_GET_GC_NUMBER = (OPCODES[0xfa] = new_opcode(
    0xfa,
    "get_gc_number",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_ANY, undefined, ParamAccess.Write)],
            },
            undefined,
            undefined,
        ),
    ],
    undefined,
));
export const OP_UNKNOWN_FB = (OPCODES[0xfb] = new_opcode(
    0xfb,
    "unknown_fb",
    undefined,
    [new_param(TYPE_WORD, undefined, undefined)],
    undefined,
));
export const OP_UNKNOWN_FC = (OPCODES[0xfc] = new_opcode(
    0xfc,
    "unknown_fc",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_FD = (OPCODES[0xfd] = new_opcode(
    0xfd,
    "unknown_fd",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_FE = (OPCODES[0xfe] = new_opcode(
    0xfe,
    "unknown_fe",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_FF = (OPCODES[0xff] = new_opcode(
    0xff,
    "unknown_ff",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F800 = (OPCODES[0xf800] = new_opcode(
    0xf800,
    "unknown_f800",
    undefined,
    [],
    undefined,
));
export const OP_SET_CHAT_CALLBACK = (OPCODES[0xf801] = new_opcode(
    0xf801,
    "set_chat_callback",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_ANY, undefined, ParamAccess.Read)],
            },
            undefined,
            undefined,
        ),
        new_param(TYPE_STRING, undefined, undefined),
    ],
    StackInteraction.Pop,
));
export const OP_UNKNOWN_F802 = (OPCODES[0xf802] = new_opcode(
    0xf802,
    "unknown_f802",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F803 = (OPCODES[0xf803] = new_opcode(
    0xf803,
    "unknown_f803",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F804 = (OPCODES[0xf804] = new_opcode(
    0xf804,
    "unknown_f804",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F805 = (OPCODES[0xf805] = new_opcode(
    0xf805,
    "unknown_f805",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F806 = (OPCODES[0xf806] = new_opcode(
    0xf806,
    "unknown_f806",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F807 = (OPCODES[0xf807] = new_opcode(
    0xf807,
    "unknown_f807",
    undefined,
    [],
    undefined,
));
export const OP_GET_DIFFICULTY_LEVEL2 = (OPCODES[0xf808] = new_opcode(
    0xf808,
    "get_difficulty_level2",
    "Sets the given register to the current difficulty. 0 For normal, 1 for hard, 2 for very hard and 3 for ultimate.",
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_DWORD, undefined, ParamAccess.Write)],
            },
            undefined,
            undefined,
        ),
    ],
    undefined,
));
export const OP_GET_NUMBER_OF_PLAYER1 = (OPCODES[0xf809] = new_opcode(
    0xf809,
    "get_number_of_player1",
    "Set the given register to the current number of players. Either 1, 2, 3 or 4.",
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_DWORD, undefined, ParamAccess.Write)],
            },
            undefined,
            undefined,
        ),
    ],
    undefined,
));
export const OP_GET_COORD_OF_PLAYER = (OPCODES[0xf80a] = new_opcode(
    0xf80a,
    "get_coord_of_player",
    "Retrieves a player's position.",
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [
                    new_param(TYPE_DWORD, "X coordinate.", ParamAccess.Write),
                    new_param(TYPE_DWORD, "Y coordinate.", ParamAccess.Write),
                    new_param(TYPE_DWORD, "Z coordinate.", ParamAccess.Write),
                ],
            },
            undefined,
            undefined,
        ),
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_DWORD, "Player slot.", ParamAccess.Read)],
            },
            undefined,
            undefined,
        ),
    ],
    undefined,
));
export const OP_ENABLE_MAP = (OPCODES[0xf80b] = new_opcode(
    0xf80b,
    "enable_map",
    undefined,
    [],
    undefined,
));
export const OP_DISABLE_MAP = (OPCODES[0xf80c] = new_opcode(
    0xf80c,
    "disable_map",
    undefined,
    [],
    undefined,
));
export const OP_MAP_DESIGNATE_EX = (OPCODES[0xf80d] = new_opcode(
    0xf80d,
    "map_designate_ex",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [
                    new_param(TYPE_DWORD, undefined, ParamAccess.Read),
                    new_param(TYPE_DWORD, undefined, ParamAccess.Read),
                    new_param(TYPE_DWORD, undefined, ParamAccess.Read),
                    new_param(TYPE_DWORD, undefined, ParamAccess.Read),
                    new_param(TYPE_DWORD, undefined, ParamAccess.Read),
                ],
            },
            undefined,
            undefined,
        ),
    ],
    undefined,
));
export const OP_UNKNOWN_F80E = (OPCODES[0xf80e] = new_opcode(
    0xf80e,
    "unknown_f80e",
    undefined,
    [new_param(TYPE_DWORD, undefined, undefined)],
    StackInteraction.Pop,
));
export const OP_UNKNOWN_F80F = (OPCODES[0xf80f] = new_opcode(
    0xf80f,
    "unknown_f80f",
    undefined,
    [new_param(TYPE_DWORD, undefined, undefined)],
    StackInteraction.Pop,
));
export const OP_BA_INITIAL_FLOOR = (OPCODES[0xf810] = new_opcode(
    0xf810,
    "ba_initial_floor",
    undefined,
    [new_param(TYPE_DWORD, undefined, undefined)],
    StackInteraction.Pop,
));
export const OP_SET_BA_RULES = (OPCODES[0xf811] = new_opcode(
    0xf811,
    "set_ba_rules",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F812 = (OPCODES[0xf812] = new_opcode(
    0xf812,
    "unknown_f812",
    undefined,
    [new_param(TYPE_DWORD, undefined, undefined)],
    StackInteraction.Pop,
));
export const OP_UNKNOWN_F813 = (OPCODES[0xf813] = new_opcode(
    0xf813,
    "unknown_f813",
    undefined,
    [new_param(TYPE_DWORD, undefined, undefined)],
    StackInteraction.Pop,
));
export const OP_UNKNOWN_F814 = (OPCODES[0xf814] = new_opcode(
    0xf814,
    "unknown_f814",
    undefined,
    [new_param(TYPE_DWORD, undefined, undefined)],
    StackInteraction.Pop,
));
export const OP_UNKNOWN_F815 = (OPCODES[0xf815] = new_opcode(
    0xf815,
    "unknown_f815",
    undefined,
    [new_param(TYPE_DWORD, undefined, undefined)],
    StackInteraction.Pop,
));
export const OP_UNKNOWN_F816 = (OPCODES[0xf816] = new_opcode(
    0xf816,
    "unknown_f816",
    undefined,
    [new_param(TYPE_DWORD, undefined, undefined)],
    StackInteraction.Pop,
));
export const OP_UNKNOWN_F817 = (OPCODES[0xf817] = new_opcode(
    0xf817,
    "unknown_f817",
    undefined,
    [new_param(TYPE_DWORD, undefined, undefined)],
    StackInteraction.Pop,
));
export const OP_UNKNOWN_F818 = (OPCODES[0xf818] = new_opcode(
    0xf818,
    "unknown_f818",
    undefined,
    [new_param(TYPE_DWORD, undefined, undefined)],
    StackInteraction.Pop,
));
export const OP_UNKNOWN_F819 = (OPCODES[0xf819] = new_opcode(
    0xf819,
    "unknown_f819",
    undefined,
    [new_param(TYPE_DWORD, undefined, undefined)],
    StackInteraction.Pop,
));
export const OP_UNKNOWN_F81A = (OPCODES[0xf81a] = new_opcode(
    0xf81a,
    "unknown_f81a",
    undefined,
    [new_param(TYPE_DWORD, undefined, undefined)],
    StackInteraction.Pop,
));
export const OP_UNKNOWN_F81B = (OPCODES[0xf81b] = new_opcode(
    0xf81b,
    "unknown_f81b",
    undefined,
    [new_param(TYPE_DWORD, undefined, undefined)],
    StackInteraction.Pop,
));
export const OP_BA_DISP_MSG = (OPCODES[0xf81c] = new_opcode(
    0xf81c,
    "ba_disp_msg",
    undefined,
    [new_param(TYPE_STRING, undefined, undefined)],
    StackInteraction.Pop,
));
export const OP_DEATH_LVL_UP = (OPCODES[0xf81d] = new_opcode(
    0xf81d,
    "death_lvl_up",
    undefined,
    [new_param(TYPE_DWORD, undefined, undefined)],
    StackInteraction.Pop,
));
export const OP_DEATH_TECH_LVL_UP = (OPCODES[0xf81e] = new_opcode(
    0xf81e,
    "death_tech_lvl_up",
    undefined,
    [new_param(TYPE_DWORD, undefined, undefined)],
    StackInteraction.Pop,
));
export const OP_UNKNOWN_F81F = (OPCODES[0xf81f] = new_opcode(
    0xf81f,
    "unknown_f81f",
    undefined,
    [],
    undefined,
));
export const OP_CMODE_STAGE = (OPCODES[0xf820] = new_opcode(
    0xf820,
    "cmode_stage",
    undefined,
    [new_param(TYPE_DWORD, undefined, undefined)],
    StackInteraction.Pop,
));
export const OP_UNKNOWN_F821 = (OPCODES[0xf821] = new_opcode(
    0xf821,
    "unknown_f821",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F822 = (OPCODES[0xf822] = new_opcode(
    0xf822,
    "unknown_f822",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F823 = (OPCODES[0xf823] = new_opcode(
    0xf823,
    "unknown_f823",
    undefined,
    [new_param(TYPE_DWORD, undefined, undefined)],
    StackInteraction.Pop,
));
export const OP_UNKNOWN_F824 = (OPCODES[0xf824] = new_opcode(
    0xf824,
    "unknown_f824",
    undefined,
    [new_param(TYPE_DWORD, undefined, undefined)],
    StackInteraction.Pop,
));
export const OP_EXP_MULTIPLICATION = (OPCODES[0xf825] = new_opcode(
    0xf825,
    "exp_multiplication",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_ANY, undefined, ParamAccess.Write)],
            },
            undefined,
            undefined,
        ),
    ],
    undefined,
));
export const OP_EXP_DIVISION = (OPCODES[0xf826] = new_opcode(
    0xf826,
    "exp_division",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_ANY, undefined, ParamAccess.Write)],
            },
            undefined,
            undefined,
        ),
    ],
    undefined,
));
export const OP_GET_USER_IS_DEAD = (OPCODES[0xf827] = new_opcode(
    0xf827,
    "get_user_is_dead",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_ANY, undefined, ParamAccess.Write)],
            },
            undefined,
            undefined,
        ),
    ],
    undefined,
));
export const OP_GO_FLOOR = (OPCODES[0xf828] = new_opcode(
    0xf828,
    "go_floor",
    "Sends a player to the given floor.",
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_DWORD, "Player slot.", ParamAccess.Read)],
            },
            undefined,
            undefined,
        ),
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_DWORD, "Floor ID.", ParamAccess.Read)],
            },
            undefined,
            undefined,
        ),
    ],
    undefined,
));
export const OP_UNKNOWN_F829 = (OPCODES[0xf829] = new_opcode(
    0xf829,
    "unknown_f829",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F82A = (OPCODES[0xf82a] = new_opcode(
    0xf82a,
    "unknown_f82a",
    undefined,
    [],
    undefined,
));
export const OP_UNLOCK_DOOR2 = (OPCODES[0xf82b] = new_opcode(
    0xf82b,
    "unlock_door2",
    undefined,
    [new_param(TYPE_DWORD, undefined, undefined), new_param(TYPE_DWORD, undefined, undefined)],
    StackInteraction.Pop,
));
export const OP_LOCK_DOOR2 = (OPCODES[0xf82c] = new_opcode(
    0xf82c,
    "lock_door2",
    undefined,
    [new_param(TYPE_DWORD, undefined, undefined), new_param(TYPE_DWORD, undefined, undefined)],
    StackInteraction.Pop,
));
export const OP_IF_SWITCH_NOT_PRESSED = (OPCODES[0xf82d] = new_opcode(
    0xf82d,
    "if_switch_not_pressed",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [
                    new_param(TYPE_DWORD, undefined, ParamAccess.Read),
                    new_param(TYPE_DWORD, undefined, ParamAccess.Write),
                ],
            },
            undefined,
            undefined,
        ),
    ],
    undefined,
));
export const OP_IF_SWITCH_PRESSED = (OPCODES[0xf82e] = new_opcode(
    0xf82e,
    "if_switch_pressed",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [
                    new_param(TYPE_DWORD, "Floor ID.", ParamAccess.Read),
                    new_param(TYPE_DWORD, "Switch ID.", ParamAccess.Read),
                    new_param(
                        TYPE_DWORD,
                        "Will be set to 1 if the switch is pressed, 0 otherwise.",
                        ParamAccess.Write,
                    ),
                ],
            },
            undefined,
            undefined,
        ),
    ],
    undefined,
));
export const OP_UNKNOWN_F82F = (OPCODES[0xf82f] = new_opcode(
    0xf82f,
    "unknown_f82f",
    undefined,
    [new_param(TYPE_DWORD, undefined, undefined), new_param(TYPE_DWORD, undefined, undefined)],
    StackInteraction.Pop,
));
export const OP_CONTROL_DRAGON = (OPCODES[0xf830] = new_opcode(
    0xf830,
    "control_dragon",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_DWORD, undefined, ParamAccess.Read)],
            },
            undefined,
            undefined,
        ),
    ],
    undefined,
));
export const OP_RELEASE_DRAGON = (OPCODES[0xf831] = new_opcode(
    0xf831,
    "release_dragon",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F832 = (OPCODES[0xf832] = new_opcode(
    0xf832,
    "unknown_f832",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F833 = (OPCODES[0xf833] = new_opcode(
    0xf833,
    "unknown_f833",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F834 = (OPCODES[0xf834] = new_opcode(
    0xf834,
    "unknown_f834",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F835 = (OPCODES[0xf835] = new_opcode(
    0xf835,
    "unknown_f835",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F836 = (OPCODES[0xf836] = new_opcode(
    0xf836,
    "unknown_f836",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F837 = (OPCODES[0xf837] = new_opcode(
    0xf837,
    "unknown_f837",
    undefined,
    [],
    undefined,
));
export const OP_SHRINK = (OPCODES[0xf838] = new_opcode(
    0xf838,
    "shrink",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_DWORD, "Player slot.", ParamAccess.Read)],
            },
            undefined,
            undefined,
        ),
    ],
    undefined,
));
export const OP_UNSHRINK = (OPCODES[0xf839] = new_opcode(
    0xf839,
    "unshrink",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_DWORD, "Player slot.", ParamAccess.Read)],
            },
            undefined,
            undefined,
        ),
    ],
    undefined,
));
export const OP_UNKNOWN_F83A = (OPCODES[0xf83a] = new_opcode(
    0xf83a,
    "unknown_f83a",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F83B = (OPCODES[0xf83b] = new_opcode(
    0xf83b,
    "unknown_f83b",
    undefined,
    [],
    undefined,
));
export const OP_DISPLAY_CLOCK2 = (OPCODES[0xf83c] = new_opcode(
    0xf83c,
    "display_clock2",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_DWORD, undefined, ParamAccess.Read)],
            },
            undefined,
            undefined,
        ),
    ],
    undefined,
));
export const OP_UNKNOWN_F83D = (OPCODES[0xf83d] = new_opcode(
    0xf83d,
    "unknown_f83d",
    undefined,
    [new_param(TYPE_DWORD, undefined, undefined)],
    StackInteraction.Pop,
));
export const OP_DELETE_AREA_TITLE = (OPCODES[0xf83e] = new_opcode(
    0xf83e,
    "delete_area_title",
    undefined,
    [new_param(TYPE_DWORD, undefined, undefined)],
    StackInteraction.Pop,
));
export const OP_UNKNOWN_F83F = (OPCODES[0xf83f] = new_opcode(
    0xf83f,
    "unknown_f83f",
    undefined,
    [],
    undefined,
));
export const OP_LOAD_NPC_DATA = (OPCODES[0xf840] = new_opcode(
    0xf840,
    "load_npc_data",
    undefined,
    [],
    undefined,
));
export const OP_GET_NPC_DATA = (OPCODES[0xf841] = new_opcode(
    0xf841,
    "get_npc_data",
    undefined,
    [new_param(TYPE_D_LABEL, undefined, undefined)],
    undefined,
));
export const OP_UNKNOWN_F842 = (OPCODES[0xf842] = new_opcode(
    0xf842,
    "unknown_f842",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F843 = (OPCODES[0xf843] = new_opcode(
    0xf843,
    "unknown_f843",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F844 = (OPCODES[0xf844] = new_opcode(
    0xf844,
    "unknown_f844",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F845 = (OPCODES[0xf845] = new_opcode(
    0xf845,
    "unknown_f845",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F846 = (OPCODES[0xf846] = new_opcode(
    0xf846,
    "unknown_f846",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F847 = (OPCODES[0xf847] = new_opcode(
    0xf847,
    "unknown_f847",
    undefined,
    [],
    undefined,
));
export const OP_GIVE_DAMAGE_SCORE = (OPCODES[0xf848] = new_opcode(
    0xf848,
    "give_damage_score",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_DWORD, undefined, ParamAccess.Write)],
            },
            undefined,
            undefined,
        ),
    ],
    undefined,
));
export const OP_TAKE_DAMAGE_SCORE = (OPCODES[0xf849] = new_opcode(
    0xf849,
    "take_damage_score",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_DWORD, undefined, ParamAccess.Write)],
            },
            undefined,
            undefined,
        ),
    ],
    undefined,
));
export const OP_UNK_SCORE_F84A = (OPCODES[0xf84a] = new_opcode(
    0xf84a,
    "unk_score_f84a",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_DWORD, undefined, ParamAccess.Write)],
            },
            undefined,
            undefined,
        ),
    ],
    undefined,
));
export const OP_UNK_SCORE_F84B = (OPCODES[0xf84b] = new_opcode(
    0xf84b,
    "unk_score_f84b",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_DWORD, undefined, ParamAccess.Write)],
            },
            undefined,
            undefined,
        ),
    ],
    undefined,
));
export const OP_KILL_SCORE = (OPCODES[0xf84c] = new_opcode(
    0xf84c,
    "kill_score",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_DWORD, undefined, ParamAccess.Write)],
            },
            undefined,
            undefined,
        ),
    ],
    undefined,
));
export const OP_DEATH_SCORE = (OPCODES[0xf84d] = new_opcode(
    0xf84d,
    "death_score",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_DWORD, undefined, ParamAccess.Write)],
            },
            undefined,
            undefined,
        ),
    ],
    undefined,
));
export const OP_UNK_SCORE_F84E = (OPCODES[0xf84e] = new_opcode(
    0xf84e,
    "unk_score_f84e",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_DWORD, undefined, ParamAccess.Write)],
            },
            undefined,
            undefined,
        ),
    ],
    undefined,
));
export const OP_ENEMY_DEATH_SCORE = (OPCODES[0xf84f] = new_opcode(
    0xf84f,
    "enemy_death_score",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_DWORD, undefined, ParamAccess.Write)],
            },
            undefined,
            undefined,
        ),
    ],
    undefined,
));
export const OP_MESETA_SCORE = (OPCODES[0xf850] = new_opcode(
    0xf850,
    "meseta_score",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_DWORD, undefined, ParamAccess.Write)],
            },
            undefined,
            undefined,
        ),
    ],
    undefined,
));
export const OP_UNKNOWN_F851 = (OPCODES[0xf851] = new_opcode(
    0xf851,
    "unknown_f851",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_DWORD, undefined, ParamAccess.Write)],
            },
            undefined,
            undefined,
        ),
    ],
    undefined,
));
export const OP_UNKNOWN_F852 = (OPCODES[0xf852] = new_opcode(
    0xf852,
    "unknown_f852",
    undefined,
    [new_param(TYPE_DWORD, undefined, undefined)],
    StackInteraction.Pop,
));
export const OP_REVERSE_WARPS = (OPCODES[0xf853] = new_opcode(
    0xf853,
    "reverse_warps",
    undefined,
    [],
    undefined,
));
export const OP_UNREVERSE_WARPS = (OPCODES[0xf854] = new_opcode(
    0xf854,
    "unreverse_warps",
    undefined,
    [],
    undefined,
));
export const OP_SET_ULT_MAP = (OPCODES[0xf855] = new_opcode(
    0xf855,
    "set_ult_map",
    undefined,
    [],
    undefined,
));
export const OP_UNSET_ULT_MAP = (OPCODES[0xf856] = new_opcode(
    0xf856,
    "unset_ult_map",
    undefined,
    [],
    undefined,
));
export const OP_SET_AREA_TITLE = (OPCODES[0xf857] = new_opcode(
    0xf857,
    "set_area_title",
    undefined,
    [new_param(TYPE_STRING, undefined, undefined)],
    StackInteraction.Pop,
));
export const OP_UNKNOWN_F858 = (OPCODES[0xf858] = new_opcode(
    0xf858,
    "unknown_f858",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F859 = (OPCODES[0xf859] = new_opcode(
    0xf859,
    "unknown_f859",
    undefined,
    [],
    undefined,
));
export const OP_EQUIP_ITEM = (OPCODES[0xf85a] = new_opcode(
    0xf85a,
    "equip_item",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_DWORD, undefined, ParamAccess.Read)],
            },
            undefined,
            undefined,
        ),
    ],
    undefined,
));
export const OP_UNEQUIP_ITEM = (OPCODES[0xf85b] = new_opcode(
    0xf85b,
    "unequip_item",
    undefined,
    [new_param(TYPE_DWORD, undefined, undefined), new_param(TYPE_DWORD, undefined, undefined)],
    StackInteraction.Pop,
));
export const OP_UNKNOWN_F85C = (OPCODES[0xf85c] = new_opcode(
    0xf85c,
    "unknown_f85c",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F85D = (OPCODES[0xf85d] = new_opcode(
    0xf85d,
    "unknown_f85d",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F85E = (OPCODES[0xf85e] = new_opcode(
    0xf85e,
    "unknown_f85e",
    undefined,
    [new_param(TYPE_DWORD, undefined, undefined)],
    StackInteraction.Pop,
));
export const OP_UNKNOWN_F85F = (OPCODES[0xf85f] = new_opcode(
    0xf85f,
    "unknown_f85f",
    undefined,
    [new_param(TYPE_DWORD, undefined, undefined)],
    StackInteraction.Pop,
));
export const OP_UNKNOWN_F860 = (OPCODES[0xf860] = new_opcode(
    0xf860,
    "unknown_f860",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F861 = (OPCODES[0xf861] = new_opcode(
    0xf861,
    "unknown_f861",
    undefined,
    [new_param(TYPE_DWORD, undefined, undefined)],
    StackInteraction.Pop,
));
export const OP_UNKNOWN_F862 = (OPCODES[0xf862] = new_opcode(
    0xf862,
    "unknown_f862",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F863 = (OPCODES[0xf863] = new_opcode(
    0xf863,
    "unknown_f863",
    undefined,
    [],
    undefined,
));
export const OP_CMODE_RANK = (OPCODES[0xf864] = new_opcode(
    0xf864,
    "cmode_rank",
    undefined,
    [new_param(TYPE_DWORD, undefined, undefined), new_param(TYPE_STRING, undefined, undefined)],
    StackInteraction.Pop,
));
export const OP_AWARD_ITEM_NAME = (OPCODES[0xf865] = new_opcode(
    0xf865,
    "award_item_name",
    undefined,
    [],
    undefined,
));
export const OP_AWARD_ITEM_SELECT = (OPCODES[0xf866] = new_opcode(
    0xf866,
    "award_item_select",
    undefined,
    [],
    undefined,
));
export const OP_AWARD_ITEM_GIVE_TO = (OPCODES[0xf867] = new_opcode(
    0xf867,
    "award_item_give_to",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_DWORD, undefined, ParamAccess.Read)],
            },
            undefined,
            undefined,
        ),
    ],
    undefined,
));
export const OP_UNKNOWN_F868 = (OPCODES[0xf868] = new_opcode(
    0xf868,
    "unknown_f868",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_ANY, undefined, ParamAccess.Write)],
            },
            undefined,
            undefined,
        ),
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_ANY, undefined, ParamAccess.Write)],
            },
            undefined,
            undefined,
        ),
    ],
    undefined,
));
export const OP_UNKNOWN_F869 = (OPCODES[0xf869] = new_opcode(
    0xf869,
    "unknown_f869",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_ANY, undefined, ParamAccess.Write)],
            },
            undefined,
            undefined,
        ),
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_ANY, undefined, ParamAccess.Write)],
            },
            undefined,
            undefined,
        ),
    ],
    undefined,
));
export const OP_ITEM_CREATE_CMODE = (OPCODES[0xf86a] = new_opcode(
    0xf86a,
    "item_create_cmode",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_ANY, undefined, ParamAccess.Write)],
            },
            undefined,
            undefined,
        ),
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_ANY, undefined, ParamAccess.Write)],
            },
            undefined,
            undefined,
        ),
    ],
    undefined,
));
export const OP_UNKNOWN_F86B = (OPCODES[0xf86b] = new_opcode(
    0xf86b,
    "unknown_f86b",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_ANY, undefined, ParamAccess.Read)],
            },
            undefined,
            undefined,
        ),
    ],
    undefined,
));
export const OP_AWARD_ITEM_OK = (OPCODES[0xf86c] = new_opcode(
    0xf86c,
    "award_item_ok",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_ANY, undefined, ParamAccess.Write)],
            },
            undefined,
            undefined,
        ),
    ],
    undefined,
));
export const OP_UNKNOWN_F86D = (OPCODES[0xf86d] = new_opcode(
    0xf86d,
    "unknown_f86d",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F86E = (OPCODES[0xf86e] = new_opcode(
    0xf86e,
    "unknown_f86e",
    undefined,
    [],
    undefined,
));
export const OP_BA_SET_LIVES = (OPCODES[0xf86f] = new_opcode(
    0xf86f,
    "ba_set_lives",
    undefined,
    [new_param(TYPE_DWORD, undefined, undefined)],
    StackInteraction.Pop,
));
export const OP_BA_SET_TECH_LVL = (OPCODES[0xf870] = new_opcode(
    0xf870,
    "ba_set_tech_lvl",
    undefined,
    [new_param(TYPE_DWORD, undefined, undefined)],
    StackInteraction.Pop,
));
export const OP_BA_SET_LVL = (OPCODES[0xf871] = new_opcode(
    0xf871,
    "ba_set_lvl",
    undefined,
    [new_param(TYPE_DWORD, undefined, undefined)],
    StackInteraction.Pop,
));
export const OP_BA_SET_TIME_LIMIT = (OPCODES[0xf872] = new_opcode(
    0xf872,
    "ba_set_time_limit",
    undefined,
    [new_param(TYPE_DWORD, undefined, undefined)],
    StackInteraction.Pop,
));
export const OP_BOSS_IS_DEAD = (OPCODES[0xf873] = new_opcode(
    0xf873,
    "boss_is_dead",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_ANY, undefined, ParamAccess.Write)],
            },
            undefined,
            undefined,
        ),
    ],
    undefined,
));
export const OP_UNKNOWN_F874 = (OPCODES[0xf874] = new_opcode(
    0xf874,
    "unknown_f874",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F875 = (OPCODES[0xf875] = new_opcode(
    0xf875,
    "unknown_f875",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F876 = (OPCODES[0xf876] = new_opcode(
    0xf876,
    "unknown_f876",
    undefined,
    [],
    undefined,
));
export const OP_ENABLE_TECHS = (OPCODES[0xf877] = new_opcode(
    0xf877,
    "enable_techs",
    "Enables technique use for the given player.",
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_DWORD, "Player slot.", ParamAccess.Read)],
            },
            undefined,
            undefined,
        ),
    ],
    undefined,
));
export const OP_DISABLE_TECHS = (OPCODES[0xf878] = new_opcode(
    0xf878,
    "disable_techs",
    "Disables technique use for the given player.",
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_DWORD, "Player slot.", ParamAccess.Read)],
            },
            undefined,
            undefined,
        ),
    ],
    undefined,
));
export const OP_GET_GENDER = (OPCODES[0xf879] = new_opcode(
    0xf879,
    "get_gender",
    "Retrieves the player's gender. 0 If male, 1 if female.",
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_DWORD, "Player slot.", ParamAccess.Read)],
            },
            undefined,
            undefined,
        ),
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_DWORD, "Player gender.", ParamAccess.Write)],
            },
            undefined,
            undefined,
        ),
    ],
    undefined,
));
export const OP_GET_CHARA_CLASS = (OPCODES[0xf87a] = new_opcode(
    0xf87a,
    "get_chara_class",
    "Retrieves the player's race and character class.",
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_DWORD, "Player slot.", ParamAccess.Read)],
            },
            undefined,
            undefined,
        ),
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [
                    new_param(
                        TYPE_DWORD,
                        "Player race. 0 If human, 1 if newman, 2 if cast.",
                        ParamAccess.Write,
                    ),
                    new_param(
                        TYPE_DWORD,
                        "Player class. 0 If hunter, 1 if ranger, 2 if force.",
                        ParamAccess.Write,
                    ),
                ],
            },
            undefined,
            undefined,
        ),
    ],
    undefined,
));
export const OP_TAKE_SLOT_MESETA = (OPCODES[0xf87b] = new_opcode(
    0xf87b,
    "take_slot_meseta",
    "Takes an amount of meseta from a player's inventory.",
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [
                    new_param(TYPE_DWORD, "Player slot.", ParamAccess.Read),
                    new_param(TYPE_DWORD, "Amount of meseta to take.", ParamAccess.Read),
                ],
            },
            undefined,
            undefined,
        ),
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [
                    new_param(
                        TYPE_DWORD,
                        "Will be set to 1 if the meseta was taken, 0 otherwise.",
                        ParamAccess.Write,
                    ),
                ],
            },
            undefined,
            undefined,
        ),
    ],
    undefined,
));
export const OP_UNKNOWN_F87C = (OPCODES[0xf87c] = new_opcode(
    0xf87c,
    "unknown_f87c",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F87D = (OPCODES[0xf87d] = new_opcode(
    0xf87d,
    "unknown_f87d",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F87E = (OPCODES[0xf87e] = new_opcode(
    0xf87e,
    "unknown_f87e",
    undefined,
    [],
    undefined,
));
export const OP_READ_GUILDCARD_FLAG = (OPCODES[0xf87f] = new_opcode(
    0xf87f,
    "read_guildcard_flag",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_ANY, undefined, ParamAccess.Write)],
            },
            undefined,
            undefined,
        ),
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_ANY, undefined, ParamAccess.Write)],
            },
            undefined,
            undefined,
        ),
    ],
    undefined,
));
export const OP_UNKNOWN_F880 = (OPCODES[0xf880] = new_opcode(
    0xf880,
    "unknown_f880",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_ANY, undefined, ParamAccess.Write)],
            },
            undefined,
            undefined,
        ),
    ],
    undefined,
));
export const OP_GET_PL_NAME = (OPCODES[0xf881] = new_opcode(
    0xf881,
    "get_pl_name",
    "Sets the value of <pl_name> to the given player's name.",
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_DWORD, "Player slot.", ParamAccess.Read)],
            },
            undefined,
            undefined,
        ),
    ],
    undefined,
));
export const OP_UNKNOWN_F882 = (OPCODES[0xf882] = new_opcode(
    0xf882,
    "unknown_f882",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F883 = (OPCODES[0xf883] = new_opcode(
    0xf883,
    "unknown_f883",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_ANY, undefined, ParamAccess.Write)],
            },
            undefined,
            undefined,
        ),
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_ANY, undefined, ParamAccess.Write)],
            },
            undefined,
            undefined,
        ),
    ],
    undefined,
));
export const OP_UNKNOWN_F884 = (OPCODES[0xf884] = new_opcode(
    0xf884,
    "unknown_f884",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F885 = (OPCODES[0xf885] = new_opcode(
    0xf885,
    "unknown_f885",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F886 = (OPCODES[0xf886] = new_opcode(
    0xf886,
    "unknown_f886",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F887 = (OPCODES[0xf887] = new_opcode(
    0xf887,
    "unknown_f887",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F888 = (OPCODES[0xf888] = new_opcode(
    0xf888,
    "unknown_f888",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F889 = (OPCODES[0xf889] = new_opcode(
    0xf889,
    "unknown_f889",
    undefined,
    [],
    undefined,
));
export const OP_GET_PLAYER_STATUS = (OPCODES[0xf88a] = new_opcode(
    0xf88a,
    "get_player_status",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_ANY, undefined, ParamAccess.Write)],
            },
            undefined,
            undefined,
        ),
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_ANY, undefined, ParamAccess.Write)],
            },
            undefined,
            undefined,
        ),
    ],
    undefined,
));
export const OP_SEND_MAIL = (OPCODES[0xf88b] = new_opcode(
    0xf88b,
    "send_mail",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_ANY, undefined, ParamAccess.Read)],
            },
            undefined,
            undefined,
        ),
        new_param(TYPE_STRING, undefined, undefined),
    ],
    StackInteraction.Pop,
));
export const OP_ONLINE_CHECK = (OPCODES[0xf88c] = new_opcode(
    0xf88c,
    "online_check",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_ANY, undefined, ParamAccess.Write)],
            },
            undefined,
            undefined,
        ),
    ],
    undefined,
));
export const OP_CHL_SET_TIMERECORD = (OPCODES[0xf88d] = new_opcode(
    0xf88d,
    "chl_set_timerecord",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_ANY, undefined, ParamAccess.Read)],
            },
            undefined,
            undefined,
        ),
    ],
    undefined,
));
export const OP_CHL_GET_TIMERECORD = (OPCODES[0xf88e] = new_opcode(
    0xf88e,
    "chl_get_timerecord",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_ANY, undefined, ParamAccess.Write)],
            },
            undefined,
            undefined,
        ),
    ],
    undefined,
));
export const OP_UNKNOWN_F88F = (OPCODES[0xf88f] = new_opcode(
    0xf88f,
    "unknown_f88f",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_ANY, undefined, ParamAccess.Write)],
            },
            undefined,
            undefined,
        ),
    ],
    undefined,
));
export const OP_UNKNOWN_F890 = (OPCODES[0xf890] = new_opcode(
    0xf890,
    "unknown_f890",
    undefined,
    [],
    undefined,
));
export const OP_LOAD_ENEMY_DATA = (OPCODES[0xf891] = new_opcode(
    0xf891,
    "load_enemy_data",
    undefined,
    [new_param(TYPE_DWORD, undefined, undefined)],
    StackInteraction.Pop,
));
export const OP_GET_PHYSICAL_DATA = (OPCODES[0xf892] = new_opcode(
    0xf892,
    "get_physical_data",
    undefined,
    [new_param(TYPE_WORD, undefined, undefined)],
    undefined,
));
export const OP_GET_ATTACK_DATA = (OPCODES[0xf893] = new_opcode(
    0xf893,
    "get_attack_data",
    undefined,
    [new_param(TYPE_WORD, undefined, undefined)],
    undefined,
));
export const OP_GET_RESIST_DATA = (OPCODES[0xf894] = new_opcode(
    0xf894,
    "get_resist_data",
    undefined,
    [new_param(TYPE_WORD, undefined, undefined)],
    undefined,
));
export const OP_GET_MOVEMENT_DATA = (OPCODES[0xf895] = new_opcode(
    0xf895,
    "get_movement_data",
    undefined,
    [new_param(TYPE_WORD, undefined, undefined)],
    undefined,
));
export const OP_UNKNOWN_F896 = (OPCODES[0xf896] = new_opcode(
    0xf896,
    "unknown_f896",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F897 = (OPCODES[0xf897] = new_opcode(
    0xf897,
    "unknown_f897",
    undefined,
    [],
    undefined,
));
export const OP_SHIFT_LEFT = (OPCODES[0xf898] = new_opcode(
    0xf898,
    "shift_left",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_DWORD, undefined, ParamAccess.Write)],
            },
            undefined,
            undefined,
        ),
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_DWORD, undefined, ParamAccess.Read)],
            },
            undefined,
            undefined,
        ),
    ],
    undefined,
));
export const OP_SHIFT_RIGHT = (OPCODES[0xf899] = new_opcode(
    0xf899,
    "shift_right",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_DWORD, undefined, ParamAccess.Write)],
            },
            undefined,
            undefined,
        ),
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_DWORD, undefined, ParamAccess.Read)],
            },
            undefined,
            undefined,
        ),
    ],
    undefined,
));
export const OP_GET_RANDOM = (OPCODES[0xf89a] = new_opcode(
    0xf89a,
    "get_random",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [
                    new_param(TYPE_DWORD, undefined, ParamAccess.Read),
                    new_param(TYPE_DWORD, undefined, ParamAccess.Read),
                ],
            },
            undefined,
            undefined,
        ),
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_DWORD, undefined, ParamAccess.Write)],
            },
            undefined,
            undefined,
        ),
    ],
    undefined,
));
export const OP_RESET_MAP = (OPCODES[0xf89b] = new_opcode(
    0xf89b,
    "reset_map",
    "Sets all registers to 0 and resets the quest.",
    [],
    undefined,
));
export const OP_DISP_CHL_RETRY_MENU = (OPCODES[0xf89c] = new_opcode(
    0xf89c,
    "disp_chl_retry_menu",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_ANY, undefined, ParamAccess.Write)],
            },
            undefined,
            undefined,
        ),
    ],
    undefined,
));
export const OP_CHL_REVERSER = (OPCODES[0xf89d] = new_opcode(
    0xf89d,
    "chl_reverser",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F89E = (OPCODES[0xf89e] = new_opcode(
    0xf89e,
    "unknown_f89e",
    undefined,
    [new_param(TYPE_DWORD, undefined, undefined)],
    StackInteraction.Pop,
));
export const OP_UNKNOWN_F89F = (OPCODES[0xf89f] = new_opcode(
    0xf89f,
    "unknown_f89f",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_ANY, undefined, ParamAccess.Write)],
            },
            undefined,
            undefined,
        ),
    ],
    undefined,
));
export const OP_UNKNOWN_F8A0 = (OPCODES[0xf8a0] = new_opcode(
    0xf8a0,
    "unknown_f8a0",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F8A1 = (OPCODES[0xf8a1] = new_opcode(
    0xf8a1,
    "unknown_f8a1",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F8A2 = (OPCODES[0xf8a2] = new_opcode(
    0xf8a2,
    "unknown_f8a2",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F8A3 = (OPCODES[0xf8a3] = new_opcode(
    0xf8a3,
    "unknown_f8a3",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F8A4 = (OPCODES[0xf8a4] = new_opcode(
    0xf8a4,
    "unknown_f8a4",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F8A5 = (OPCODES[0xf8a5] = new_opcode(
    0xf8a5,
    "unknown_f8a5",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F8A6 = (OPCODES[0xf8a6] = new_opcode(
    0xf8a6,
    "unknown_f8a6",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F8A7 = (OPCODES[0xf8a7] = new_opcode(
    0xf8a7,
    "unknown_f8a7",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F8A8 = (OPCODES[0xf8a8] = new_opcode(
    0xf8a8,
    "unknown_f8a8",
    undefined,
    [new_param(TYPE_DWORD, undefined, undefined)],
    StackInteraction.Pop,
));
export const OP_UNKNOWN_F8A9 = (OPCODES[0xf8a9] = new_opcode(
    0xf8a9,
    "unknown_f8a9",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_ANY, undefined, ParamAccess.Write)],
            },
            undefined,
            undefined,
        ),
    ],
    undefined,
));
export const OP_UNKNOWN_F8AA = (OPCODES[0xf8aa] = new_opcode(
    0xf8aa,
    "unknown_f8aa",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F8AB = (OPCODES[0xf8ab] = new_opcode(
    0xf8ab,
    "unknown_f8ab",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F8AC = (OPCODES[0xf8ac] = new_opcode(
    0xf8ac,
    "unknown_f8ac",
    undefined,
    [],
    undefined,
));
export const OP_GET_NUMBER_OF_PLAYER2 = (OPCODES[0xf8ad] = new_opcode(
    0xf8ad,
    "get_number_of_player2",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_ANY, undefined, ParamAccess.Write)],
            },
            undefined,
            undefined,
        ),
    ],
    undefined,
));
export const OP_UNKNOWN_F8AE = (OPCODES[0xf8ae] = new_opcode(
    0xf8ae,
    "unknown_f8ae",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F8AF = (OPCODES[0xf8af] = new_opcode(
    0xf8af,
    "unknown_f8af",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F8B0 = (OPCODES[0xf8b0] = new_opcode(
    0xf8b0,
    "unknown_f8b0",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F8B1 = (OPCODES[0xf8b1] = new_opcode(
    0xf8b1,
    "unknown_f8b1",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F8B2 = (OPCODES[0xf8b2] = new_opcode(
    0xf8b2,
    "unknown_f8b2",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F8B3 = (OPCODES[0xf8b3] = new_opcode(
    0xf8b3,
    "unknown_f8b3",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F8B4 = (OPCODES[0xf8b4] = new_opcode(
    0xf8b4,
    "unknown_f8b4",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F8B5 = (OPCODES[0xf8b5] = new_opcode(
    0xf8b5,
    "unknown_f8b5",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F8B6 = (OPCODES[0xf8b6] = new_opcode(
    0xf8b6,
    "unknown_f8b6",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F8B7 = (OPCODES[0xf8b7] = new_opcode(
    0xf8b7,
    "unknown_f8b7",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F8B8 = (OPCODES[0xf8b8] = new_opcode(
    0xf8b8,
    "unknown_f8b8",
    undefined,
    [],
    undefined,
));
export const OP_CHL_RECOVERY = (OPCODES[0xf8b9] = new_opcode(
    0xf8b9,
    "chl_recovery",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F8BA = (OPCODES[0xf8ba] = new_opcode(
    0xf8ba,
    "unknown_f8ba",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F8BB = (OPCODES[0xf8bb] = new_opcode(
    0xf8bb,
    "unknown_f8bb",
    undefined,
    [],
    undefined,
));
export const OP_SET_EPISODE = (OPCODES[0xf8bc] = new_opcode(
    0xf8bc,
    "set_episode",
    undefined,
    [new_param(TYPE_DWORD, undefined, undefined)],
    undefined,
));
export const OP_UNKNOWN_F8BD = (OPCODES[0xf8bd] = new_opcode(
    0xf8bd,
    "unknown_f8bd",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F8BE = (OPCODES[0xf8be] = new_opcode(
    0xf8be,
    "unknown_f8be",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F8BF = (OPCODES[0xf8bf] = new_opcode(
    0xf8bf,
    "unknown_f8bf",
    undefined,
    [],
    undefined,
));
export const OP_FILE_DL_REQ = (OPCODES[0xf8c0] = new_opcode(
    0xf8c0,
    "file_dl_req",
    undefined,
    [new_param(TYPE_DWORD, undefined, undefined), new_param(TYPE_STRING, undefined, undefined)],
    StackInteraction.Pop,
));
export const OP_GET_DL_STATUS = (OPCODES[0xf8c1] = new_opcode(
    0xf8c1,
    "get_dl_status",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_ANY, undefined, ParamAccess.Write)],
            },
            undefined,
            undefined,
        ),
    ],
    undefined,
));
export const OP_GBA_UNKNOWN4 = (OPCODES[0xf8c2] = new_opcode(
    0xf8c2,
    "gba_unknown4",
    undefined,
    [],
    undefined,
));
export const OP_GET_GBA_STATE = (OPCODES[0xf8c3] = new_opcode(
    0xf8c3,
    "get_gba_state",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_ANY, undefined, ParamAccess.Write)],
            },
            undefined,
            undefined,
        ),
    ],
    undefined,
));
export const OP_UNKNOWN_F8C4 = (OPCODES[0xf8c4] = new_opcode(
    0xf8c4,
    "unknown_f8c4",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_ANY, undefined, ParamAccess.Write)],
            },
            undefined,
            undefined,
        ),
    ],
    undefined,
));
export const OP_UNKNOWN_F8C5 = (OPCODES[0xf8c5] = new_opcode(
    0xf8c5,
    "unknown_f8c5",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_ANY, undefined, ParamAccess.Write)],
            },
            undefined,
            undefined,
        ),
    ],
    undefined,
));
export const OP_QEXIT = (OPCODES[0xf8c6] = new_opcode(0xf8c6, "qexit", undefined, [], undefined));
export const OP_USE_ANIMATION = (OPCODES[0xf8c7] = new_opcode(
    0xf8c7,
    "use_animation",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_DWORD, "Player slot.", ParamAccess.Read)],
            },
            undefined,
            undefined,
        ),
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [
                    new_param(TYPE_DWORD, "Animation ID.", ParamAccess.Read),
                    new_param(
                        TYPE_DWORD,
                        "Animation duration in number of frames.",
                        ParamAccess.Read,
                    ),
                ],
            },
            undefined,
            undefined,
        ),
    ],
    undefined,
));
export const OP_STOP_ANIMATION = (OPCODES[0xf8c8] = new_opcode(
    0xf8c8,
    "stop_animation",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_DWORD, "Player slot.", ParamAccess.Read)],
            },
            undefined,
            undefined,
        ),
    ],
    undefined,
));
export const OP_RUN_TO_COORD = (OPCODES[0xf8c9] = new_opcode(
    0xf8c9,
    "run_to_coord",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_ANY, undefined, ParamAccess.Read)],
            },
            undefined,
            undefined,
        ),
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_ANY, undefined, ParamAccess.Read)],
            },
            undefined,
            undefined,
        ),
    ],
    undefined,
));
export const OP_SET_SLOT_INVINCIBLE = (OPCODES[0xf8ca] = new_opcode(
    0xf8ca,
    "set_slot_invincible",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_DWORD, "Player slot.", ParamAccess.Read)],
            },
            undefined,
            undefined,
        ),
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_DWORD, undefined, ParamAccess.Read)],
            },
            undefined,
            undefined,
        ),
    ],
    undefined,
));
export const OP_UNKNOWN_F8CB = (OPCODES[0xf8cb] = new_opcode(
    0xf8cb,
    "unknown_f8cb",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_ANY, undefined, ParamAccess.Read)],
            },
            undefined,
            undefined,
        ),
    ],
    undefined,
));
export const OP_SET_SLOT_POISON = (OPCODES[0xf8cc] = new_opcode(
    0xf8cc,
    "set_slot_poison",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_DWORD, "Player slot.", ParamAccess.Read)],
            },
            undefined,
            undefined,
        ),
    ],
    undefined,
));
export const OP_SET_SLOT_PARALYZE = (OPCODES[0xf8cd] = new_opcode(
    0xf8cd,
    "set_slot_paralyze",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_DWORD, "Player slot.", ParamAccess.Read)],
            },
            undefined,
            undefined,
        ),
    ],
    undefined,
));
export const OP_SET_SLOT_SHOCK = (OPCODES[0xf8ce] = new_opcode(
    0xf8ce,
    "set_slot_shock",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_DWORD, "Player slot.", ParamAccess.Read)],
            },
            undefined,
            undefined,
        ),
    ],
    undefined,
));
export const OP_SET_SLOT_FREEZE = (OPCODES[0xf8cf] = new_opcode(
    0xf8cf,
    "set_slot_freeze",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_DWORD, "Player slot.", ParamAccess.Read)],
            },
            undefined,
            undefined,
        ),
    ],
    undefined,
));
export const OP_SET_SLOT_SLOW = (OPCODES[0xf8d0] = new_opcode(
    0xf8d0,
    "set_slot_slow",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_DWORD, "Player slot.", ParamAccess.Read)],
            },
            undefined,
            undefined,
        ),
    ],
    undefined,
));
export const OP_SET_SLOT_CONFUSE = (OPCODES[0xf8d1] = new_opcode(
    0xf8d1,
    "set_slot_confuse",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_DWORD, "Player slot.", ParamAccess.Read)],
            },
            undefined,
            undefined,
        ),
    ],
    undefined,
));
export const OP_SET_SLOT_SHIFTA = (OPCODES[0xf8d2] = new_opcode(
    0xf8d2,
    "set_slot_shifta",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_DWORD, "Player slot.", ParamAccess.Read)],
            },
            undefined,
            undefined,
        ),
    ],
    undefined,
));
export const OP_SET_SLOT_DEBAND = (OPCODES[0xf8d3] = new_opcode(
    0xf8d3,
    "set_slot_deband",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_DWORD, "Player slot.", ParamAccess.Read)],
            },
            undefined,
            undefined,
        ),
    ],
    undefined,
));
export const OP_SET_SLOT_JELLEN = (OPCODES[0xf8d4] = new_opcode(
    0xf8d4,
    "set_slot_jellen",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_DWORD, "Player slot.", ParamAccess.Read)],
            },
            undefined,
            undefined,
        ),
    ],
    undefined,
));
export const OP_SET_SLOT_ZALURE = (OPCODES[0xf8d5] = new_opcode(
    0xf8d5,
    "set_slot_zalure",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_DWORD, "Player slot.", ParamAccess.Read)],
            },
            undefined,
            undefined,
        ),
    ],
    undefined,
));
export const OP_FLETI_FIXED_CAMERA = (OPCODES[0xf8d6] = new_opcode(
    0xf8d6,
    "fleti_fixed_camera",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_ANY, undefined, ParamAccess.Read)],
            },
            undefined,
            undefined,
        ),
    ],
    StackInteraction.Pop,
));
export const OP_FLETI_LOCKED_CAMERA = (OPCODES[0xf8d7] = new_opcode(
    0xf8d7,
    "fleti_locked_camera",
    undefined,
    [
        new_param(TYPE_DWORD, undefined, undefined),
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_ANY, undefined, ParamAccess.Read)],
            },
            undefined,
            undefined,
        ),
    ],
    StackInteraction.Pop,
));
export const OP_DEFAULT_CAMERA_POS2 = (OPCODES[0xf8d8] = new_opcode(
    0xf8d8,
    "default_camera_pos2",
    undefined,
    [],
    undefined,
));
export const OP_SET_MOTION_BLUR = (OPCODES[0xf8d9] = new_opcode(
    0xf8d9,
    "set_motion_blur",
    undefined,
    [],
    undefined,
));
export const OP_SET_SCREEN_BW = (OPCODES[0xf8da] = new_opcode(
    0xf8da,
    "set_screen_bw",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F8DB = (OPCODES[0xf8db] = new_opcode(
    0xf8db,
    "unknown_f8db",
    undefined,
    [
        new_param(TYPE_DWORD, undefined, undefined),
        new_param(TYPE_DWORD, undefined, undefined),
        new_param(TYPE_DWORD, undefined, undefined),
        new_param(TYPE_DWORD, undefined, undefined),
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_ANY, undefined, ParamAccess.Read)],
            },
            undefined,
            undefined,
        ),
        new_param(TYPE_WORD, undefined, undefined),
    ],
    StackInteraction.Pop,
));
export const OP_NPC_ACTION_STRING = (OPCODES[0xf8dc] = new_opcode(
    0xf8dc,
    "npc_action_string",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_DWORD, undefined, ParamAccess.Read)],
            },
            undefined,
            undefined,
        ),
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_DWORD, undefined, ParamAccess.Read)],
            },
            undefined,
            undefined,
        ),
        new_param(TYPE_S_LABEL, undefined, undefined),
    ],
    undefined,
));
export const OP_GET_PAD_COND = (OPCODES[0xf8dd] = new_opcode(
    0xf8dd,
    "get_pad_cond",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_ANY, undefined, ParamAccess.Write)],
            },
            undefined,
            undefined,
        ),
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_ANY, undefined, ParamAccess.Write)],
            },
            undefined,
            undefined,
        ),
    ],
    undefined,
));
export const OP_GET_BUTTON_COND = (OPCODES[0xf8de] = new_opcode(
    0xf8de,
    "get_button_cond",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_ANY, undefined, ParamAccess.Write)],
            },
            undefined,
            undefined,
        ),
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_ANY, undefined, ParamAccess.Write)],
            },
            undefined,
            undefined,
        ),
    ],
    undefined,
));
export const OP_FREEZE_ENEMIES = (OPCODES[0xf8df] = new_opcode(
    0xf8df,
    "freeze_enemies",
    undefined,
    [],
    undefined,
));
export const OP_UNFREEZE_ENEMIES = (OPCODES[0xf8e0] = new_opcode(
    0xf8e0,
    "unfreeze_enemies",
    undefined,
    [],
    undefined,
));
export const OP_FREEZE_EVERYTHING = (OPCODES[0xf8e1] = new_opcode(
    0xf8e1,
    "freeze_everything",
    undefined,
    [],
    undefined,
));
export const OP_UNFREEZE_EVERYTHING = (OPCODES[0xf8e2] = new_opcode(
    0xf8e2,
    "unfreeze_everything",
    undefined,
    [],
    undefined,
));
export const OP_RESTORE_HP = (OPCODES[0xf8e3] = new_opcode(
    0xf8e3,
    "restore_hp",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_ANY, undefined, ParamAccess.Read)],
            },
            undefined,
            undefined,
        ),
    ],
    undefined,
));
export const OP_RESTORE_TP = (OPCODES[0xf8e4] = new_opcode(
    0xf8e4,
    "restore_tp",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_ANY, undefined, ParamAccess.Read)],
            },
            undefined,
            undefined,
        ),
    ],
    undefined,
));
export const OP_CLOSE_CHAT_BUBBLE = (OPCODES[0xf8e5] = new_opcode(
    0xf8e5,
    "close_chat_bubble",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_ANY, undefined, ParamAccess.Read)],
            },
            undefined,
            undefined,
        ),
    ],
    undefined,
));
export const OP_MOVE_COORDS_OBJECT = (OPCODES[0xf8e6] = new_opcode(
    0xf8e6,
    "move_coords_object",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_ANY, undefined, ParamAccess.Read)],
            },
            undefined,
            undefined,
        ),
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_ANY, undefined, ParamAccess.Read)],
            },
            undefined,
            undefined,
        ),
    ],
    undefined,
));
export const OP_AT_COORDS_CALL_EX = (OPCODES[0xf8e7] = new_opcode(
    0xf8e7,
    "at_coords_call_ex",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_ANY, undefined, ParamAccess.Read)],
            },
            undefined,
            undefined,
        ),
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_ANY, undefined, ParamAccess.Read)],
            },
            undefined,
            undefined,
        ),
    ],
    undefined,
));
export const OP_UNKNOWN_F8E8 = (OPCODES[0xf8e8] = new_opcode(
    0xf8e8,
    "unknown_f8e8",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_ANY, undefined, ParamAccess.Read)],
            },
            undefined,
            undefined,
        ),
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_ANY, undefined, ParamAccess.Read)],
            },
            undefined,
            undefined,
        ),
    ],
    undefined,
));
export const OP_UNKNOWN_F8E9 = (OPCODES[0xf8e9] = new_opcode(
    0xf8e9,
    "unknown_f8e9",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_ANY, undefined, ParamAccess.Read)],
            },
            undefined,
            undefined,
        ),
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_ANY, undefined, ParamAccess.Read)],
            },
            undefined,
            undefined,
        ),
    ],
    undefined,
));
export const OP_UNKNOWN_F8EA = (OPCODES[0xf8ea] = new_opcode(
    0xf8ea,
    "unknown_f8ea",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_ANY, undefined, ParamAccess.Read)],
            },
            undefined,
            undefined,
        ),
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_ANY, undefined, ParamAccess.Read)],
            },
            undefined,
            undefined,
        ),
    ],
    undefined,
));
export const OP_UNKNOWN_F8EB = (OPCODES[0xf8eb] = new_opcode(
    0xf8eb,
    "unknown_f8eb",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_ANY, undefined, ParamAccess.Read)],
            },
            undefined,
            undefined,
        ),
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_ANY, undefined, ParamAccess.Read)],
            },
            undefined,
            undefined,
        ),
    ],
    undefined,
));
export const OP_UNKNOWN_F8EC = (OPCODES[0xf8ec] = new_opcode(
    0xf8ec,
    "unknown_f8ec",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_ANY, undefined, ParamAccess.Read)],
            },
            undefined,
            undefined,
        ),
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_ANY, undefined, ParamAccess.Read)],
            },
            undefined,
            undefined,
        ),
    ],
    undefined,
));
export const OP_ANIMATION_CHECK = (OPCODES[0xf8ed] = new_opcode(
    0xf8ed,
    "animation_check",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_ANY, undefined, ParamAccess.Read)],
            },
            undefined,
            undefined,
        ),
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_ANY, undefined, ParamAccess.Read)],
            },
            undefined,
            undefined,
        ),
    ],
    undefined,
));
export const OP_CALL_IMAGE_DATA = (OPCODES[0xf8ee] = new_opcode(
    0xf8ee,
    "call_image_data",
    undefined,
    [new_param(TYPE_DWORD, undefined, undefined), new_param(TYPE_WORD, undefined, undefined)],
    StackInteraction.Pop,
));
export const OP_UNKNOWN_F8EF = (OPCODES[0xf8ef] = new_opcode(
    0xf8ef,
    "unknown_f8ef",
    undefined,
    [],
    undefined,
));
export const OP_TURN_OFF_BGM_P2 = (OPCODES[0xf8f0] = new_opcode(
    0xf8f0,
    "turn_off_bgm_p2",
    undefined,
    [],
    undefined,
));
export const OP_TURN_ON_BGM_P2 = (OPCODES[0xf8f1] = new_opcode(
    0xf8f1,
    "turn_on_bgm_p2",
    undefined,
    [],
    undefined,
));
export const OP_LOAD_UNK_DATA = (OPCODES[0xf8f2] = new_opcode(
    0xf8f2,
    "load_unk_data",
    undefined,
    [
        new_param(TYPE_DWORD, undefined, undefined),
        new_param(TYPE_DWORD, undefined, undefined),
        new_param(TYPE_DWORD, undefined, undefined),
        new_param(TYPE_DWORD, undefined, undefined),
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_ANY, undefined, ParamAccess.Read)],
            },
            undefined,
            undefined,
        ),
        new_param(TYPE_D_LABEL, undefined, undefined),
    ],
    StackInteraction.Pop,
));
export const OP_PARTICLE2 = (OPCODES[0xf8f3] = new_opcode(
    0xf8f3,
    "particle2",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_ANY, undefined, ParamAccess.Read)],
            },
            undefined,
            undefined,
        ),
        new_param(TYPE_DWORD, undefined, undefined),
        new_param(TYPE_FLOAT, undefined, undefined),
    ],
    StackInteraction.Pop,
));
export const OP_UNKNOWN_F8F4 = (OPCODES[0xf8f4] = new_opcode(
    0xf8f4,
    "unknown_f8f4",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F8F5 = (OPCODES[0xf8f5] = new_opcode(
    0xf8f5,
    "unknown_f8f5",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F8F6 = (OPCODES[0xf8f6] = new_opcode(
    0xf8f6,
    "unknown_f8f6",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F8F7 = (OPCODES[0xf8f7] = new_opcode(
    0xf8f7,
    "unknown_f8f7",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F8F8 = (OPCODES[0xf8f8] = new_opcode(
    0xf8f8,
    "unknown_f8f8",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F8F9 = (OPCODES[0xf8f9] = new_opcode(
    0xf8f9,
    "unknown_f8f9",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F8FA = (OPCODES[0xf8fa] = new_opcode(
    0xf8fa,
    "unknown_f8fa",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F8FB = (OPCODES[0xf8fb] = new_opcode(
    0xf8fb,
    "unknown_f8fb",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F8FC = (OPCODES[0xf8fc] = new_opcode(
    0xf8fc,
    "unknown_f8fc",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F8FD = (OPCODES[0xf8fd] = new_opcode(
    0xf8fd,
    "unknown_f8fd",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F8FE = (OPCODES[0xf8fe] = new_opcode(
    0xf8fe,
    "unknown_f8fe",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F8FF = (OPCODES[0xf8ff] = new_opcode(
    0xf8ff,
    "unknown_f8ff",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F900 = (OPCODES[0xf900] = new_opcode(
    0xf900,
    "unknown_f900",
    undefined,
    [],
    undefined,
));
export const OP_DEC2FLOAT = (OPCODES[0xf901] = new_opcode(
    0xf901,
    "dec2float",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_FLOAT, undefined, ParamAccess.Write)],
            },
            undefined,
            undefined,
        ),
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_DWORD, undefined, ParamAccess.Read)],
            },
            undefined,
            undefined,
        ),
    ],
    undefined,
));
export const OP_FLOAT2DEC = (OPCODES[0xf902] = new_opcode(
    0xf902,
    "float2dec",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_DWORD, undefined, ParamAccess.Write)],
            },
            undefined,
            undefined,
        ),
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_FLOAT, undefined, ParamAccess.Read)],
            },
            undefined,
            undefined,
        ),
    ],
    undefined,
));
export const OP_FLET = (OPCODES[0xf903] = new_opcode(
    0xf903,
    "flet",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_FLOAT, undefined, ParamAccess.Write)],
            },
            undefined,
            undefined,
        ),
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_FLOAT, undefined, ParamAccess.Read)],
            },
            undefined,
            undefined,
        ),
    ],
    undefined,
));
export const OP_FLETI = (OPCODES[0xf904] = new_opcode(
    0xf904,
    "fleti",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_FLOAT, undefined, ParamAccess.Write)],
            },
            undefined,
            undefined,
        ),
        new_param(TYPE_FLOAT, undefined, undefined),
    ],
    undefined,
));
export const OP_UNKNOWN_F905 = (OPCODES[0xf905] = new_opcode(
    0xf905,
    "unknown_f905",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F906 = (OPCODES[0xf906] = new_opcode(
    0xf906,
    "unknown_f906",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F907 = (OPCODES[0xf907] = new_opcode(
    0xf907,
    "unknown_f907",
    undefined,
    [],
    undefined,
));
export const OP_FADD = (OPCODES[0xf908] = new_opcode(
    0xf908,
    "fadd",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_FLOAT, undefined, ParamAccess.Write)],
            },
            undefined,
            undefined,
        ),
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_FLOAT, undefined, ParamAccess.Read)],
            },
            undefined,
            undefined,
        ),
    ],
    undefined,
));
export const OP_FADDI = (OPCODES[0xf909] = new_opcode(
    0xf909,
    "faddi",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_FLOAT, undefined, ParamAccess.Write)],
            },
            undefined,
            undefined,
        ),
        new_param(TYPE_FLOAT, undefined, undefined),
    ],
    undefined,
));
export const OP_FSUB = (OPCODES[0xf90a] = new_opcode(
    0xf90a,
    "fsub",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_FLOAT, undefined, ParamAccess.Write)],
            },
            undefined,
            undefined,
        ),
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_FLOAT, undefined, ParamAccess.Read)],
            },
            undefined,
            undefined,
        ),
    ],
    undefined,
));
export const OP_FSUBI = (OPCODES[0xf90b] = new_opcode(
    0xf90b,
    "fsubi",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_FLOAT, undefined, ParamAccess.Write)],
            },
            undefined,
            undefined,
        ),
        new_param(TYPE_FLOAT, undefined, undefined),
    ],
    undefined,
));
export const OP_FMUL = (OPCODES[0xf90c] = new_opcode(
    0xf90c,
    "fmul",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_FLOAT, undefined, ParamAccess.Write)],
            },
            undefined,
            undefined,
        ),
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_FLOAT, undefined, ParamAccess.Read)],
            },
            undefined,
            undefined,
        ),
    ],
    undefined,
));
export const OP_FMULI = (OPCODES[0xf90d] = new_opcode(
    0xf90d,
    "fmuli",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_FLOAT, undefined, ParamAccess.Write)],
            },
            undefined,
            undefined,
        ),
        new_param(TYPE_FLOAT, undefined, undefined),
    ],
    undefined,
));
export const OP_FDIV = (OPCODES[0xf90e] = new_opcode(
    0xf90e,
    "fdiv",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_FLOAT, undefined, ParamAccess.Write)],
            },
            undefined,
            undefined,
        ),
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_FLOAT, undefined, ParamAccess.Read)],
            },
            undefined,
            undefined,
        ),
    ],
    undefined,
));
export const OP_FDIVI = (OPCODES[0xf90f] = new_opcode(
    0xf90f,
    "fdivi",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_FLOAT, undefined, ParamAccess.Write)],
            },
            undefined,
            undefined,
        ),
        new_param(TYPE_FLOAT, undefined, undefined),
    ],
    undefined,
));
export const OP_GET_UNKNOWN_COUNT = (OPCODES[0xf910] = new_opcode(
    0xf910,
    "get_unknown_count",
    undefined,
    [
        new_param(TYPE_DWORD, undefined, undefined),
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_DWORD, undefined, ParamAccess.Write)],
            },
            undefined,
            undefined,
        ),
    ],
    StackInteraction.Pop,
));
export const OP_GET_STACKABLE_ITEM_COUNT = (OPCODES[0xf911] = new_opcode(
    0xf911,
    "get_stackable_item_count",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [
                    new_param(TYPE_DWORD, "Player slot.", ParamAccess.Read),
                    new_param(TYPE_DWORD, undefined, ParamAccess.Read),
                    new_param(TYPE_DWORD, undefined, ParamAccess.Read),
                    new_param(TYPE_DWORD, undefined, ParamAccess.Read),
                ],
            },
            undefined,
            undefined,
        ),
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_DWORD, undefined, ParamAccess.Write)],
            },
            undefined,
            undefined,
        ),
    ],
    undefined,
));
export const OP_FREEZE_AND_HIDE_EQUIP = (OPCODES[0xf912] = new_opcode(
    0xf912,
    "freeze_and_hide_equip",
    undefined,
    [],
    undefined,
));
export const OP_THAW_AND_SHOW_EQUIP = (OPCODES[0xf913] = new_opcode(
    0xf913,
    "thaw_and_show_equip",
    undefined,
    [],
    undefined,
));
export const OP_SET_PALETTEX_CALLBACK = (OPCODES[0xf914] = new_opcode(
    0xf914,
    "set_palettex_callback",
    undefined,
    [
        new_param(TYPE_DWORD, "Player slot.", undefined),
        new_param(TYPE_I_LABEL, undefined, undefined),
    ],
    StackInteraction.Pop,
));
export const OP_ACTIVATE_PALETTEX = (OPCODES[0xf915] = new_opcode(
    0xf915,
    "activate_palettex",
    undefined,
    [new_param(TYPE_DWORD, "Player slot.", undefined)],
    StackInteraction.Pop,
));
export const OP_ENABLE_PALETTEX = (OPCODES[0xf916] = new_opcode(
    0xf916,
    "enable_palettex",
    undefined,
    [new_param(TYPE_DWORD, "Player slot.", undefined)],
    StackInteraction.Pop,
));
export const OP_RESTORE_PALETTEX = (OPCODES[0xf917] = new_opcode(
    0xf917,
    "restore_palettex",
    undefined,
    [new_param(TYPE_DWORD, "Player slot.", undefined)],
    StackInteraction.Pop,
));
export const OP_DISABLE_PALETTEX = (OPCODES[0xf918] = new_opcode(
    0xf918,
    "disable_palettex",
    undefined,
    [new_param(TYPE_DWORD, "Player slot.", undefined)],
    StackInteraction.Pop,
));
export const OP_GET_PALETTEX_ACTIVATED = (OPCODES[0xf919] = new_opcode(
    0xf919,
    "get_palettex_activated",
    undefined,
    [
        new_param(TYPE_DWORD, "Player slot.", undefined),
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_DWORD, undefined, ParamAccess.Write)],
            },
            undefined,
            undefined,
        ),
    ],
    StackInteraction.Pop,
));
export const OP_GET_UNKNOWN_PALETTEX_STATUS = (OPCODES[0xf91a] = new_opcode(
    0xf91a,
    "get_unknown_palettex_status",
    undefined,
    [
        new_param(TYPE_DWORD, undefined, undefined),
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_DWORD, undefined, ParamAccess.Write)],
            },
            undefined,
            undefined,
        ),
    ],
    StackInteraction.Pop,
));
export const OP_DISABLE_MOVEMENT2 = (OPCODES[0xf91b] = new_opcode(
    0xf91b,
    "disable_movement2",
    undefined,
    [new_param(TYPE_DWORD, "Player slot.", undefined)],
    StackInteraction.Pop,
));
export const OP_ENABLE_MOVEMENT2 = (OPCODES[0xf91c] = new_opcode(
    0xf91c,
    "enable_movement2",
    undefined,
    [new_param(TYPE_DWORD, "Player slot.", undefined)],
    StackInteraction.Pop,
));
export const OP_GET_TIME_PLAYED = (OPCODES[0xf91d] = new_opcode(
    0xf91d,
    "get_time_played",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_DWORD, undefined, ParamAccess.Write)],
            },
            undefined,
            undefined,
        ),
    ],
    undefined,
));
export const OP_GET_GUILDCARD_TOTAL = (OPCODES[0xf91e] = new_opcode(
    0xf91e,
    "get_guildcard_total",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_DWORD, undefined, ParamAccess.Write)],
            },
            undefined,
            undefined,
        ),
    ],
    undefined,
));
export const OP_GET_SLOT_MESETA = (OPCODES[0xf91f] = new_opcode(
    0xf91f,
    "get_slot_meseta",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_DWORD, undefined, ParamAccess.Write)],
            },
            undefined,
            undefined,
        ),
    ],
    undefined,
));
export const OP_GET_PLAYER_LEVEL = (OPCODES[0xf920] = new_opcode(
    0xf920,
    "get_player_level",
    undefined,
    [
        new_param(TYPE_DWORD, "Player slot.", undefined),
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_DWORD, undefined, ParamAccess.Write)],
            },
            undefined,
            undefined,
        ),
    ],
    StackInteraction.Pop,
));
export const OP_GET_SECTION_ID = (OPCODES[0xf921] = new_opcode(
    0xf921,
    "get_section_id",
    undefined,
    [
        new_param(TYPE_DWORD, "Player slot.", undefined),
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_DWORD, undefined, ParamAccess.Write)],
            },
            undefined,
            undefined,
        ),
    ],
    StackInteraction.Pop,
));
export const OP_GET_PLAYER_HP = (OPCODES[0xf922] = new_opcode(
    0xf922,
    "get_player_hp",
    undefined,
    [
        new_param(TYPE_DWORD, "Player slot.", undefined),
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [
                    new_param(TYPE_DWORD, "Maximum HP.", ParamAccess.Write),
                    new_param(TYPE_DWORD, "Current HP.", ParamAccess.Write),
                    new_param(TYPE_DWORD, "Maximum TP.", ParamAccess.Write),
                    new_param(TYPE_DWORD, "Current TP.", ParamAccess.Write),
                ],
            },
            undefined,
            undefined,
        ),
    ],
    StackInteraction.Pop,
));
export const OP_GET_FLOOR_NUMBER = (OPCODES[0xf923] = new_opcode(
    0xf923,
    "get_floor_number",
    undefined,
    [
        new_param(TYPE_DWORD, "Player slot.", undefined),
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_DWORD, undefined, ParamAccess.Write)],
            },
            undefined,
            undefined,
        ),
    ],
    StackInteraction.Pop,
));
export const OP_GET_COORD_PLAYER_DETECT = (OPCODES[0xf924] = new_opcode(
    0xf924,
    "get_coord_player_detect",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_ANY, "Player slot.", ParamAccess.Read)],
            },
            undefined,
            undefined,
        ),
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_ANY, undefined, ParamAccess.Read)],
            },
            undefined,
            undefined,
        ),
    ],
    undefined,
));
export const OP_READ_GLOBAL_FLAG = (OPCODES[0xf925] = new_opcode(
    0xf925,
    "read_global_flag",
    undefined,
    [
        new_param(TYPE_DWORD, undefined, undefined),
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_DWORD, undefined, ParamAccess.Write)],
            },
            undefined,
            undefined,
        ),
    ],
    StackInteraction.Pop,
));
export const OP_WRITE_GLOBAL_FLAG = (OPCODES[0xf926] = new_opcode(
    0xf926,
    "write_global_flag",
    undefined,
    [new_param(TYPE_DWORD, undefined, undefined), new_param(TYPE_DWORD, undefined, undefined)],
    StackInteraction.Pop,
));
export const OP_UNKNOWN_F927 = (OPCODES[0xf927] = new_opcode(
    0xf927,
    "unknown_f927",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_ANY, undefined, ParamAccess.Write)],
            },
            undefined,
            undefined,
        ),
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_ANY, undefined, ParamAccess.Write)],
            },
            undefined,
            undefined,
        ),
    ],
    undefined,
));
export const OP_FLOOR_PLAYER_DETECT = (OPCODES[0xf928] = new_opcode(
    0xf928,
    "floor_player_detect",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [
                    new_param(TYPE_DWORD, undefined, ParamAccess.Write),
                    new_param(TYPE_DWORD, undefined, ParamAccess.Write),
                    new_param(TYPE_DWORD, undefined, ParamAccess.Write),
                    new_param(TYPE_DWORD, undefined, ParamAccess.Write),
                ],
            },
            undefined,
            undefined,
        ),
    ],
    undefined,
));
export const OP_READ_DISK_FILE = (OPCODES[0xf929] = new_opcode(
    0xf929,
    "read_disk_file",
    undefined,
    [new_param(TYPE_STRING, undefined, undefined)],
    StackInteraction.Pop,
));
export const OP_OPEN_PACK_SELECT = (OPCODES[0xf92a] = new_opcode(
    0xf92a,
    "open_pack_select",
    undefined,
    [],
    undefined,
));
export const OP_ITEM_SELECT = (OPCODES[0xf92b] = new_opcode(
    0xf92b,
    "item_select",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_ANY, undefined, ParamAccess.Write)],
            },
            undefined,
            undefined,
        ),
    ],
    undefined,
));
export const OP_GET_ITEM_ID = (OPCODES[0xf92c] = new_opcode(
    0xf92c,
    "get_item_id",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_ANY, undefined, ParamAccess.Write)],
            },
            undefined,
            undefined,
        ),
    ],
    undefined,
));
export const OP_COLOR_CHANGE = (OPCODES[0xf92d] = new_opcode(
    0xf92d,
    "color_change",
    undefined,
    [
        new_param(TYPE_DWORD, undefined, undefined),
        new_param(TYPE_DWORD, undefined, undefined),
        new_param(TYPE_DWORD, undefined, undefined),
        new_param(TYPE_DWORD, undefined, undefined),
        new_param(TYPE_DWORD, undefined, undefined),
    ],
    StackInteraction.Pop,
));
export const OP_SEND_STATISTIC = (OPCODES[0xf92e] = new_opcode(
    0xf92e,
    "send_statistic",
    undefined,
    [
        new_param(TYPE_DWORD, undefined, undefined),
        new_param(TYPE_DWORD, undefined, undefined),
        new_param(TYPE_DWORD, undefined, undefined),
        new_param(TYPE_DWORD, undefined, undefined),
        new_param(TYPE_DWORD, undefined, undefined),
        new_param(TYPE_DWORD, undefined, undefined),
        new_param(TYPE_DWORD, undefined, undefined),
        new_param(TYPE_DWORD, undefined, undefined),
    ],
    StackInteraction.Pop,
));
export const OP_UNKNOWN_F92F = (OPCODES[0xf92f] = new_opcode(
    0xf92f,
    "unknown_f92f",
    undefined,
    [new_param(TYPE_DWORD, undefined, undefined), new_param(TYPE_DWORD, undefined, undefined)],
    StackInteraction.Pop,
));
export const OP_CHAT_BOX = (OPCODES[0xf930] = new_opcode(
    0xf930,
    "chat_box",
    undefined,
    [
        new_param(TYPE_DWORD, undefined, undefined),
        new_param(TYPE_DWORD, undefined, undefined),
        new_param(TYPE_DWORD, undefined, undefined),
        new_param(TYPE_DWORD, undefined, undefined),
        new_param(TYPE_DWORD, undefined, undefined),
        new_param(TYPE_STRING, undefined, undefined),
    ],
    StackInteraction.Pop,
));
export const OP_CHAT_BUBBLE = (OPCODES[0xf931] = new_opcode(
    0xf931,
    "chat_bubble",
    undefined,
    [new_param(TYPE_DWORD, undefined, undefined), new_param(TYPE_STRING, undefined, undefined)],
    StackInteraction.Pop,
));
export const OP_UNKNOWN_F932 = (OPCODES[0xf932] = new_opcode(
    0xf932,
    "unknown_f932",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F933 = (OPCODES[0xf933] = new_opcode(
    0xf933,
    "unknown_f933",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_ANY, undefined, ParamAccess.Write)],
            },
            undefined,
            undefined,
        ),
    ],
    undefined,
));
export const OP_SCROLL_TEXT = (OPCODES[0xf934] = new_opcode(
    0xf934,
    "scroll_text",
    undefined,
    [
        new_param(TYPE_DWORD, undefined, undefined),
        new_param(TYPE_DWORD, undefined, undefined),
        new_param(TYPE_DWORD, undefined, undefined),
        new_param(TYPE_DWORD, undefined, undefined),
        new_param(TYPE_DWORD, undefined, undefined),
        new_param(TYPE_FLOAT, undefined, undefined),
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_DWORD, undefined, ParamAccess.Write)],
            },
            undefined,
            undefined,
        ),
        new_param(TYPE_STRING, undefined, undefined),
    ],
    StackInteraction.Pop,
));
export const OP_GBA_UNKNOWN1 = (OPCODES[0xf935] = new_opcode(
    0xf935,
    "gba_unknown1",
    undefined,
    [],
    undefined,
));
export const OP_GBA_UNKNOWN2 = (OPCODES[0xf936] = new_opcode(
    0xf936,
    "gba_unknown2",
    undefined,
    [],
    undefined,
));
export const OP_GBA_UNKNOWN3 = (OPCODES[0xf937] = new_opcode(
    0xf937,
    "gba_unknown3",
    undefined,
    [],
    undefined,
));
export const OP_ADD_DAMAGE_TO = (OPCODES[0xf938] = new_opcode(
    0xf938,
    "add_damage_to",
    undefined,
    [new_param(TYPE_DWORD, undefined, undefined), new_param(TYPE_DWORD, undefined, undefined)],
    StackInteraction.Pop,
));
export const OP_ITEM_DELETE3 = (OPCODES[0xf939] = new_opcode(
    0xf939,
    "item_delete3",
    undefined,
    [new_param(TYPE_DWORD, undefined, undefined)],
    StackInteraction.Pop,
));
export const OP_GET_ITEM_INFO = (OPCODES[0xf93a] = new_opcode(
    0xf93a,
    "get_item_info",
    undefined,
    [
        new_param(TYPE_DWORD, undefined, undefined),
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_ANY, undefined, ParamAccess.Write)],
            },
            undefined,
            undefined,
        ),
    ],
    StackInteraction.Pop,
));
export const OP_ITEM_PACKING1 = (OPCODES[0xf93b] = new_opcode(
    0xf93b,
    "item_packing1",
    undefined,
    [new_param(TYPE_DWORD, undefined, undefined)],
    StackInteraction.Pop,
));
export const OP_ITEM_PACKING2 = (OPCODES[0xf93c] = new_opcode(
    0xf93c,
    "item_packing2",
    undefined,
    [new_param(TYPE_DWORD, undefined, undefined), new_param(TYPE_DWORD, undefined, undefined)],
    StackInteraction.Pop,
));
export const OP_GET_LANG_SETTING = (OPCODES[0xf93d] = new_opcode(
    0xf93d,
    "get_lang_setting",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_ANY, undefined, ParamAccess.Write)],
            },
            undefined,
            undefined,
        ),
    ],
    StackInteraction.Pop,
));
export const OP_PREPARE_STATISTIC = (OPCODES[0xf93e] = new_opcode(
    0xf93e,
    "prepare_statistic",
    undefined,
    [
        new_param(TYPE_DWORD, undefined, undefined),
        new_param(TYPE_I_LABEL, undefined, undefined),
        new_param(TYPE_I_LABEL, undefined, undefined),
    ],
    StackInteraction.Pop,
));
export const OP_KEYWORD_DETECT = (OPCODES[0xf93f] = new_opcode(
    0xf93f,
    "keyword_detect",
    undefined,
    [],
    undefined,
));
export const OP_KEYWORD = (OPCODES[0xf940] = new_opcode(
    0xf940,
    "keyword",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_DWORD, undefined, ParamAccess.Write)],
            },
            undefined,
            undefined,
        ),
        new_param(TYPE_DWORD, "Player slot.", undefined),
        new_param(TYPE_STRING, undefined, undefined),
    ],
    StackInteraction.Pop,
));
export const OP_GET_GUILDCARD_NUM = (OPCODES[0xf941] = new_opcode(
    0xf941,
    "get_guildcard_num",
    undefined,
    [
        new_param(TYPE_DWORD, "Player slot.", undefined),
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_DWORD, undefined, ParamAccess.Write)],
            },
            undefined,
            undefined,
        ),
    ],
    StackInteraction.Pop,
));
export const OP_UNKNOWN_F942 = (OPCODES[0xf942] = new_opcode(
    0xf942,
    "unknown_f942",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F943 = (OPCODES[0xf943] = new_opcode(
    0xf943,
    "unknown_f943",
    undefined,
    [],
    undefined,
));
export const OP_GET_WRAP_STATUS = (OPCODES[0xf944] = new_opcode(
    0xf944,
    "get_wrap_status",
    undefined,
    [
        new_param(TYPE_DWORD, "Player slot.", undefined),
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_DWORD, undefined, ParamAccess.Write)],
            },
            undefined,
            undefined,
        ),
    ],
    StackInteraction.Pop,
));
export const OP_INITIAL_FLOOR = (OPCODES[0xf945] = new_opcode(
    0xf945,
    "initial_floor",
    undefined,
    [new_param(TYPE_DWORD, undefined, undefined)],
    StackInteraction.Pop,
));
export const OP_SIN = (OPCODES[0xf946] = new_opcode(
    0xf946,
    "sin",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_DWORD, undefined, ParamAccess.Write)],
            },
            undefined,
            undefined,
        ),
        new_param(TYPE_DWORD, undefined, undefined),
    ],
    StackInteraction.Pop,
));
export const OP_COS = (OPCODES[0xf947] = new_opcode(
    0xf947,
    "cos",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_DWORD, undefined, ParamAccess.Write)],
            },
            undefined,
            undefined,
        ),
        new_param(TYPE_DWORD, undefined, undefined),
    ],
    StackInteraction.Pop,
));
export const OP_UNKNOWN_F948 = (OPCODES[0xf948] = new_opcode(
    0xf948,
    "unknown_f948",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F949 = (OPCODES[0xf949] = new_opcode(
    0xf949,
    "unknown_f949",
    undefined,
    [],
    undefined,
));
export const OP_BOSS_IS_DEAD2 = (OPCODES[0xf94a] = new_opcode(
    0xf94a,
    "boss_is_dead2",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_DWORD, undefined, ParamAccess.Write)],
            },
            undefined,
            undefined,
        ),
    ],
    undefined,
));
export const OP_UNKNOWN_F94B = (OPCODES[0xf94b] = new_opcode(
    0xf94b,
    "unknown_f94b",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_DWORD, undefined, ParamAccess.Write)],
            },
            undefined,
            undefined,
        ),
    ],
    undefined,
));
export const OP_UNKNOWN_F94C = (OPCODES[0xf94c] = new_opcode(
    0xf94c,
    "unknown_f94c",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_DWORD, undefined, ParamAccess.Write)],
            },
            undefined,
            undefined,
        ),
    ],
    undefined,
));
export const OP_IS_THERE_CARDBATTLE = (OPCODES[0xf94d] = new_opcode(
    0xf94d,
    "is_there_cardbattle",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_DWORD, undefined, ParamAccess.Write)],
            },
            undefined,
            undefined,
        ),
    ],
    undefined,
));
export const OP_UNKNOWN_F94E = (OPCODES[0xf94e] = new_opcode(
    0xf94e,
    "unknown_f94e",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F94F = (OPCODES[0xf94f] = new_opcode(
    0xf94f,
    "unknown_f94f",
    undefined,
    [],
    undefined,
));
export const OP_BB_P2_MENU = (OPCODES[0xf950] = new_opcode(
    0xf950,
    "bb_p2_menu",
    undefined,
    [new_param(TYPE_DWORD, undefined, undefined)],
    StackInteraction.Pop,
));
export const OP_BB_MAP_DESIGNATE = (OPCODES[0xf951] = new_opcode(
    0xf951,
    "bb_map_designate",
    undefined,
    [
        new_param(TYPE_BYTE, undefined, undefined),
        new_param(TYPE_WORD, undefined, undefined),
        new_param(TYPE_BYTE, undefined, undefined),
        new_param(TYPE_BYTE, undefined, undefined),
    ],
    undefined,
));
export const OP_BB_GET_NUMBER_IN_PACK = (OPCODES[0xf952] = new_opcode(
    0xf952,
    "bb_get_number_in_pack",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_DWORD, undefined, ParamAccess.Write)],
            },
            undefined,
            undefined,
        ),
    ],
    undefined,
));
export const OP_BB_SWAP_ITEM = (OPCODES[0xf953] = new_opcode(
    0xf953,
    "bb_swap_item",
    undefined,
    [
        new_param(TYPE_DWORD, undefined, undefined),
        new_param(TYPE_DWORD, undefined, undefined),
        new_param(TYPE_DWORD, undefined, undefined),
        new_param(TYPE_DWORD, undefined, undefined),
        new_param(TYPE_DWORD, undefined, undefined),
        new_param(TYPE_DWORD, undefined, undefined),
        new_param(TYPE_I_LABEL, undefined, undefined),
        new_param(TYPE_I_LABEL, undefined, undefined),
    ],
    StackInteraction.Pop,
));
export const OP_BB_CHECK_WRAP = (OPCODES[0xf954] = new_opcode(
    0xf954,
    "bb_check_wrap",
    undefined,
    [
        new_param(TYPE_DWORD, undefined, undefined),
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_DWORD, undefined, ParamAccess.Write)],
            },
            undefined,
            undefined,
        ),
    ],
    StackInteraction.Pop,
));
export const OP_BB_EXCHANGE_PD_ITEM = (OPCODES[0xf955] = new_opcode(
    0xf955,
    "bb_exchange_pd_item",
    undefined,
    [
        new_param(TYPE_DWORD, undefined, undefined),
        new_param(TYPE_DWORD, undefined, undefined),
        new_param(TYPE_DWORD, undefined, undefined),
        new_param(TYPE_I_LABEL, undefined, undefined),
        new_param(TYPE_I_LABEL, undefined, undefined),
    ],
    StackInteraction.Pop,
));
export const OP_BB_EXCHANGE_PD_SRANK = (OPCODES[0xf956] = new_opcode(
    0xf956,
    "bb_exchange_pd_srank",
    undefined,
    [
        new_param(TYPE_DWORD, undefined, undefined),
        new_param(TYPE_DWORD, undefined, undefined),
        new_param(TYPE_DWORD, undefined, undefined),
        new_param(TYPE_DWORD, undefined, undefined),
        new_param(TYPE_DWORD, undefined, undefined),
        new_param(TYPE_I_LABEL, undefined, undefined),
        new_param(TYPE_I_LABEL, undefined, undefined),
    ],
    StackInteraction.Pop,
));
export const OP_BB_EXCHANGE_PD_SPECIAL = (OPCODES[0xf957] = new_opcode(
    0xf957,
    "bb_exchange_pd_special",
    undefined,
    [
        new_param(TYPE_DWORD, undefined, undefined),
        new_param(TYPE_DWORD, undefined, undefined),
        new_param(TYPE_DWORD, undefined, undefined),
        new_param(TYPE_DWORD, undefined, undefined),
        new_param(TYPE_DWORD, undefined, undefined),
        new_param(TYPE_DWORD, undefined, undefined),
        new_param(TYPE_I_LABEL, undefined, undefined),
        new_param(TYPE_I_LABEL, undefined, undefined),
    ],
    StackInteraction.Pop,
));
export const OP_BB_EXCHANGE_PD_PERCENT = (OPCODES[0xf958] = new_opcode(
    0xf958,
    "bb_exchange_pd_percent",
    undefined,
    [
        new_param(TYPE_DWORD, undefined, undefined),
        new_param(TYPE_DWORD, undefined, undefined),
        new_param(TYPE_DWORD, undefined, undefined),
        new_param(TYPE_DWORD, undefined, undefined),
        new_param(TYPE_DWORD, undefined, undefined),
        new_param(TYPE_DWORD, undefined, undefined),
        new_param(TYPE_I_LABEL, undefined, undefined),
        new_param(TYPE_I_LABEL, undefined, undefined),
    ],
    StackInteraction.Pop,
));
export const OP_UNKNOWN_F959 = (OPCODES[0xf959] = new_opcode(
    0xf959,
    "unknown_f959",
    undefined,
    [new_param(TYPE_DWORD, undefined, undefined)],
    StackInteraction.Pop,
));
export const OP_UNKNOWN_F95A = (OPCODES[0xf95a] = new_opcode(
    0xf95a,
    "unknown_f95a",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F95B = (OPCODES[0xf95b] = new_opcode(
    0xf95b,
    "unknown_f95b",
    undefined,
    [],
    undefined,
));
export const OP_BB_EXCHANGE_SLT = (OPCODES[0xf95c] = new_opcode(
    0xf95c,
    "bb_exchange_slt",
    undefined,
    [
        new_param(TYPE_DWORD, undefined, undefined),
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_DWORD, undefined, ParamAccess.Write)],
            },
            undefined,
            undefined,
        ),
        new_param(TYPE_I_LABEL, undefined, undefined),
        new_param(TYPE_I_LABEL, undefined, undefined),
    ],
    StackInteraction.Pop,
));
export const OP_BB_EXCHANGE_PC = (OPCODES[0xf95d] = new_opcode(
    0xf95d,
    "bb_exchange_pc",
    undefined,
    [],
    undefined,
));
export const OP_BB_BOX_CREATE_BP = (OPCODES[0xf95e] = new_opcode(
    0xf95e,
    "bb_box_create_bp",
    undefined,
    [
        new_param(TYPE_DWORD, undefined, undefined),
        new_param(TYPE_FLOAT, undefined, undefined),
        new_param(TYPE_FLOAT, undefined, undefined),
    ],
    StackInteraction.Pop,
));
export const OP_BB_EXCHANGE_PT = (OPCODES[0xf95f] = new_opcode(
    0xf95f,
    "bb_exchange_pt",
    undefined,
    [
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_DWORD, undefined, ParamAccess.Write)],
            },
            undefined,
            undefined,
        ),
        new_param(
            {
                kind: Kind.RegTupRef,
                register_tuples: [new_param(TYPE_DWORD, undefined, ParamAccess.Write)],
            },
            undefined,
            undefined,
        ),
        new_param(TYPE_DWORD, undefined, undefined),
        new_param(TYPE_I_LABEL, undefined, undefined),
        new_param(TYPE_I_LABEL, undefined, undefined),
    ],
    StackInteraction.Pop,
));
export const OP_UNKNOWN_F960 = (OPCODES[0xf960] = new_opcode(
    0xf960,
    "unknown_f960",
    undefined,
    [new_param(TYPE_DWORD, undefined, undefined)],
    StackInteraction.Pop,
));
export const OP_UNKNOWN_F961 = (OPCODES[0xf961] = new_opcode(
    0xf961,
    "unknown_f961",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F962 = (OPCODES[0xf962] = new_opcode(
    0xf962,
    "unknown_f962",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F963 = (OPCODES[0xf963] = new_opcode(
    0xf963,
    "unknown_f963",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F964 = (OPCODES[0xf964] = new_opcode(
    0xf964,
    "unknown_f964",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F965 = (OPCODES[0xf965] = new_opcode(
    0xf965,
    "unknown_f965",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F966 = (OPCODES[0xf966] = new_opcode(
    0xf966,
    "unknown_f966",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F967 = (OPCODES[0xf967] = new_opcode(
    0xf967,
    "unknown_f967",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F968 = (OPCODES[0xf968] = new_opcode(
    0xf968,
    "unknown_f968",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F969 = (OPCODES[0xf969] = new_opcode(
    0xf969,
    "unknown_f969",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F96A = (OPCODES[0xf96a] = new_opcode(
    0xf96a,
    "unknown_f96a",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F96B = (OPCODES[0xf96b] = new_opcode(
    0xf96b,
    "unknown_f96b",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F96C = (OPCODES[0xf96c] = new_opcode(
    0xf96c,
    "unknown_f96c",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F96D = (OPCODES[0xf96d] = new_opcode(
    0xf96d,
    "unknown_f96d",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F96E = (OPCODES[0xf96e] = new_opcode(
    0xf96e,
    "unknown_f96e",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F96F = (OPCODES[0xf96f] = new_opcode(
    0xf96f,
    "unknown_f96f",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F970 = (OPCODES[0xf970] = new_opcode(
    0xf970,
    "unknown_f970",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F971 = (OPCODES[0xf971] = new_opcode(
    0xf971,
    "unknown_f971",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F972 = (OPCODES[0xf972] = new_opcode(
    0xf972,
    "unknown_f972",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F973 = (OPCODES[0xf973] = new_opcode(
    0xf973,
    "unknown_f973",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F974 = (OPCODES[0xf974] = new_opcode(
    0xf974,
    "unknown_f974",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F975 = (OPCODES[0xf975] = new_opcode(
    0xf975,
    "unknown_f975",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F976 = (OPCODES[0xf976] = new_opcode(
    0xf976,
    "unknown_f976",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F977 = (OPCODES[0xf977] = new_opcode(
    0xf977,
    "unknown_f977",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F978 = (OPCODES[0xf978] = new_opcode(
    0xf978,
    "unknown_f978",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F979 = (OPCODES[0xf979] = new_opcode(
    0xf979,
    "unknown_f979",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F97A = (OPCODES[0xf97a] = new_opcode(
    0xf97a,
    "unknown_f97a",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F97B = (OPCODES[0xf97b] = new_opcode(
    0xf97b,
    "unknown_f97b",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F97C = (OPCODES[0xf97c] = new_opcode(
    0xf97c,
    "unknown_f97c",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F97D = (OPCODES[0xf97d] = new_opcode(
    0xf97d,
    "unknown_f97d",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F97E = (OPCODES[0xf97e] = new_opcode(
    0xf97e,
    "unknown_f97e",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F97F = (OPCODES[0xf97f] = new_opcode(
    0xf97f,
    "unknown_f97f",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F980 = (OPCODES[0xf980] = new_opcode(
    0xf980,
    "unknown_f980",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F981 = (OPCODES[0xf981] = new_opcode(
    0xf981,
    "unknown_f981",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F982 = (OPCODES[0xf982] = new_opcode(
    0xf982,
    "unknown_f982",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F983 = (OPCODES[0xf983] = new_opcode(
    0xf983,
    "unknown_f983",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F984 = (OPCODES[0xf984] = new_opcode(
    0xf984,
    "unknown_f984",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F985 = (OPCODES[0xf985] = new_opcode(
    0xf985,
    "unknown_f985",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F986 = (OPCODES[0xf986] = new_opcode(
    0xf986,
    "unknown_f986",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F987 = (OPCODES[0xf987] = new_opcode(
    0xf987,
    "unknown_f987",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F988 = (OPCODES[0xf988] = new_opcode(
    0xf988,
    "unknown_f988",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F989 = (OPCODES[0xf989] = new_opcode(
    0xf989,
    "unknown_f989",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F98A = (OPCODES[0xf98a] = new_opcode(
    0xf98a,
    "unknown_f98a",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F98B = (OPCODES[0xf98b] = new_opcode(
    0xf98b,
    "unknown_f98b",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F98C = (OPCODES[0xf98c] = new_opcode(
    0xf98c,
    "unknown_f98c",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F98D = (OPCODES[0xf98d] = new_opcode(
    0xf98d,
    "unknown_f98d",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F98E = (OPCODES[0xf98e] = new_opcode(
    0xf98e,
    "unknown_f98e",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F98F = (OPCODES[0xf98f] = new_opcode(
    0xf98f,
    "unknown_f98f",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F990 = (OPCODES[0xf990] = new_opcode(
    0xf990,
    "unknown_f990",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F991 = (OPCODES[0xf991] = new_opcode(
    0xf991,
    "unknown_f991",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F992 = (OPCODES[0xf992] = new_opcode(
    0xf992,
    "unknown_f992",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F993 = (OPCODES[0xf993] = new_opcode(
    0xf993,
    "unknown_f993",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F994 = (OPCODES[0xf994] = new_opcode(
    0xf994,
    "unknown_f994",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F995 = (OPCODES[0xf995] = new_opcode(
    0xf995,
    "unknown_f995",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F996 = (OPCODES[0xf996] = new_opcode(
    0xf996,
    "unknown_f996",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F997 = (OPCODES[0xf997] = new_opcode(
    0xf997,
    "unknown_f997",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F998 = (OPCODES[0xf998] = new_opcode(
    0xf998,
    "unknown_f998",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F999 = (OPCODES[0xf999] = new_opcode(
    0xf999,
    "unknown_f999",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F99A = (OPCODES[0xf99a] = new_opcode(
    0xf99a,
    "unknown_f99a",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F99B = (OPCODES[0xf99b] = new_opcode(
    0xf99b,
    "unknown_f99b",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F99C = (OPCODES[0xf99c] = new_opcode(
    0xf99c,
    "unknown_f99c",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F99D = (OPCODES[0xf99d] = new_opcode(
    0xf99d,
    "unknown_f99d",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F99E = (OPCODES[0xf99e] = new_opcode(
    0xf99e,
    "unknown_f99e",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F99F = (OPCODES[0xf99f] = new_opcode(
    0xf99f,
    "unknown_f99f",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F9A0 = (OPCODES[0xf9a0] = new_opcode(
    0xf9a0,
    "unknown_f9a0",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F9A1 = (OPCODES[0xf9a1] = new_opcode(
    0xf9a1,
    "unknown_f9a1",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F9A2 = (OPCODES[0xf9a2] = new_opcode(
    0xf9a2,
    "unknown_f9a2",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F9A3 = (OPCODES[0xf9a3] = new_opcode(
    0xf9a3,
    "unknown_f9a3",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F9A4 = (OPCODES[0xf9a4] = new_opcode(
    0xf9a4,
    "unknown_f9a4",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F9A5 = (OPCODES[0xf9a5] = new_opcode(
    0xf9a5,
    "unknown_f9a5",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F9A6 = (OPCODES[0xf9a6] = new_opcode(
    0xf9a6,
    "unknown_f9a6",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F9A7 = (OPCODES[0xf9a7] = new_opcode(
    0xf9a7,
    "unknown_f9a7",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F9A8 = (OPCODES[0xf9a8] = new_opcode(
    0xf9a8,
    "unknown_f9a8",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F9A9 = (OPCODES[0xf9a9] = new_opcode(
    0xf9a9,
    "unknown_f9a9",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F9AA = (OPCODES[0xf9aa] = new_opcode(
    0xf9aa,
    "unknown_f9aa",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F9AB = (OPCODES[0xf9ab] = new_opcode(
    0xf9ab,
    "unknown_f9ab",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F9AC = (OPCODES[0xf9ac] = new_opcode(
    0xf9ac,
    "unknown_f9ac",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F9AD = (OPCODES[0xf9ad] = new_opcode(
    0xf9ad,
    "unknown_f9ad",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F9AE = (OPCODES[0xf9ae] = new_opcode(
    0xf9ae,
    "unknown_f9ae",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F9AF = (OPCODES[0xf9af] = new_opcode(
    0xf9af,
    "unknown_f9af",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F9B0 = (OPCODES[0xf9b0] = new_opcode(
    0xf9b0,
    "unknown_f9b0",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F9B1 = (OPCODES[0xf9b1] = new_opcode(
    0xf9b1,
    "unknown_f9b1",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F9B2 = (OPCODES[0xf9b2] = new_opcode(
    0xf9b2,
    "unknown_f9b2",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F9B3 = (OPCODES[0xf9b3] = new_opcode(
    0xf9b3,
    "unknown_f9b3",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F9B4 = (OPCODES[0xf9b4] = new_opcode(
    0xf9b4,
    "unknown_f9b4",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F9B5 = (OPCODES[0xf9b5] = new_opcode(
    0xf9b5,
    "unknown_f9b5",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F9B6 = (OPCODES[0xf9b6] = new_opcode(
    0xf9b6,
    "unknown_f9b6",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F9B7 = (OPCODES[0xf9b7] = new_opcode(
    0xf9b7,
    "unknown_f9b7",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F9B8 = (OPCODES[0xf9b8] = new_opcode(
    0xf9b8,
    "unknown_f9b8",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F9B9 = (OPCODES[0xf9b9] = new_opcode(
    0xf9b9,
    "unknown_f9b9",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F9BA = (OPCODES[0xf9ba] = new_opcode(
    0xf9ba,
    "unknown_f9ba",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F9BB = (OPCODES[0xf9bb] = new_opcode(
    0xf9bb,
    "unknown_f9bb",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F9BC = (OPCODES[0xf9bc] = new_opcode(
    0xf9bc,
    "unknown_f9bc",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F9BD = (OPCODES[0xf9bd] = new_opcode(
    0xf9bd,
    "unknown_f9bd",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F9BE = (OPCODES[0xf9be] = new_opcode(
    0xf9be,
    "unknown_f9be",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F9BF = (OPCODES[0xf9bf] = new_opcode(
    0xf9bf,
    "unknown_f9bf",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F9C0 = (OPCODES[0xf9c0] = new_opcode(
    0xf9c0,
    "unknown_f9c0",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F9C1 = (OPCODES[0xf9c1] = new_opcode(
    0xf9c1,
    "unknown_f9c1",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F9C2 = (OPCODES[0xf9c2] = new_opcode(
    0xf9c2,
    "unknown_f9c2",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F9C3 = (OPCODES[0xf9c3] = new_opcode(
    0xf9c3,
    "unknown_f9c3",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F9C4 = (OPCODES[0xf9c4] = new_opcode(
    0xf9c4,
    "unknown_f9c4",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F9C5 = (OPCODES[0xf9c5] = new_opcode(
    0xf9c5,
    "unknown_f9c5",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F9C6 = (OPCODES[0xf9c6] = new_opcode(
    0xf9c6,
    "unknown_f9c6",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F9C7 = (OPCODES[0xf9c7] = new_opcode(
    0xf9c7,
    "unknown_f9c7",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F9C8 = (OPCODES[0xf9c8] = new_opcode(
    0xf9c8,
    "unknown_f9c8",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F9C9 = (OPCODES[0xf9c9] = new_opcode(
    0xf9c9,
    "unknown_f9c9",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F9CA = (OPCODES[0xf9ca] = new_opcode(
    0xf9ca,
    "unknown_f9ca",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F9CB = (OPCODES[0xf9cb] = new_opcode(
    0xf9cb,
    "unknown_f9cb",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F9CC = (OPCODES[0xf9cc] = new_opcode(
    0xf9cc,
    "unknown_f9cc",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F9CD = (OPCODES[0xf9cd] = new_opcode(
    0xf9cd,
    "unknown_f9cd",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F9CE = (OPCODES[0xf9ce] = new_opcode(
    0xf9ce,
    "unknown_f9ce",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F9CF = (OPCODES[0xf9cf] = new_opcode(
    0xf9cf,
    "unknown_f9cf",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F9D0 = (OPCODES[0xf9d0] = new_opcode(
    0xf9d0,
    "unknown_f9d0",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F9D1 = (OPCODES[0xf9d1] = new_opcode(
    0xf9d1,
    "unknown_f9d1",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F9D2 = (OPCODES[0xf9d2] = new_opcode(
    0xf9d2,
    "unknown_f9d2",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F9D3 = (OPCODES[0xf9d3] = new_opcode(
    0xf9d3,
    "unknown_f9d3",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F9D4 = (OPCODES[0xf9d4] = new_opcode(
    0xf9d4,
    "unknown_f9d4",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F9D5 = (OPCODES[0xf9d5] = new_opcode(
    0xf9d5,
    "unknown_f9d5",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F9D6 = (OPCODES[0xf9d6] = new_opcode(
    0xf9d6,
    "unknown_f9d6",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F9D7 = (OPCODES[0xf9d7] = new_opcode(
    0xf9d7,
    "unknown_f9d7",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F9D8 = (OPCODES[0xf9d8] = new_opcode(
    0xf9d8,
    "unknown_f9d8",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F9D9 = (OPCODES[0xf9d9] = new_opcode(
    0xf9d9,
    "unknown_f9d9",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F9DA = (OPCODES[0xf9da] = new_opcode(
    0xf9da,
    "unknown_f9da",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F9DB = (OPCODES[0xf9db] = new_opcode(
    0xf9db,
    "unknown_f9db",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F9DC = (OPCODES[0xf9dc] = new_opcode(
    0xf9dc,
    "unknown_f9dc",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F9DD = (OPCODES[0xf9dd] = new_opcode(
    0xf9dd,
    "unknown_f9dd",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F9DE = (OPCODES[0xf9de] = new_opcode(
    0xf9de,
    "unknown_f9de",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F9DF = (OPCODES[0xf9df] = new_opcode(
    0xf9df,
    "unknown_f9df",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F9E0 = (OPCODES[0xf9e0] = new_opcode(
    0xf9e0,
    "unknown_f9e0",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F9E1 = (OPCODES[0xf9e1] = new_opcode(
    0xf9e1,
    "unknown_f9e1",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F9E2 = (OPCODES[0xf9e2] = new_opcode(
    0xf9e2,
    "unknown_f9e2",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F9E3 = (OPCODES[0xf9e3] = new_opcode(
    0xf9e3,
    "unknown_f9e3",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F9E4 = (OPCODES[0xf9e4] = new_opcode(
    0xf9e4,
    "unknown_f9e4",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F9E5 = (OPCODES[0xf9e5] = new_opcode(
    0xf9e5,
    "unknown_f9e5",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F9E6 = (OPCODES[0xf9e6] = new_opcode(
    0xf9e6,
    "unknown_f9e6",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F9E7 = (OPCODES[0xf9e7] = new_opcode(
    0xf9e7,
    "unknown_f9e7",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F9E8 = (OPCODES[0xf9e8] = new_opcode(
    0xf9e8,
    "unknown_f9e8",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F9E9 = (OPCODES[0xf9e9] = new_opcode(
    0xf9e9,
    "unknown_f9e9",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F9EA = (OPCODES[0xf9ea] = new_opcode(
    0xf9ea,
    "unknown_f9ea",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F9EB = (OPCODES[0xf9eb] = new_opcode(
    0xf9eb,
    "unknown_f9eb",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F9EC = (OPCODES[0xf9ec] = new_opcode(
    0xf9ec,
    "unknown_f9ec",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F9ED = (OPCODES[0xf9ed] = new_opcode(
    0xf9ed,
    "unknown_f9ed",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F9EE = (OPCODES[0xf9ee] = new_opcode(
    0xf9ee,
    "unknown_f9ee",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F9EF = (OPCODES[0xf9ef] = new_opcode(
    0xf9ef,
    "unknown_f9ef",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F9F0 = (OPCODES[0xf9f0] = new_opcode(
    0xf9f0,
    "unknown_f9f0",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F9F1 = (OPCODES[0xf9f1] = new_opcode(
    0xf9f1,
    "unknown_f9f1",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F9F2 = (OPCODES[0xf9f2] = new_opcode(
    0xf9f2,
    "unknown_f9f2",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F9F3 = (OPCODES[0xf9f3] = new_opcode(
    0xf9f3,
    "unknown_f9f3",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F9F4 = (OPCODES[0xf9f4] = new_opcode(
    0xf9f4,
    "unknown_f9f4",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F9F5 = (OPCODES[0xf9f5] = new_opcode(
    0xf9f5,
    "unknown_f9f5",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F9F6 = (OPCODES[0xf9f6] = new_opcode(
    0xf9f6,
    "unknown_f9f6",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F9F7 = (OPCODES[0xf9f7] = new_opcode(
    0xf9f7,
    "unknown_f9f7",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F9F8 = (OPCODES[0xf9f8] = new_opcode(
    0xf9f8,
    "unknown_f9f8",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F9F9 = (OPCODES[0xf9f9] = new_opcode(
    0xf9f9,
    "unknown_f9f9",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F9FA = (OPCODES[0xf9fa] = new_opcode(
    0xf9fa,
    "unknown_f9fa",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F9FB = (OPCODES[0xf9fb] = new_opcode(
    0xf9fb,
    "unknown_f9fb",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F9FC = (OPCODES[0xf9fc] = new_opcode(
    0xf9fc,
    "unknown_f9fc",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F9FD = (OPCODES[0xf9fd] = new_opcode(
    0xf9fd,
    "unknown_f9fd",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F9FE = (OPCODES[0xf9fe] = new_opcode(
    0xf9fe,
    "unknown_f9fe",
    undefined,
    [],
    undefined,
));
export const OP_UNKNOWN_F9FF = (OPCODES[0xf9ff] = new_opcode(
    0xf9ff,
    "unknown_f9ff",
    undefined,
    [],
    undefined,
));
// !!! GENERATED_CODE_END !!!

OPCODES.forEach(opcode => {
    OPCODES_BY_MNEMONIC.set(opcode.mnemonic, opcode);
});

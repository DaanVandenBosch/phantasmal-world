/**
 * Instruction parameter types.
 */
export enum Type {
    /**
     * Unsigned 8-bit integer.
     */
    U8,
    /**
     * Unsigned 16-bit integer.
     */
    U16,
    /**
     * Unsigned 32-bit integer.
     */
    U32,
    /**
     * Signed 32-bit integer.
     */
    I32,
    /**
     * 32-Bit floating point number.
     */
    F32,
    /**
     * Register reference.
     */
    Register,
    /**
     * Named reference to an instruction.
     */
    ILabel,
    /**
     * Named reference to a data segment.
     */
    DLabel,
    /**
     * Arbitrary amount of U8 arguments.
     */
    U8Var,
    /**
     * Arbitrary amount of ILabel arguments.
     */
    ILabelVar,
    /**
     * String of arbitrary size.
     */
    String,
}

export type Param = {
    type: Type;
};

export const OPCODES: Opcode[] = [];

/**
 * Script object code instruction. Invoked by {@link ../bin/Instruction}s.
 */
export class Opcode {
    /**
     * Byte size of the instruction code, either 1 or 2.
     */
    readonly size: number;

    constructor(
        /**
         * 1- Or 2-byte instruction code used to invoke this opcode.
         */
        readonly code: number,
        readonly mnemonic: string,
        /**
         * Directly passed in arguments.
         */
        readonly params: Param[],
        /**
         * If true, this opcode pushes arguments onto the stack.
         */
        readonly push_stack: boolean,
        /**
         * Arguments passed in via the stack.
         * These arguments are popped from the stack after the opcode has executed.
         */
        readonly stack_params: Param[]
    ) {
        this.size = this.code < 256 ? 1 : 2;
    }

    static readonly nop = (OPCODES[0x00] = new Opcode(0x00, "nop", [], false, []));
    static readonly ret = (OPCODES[0x01] = new Opcode(0x01, "ret", [], false, []));
    static readonly sync = (OPCODES[0x02] = new Opcode(0x02, "sync", [], false, []));
    static readonly exit = (OPCODES[0x03] = new Opcode(0x03, "exit", [], false, [
        { type: Type.U32 },
    ]));
    static readonly thread = (OPCODES[0x04] = new Opcode(
        0x04,
        "thread",
        [{ type: Type.ILabel }],
        false,
        []
    ));
    static readonly va_start = (OPCODES[0x05] = new Opcode(0x05, "va_start", [], false, []));
    static readonly va_end = (OPCODES[0x06] = new Opcode(0x06, "va_end", [], false, []));
    static readonly va_call = (OPCODES[0x07] = new Opcode(
        0x07,
        "va_call",
        [{ type: Type.ILabel }],
        false,
        []
    ));
    static readonly let = (OPCODES[0x08] = new Opcode(
        0x08,
        "let",
        [{ type: Type.Register }, { type: Type.Register }],
        false,
        []
    ));
    static readonly leti = (OPCODES[0x09] = new Opcode(
        0x09,
        "leti",
        [{ type: Type.Register }, { type: Type.I32 }],
        false,
        []
    ));
    static readonly letb = (OPCODES[0x0a] = new Opcode(
        0x0a,
        "letb",
        [{ type: Type.Register }, { type: Type.U8 }],
        false,
        []
    ));
    static readonly letw = (OPCODES[0x0b] = new Opcode(
        0x0b,
        "letw",
        [{ type: Type.Register }, { type: Type.U16 }],
        false,
        []
    ));
    static readonly leta = (OPCODES[0x0c] = new Opcode(
        0x0c,
        "leta",
        [{ type: Type.Register }, { type: Type.Register }],
        false,
        []
    ));
    static readonly leto = (OPCODES[0x0d] = new Opcode(
        0x0d,
        "leto",
        [{ type: Type.Register }, { type: Type.U16 /* ILabel or DLabel */ }],
        false,
        []
    ));
    static readonly unknown_0e = (OPCODES[0x0e] = new Opcode(0x0e, "unknown_0e", [], false, []));
    static readonly unknown_0f = (OPCODES[0x0f] = new Opcode(0x0f, "unknown_0f", [], false, []));
    static readonly set = (OPCODES[0x10] = new Opcode(
        0x10,
        "set",
        [{ type: Type.Register }],
        false,
        []
    ));
    static readonly clear = (OPCODES[0x11] = new Opcode(
        0x11,
        "clear",
        [{ type: Type.Register }],
        false,
        []
    ));
    static readonly rev = (OPCODES[0x12] = new Opcode(
        0x12,
        "rev",
        [{ type: Type.Register }],
        false,
        []
    ));
    static readonly gset = (OPCODES[0x13] = new Opcode(
        0x13,
        "gset",
        [{ type: Type.U16 }],
        false,
        []
    ));
    static readonly gclear = (OPCODES[0x14] = new Opcode(
        0x14,
        "gclear",
        [{ type: Type.U16 }],
        false,
        []
    ));
    static readonly grev = (OPCODES[0x15] = new Opcode(
        0x15,
        "grev",
        [{ type: Type.U16 }],
        false,
        []
    ));
    static readonly glet = (OPCODES[0x16] = new Opcode(
        0x16,
        "glet",
        [{ type: Type.U16 }],
        false,
        []
    ));
    static readonly gget = (OPCODES[0x17] = new Opcode(
        0x17,
        "gget",
        [{ type: Type.U16 }, { type: Type.Register }],
        false,
        []
    ));
    static readonly add = (OPCODES[0x18] = new Opcode(
        0x18,
        "add",
        [{ type: Type.Register }, { type: Type.Register }],
        false,
        []
    ));
    static readonly addi = (OPCODES[0x19] = new Opcode(
        0x19,
        "addi",
        [{ type: Type.Register }, { type: Type.I32 }],
        false,
        []
    ));
    static readonly sub = (OPCODES[0x1a] = new Opcode(
        0x1a,
        "sub",
        [{ type: Type.Register }, { type: Type.Register }],
        false,
        []
    ));
    static readonly subi = (OPCODES[0x1b] = new Opcode(
        0x1b,
        "subi",
        [{ type: Type.Register }, { type: Type.I32 }],
        false,
        []
    ));
    static readonly mul = (OPCODES[0x1c] = new Opcode(
        0x1c,
        "mul",
        [{ type: Type.Register }, { type: Type.Register }],
        false,
        []
    ));
    static readonly muli = (OPCODES[0x1d] = new Opcode(
        0x1d,
        "muli",
        [{ type: Type.Register }, { type: Type.I32 }],
        false,
        []
    ));
    static readonly div = (OPCODES[0x1e] = new Opcode(
        0x1e,
        "div",
        [{ type: Type.Register }, { type: Type.Register }],
        false,
        []
    ));
    static readonly divi = (OPCODES[0x1f] = new Opcode(
        0x1f,
        "divi",
        [{ type: Type.Register }, { type: Type.I32 }],
        false,
        []
    ));
    static readonly and = (OPCODES[0x20] = new Opcode(
        0x20,
        "and",
        [{ type: Type.Register }, { type: Type.Register }],
        false,
        []
    ));
    static readonly andi = (OPCODES[0x21] = new Opcode(
        0x21,
        "andi",
        [{ type: Type.Register }, { type: Type.I32 }],
        false,
        []
    ));
    static readonly or = (OPCODES[0x22] = new Opcode(
        0x22,
        "or",
        [{ type: Type.Register }, { type: Type.Register }],
        false,
        []
    ));
    static readonly ori = (OPCODES[0x23] = new Opcode(
        0x23,
        "ori",
        [{ type: Type.Register }, { type: Type.I32 }],
        false,
        []
    ));
    static readonly xor = (OPCODES[0x24] = new Opcode(
        0x24,
        "xor",
        [{ type: Type.Register }, { type: Type.Register }],
        false,
        []
    ));
    static readonly xori = (OPCODES[0x25] = new Opcode(
        0x25,
        "xori",
        [{ type: Type.Register }, { type: Type.I32 }],
        false,
        []
    ));
    static readonly mod = (OPCODES[0x26] = new Opcode(
        0x26,
        "mod",
        [{ type: Type.Register }, { type: Type.Register }],
        false,
        []
    ));
    static readonly modi = (OPCODES[0x27] = new Opcode(
        0x27,
        "modi",
        [{ type: Type.Register }, { type: Type.I32 }],
        false,
        []
    ));
    static readonly jmp = (OPCODES[0x28] = new Opcode(
        0x28,
        "jmp",
        [{ type: Type.ILabel }],
        false,
        []
    ));
    static readonly call = (OPCODES[0x29] = new Opcode(
        0x29,
        "call",
        [{ type: Type.ILabel }],
        false,
        []
    ));
    static readonly jmp_on = (OPCODES[0x2a] = new Opcode(
        0x2a,
        "jmp_on",
        [{ type: Type.ILabel }, { type: Type.U8Var }],
        false,
        []
    ));
    static readonly jmp_off = (OPCODES[0x2b] = new Opcode(
        0x2b,
        "jmp_off",
        [{ type: Type.ILabel }, { type: Type.U8Var }],
        false,
        []
    ));
    static readonly jmp_e = (OPCODES[0x2c] = new Opcode(
        0x2c,
        "jmp_=",
        [{ type: Type.Register }, { type: Type.Register }, { type: Type.ILabel }],
        false,
        []
    ));
    static readonly jmpi_e = (OPCODES[0x2d] = new Opcode(
        0x2d,
        "jmpi_=",
        [{ type: Type.Register }, { type: Type.I32 }, { type: Type.ILabel }],
        false,
        []
    ));
    static readonly jmp_ne = (OPCODES[0x2e] = new Opcode(
        0x2e,
        "jmp_!=",
        [{ type: Type.Register }, { type: Type.Register }, { type: Type.ILabel }],
        false,
        []
    ));
    static readonly jmpi_ne = (OPCODES[0x2f] = new Opcode(
        0x2f,
        "jmpi_!=",
        [{ type: Type.Register }, { type: Type.I32 }, { type: Type.ILabel }],
        false,
        []
    ));
    static readonly ujmp_g = (OPCODES[0x30] = new Opcode(
        0x30,
        "ujmp_>",
        [{ type: Type.Register }, { type: Type.Register }, { type: Type.ILabel }],
        false,
        []
    ));
    static readonly ujmpi_g = (OPCODES[0x31] = new Opcode(
        0x31,
        "ujmpi_>",
        [{ type: Type.Register }, { type: Type.U32 }, { type: Type.ILabel }],
        false,
        []
    ));
    static readonly jmp_g = (OPCODES[0x32] = new Opcode(
        0x32,
        "jmp_>",
        [{ type: Type.Register }, { type: Type.Register }, { type: Type.ILabel }],
        false,
        []
    ));
    static readonly jmpi_g = (OPCODES[0x33] = new Opcode(
        0x33,
        "jmpi_>",
        [{ type: Type.Register }, { type: Type.I32 }, { type: Type.ILabel }],
        false,
        []
    ));
    static readonly ujmp_l = (OPCODES[0x34] = new Opcode(
        0x34,
        "ujmp_<",
        [{ type: Type.Register }, { type: Type.Register }, { type: Type.ILabel }],
        false,
        []
    ));
    static readonly ujmpi_l = (OPCODES[0x35] = new Opcode(
        0x35,
        "ujmpi_<",
        [{ type: Type.Register }, { type: Type.U32 }, { type: Type.ILabel }],
        false,
        []
    ));
    static readonly jmp_l = (OPCODES[0x36] = new Opcode(
        0x36,
        "jmp_<",
        [{ type: Type.Register }, { type: Type.Register }, { type: Type.ILabel }],
        false,
        []
    ));
    static readonly jmpi_l = (OPCODES[0x37] = new Opcode(
        0x37,
        "jmpi_<",
        [{ type: Type.Register }, { type: Type.I32 }, { type: Type.ILabel }],
        false,
        []
    ));
    static readonly ujmp_ge = (OPCODES[0x38] = new Opcode(
        0x38,
        "ujmp_>=",
        [{ type: Type.Register }, { type: Type.Register }, { type: Type.ILabel }],
        false,
        []
    ));
    static readonly ujmpi_ge = (OPCODES[0x39] = new Opcode(
        0x39,
        "ujmpi_>=",
        [{ type: Type.Register }, { type: Type.U32 }, { type: Type.ILabel }],
        false,
        []
    ));
    static readonly jmp_ge = (OPCODES[0x3a] = new Opcode(
        0x3a,
        "jmp_>=",
        [{ type: Type.Register }, { type: Type.Register }, { type: Type.ILabel }],
        false,
        []
    ));
    static readonly jmpi_ge = (OPCODES[0x3b] = new Opcode(
        0x3b,
        "jmpi_>=",
        [{ type: Type.Register }, { type: Type.I32 }, { type: Type.ILabel }],
        false,
        []
    ));
    static readonly ujmp_le = (OPCODES[0x3c] = new Opcode(
        0x3c,
        "ujmp_<=",
        [{ type: Type.Register }, { type: Type.Register }, { type: Type.ILabel }],
        false,
        []
    ));
    static readonly ujmpi_le = (OPCODES[0x3d] = new Opcode(
        0x3d,
        "ujmpi_<=",
        [{ type: Type.Register }, { type: Type.U32 }, { type: Type.ILabel }],
        false,
        []
    ));
    static readonly jmp_le = (OPCODES[0x3e] = new Opcode(
        0x3e,
        "jmp_<=",
        [{ type: Type.Register }, { type: Type.Register }, { type: Type.ILabel }],
        false,
        []
    ));
    static readonly jmpi_le = (OPCODES[0x3f] = new Opcode(
        0x3f,
        "jmpi_<=",
        [{ type: Type.Register }, { type: Type.I32 }, { type: Type.ILabel }],
        false,
        []
    ));
    static readonly switch_jmp = (OPCODES[0x40] = new Opcode(
        0x40,
        "switch_jmp",
        [{ type: Type.Register }, { type: Type.ILabelVar }],
        false,
        []
    ));
    static readonly switch_call = (OPCODES[0x41] = new Opcode(
        0x41,
        "switch_call",
        [{ type: Type.Register }, { type: Type.ILabelVar }],
        false,
        []
    ));
    static readonly stack_push = (OPCODES[0x42] = new Opcode(
        0x42,
        "stack_push",
        [{ type: Type.Register }],
        false,
        []
    ));
    static readonly stack_pop = (OPCODES[0x43] = new Opcode(
        0x43,
        "stack_pop",
        [{ type: Type.Register }],
        false,
        []
    ));
    static readonly stack_pushm = (OPCODES[0x44] = new Opcode(
        0x44,
        "stack_pushm",
        [{ type: Type.Register }, { type: Type.U32 }],
        false,
        []
    ));
    static readonly stack_popm = (OPCODES[0x45] = new Opcode(
        0x45,
        "stack_popm",
        [{ type: Type.Register }, { type: Type.U32 }],
        false,
        []
    ));
    static readonly unknown_46 = (OPCODES[0x46] = new Opcode(0x46, "unknown_46", [], false, []));
    static readonly unknown_47 = (OPCODES[0x47] = new Opcode(0x47, "unknown_47", [], false, []));
    static readonly arg_pushr = (OPCODES[0x48] = new Opcode(
        0x48,
        "arg_pushr",
        [{ type: Type.Register }],
        true,
        []
    ));
    static readonly arg_pushl = (OPCODES[0x49] = new Opcode(
        0x49,
        "arg_pushl",
        [{ type: Type.I32 }],
        true,
        []
    ));
    static readonly arg_pushb = (OPCODES[0x4a] = new Opcode(
        0x4a,
        "arg_pushb",
        [{ type: Type.U8 }],
        true,
        []
    ));
    static readonly arg_pushw = (OPCODES[0x4b] = new Opcode(
        0x4b,
        "arg_pushw",
        [{ type: Type.U16 }],
        true,
        []
    ));
    static readonly unknown_4c = (OPCODES[0x4c] = new Opcode(0x4c, "unknown_4c", [], false, []));
    static readonly unknown_4d = (OPCODES[0x4d] = new Opcode(0x4d, "unknown_4d", [], false, []));
    static readonly arg_pushs = (OPCODES[0x4e] = new Opcode(
        0x4e,
        "arg_pushs",
        [{ type: Type.String }],
        true,
        []
    ));
    static readonly unknown_4f = (OPCODES[0x4f] = new Opcode(
        0x4f,
        "unknown_4f",
        [{ type: Type.Register }, { type: Type.Register }],
        false,
        []
    ));
    static readonly message = (OPCODES[0x50] = new Opcode(0x50, "message", [], false, [
        { type: Type.U32 },
        { type: Type.String },
    ]));
    static readonly list = (OPCODES[0x51] = new Opcode(0x51, "list", [], false, [
        { type: Type.Register },
        { type: Type.String },
    ]));
    static readonly fadein = (OPCODES[0x52] = new Opcode(0x52, "fadein", [], false, []));
    static readonly fadeout = (OPCODES[0x53] = new Opcode(0x53, "fadeout", [], false, []));
    static readonly se = (OPCODES[0x54] = new Opcode(0x54, "se", [], false, [{ type: Type.U32 }]));
    static readonly bgm = (OPCODES[0x55] = new Opcode(0x55, "bgm", [], false, [
        { type: Type.U32 },
    ]));
    static readonly unknown_56 = (OPCODES[0x56] = new Opcode(0x56, "unknown_56", [], false, []));
    static readonly unknown_57 = (OPCODES[0x57] = new Opcode(0x57, "unknown_57", [], false, []));
    static readonly enable = (OPCODES[0x58] = new Opcode(0x58, "enable", [], false, [
        { type: Type.U32 },
    ]));
    static readonly disable = (OPCODES[0x59] = new Opcode(0x59, "disable", [], false, [
        { type: Type.U32 },
    ]));
    static readonly window_msg = (OPCODES[0x5a] = new Opcode(0x5a, "window_msg", [], false, [
        { type: Type.String },
    ]));
    static readonly add_msg = (OPCODES[0x5b] = new Opcode(0x5b, "add_msg", [], false, [
        { type: Type.String },
    ]));
    static readonly mesend = (OPCODES[0x5c] = new Opcode(0x5c, "mesend", [], false, []));
    static readonly gettime = (OPCODES[0x5d] = new Opcode(
        0x5d,
        "gettime",
        [{ type: Type.Register }],
        false,
        []
    ));
    static readonly winend = (OPCODES[0x5e] = new Opcode(0x5e, "winend", [], false, []));
    static readonly unknown_5f = (OPCODES[0x5f] = new Opcode(0x5f, "unknown_5f", [], false, []));
    static readonly npc_crt_v3 = (OPCODES[0x60] = new Opcode(
        0x60,
        "npc_crt_v3",
        [{ type: Type.Register }],
        false,
        []
    ));
    static readonly npc_stop = (OPCODES[0x61] = new Opcode(0x61, "npc_stop", [], false, [
        { type: Type.Register },
    ]));
    static readonly npc_play = (OPCODES[0x62] = new Opcode(0x62, "npc_play", [], false, [
        { type: Type.U32 },
    ]));
    static readonly npc_kill = (OPCODES[0x63] = new Opcode(0x63, "npc_kill", [], false, [
        { type: Type.Register },
    ]));
    static readonly npc_nont = (OPCODES[0x64] = new Opcode(0x64, "npc_nont", [], false, []));
    static readonly npc_talk = (OPCODES[0x65] = new Opcode(0x65, "npc_talk", [], false, []));
    static readonly npc_crp_v3 = (OPCODES[0x66] = new Opcode(
        0x66,
        "npc_crp_v3",
        [{ type: Type.Register }],
        false,
        []
    ));
    static readonly unknown_67 = (OPCODES[0x67] = new Opcode(0x67, "unknown_67", [], false, []));
    static readonly create_pipe = (OPCODES[0x68] = new Opcode(0x68, "create_pipe", [], false, [
        { type: Type.U32 },
    ]));
    static readonly p_hpstat_v3 = (OPCODES[0x69] = new Opcode(0x69, "p_hpstat_v3", [], false, [
        { type: Type.Register },
        { type: Type.U32 },
    ]));
    static readonly p_dead_v3 = (OPCODES[0x6a] = new Opcode(0x6a, "p_dead_v3", [], false, [
        { type: Type.Register },
        { type: Type.U32 },
    ]));
    static readonly p_disablewarp = (OPCODES[0x6b] = new Opcode(
        0x6b,
        "p_disablewarp",
        [],
        false,
        []
    ));
    static readonly p_enablewarp = (OPCODES[0x6c] = new Opcode(
        0x6c,
        "p_enablewarp",
        [],
        false,
        []
    ));
    static readonly p_move_v3 = (OPCODES[0x6d] = new Opcode(
        0x6d,
        "p_move_v3",
        [{ type: Type.Register }],
        false,
        []
    ));
    static readonly p_look = (OPCODES[0x6e] = new Opcode(0x6e, "p_look", [], false, [
        { type: Type.U32 },
    ]));
    static readonly unknown_6f = (OPCODES[0x6f] = new Opcode(0x6f, "unknown_6f", [], false, []));
    static readonly p_action_disable = (OPCODES[0x70] = new Opcode(
        0x70,
        "p_action_disable",
        [],
        false,
        []
    ));
    static readonly p_action_enable = (OPCODES[0x71] = new Opcode(
        0x71,
        "p_action_enable",
        [],
        false,
        []
    ));
    static readonly disable_movement1 = (OPCODES[0x72] = new Opcode(
        0x72,
        "disable_movement1",
        [],
        false,
        [{ type: Type.Register }]
    ));
    static readonly enable_movement1 = (OPCODES[0x73] = new Opcode(
        0x73,
        "enable_movement1",
        [],
        false,
        [{ type: Type.Register }]
    ));
    static readonly p_noncol = (OPCODES[0x74] = new Opcode(0x74, "p_noncol", [], false, []));
    static readonly p_col = (OPCODES[0x75] = new Opcode(0x75, "p_col", [], false, []));
    static readonly p_setpos = (OPCODES[0x76] = new Opcode(0x76, "p_setpos", [], false, [
        { type: Type.U32 },
        { type: Type.Register },
    ]));
    static readonly p_return_guild = (OPCODES[0x77] = new Opcode(
        0x77,
        "p_return_guild",
        [],
        false,
        []
    ));
    static readonly p_talk_guild = (OPCODES[0x78] = new Opcode(0x78, "p_talk_guild", [], false, [
        { type: Type.U32 },
    ]));
    static readonly npc_talk_pl_v3 = (OPCODES[0x79] = new Opcode(
        0x79,
        "npc_talk_pl_v3",
        [{ type: Type.Register }],
        false,
        []
    ));
    static readonly npc_talk_kill = (OPCODES[0x7a] = new Opcode(0x7a, "npc_talk_kill", [], false, [
        { type: Type.U32 },
    ]));
    static readonly npc_crtpk_v3 = (OPCODES[0x7b] = new Opcode(
        0x7b,
        "npc_crtpk_v3",
        [{ type: Type.Register }],
        false,
        []
    ));
    static readonly npc_crppk_v3 = (OPCODES[0x7c] = new Opcode(
        0x7c,
        "npc_crppk_v3",
        [{ type: Type.Register }],
        false,
        []
    ));
    static readonly npc_crptalk_v3 = (OPCODES[0x7d] = new Opcode(
        0x7d,
        "npc_crptalk_v3",
        [{ type: Type.Register }],
        false,
        []
    ));
    static readonly p_look_at_v1 = (OPCODES[0x7e] = new Opcode(0x7e, "p_look_at_v1", [], false, [
        { type: Type.U32 },
        { type: Type.U32 },
    ]));
    static readonly npc_crp_id_v3 = (OPCODES[0x7f] = new Opcode(
        0x7f,
        "npc_crp_id_v3",
        [{ type: Type.Register }],
        false,
        []
    ));
    static readonly cam_quake = (OPCODES[0x80] = new Opcode(0x80, "cam_quake", [], false, []));
    static readonly cam_adj = (OPCODES[0x81] = new Opcode(0x81, "cam_adj", [], false, []));
    static readonly cam_zmin = (OPCODES[0x82] = new Opcode(0x82, "cam_zmin", [], false, []));
    static readonly cam_zmout = (OPCODES[0x83] = new Opcode(0x83, "cam_zmout", [], false, []));
    static readonly cam_pan_v3 = (OPCODES[0x84] = new Opcode(
        0x84,
        "cam_pan_v3",
        [{ type: Type.Register }],
        false,
        []
    ));
    static readonly game_lev_super = (OPCODES[0x85] = new Opcode(
        0x85,
        "game_lev_super",
        [],
        false,
        []
    ));
    static readonly game_lev_reset = (OPCODES[0x86] = new Opcode(
        0x86,
        "game_lev_reset",
        [],
        false,
        []
    ));
    static readonly pos_pipe_v3 = (OPCODES[0x87] = new Opcode(
        0x87,
        "pos_pipe_v3",
        [{ type: Type.Register }],
        false,
        []
    ));
    static readonly if_zone_clear = (OPCODES[0x88] = new Opcode(
        0x88,
        "if_zone_clear",
        [{ type: Type.Register }, { type: Type.Register }],
        false,
        []
    ));
    static readonly chk_ene_num = (OPCODES[0x89] = new Opcode(
        0x89,
        "chk_ene_num",
        [{ type: Type.Register }],
        false,
        []
    ));
    static readonly unhide_obj = (OPCODES[0x8a] = new Opcode(
        0x8a,
        "unhide_obj",
        [{ type: Type.Register }],
        false,
        []
    ));
    static readonly unhide_ene = (OPCODES[0x8b] = new Opcode(
        0x8b,
        "unhide_ene",
        [{ type: Type.Register }],
        false,
        []
    ));
    static readonly at_coords_call = (OPCODES[0x8c] = new Opcode(
        0x8c,
        "at_coords_call",
        [{ type: Type.Register }],
        false,
        []
    ));
    static readonly at_coords_talk = (OPCODES[0x8d] = new Opcode(
        0x8d,
        "at_coords_talk",
        [{ type: Type.Register }],
        false,
        []
    ));
    static readonly col_npcin = (OPCODES[0x8e] = new Opcode(
        0x8e,
        "col_npcin",
        [{ type: Type.Register }],
        false,
        []
    ));
    static readonly col_npcinr = (OPCODES[0x8f] = new Opcode(
        0x8f,
        "col_npcinr",
        [{ type: Type.Register }],
        false,
        []
    ));
    static readonly switch_on = (OPCODES[0x90] = new Opcode(0x90, "switch_on", [], false, [
        { type: Type.U32 },
    ]));
    static readonly switch_off = (OPCODES[0x91] = new Opcode(0x91, "switch_off", [], false, [
        { type: Type.U32 },
    ]));
    static readonly playbgm_epi = (OPCODES[0x92] = new Opcode(0x92, "playbgm_epi", [], false, [
        { type: Type.U32 },
    ]));
    static readonly set_mainwarp = (OPCODES[0x93] = new Opcode(0x93, "set_mainwarp", [], false, [
        { type: Type.U32 },
    ]));
    static readonly set_obj_param = (OPCODES[0x94] = new Opcode(
        0x94,
        "set_obj_param",
        [{ type: Type.Register }, { type: Type.Register }],
        false,
        []
    ));
    static readonly set_floor_handler = (OPCODES[0x95] = new Opcode(
        0x95,
        "set_floor_handler",
        [],
        false,
        [{ type: Type.U32 }, { type: Type.ILabel }]
    ));
    static readonly clr_floor_handler = (OPCODES[0x96] = new Opcode(
        0x96,
        "clr_floor_handler",
        [],
        false,
        [{ type: Type.U32 }]
    ));
    static readonly col_plinaw = (OPCODES[0x97] = new Opcode(
        0x97,
        "col_plinaw",
        [{ type: Type.Register }],
        false,
        []
    ));
    static readonly hud_hide = (OPCODES[0x98] = new Opcode(0x98, "hud_hide", [], false, []));
    static readonly hud_show = (OPCODES[0x99] = new Opcode(0x99, "hud_show", [], false, []));
    static readonly cine_enable = (OPCODES[0x9a] = new Opcode(0x9a, "cine_enable", [], false, []));
    static readonly cine_disable = (OPCODES[0x9b] = new Opcode(
        0x9b,
        "cine_disable",
        [],
        false,
        []
    ));
    static readonly unknown_9c = (OPCODES[0x9c] = new Opcode(0x9c, "unknown_9c", [], false, []));
    static readonly unknown_9d = (OPCODES[0x9d] = new Opcode(0x9d, "unknown_9d", [], false, []));
    static readonly unknown_9e = (OPCODES[0x9e] = new Opcode(0x9e, "unknown_9e", [], false, []));
    static readonly unknown_9f = (OPCODES[0x9f] = new Opcode(0x9f, "unknown_9f", [], false, []));
    static readonly unknown_a0 = (OPCODES[0xa0] = new Opcode(0xa0, "unknown_a0", [], false, []));
    static readonly set_qt_failure = (OPCODES[0xa1] = new Opcode(
        0xa1,
        "set_qt_failure",
        [{ type: Type.ILabel }],
        false,
        []
    ));
    static readonly set_qt_success = (OPCODES[0xa2] = new Opcode(
        0xa2,
        "set_qt_success",
        [{ type: Type.ILabel }],
        false,
        []
    ));
    static readonly clr_qt_failure = (OPCODES[0xa3] = new Opcode(
        0xa3,
        "clr_qt_failure",
        [],
        false,
        []
    ));
    static readonly clr_qt_success = (OPCODES[0xa4] = new Opcode(
        0xa4,
        "clr_qt_success",
        [],
        false,
        []
    ));
    static readonly set_qt_cancel = (OPCODES[0xa5] = new Opcode(
        0xa5,
        "set_qt_cancel",
        [{ type: Type.ILabel }],
        false,
        []
    ));
    static readonly clr_qt_cancel = (OPCODES[0xa6] = new Opcode(
        0xa6,
        "clr_qt_cancel",
        [],
        false,
        []
    ));
    static readonly unknown_a7 = (OPCODES[0xa7] = new Opcode(0xa7, "unknown_a7", [], false, []));
    static readonly pl_walk_v3 = (OPCODES[0xa8] = new Opcode(
        0xa8,
        "pl_walk_v3",
        [{ type: Type.Register }],
        false,
        []
    ));
    static readonly unknown_a9 = (OPCODES[0xa9] = new Opcode(0xa9, "unknown_a9", [], false, []));
    static readonly unknown_aa = (OPCODES[0xaa] = new Opcode(0xaa, "unknown_aa", [], false, []));
    static readonly unknown_ab = (OPCODES[0xab] = new Opcode(0xab, "unknown_ab", [], false, []));
    static readonly unknown_ac = (OPCODES[0xac] = new Opcode(0xac, "unknown_ac", [], false, []));
    static readonly unknown_ad = (OPCODES[0xad] = new Opcode(0xad, "unknown_ad", [], false, []));
    static readonly unknown_ae = (OPCODES[0xae] = new Opcode(0xae, "unknown_ae", [], false, []));
    static readonly unknown_af = (OPCODES[0xaf] = new Opcode(0xaf, "unknown_af", [], false, []));
    static readonly pl_add_meseta = (OPCODES[0xb0] = new Opcode(0xb0, "pl_add_meseta", [], false, [
        { type: Type.U32 },
        { type: Type.U32 },
    ]));
    static readonly thread_stg = (OPCODES[0xb1] = new Opcode(
        0xb1,
        "thread_stg",
        [{ type: Type.ILabel }],
        false,
        []
    ));
    static readonly del_obj_param = (OPCODES[0xb2] = new Opcode(
        0xb2,
        "del_obj_param",
        [{ type: Type.Register }],
        false,
        []
    ));
    static readonly item_create = (OPCODES[0xb3] = new Opcode(
        0xb3,
        "item_create",
        [{ type: Type.Register }, { type: Type.Register }],
        false,
        []
    ));
    static readonly item_create2 = (OPCODES[0xb4] = new Opcode(
        0xb4,
        "item_create2",
        [{ type: Type.Register }, { type: Type.Register }],
        false,
        []
    ));
    static readonly item_delete = (OPCODES[0xb5] = new Opcode(
        0xb5,
        "item_delete",
        [{ type: Type.Register }, { type: Type.Register }],
        false,
        []
    ));
    static readonly item_delete2 = (OPCODES[0xb6] = new Opcode(
        0xb6,
        "item_delete2",
        [{ type: Type.Register }, { type: Type.Register }],
        false,
        []
    ));
    static readonly item_check = (OPCODES[0xb7] = new Opcode(
        0xb7,
        "item_check",
        [{ type: Type.Register }, { type: Type.Register }],
        false,
        []
    ));
    static readonly setevt = (OPCODES[0xb8] = new Opcode(0xb8, "setevt", [], false, [
        { type: Type.U32 },
    ]));
    static readonly get_difflvl = (OPCODES[0xb9] = new Opcode(
        0xb9,
        "get_difflvl",
        [{ type: Type.Register }],
        false,
        []
    ));
    static readonly set_qt_exit = (OPCODES[0xba] = new Opcode(
        0xba,
        "set_qt_exit",
        [{ type: Type.ILabel }],
        false,
        []
    ));
    static readonly clr_qt_exit = (OPCODES[0xbb] = new Opcode(0xbb, "clr_qt_exit", [], false, []));
    static readonly unknown_bc = (OPCODES[0xbc] = new Opcode(0xbc, "unknown_bc", [], false, []));
    static readonly unknown_bd = (OPCODES[0xbd] = new Opcode(0xbd, "unknown_bd", [], false, []));
    static readonly unknown_be = (OPCODES[0xbe] = new Opcode(0xbe, "unknown_be", [], false, []));
    static readonly unknown_bf = (OPCODES[0xbf] = new Opcode(0xbf, "unknown_bf", [], false, []));
    static readonly particle_v3 = (OPCODES[0xc0] = new Opcode(
        0xc0,
        "particle_v3",
        [{ type: Type.Register }],
        false,
        []
    ));
    static readonly npc_text = (OPCODES[0xc1] = new Opcode(0xc1, "npc_text", [], false, [
        { type: Type.U32 },
        { type: Type.String },
    ]));
    static readonly npc_chkwarp = (OPCODES[0xc2] = new Opcode(0xc2, "npc_chkwarp", [], false, []));
    static readonly pl_pkoff = (OPCODES[0xc3] = new Opcode(0xc3, "pl_pkoff", [], false, []));
    static readonly map_designate = (OPCODES[0xc4] = new Opcode(
        0xc4,
        "map_designate",
        [{ type: Type.Register }],
        false,
        []
    ));
    static readonly masterkey_on = (OPCODES[0xc5] = new Opcode(
        0xc5,
        "masterkey_on",
        [],
        false,
        []
    ));
    static readonly masterkey_off = (OPCODES[0xc6] = new Opcode(
        0xc6,
        "masterkey_off",
        [],
        false,
        []
    ));
    static readonly window_time = (OPCODES[0xc7] = new Opcode(0xc7, "window_time", [], false, []));
    static readonly winend_time = (OPCODES[0xc8] = new Opcode(0xc8, "winend_time", [], false, []));
    static readonly winset_time = (OPCODES[0xc9] = new Opcode(
        0xc9,
        "winset_time",
        [{ type: Type.Register }],
        false,
        []
    ));
    static readonly getmtime = (OPCODES[0xca] = new Opcode(
        0xca,
        "getmtime",
        [{ type: Type.Register }],
        false,
        []
    ));
    static readonly set_quest_board_handler = (OPCODES[0xcb] = new Opcode(
        0xcb,
        "set_quest_board_handler",
        [],
        false,
        [{ type: Type.U32 }, { type: Type.ILabel }, { type: Type.String }]
    ));
    static readonly clear_quest_board_handler = (OPCODES[0xcc] = new Opcode(
        0xcc,
        "clear_quest_board_handler",
        [],
        false,
        [{ type: Type.U32 }]
    ));
    static readonly particle_id_v3 = (OPCODES[0xcd] = new Opcode(
        0xcd,
        "particle_id_v3",
        [{ type: Type.Register }],
        false,
        []
    ));
    static readonly npc_crptalk_id_v3 = (OPCODES[0xce] = new Opcode(
        0xce,
        "npc_crptalk_id_v3",
        [{ type: Type.Register }],
        false,
        []
    ));
    static readonly npc_lang_clean = (OPCODES[0xcf] = new Opcode(
        0xcf,
        "npc_lang_clean",
        [],
        false,
        []
    ));
    static readonly pl_pkon = (OPCODES[0xd0] = new Opcode(0xd0, "pl_pkon", [], false, []));
    static readonly pl_chk_item2 = (OPCODES[0xd1] = new Opcode(
        0xd1,
        "pl_chk_item2",
        [{ type: Type.Register }, { type: Type.Register }],
        false,
        []
    ));
    static readonly enable_mainmenu = (OPCODES[0xd2] = new Opcode(
        0xd2,
        "enable_mainmenu",
        [],
        false,
        []
    ));
    static readonly disable_mainmenu = (OPCODES[0xd3] = new Opcode(
        0xd3,
        "disable_mainmenu",
        [],
        false,
        []
    ));
    static readonly start_battlebgm = (OPCODES[0xd4] = new Opcode(
        0xd4,
        "start_battlebgm",
        [],
        false,
        []
    ));
    static readonly end_battlebgm = (OPCODES[0xd5] = new Opcode(
        0xd5,
        "end_battlebgm",
        [],
        false,
        []
    ));
    static readonly disp_msg_qb = (OPCODES[0xd6] = new Opcode(0xd6, "disp_msg_qb", [], false, [
        { type: Type.String },
    ]));
    static readonly close_msg_qb = (OPCODES[0xd7] = new Opcode(
        0xd7,
        "close_msg_qb",
        [],
        false,
        []
    ));
    static readonly set_eventflag_v3 = (OPCODES[0xd8] = new Opcode(
        0xd8,
        "set_eventflag_v3",
        [],
        false,
        [{ type: Type.U32 }, { type: Type.U32 }]
    ));
    static readonly sync_leti = (OPCODES[0xd9] = new Opcode(0xd9, "sync_leti", [], false, []));
    static readonly set_returnhunter = (OPCODES[0xda] = new Opcode(
        0xda,
        "set_returnhunter",
        [],
        false,
        []
    ));
    static readonly set_returncity = (OPCODES[0xdb] = new Opcode(
        0xdb,
        "set_returncity",
        [],
        false,
        []
    ));
    static readonly load_pvr = (OPCODES[0xdc] = new Opcode(0xdc, "load_pvr", [], false, []));
    static readonly load_midi = (OPCODES[0xdd] = new Opcode(0xdd, "load_midi", [], false, []));
    static readonly unknown_de = (OPCODES[0xde] = new Opcode(0xde, "unknown_de", [], false, []));
    static readonly npc_param_v3 = (OPCODES[0xdf] = new Opcode(0xdf, "npc_param_v3", [], false, [
        { type: Type.Register },
        { type: Type.U32 },
    ]));
    static readonly pad_dragon = (OPCODES[0xe0] = new Opcode(0xe0, "pad_dragon", [], false, []));
    static readonly clear_mainwarp = (OPCODES[0xe1] = new Opcode(
        0xe1,
        "clear_mainwarp",
        [],
        false,
        [{ type: Type.U32 }]
    ));
    static readonly pcam_param_v3 = (OPCODES[0xe2] = new Opcode(
        0xe2,
        "pcam_param_v3",
        [{ type: Type.Register }],
        false,
        []
    ));
    static readonly start_setevt_v3 = (OPCODES[0xe3] = new Opcode(
        0xe3,
        "start_setevt_v3",
        [],
        false,
        [{ type: Type.Register }, { type: Type.U32 }]
    ));
    static readonly warp_on = (OPCODES[0xe4] = new Opcode(0xe4, "warp_on", [], false, []));
    static readonly warp_off = (OPCODES[0xe5] = new Opcode(0xe5, "warp_off", [], false, []));
    static readonly get_slotnumber = (OPCODES[0xe6] = new Opcode(
        0xe6,
        "get_slotnumber",
        [{ type: Type.Register }],
        false,
        []
    ));
    static readonly get_servernumber = (OPCODES[0xe7] = new Opcode(
        0xe7,
        "get_servernumber",
        [{ type: Type.Register }],
        false,
        []
    ));
    static readonly set_eventflag2 = (OPCODES[0xe8] = new Opcode(
        0xe8,
        "set_eventflag2",
        [],
        false,
        [{ type: Type.U32 }, { type: Type.Register }]
    ));
    static readonly res = (OPCODES[0xe9] = new Opcode(
        0xe9,
        "res",
        [{ type: Type.Register }, { type: Type.Register }],
        false,
        []
    ));
    static readonly unknown_ea = (OPCODES[0xea] = new Opcode(
        0xea,
        "unknown_ea",
        [{ type: Type.Register }, { type: Type.U32 }],
        false,
        []
    ));
    static readonly enable_bgmctrl = (OPCODES[0xeb] = new Opcode(
        0xeb,
        "enable_bgmctrl",
        [],
        false,
        [{ type: Type.U32 }]
    ));
    static readonly sw_send = (OPCODES[0xec] = new Opcode(
        0xec,
        "sw_send",
        [{ type: Type.Register }],
        false,
        []
    ));
    static readonly create_bgmctrl = (OPCODES[0xed] = new Opcode(
        0xed,
        "create_bgmctrl",
        [],
        false,
        []
    ));
    static readonly pl_add_meseta2 = (OPCODES[0xee] = new Opcode(
        0xee,
        "pl_add_meseta2",
        [],
        false,
        [{ type: Type.U32 }]
    ));
    static readonly sync_register = (OPCODES[0xef] = new Opcode(0xef, "sync_register", [], false, [
        { type: Type.Register },
        { type: Type.U32 /* TODO: Can be U32 or Register. */ },
    ]));
    static readonly send_regwork = (OPCODES[0xf0] = new Opcode(
        0xf0,
        "send_regwork",
        [],
        false,
        []
    ));
    static readonly leti_fixed_camera_v3 = (OPCODES[0xf1] = new Opcode(
        0xf1,
        "leti_fixed_camera_v3",
        [{ type: Type.Register }],
        false,
        []
    ));
    static readonly default_camera_pos1 = (OPCODES[0xf2] = new Opcode(
        0xf2,
        "default_camera_pos1",
        [],
        false,
        []
    ));
    static readonly unknown_f3 = (OPCODES[0xf3] = new Opcode(0xf3, "unknown_f3", [], false, []));
    static readonly unknown_f4 = (OPCODES[0xf4] = new Opcode(0xf4, "unknown_f4", [], false, []));
    static readonly unknown_f5 = (OPCODES[0xf5] = new Opcode(0xf5, "unknown_f5", [], false, []));
    static readonly unknown_f6 = (OPCODES[0xf6] = new Opcode(0xf6, "unknown_f6", [], false, []));
    static readonly unknown_f7 = (OPCODES[0xf7] = new Opcode(0xf7, "unknown_f7", [], false, []));
    static readonly unknown_f8 = (OPCODES[0xf8] = new Opcode(
        0xf8,
        "unknown_f8",
        [{ type: Type.Register }],
        false,
        []
    ));
    static readonly unknown_f9 = (OPCODES[0xf9] = new Opcode(0xf9, "unknown_f9", [], false, []));
    static readonly get_gc_number = (OPCODES[0xfa] = new Opcode(
        0xfa,
        "get_gc_number",
        [{ type: Type.Register }],
        false,
        []
    ));
    static readonly unknown_fb = (OPCODES[0xfb] = new Opcode(
        0xfb,
        "unknown_fb",
        [{ type: Type.U16 }],
        false,
        []
    ));
    static readonly unknown_fc = (OPCODES[0xfc] = new Opcode(0xfc, "unknown_fc", [], false, []));
    static readonly unknown_fd = (OPCODES[0xfd] = new Opcode(0xfd, "unknown_fd", [], false, []));
    static readonly unknown_fe = (OPCODES[0xfe] = new Opcode(0xfe, "unknown_fe", [], false, []));
    static readonly unknown_ff = (OPCODES[0xff] = new Opcode(0xff, "unknown_ff", [], false, []));
    static readonly unknown_f800 = (OPCODES[0xf800] = new Opcode(
        0xf800,
        "unknown_f800",
        [],
        false,
        []
    ));
    static readonly set_chat_callback = (OPCODES[0xf801] = new Opcode(
        0xf801,
        "set_chat_callback",
        [],
        false,
        [{ type: Type.Register }, { type: Type.String }]
    ));
    static readonly unknown_f802 = (OPCODES[0xf802] = new Opcode(
        0xf802,
        "unknown_f802",
        [],
        false,
        []
    ));
    static readonly unknown_f803 = (OPCODES[0xf803] = new Opcode(
        0xf803,
        "unknown_f803",
        [],
        false,
        []
    ));
    static readonly unknown_f804 = (OPCODES[0xf804] = new Opcode(
        0xf804,
        "unknown_f804",
        [],
        false,
        []
    ));
    static readonly unknown_f805 = (OPCODES[0xf805] = new Opcode(
        0xf805,
        "unknown_f805",
        [],
        false,
        []
    ));
    static readonly unknown_f806 = (OPCODES[0xf806] = new Opcode(
        0xf806,
        "unknown_f806",
        [],
        false,
        []
    ));
    static readonly unknown_f807 = (OPCODES[0xf807] = new Opcode(
        0xf807,
        "unknown_f807",
        [],
        false,
        []
    ));
    static readonly get_difficulty_level2 = (OPCODES[0xf808] = new Opcode(
        0xf808,
        "get_difficulty_level2",
        [{ type: Type.Register }],
        false,
        []
    ));
    static readonly get_number_of_player1 = (OPCODES[0xf809] = new Opcode(
        0xf809,
        "get_number_of_player1",
        [{ type: Type.Register }],
        false,
        []
    ));
    static readonly get_coord_of_player = (OPCODES[0xf80a] = new Opcode(
        0xf80a,
        "get_coord_of_player",
        [{ type: Type.Register }, { type: Type.Register }],
        false,
        []
    ));
    static readonly unknown_f80b = (OPCODES[0xf80b] = new Opcode(
        0xf80b,
        "unknown_f80b",
        [],
        false,
        []
    ));
    static readonly unknown_f80c = (OPCODES[0xf80c] = new Opcode(
        0xf80c,
        "unknown_f80c",
        [],
        false,
        []
    ));
    static readonly map_designate_ex = (OPCODES[0xf80d] = new Opcode(
        0xf80d,
        "map_designate_ex",
        [{ type: Type.Register }],
        false,
        []
    ));
    static readonly unknown_f80e = (OPCODES[0xf80e] = new Opcode(
        0xf80e,
        "unknown_f80e",
        [],
        false,
        [{ type: Type.U32 }]
    ));
    static readonly unknown_f80f = (OPCODES[0xf80f] = new Opcode(
        0xf80f,
        "unknown_f80f",
        [],
        false,
        [{ type: Type.U32 }]
    ));
    static readonly ba_initial_floor = (OPCODES[0xf810] = new Opcode(
        0xf810,
        "ba_initial_floor",
        [],
        false,
        [{ type: Type.U32 }]
    ));
    static readonly set_ba_rules = (OPCODES[0xf811] = new Opcode(
        0xf811,
        "set_ba_rules",
        [],
        false,
        []
    ));
    static readonly unknown_f812 = (OPCODES[0xf812] = new Opcode(
        0xf812,
        "unknown_f812",
        [],
        false,
        [{ type: Type.U32 }]
    ));
    static readonly unknown_f813 = (OPCODES[0xf813] = new Opcode(
        0xf813,
        "unknown_f813",
        [],
        false,
        [{ type: Type.U32 }]
    ));
    static readonly unknown_f814 = (OPCODES[0xf814] = new Opcode(
        0xf814,
        "unknown_f814",
        [],
        false,
        [{ type: Type.U32 }]
    ));
    static readonly unknown_f815 = (OPCODES[0xf815] = new Opcode(
        0xf815,
        "unknown_f815",
        [],
        false,
        [{ type: Type.U32 }]
    ));
    static readonly unknown_f816 = (OPCODES[0xf816] = new Opcode(
        0xf816,
        "unknown_f816",
        [],
        false,
        [{ type: Type.U32 }]
    ));
    static readonly unknown_f817 = (OPCODES[0xf817] = new Opcode(
        0xf817,
        "unknown_f817",
        [],
        false,
        [{ type: Type.U32 }]
    ));
    static readonly unknown_f818 = (OPCODES[0xf818] = new Opcode(
        0xf818,
        "unknown_f818",
        [],
        false,
        [{ type: Type.U32 }]
    ));
    static readonly unknown_f819 = (OPCODES[0xf819] = new Opcode(
        0xf819,
        "unknown_f819",
        [],
        false,
        [{ type: Type.U32 }]
    ));
    static readonly unknown_f81a = (OPCODES[0xf81a] = new Opcode(
        0xf81a,
        "unknown_f81a",
        [],
        false,
        [{ type: Type.U32 }]
    ));
    static readonly unknown_f81b = (OPCODES[0xf81b] = new Opcode(
        0xf81b,
        "unknown_f81b",
        [],
        false,
        [{ type: Type.U32 }]
    ));
    static readonly ba_disp_msg = (OPCODES[0xf81c] = new Opcode(0xf81c, "ba_disp_msg", [], false, [
        { type: Type.String },
    ]));
    static readonly death_lvl_up = (OPCODES[0xf81d] = new Opcode(
        0xf81d,
        "death_lvl_up",
        [],
        false,
        [{ type: Type.U32 }]
    ));
    static readonly death_tech_lvl_up = (OPCODES[0xf81e] = new Opcode(
        0xf81e,
        "death_tech_lvl_up",
        [],
        false,
        [{ type: Type.U32 }]
    ));
    static readonly unknown_f81f = (OPCODES[0xf81f] = new Opcode(
        0xf81f,
        "unknown_f81f",
        [],
        false,
        []
    ));
    static readonly cmode_stage = (OPCODES[0xf820] = new Opcode(0xf820, "cmode_stage", [], false, [
        { type: Type.U32 },
    ]));
    static readonly unknown_f821 = (OPCODES[0xf821] = new Opcode(
        0xf821,
        "unknown_f821",
        [],
        false,
        []
    ));
    static readonly unknown_f822 = (OPCODES[0xf822] = new Opcode(
        0xf822,
        "unknown_f822",
        [],
        false,
        []
    ));
    static readonly unknown_f823 = (OPCODES[0xf823] = new Opcode(
        0xf823,
        "unknown_f823",
        [],
        false,
        [{ type: Type.U32 }]
    ));
    static readonly unknown_f824 = (OPCODES[0xf824] = new Opcode(
        0xf824,
        "unknown_f824",
        [],
        false,
        [{ type: Type.U32 }]
    ));
    static readonly exp_multiplication = (OPCODES[0xf825] = new Opcode(
        0xf825,
        "exp_multiplication",
        [{ type: Type.Register }],
        false,
        []
    ));
    static readonly exp_division = (OPCODES[0xf826] = new Opcode(
        0xf826,
        "exp_division",
        [{ type: Type.Register }],
        false,
        []
    ));
    static readonly get_user_is_dead = (OPCODES[0xf827] = new Opcode(
        0xf827,
        "get_user_is_dead",
        [{ type: Type.Register }],
        false,
        []
    ));
    static readonly go_floor = (OPCODES[0xf828] = new Opcode(
        0xf828,
        "go_floor",
        [{ type: Type.Register }, { type: Type.Register }],
        false,
        []
    ));
    static readonly unknown_f829 = (OPCODES[0xf829] = new Opcode(
        0xf829,
        "unknown_f829",
        [],
        false,
        []
    ));
    static readonly unknown_f82a = (OPCODES[0xf82a] = new Opcode(
        0xf82a,
        "unknown_f82a",
        [],
        false,
        []
    ));
    static readonly unlock_door2 = (OPCODES[0xf82b] = new Opcode(
        0xf82b,
        "unlock_door2",
        [],
        false,
        [{ type: Type.U32 }, { type: Type.U32 }]
    ));
    static readonly lock_door2 = (OPCODES[0xf82c] = new Opcode(0xf82c, "lock_door2", [], false, [
        { type: Type.U32 },
        { type: Type.U32 },
    ]));
    static readonly if_switch_not_pressed = (OPCODES[0xf82d] = new Opcode(
        0xf82d,
        "if_switch_not_pressed",
        [{ type: Type.Register }],
        false,
        []
    ));
    static readonly if_switch_pressed = (OPCODES[0xf82e] = new Opcode(
        0xf82e,
        "if_switch_pressed",
        [{ type: Type.Register }],
        false,
        []
    ));
    static readonly unknown_f82f = (OPCODES[0xf82f] = new Opcode(
        0xf82f,
        "unknown_f82f",
        [],
        false,
        [{ type: Type.U32 }, { type: Type.U32 }]
    ));
    static readonly control_dragon = (OPCODES[0xf830] = new Opcode(
        0xf830,
        "control_dragon",
        [{ type: Type.Register }],
        false,
        []
    ));
    static readonly release_dragon = (OPCODES[0xf831] = new Opcode(
        0xf831,
        "release_dragon",
        [],
        false,
        []
    ));
    static readonly unknown_f832 = (OPCODES[0xf832] = new Opcode(
        0xf832,
        "unknown_f832",
        [],
        false,
        []
    ));
    static readonly unknown_f833 = (OPCODES[0xf833] = new Opcode(
        0xf833,
        "unknown_f833",
        [],
        false,
        []
    ));
    static readonly unknown_f834 = (OPCODES[0xf834] = new Opcode(
        0xf834,
        "unknown_f834",
        [],
        false,
        []
    ));
    static readonly unknown_f835 = (OPCODES[0xf835] = new Opcode(
        0xf835,
        "unknown_f835",
        [],
        false,
        []
    ));
    static readonly unknown_f836 = (OPCODES[0xf836] = new Opcode(
        0xf836,
        "unknown_f836",
        [],
        false,
        []
    ));
    static readonly unknown_f837 = (OPCODES[0xf837] = new Opcode(
        0xf837,
        "unknown_f837",
        [],
        false,
        []
    ));
    static readonly shrink = (OPCODES[0xf838] = new Opcode(
        0xf838,
        "shrink",
        [{ type: Type.Register }],
        false,
        []
    ));
    static readonly unshrink = (OPCODES[0xf839] = new Opcode(
        0xf839,
        "unshrink",
        [{ type: Type.Register }],
        false,
        []
    ));
    static readonly unknown_f83a = (OPCODES[0xf83a] = new Opcode(
        0xf83a,
        "unknown_f83a",
        [],
        false,
        []
    ));
    static readonly unknown_f83b = (OPCODES[0xf83b] = new Opcode(
        0xf83b,
        "unknown_f83b",
        [],
        false,
        []
    ));
    static readonly display_clock2 = (OPCODES[0xf83c] = new Opcode(
        0xf83c,
        "display_clock2",
        [{ type: Type.Register }],
        false,
        []
    ));
    static readonly unknown_f83d = (OPCODES[0xf83d] = new Opcode(
        0xf83d,
        "unknown_f83d",
        [],
        false,
        [{ type: Type.U32 }]
    ));
    static readonly delete_area_title = (OPCODES[0xf83e] = new Opcode(
        0xf83e,
        "delete_area_title",
        [],
        false,
        [{ type: Type.U32 }]
    ));
    static readonly unknown_f83f = (OPCODES[0xf83f] = new Opcode(
        0xf83f,
        "unknown_f83f",
        [],
        false,
        []
    ));
    static readonly load_npc_data = (OPCODES[0xf840] = new Opcode(
        0xf840,
        "load_npc_data",
        [],
        false,
        []
    ));
    static readonly get_npc_data = (OPCODES[0xf841] = new Opcode(
        0xf841,
        "get_npc_data",
        [{ type: Type.DLabel }],
        false,
        []
    ));
    static readonly unknown_f842 = (OPCODES[0xf842] = new Opcode(
        0xf842,
        "unknown_f842",
        [],
        false,
        []
    ));
    static readonly unknown_f843 = (OPCODES[0xf843] = new Opcode(
        0xf843,
        "unknown_f843",
        [],
        false,
        []
    ));
    static readonly unknown_f844 = (OPCODES[0xf844] = new Opcode(
        0xf844,
        "unknown_f844",
        [],
        false,
        []
    ));
    static readonly unknown_f845 = (OPCODES[0xf845] = new Opcode(
        0xf845,
        "unknown_f845",
        [],
        false,
        []
    ));
    static readonly unknown_f846 = (OPCODES[0xf846] = new Opcode(
        0xf846,
        "unknown_f846",
        [],
        false,
        []
    ));
    static readonly unknown_f847 = (OPCODES[0xf847] = new Opcode(
        0xf847,
        "unknown_f847",
        [],
        false,
        []
    ));
    static readonly give_damage_score = (OPCODES[0xf848] = new Opcode(
        0xf848,
        "give_damage_score",
        [{ type: Type.Register }],
        false,
        []
    ));
    static readonly take_damage_score = (OPCODES[0xf849] = new Opcode(
        0xf849,
        "take_damage_score",
        [{ type: Type.Register }],
        false,
        []
    ));
    static readonly unk_score_f84a = (OPCODES[0xf84a] = new Opcode(
        0xf84a,
        "unk_score_f84a",
        [{ type: Type.Register }],
        false,
        []
    ));
    static readonly unk_score_f84b = (OPCODES[0xf84b] = new Opcode(
        0xf84b,
        "unk_score_f84b",
        [{ type: Type.Register }],
        false,
        []
    ));
    static readonly kill_score = (OPCODES[0xf84c] = new Opcode(
        0xf84c,
        "kill_score",
        [{ type: Type.Register }],
        false,
        []
    ));
    static readonly death_score = (OPCODES[0xf84d] = new Opcode(
        0xf84d,
        "death_score",
        [{ type: Type.Register }],
        false,
        []
    ));
    static readonly unk_score_f84e = (OPCODES[0xf84e] = new Opcode(
        0xf84e,
        "unk_score_f84e",
        [{ type: Type.Register }],
        false,
        []
    ));
    static readonly enemy_death_score = (OPCODES[0xf84f] = new Opcode(
        0xf84f,
        "enemy_death_score",
        [{ type: Type.Register }],
        false,
        []
    ));
    static readonly meseta_score = (OPCODES[0xf850] = new Opcode(
        0xf850,
        "meseta_score",
        [{ type: Type.Register }],
        false,
        []
    ));
    static readonly unknown_f851 = (OPCODES[0xf851] = new Opcode(
        0xf851,
        "unknown_f851",
        [{ type: Type.Register }],
        false,
        []
    ));
    static readonly unknown_f852 = (OPCODES[0xf852] = new Opcode(
        0xf852,
        "unknown_f852",
        [],
        false,
        [{ type: Type.U32 }]
    ));
    static readonly reverse_warps = (OPCODES[0xf853] = new Opcode(
        0xf853,
        "reverse_warps",
        [],
        false,
        []
    ));
    static readonly unreverse_warps = (OPCODES[0xf854] = new Opcode(
        0xf854,
        "unreverse_warps",
        [],
        false,
        []
    ));
    static readonly set_ult_map = (OPCODES[0xf855] = new Opcode(
        0xf855,
        "set_ult_map",
        [],
        false,
        []
    ));
    static readonly unset_ult_map = (OPCODES[0xf856] = new Opcode(
        0xf856,
        "unset_ult_map",
        [],
        false,
        []
    ));
    static readonly set_area_title = (OPCODES[0xf857] = new Opcode(
        0xf857,
        "set_area_title",
        [],
        false,
        [{ type: Type.String }]
    ));
    static readonly unknown_f858 = (OPCODES[0xf858] = new Opcode(
        0xf858,
        "unknown_f858",
        [],
        false,
        []
    ));
    static readonly unknown_f859 = (OPCODES[0xf859] = new Opcode(
        0xf859,
        "unknown_f859",
        [],
        false,
        []
    ));
    static readonly equip_item = (OPCODES[0xf85a] = new Opcode(
        0xf85a,
        "equip_item",
        [{ type: Type.Register }],
        false,
        []
    ));
    static readonly unequip_item = (OPCODES[0xf85b] = new Opcode(
        0xf85b,
        "unequip_item",
        [],
        false,
        [{ type: Type.U32 }, { type: Type.U32 }]
    ));
    static readonly unknown_f85c = (OPCODES[0xf85c] = new Opcode(
        0xf85c,
        "unknown_f85c",
        [],
        false,
        []
    ));
    static readonly unknown_f85d = (OPCODES[0xf85d] = new Opcode(
        0xf85d,
        "unknown_f85d",
        [],
        false,
        []
    ));
    static readonly unknown_f85e = (OPCODES[0xf85e] = new Opcode(
        0xf85e,
        "unknown_f85e",
        [],
        false,
        [{ type: Type.U32 }]
    ));
    static readonly unknown_f85f = (OPCODES[0xf85f] = new Opcode(
        0xf85f,
        "unknown_f85f",
        [],
        false,
        [{ type: Type.U32 }]
    ));
    static readonly unknown_f860 = (OPCODES[0xf860] = new Opcode(
        0xf860,
        "unknown_f860",
        [],
        false,
        []
    ));
    static readonly unknown_f861 = (OPCODES[0xf861] = new Opcode(
        0xf861,
        "unknown_f861",
        [],
        false,
        [{ type: Type.U32 }]
    ));
    static readonly unknown_f862 = (OPCODES[0xf862] = new Opcode(
        0xf862,
        "unknown_f862",
        [],
        false,
        []
    ));
    static readonly unknown_f863 = (OPCODES[0xf863] = new Opcode(
        0xf863,
        "unknown_f863",
        [],
        false,
        []
    ));
    static readonly cmode_rank = (OPCODES[0xf864] = new Opcode(0xf864, "cmode_rank", [], false, [
        { type: Type.U32 },
        { type: Type.String },
    ]));
    static readonly award_item_name = (OPCODES[0xf865] = new Opcode(
        0xf865,
        "award_item_name",
        [],
        false,
        []
    ));
    static readonly award_item_select = (OPCODES[0xf866] = new Opcode(
        0xf866,
        "award_item_select",
        [],
        false,
        []
    ));
    static readonly award_item_give_to = (OPCODES[0xf867] = new Opcode(
        0xf867,
        "award_item_give_to",
        [{ type: Type.Register }],
        false,
        []
    ));
    static readonly unknown_f868 = (OPCODES[0xf868] = new Opcode(
        0xf868,
        "unknown_f868",
        [{ type: Type.Register }, { type: Type.Register }],
        false,
        []
    ));
    static readonly unknown_f869 = (OPCODES[0xf869] = new Opcode(
        0xf869,
        "unknown_f869",
        [{ type: Type.Register }, { type: Type.Register }],
        false,
        []
    ));
    static readonly item_create_cmode = (OPCODES[0xf86a] = new Opcode(
        0xf86a,
        "item_create_cmode",
        [{ type: Type.Register }, { type: Type.Register }],
        false,
        []
    ));
    static readonly unknown_f86b = (OPCODES[0xf86b] = new Opcode(
        0xf86b,
        "unknown_f86b",
        [{ type: Type.Register }],
        false,
        []
    ));
    static readonly award_item_ok = (OPCODES[0xf86c] = new Opcode(
        0xf86c,
        "award_item_ok",
        [{ type: Type.Register }],
        false,
        []
    ));
    static readonly unknown_f86d = (OPCODES[0xf86d] = new Opcode(
        0xf86d,
        "unknown_f86d",
        [],
        false,
        []
    ));
    static readonly unknown_f86e = (OPCODES[0xf86e] = new Opcode(
        0xf86e,
        "unknown_f86e",
        [],
        false,
        []
    ));
    static readonly ba_set_lives = (OPCODES[0xf86f] = new Opcode(
        0xf86f,
        "ba_set_lives",
        [],
        false,
        [{ type: Type.U32 }]
    ));
    static readonly ba_set_tech_lvl = (OPCODES[0xf870] = new Opcode(
        0xf870,
        "ba_set_tech_lvl",
        [],
        false,
        [{ type: Type.U32 }]
    ));
    static readonly ba_set_lvl = (OPCODES[0xf871] = new Opcode(0xf871, "ba_set_lvl", [], false, [
        { type: Type.U32 },
    ]));
    static readonly ba_set_time_limit = (OPCODES[0xf872] = new Opcode(
        0xf872,
        "ba_set_time_limit",
        [],
        false,
        [{ type: Type.U32 }]
    ));
    static readonly boss_is_dead = (OPCODES[0xf873] = new Opcode(
        0xf873,
        "boss_is_dead",
        [{ type: Type.Register }],
        false,
        []
    ));
    static readonly unknown_f874 = (OPCODES[0xf874] = new Opcode(
        0xf874,
        "unknown_f874",
        [],
        false,
        []
    ));
    static readonly unknown_f875 = (OPCODES[0xf875] = new Opcode(
        0xf875,
        "unknown_f875",
        [],
        false,
        []
    ));
    static readonly unknown_f876 = (OPCODES[0xf876] = new Opcode(
        0xf876,
        "unknown_f876",
        [],
        false,
        []
    ));
    static readonly enable_techs = (OPCODES[0xf877] = new Opcode(
        0xf877,
        "enable_techs",
        [{ type: Type.Register }],
        false,
        []
    ));
    static readonly disable_techs = (OPCODES[0xf878] = new Opcode(
        0xf878,
        "disable_techs",
        [{ type: Type.Register }],
        false,
        []
    ));
    static readonly get_gender = (OPCODES[0xf879] = new Opcode(
        0xf879,
        "get_gender",
        [{ type: Type.Register }, { type: Type.Register }],
        false,
        []
    ));
    static readonly get_chara_class = (OPCODES[0xf87a] = new Opcode(
        0xf87a,
        "get_chara_class",
        [{ type: Type.Register }, { type: Type.Register }],
        false,
        []
    ));
    static readonly take_slot_meseta = (OPCODES[0xf87b] = new Opcode(
        0xf87b,
        "take_slot_meseta",
        [{ type: Type.Register }, { type: Type.Register }],
        false,
        []
    ));
    static readonly unknown_f87c = (OPCODES[0xf87c] = new Opcode(
        0xf87c,
        "unknown_f87c",
        [],
        false,
        []
    ));
    static readonly unknown_f87d = (OPCODES[0xf87d] = new Opcode(
        0xf87d,
        "unknown_f87d",
        [],
        false,
        []
    ));
    static readonly unknown_f87e = (OPCODES[0xf87e] = new Opcode(
        0xf87e,
        "unknown_f87e",
        [],
        false,
        []
    ));
    static readonly read_guildcard_flag = (OPCODES[0xf87f] = new Opcode(
        0xf87f,
        "read_guildcard_flag",
        [{ type: Type.Register }, { type: Type.Register }],
        false,
        []
    ));
    static readonly unknown_f880 = (OPCODES[0xf880] = new Opcode(
        0xf880,
        "unknown_f880",
        [{ type: Type.Register }],
        false,
        []
    ));
    static readonly get_pl_name = (OPCODES[0xf881] = new Opcode(
        0xf881,
        "get_pl_name",
        [{ type: Type.Register }],
        false,
        []
    ));
    static readonly unknown_f882 = (OPCODES[0xf882] = new Opcode(
        0xf882,
        "unknown_f882",
        [],
        false,
        []
    ));
    static readonly unknown_f883 = (OPCODES[0xf883] = new Opcode(
        0xf883,
        "unknown_f883",
        [{ type: Type.Register }, { type: Type.Register }],
        false,
        []
    ));
    static readonly unknown_f884 = (OPCODES[0xf884] = new Opcode(
        0xf884,
        "unknown_f884",
        [],
        false,
        []
    ));
    static readonly unknown_f885 = (OPCODES[0xf885] = new Opcode(
        0xf885,
        "unknown_f885",
        [],
        false,
        []
    ));
    static readonly unknown_f886 = (OPCODES[0xf886] = new Opcode(
        0xf886,
        "unknown_f886",
        [],
        false,
        []
    ));
    static readonly unknown_f887 = (OPCODES[0xf887] = new Opcode(
        0xf887,
        "unknown_f887",
        [],
        false,
        []
    ));
    static readonly ba_close_msg = (OPCODES[0xf888] = new Opcode(
        0xf888,
        "ba_close_msg",
        [],
        false,
        []
    ));
    static readonly unknown_f889 = (OPCODES[0xf889] = new Opcode(
        0xf889,
        "unknown_f889",
        [],
        false,
        []
    ));
    static readonly get_player_status = (OPCODES[0xf88a] = new Opcode(
        0xf88a,
        "get_player_status",
        [{ type: Type.Register }, { type: Type.Register }],
        false,
        []
    ));
    static readonly send_mail = (OPCODES[0xf88b] = new Opcode(0xf88b, "send_mail", [], false, [
        { type: Type.Register },
        { type: Type.String },
    ]));
    static readonly online_check = (OPCODES[0xf88c] = new Opcode(
        0xf88c,
        "online_check",
        [{ type: Type.Register }],
        false,
        []
    ));
    static readonly chl_set_timerecord = (OPCODES[0xf88d] = new Opcode(
        0xf88d,
        "chl_set_timerecord",
        [{ type: Type.Register }],
        false,
        []
    ));
    static readonly chl_get_timerecord = (OPCODES[0xf88e] = new Opcode(
        0xf88e,
        "chl_get_timerecord",
        [{ type: Type.Register }],
        false,
        []
    ));
    static readonly unknown_f88f = (OPCODES[0xf88f] = new Opcode(
        0xf88f,
        "unknown_f88f",
        [{ type: Type.Register }],
        false,
        []
    ));
    static readonly unknown_f890 = (OPCODES[0xf890] = new Opcode(
        0xf890,
        "unknown_f890",
        [],
        false,
        []
    ));
    static readonly load_enemy_data = (OPCODES[0xf891] = new Opcode(
        0xf891,
        "load_enemy_data",
        [],
        false,
        [{ type: Type.U32 }]
    ));
    static readonly get_physical_data = (OPCODES[0xf892] = new Opcode(
        0xf892,
        "get_physical_data",
        [{ type: Type.U16 }],
        false,
        []
    ));
    static readonly get_attack_data = (OPCODES[0xf893] = new Opcode(
        0xf893,
        "get_attack_data",
        [{ type: Type.U16 }],
        false,
        []
    ));
    static readonly get_resist_data = (OPCODES[0xf894] = new Opcode(
        0xf894,
        "get_resist_data",
        [{ type: Type.U16 }],
        false,
        []
    ));
    static readonly get_movement_data = (OPCODES[0xf895] = new Opcode(
        0xf895,
        "get_movement_data",
        [{ type: Type.U16 }],
        false,
        []
    ));
    static readonly unknown_f896 = (OPCODES[0xf896] = new Opcode(
        0xf896,
        "unknown_f896",
        [],
        false,
        []
    ));
    static readonly unknown_f897 = (OPCODES[0xf897] = new Opcode(
        0xf897,
        "unknown_f897",
        [],
        false,
        []
    ));
    static readonly shift_left = (OPCODES[0xf898] = new Opcode(
        0xf898,
        "shift_left",
        [{ type: Type.Register }, { type: Type.Register }],
        false,
        []
    ));
    static readonly shift_right = (OPCODES[0xf899] = new Opcode(
        0xf899,
        "shift_right",
        [{ type: Type.Register }, { type: Type.Register }],
        false,
        []
    ));
    static readonly get_random = (OPCODES[0xf89a] = new Opcode(
        0xf89a,
        "get_random",
        [{ type: Type.Register }, { type: Type.Register }],
        false,
        []
    ));
    static readonly reset_map = (OPCODES[0xf89b] = new Opcode(0xf89b, "reset_map", [], false, []));
    static readonly disp_chl_retry_menu = (OPCODES[0xf89c] = new Opcode(
        0xf89c,
        "disp_chl_retry_menu",
        [{ type: Type.Register }],
        false,
        []
    ));
    static readonly chl_reverser = (OPCODES[0xf89d] = new Opcode(
        0xf89d,
        "chl_reverser",
        [],
        false,
        []
    ));
    static readonly unknown_f89e = (OPCODES[0xf89e] = new Opcode(
        0xf89e,
        "unknown_f89e",
        [],
        false,
        [{ type: Type.U32 }]
    ));
    static readonly unknown_f89f = (OPCODES[0xf89f] = new Opcode(
        0xf89f,
        "unknown_f89f",
        [{ type: Type.Register }],
        false,
        []
    ));
    static readonly unknown_f8a0 = (OPCODES[0xf8a0] = new Opcode(
        0xf8a0,
        "unknown_f8a0",
        [],
        false,
        []
    ));
    static readonly unknown_f8a1 = (OPCODES[0xf8a1] = new Opcode(
        0xf8a1,
        "unknown_f8a1",
        [],
        false,
        []
    ));
    static readonly unknown_f8a2 = (OPCODES[0xf8a2] = new Opcode(
        0xf8a2,
        "unknown_f8a2",
        [],
        false,
        []
    ));
    static readonly unknown_f8a3 = (OPCODES[0xf8a3] = new Opcode(
        0xf8a3,
        "unknown_f8a3",
        [],
        false,
        []
    ));
    static readonly unknown_f8a4 = (OPCODES[0xf8a4] = new Opcode(
        0xf8a4,
        "unknown_f8a4",
        [],
        false,
        []
    ));
    static readonly unknown_f8a5 = (OPCODES[0xf8a5] = new Opcode(
        0xf8a5,
        "unknown_f8a5",
        [],
        false,
        []
    ));
    static readonly unknown_f8a6 = (OPCODES[0xf8a6] = new Opcode(
        0xf8a6,
        "unknown_f8a6",
        [],
        false,
        []
    ));
    static readonly unknown_f8a7 = (OPCODES[0xf8a7] = new Opcode(
        0xf8a7,
        "unknown_f8a7",
        [],
        false,
        []
    ));
    static readonly unknown_f8a8 = (OPCODES[0xf8a8] = new Opcode(
        0xf8a8,
        "unknown_f8a8",
        [],
        false,
        [{ type: Type.U32 }]
    ));
    static readonly unknown_f8a9 = (OPCODES[0xf8a9] = new Opcode(
        0xf8a9,
        "unknown_f8a9",
        [{ type: Type.Register }],
        false,
        []
    ));
    static readonly unknown_f8aa = (OPCODES[0xf8aa] = new Opcode(
        0xf8aa,
        "unknown_f8aa",
        [],
        false,
        []
    ));
    static readonly unknown_f8ab = (OPCODES[0xf8ab] = new Opcode(
        0xf8ab,
        "unknown_f8ab",
        [],
        false,
        []
    ));
    static readonly unknown_f8ac = (OPCODES[0xf8ac] = new Opcode(
        0xf8ac,
        "unknown_f8ac",
        [],
        false,
        []
    ));
    static readonly get_number_of_player2 = (OPCODES[0xf8ad] = new Opcode(
        0xf8ad,
        "get_number_of_player2",
        [{ type: Type.Register }],
        false,
        []
    ));
    static readonly unknown_f8ae = (OPCODES[0xf8ae] = new Opcode(
        0xf8ae,
        "unknown_f8ae",
        [],
        false,
        []
    ));
    static readonly unknown_f8af = (OPCODES[0xf8af] = new Opcode(
        0xf8af,
        "unknown_f8af",
        [],
        false,
        []
    ));
    static readonly unknown_f8b0 = (OPCODES[0xf8b0] = new Opcode(
        0xf8b0,
        "unknown_f8b0",
        [],
        false,
        []
    ));
    static readonly unknown_f8b1 = (OPCODES[0xf8b1] = new Opcode(
        0xf8b1,
        "unknown_f8b1",
        [],
        false,
        []
    ));
    static readonly unknown_f8b2 = (OPCODES[0xf8b2] = new Opcode(
        0xf8b2,
        "unknown_f8b2",
        [],
        false,
        []
    ));
    static readonly unknown_f8b3 = (OPCODES[0xf8b3] = new Opcode(
        0xf8b3,
        "unknown_f8b3",
        [],
        false,
        []
    ));
    static readonly unknown_f8b4 = (OPCODES[0xf8b4] = new Opcode(
        0xf8b4,
        "unknown_f8b4",
        [],
        false,
        []
    ));
    static readonly unknown_f8b5 = (OPCODES[0xf8b5] = new Opcode(
        0xf8b5,
        "unknown_f8b5",
        [],
        false,
        []
    ));
    static readonly unknown_f8b6 = (OPCODES[0xf8b6] = new Opcode(
        0xf8b6,
        "unknown_f8b6",
        [],
        false,
        []
    ));
    static readonly unknown_f8b7 = (OPCODES[0xf8b7] = new Opcode(
        0xf8b7,
        "unknown_f8b7",
        [],
        false,
        []
    ));
    static readonly unknown_f8b8 = (OPCODES[0xf8b8] = new Opcode(
        0xf8b8,
        "unknown_f8b8",
        [],
        false,
        []
    ));
    static readonly chl_recovery = (OPCODES[0xf8b9] = new Opcode(
        0xf8b9,
        "chl_recovery",
        [],
        false,
        []
    ));
    static readonly unknown_f8ba = (OPCODES[0xf8ba] = new Opcode(
        0xf8ba,
        "unknown_f8ba",
        [],
        false,
        []
    ));
    static readonly unknown_f8bb = (OPCODES[0xf8bb] = new Opcode(
        0xf8bb,
        "unknown_f8bb",
        [],
        false,
        []
    ));
    static readonly set_episode = (OPCODES[0xf8bc] = new Opcode(
        0xf8bc,
        "set_episode",
        [{ type: Type.U32 }],
        false,
        []
    ));
    static readonly unknown_f8bd = (OPCODES[0xf8bd] = new Opcode(
        0xf8bd,
        "unknown_f8bd",
        [],
        false,
        []
    ));
    static readonly unknown_f8be = (OPCODES[0xf8be] = new Opcode(
        0xf8be,
        "unknown_f8be",
        [],
        false,
        []
    ));
    static readonly unknown_f8bf = (OPCODES[0xf8bf] = new Opcode(
        0xf8bf,
        "unknown_f8bf",
        [],
        false,
        []
    ));
    static readonly file_dl_req = (OPCODES[0xf8c0] = new Opcode(0xf8c0, "file_dl_req", [], false, [
        { type: Type.U32 },
        { type: Type.String },
    ]));
    static readonly get_dl_status = (OPCODES[0xf8c1] = new Opcode(
        0xf8c1,
        "get_dl_status",
        [{ type: Type.Register }],
        false,
        []
    ));
    static readonly gba_unknown4 = (OPCODES[0xf8c2] = new Opcode(
        0xf8c2,
        "gba_unknown4",
        [],
        false,
        []
    ));
    static readonly get_gba_state = (OPCODES[0xf8c3] = new Opcode(
        0xf8c3,
        "get_gba_state",
        [{ type: Type.Register }],
        false,
        []
    ));
    static readonly unknown_f8c4 = (OPCODES[0xf8c4] = new Opcode(
        0xf8c4,
        "unknown_f8c4",
        [{ type: Type.Register }],
        false,
        []
    ));
    static readonly unknown_f8c5 = (OPCODES[0xf8c5] = new Opcode(
        0xf8c5,
        "unknown_f8c5",
        [{ type: Type.Register }],
        false,
        []
    ));
    static readonly qexit = (OPCODES[0xf8c6] = new Opcode(0xf8c6, "qexit", [], false, []));
    static readonly use_animation = (OPCODES[0xf8c7] = new Opcode(
        0xf8c7,
        "use_animation",
        [{ type: Type.Register }, { type: Type.Register }],
        false,
        []
    ));
    static readonly stop_animation = (OPCODES[0xf8c8] = new Opcode(
        0xf8c8,
        "stop_animation",
        [{ type: Type.Register }],
        false,
        []
    ));
    static readonly run_to_coord = (OPCODES[0xf8c9] = new Opcode(
        0xf8c9,
        "run_to_coord",
        [{ type: Type.Register }, { type: Type.Register }],
        false,
        []
    ));
    static readonly set_slot_invincible = (OPCODES[0xf8ca] = new Opcode(
        0xf8ca,
        "set_slot_invincible",
        [{ type: Type.Register }, { type: Type.Register }],
        false,
        []
    ));
    static readonly unknown_f8cb = (OPCODES[0xf8cb] = new Opcode(
        0xf8cb,
        "unknown_f8cb",
        [{ type: Type.Register }],
        false,
        []
    ));
    static readonly set_slot_poison = (OPCODES[0xf8cc] = new Opcode(
        0xf8cc,
        "set_slot_poison",
        [{ type: Type.Register }],
        false,
        []
    ));
    static readonly set_slot_paralyze = (OPCODES[0xf8cd] = new Opcode(
        0xf8cd,
        "set_slot_paralyze",
        [{ type: Type.Register }],
        false,
        []
    ));
    static readonly set_slot_shock = (OPCODES[0xf8ce] = new Opcode(
        0xf8ce,
        "set_slot_shock",
        [{ type: Type.Register }],
        false,
        []
    ));
    static readonly set_slot_freeze = (OPCODES[0xf8cf] = new Opcode(
        0xf8cf,
        "set_slot_freeze",
        [{ type: Type.Register }],
        false,
        []
    ));
    static readonly set_slot_slow = (OPCODES[0xf8d0] = new Opcode(
        0xf8d0,
        "set_slot_slow",
        [{ type: Type.Register }],
        false,
        []
    ));
    static readonly set_slot_confuse = (OPCODES[0xf8d1] = new Opcode(
        0xf8d1,
        "set_slot_confuse",
        [{ type: Type.Register }],
        false,
        []
    ));
    static readonly set_slot_shifta = (OPCODES[0xf8d2] = new Opcode(
        0xf8d2,
        "set_slot_shifta",
        [{ type: Type.Register }],
        false,
        []
    ));
    static readonly set_slot_deband = (OPCODES[0xf8d3] = new Opcode(
        0xf8d3,
        "set_slot_deband",
        [{ type: Type.Register }],
        false,
        []
    ));
    static readonly set_slot_jellen = (OPCODES[0xf8d4] = new Opcode(
        0xf8d4,
        "set_slot_jellen",
        [{ type: Type.Register }],
        false,
        []
    ));
    static readonly set_slot_zalure = (OPCODES[0xf8d5] = new Opcode(
        0xf8d5,
        "set_slot_zalure",
        [{ type: Type.Register }],
        false,
        []
    ));
    static readonly fleti_fixed_camera = (OPCODES[0xf8d6] = new Opcode(
        0xf8d6,
        "fleti_fixed_camera",
        [],
        false,
        [{ type: Type.Register }]
    ));
    static readonly fleti_locked_camera = (OPCODES[0xf8d7] = new Opcode(
        0xf8d7,
        "fleti_locked_camera",
        [],
        false,
        [{ type: Type.U32 }, { type: Type.Register }]
    ));
    static readonly default_camera_pos2 = (OPCODES[0xf8d8] = new Opcode(
        0xf8d8,
        "default_camera_pos2",
        [],
        false,
        []
    ));
    static readonly set_motion_blur = (OPCODES[0xf8d9] = new Opcode(
        0xf8d9,
        "set_motion_blur",
        [],
        false,
        []
    ));
    static readonly set_screen_bw = (OPCODES[0xf8da] = new Opcode(
        0xf8da,
        "set_screen_bw",
        [],
        false,
        []
    ));
    static readonly unknown_f8db = (OPCODES[0xf8db] = new Opcode(
        0xf8db,
        "unknown_f8db",
        [],
        false,
        [
            { type: Type.U32 },
            { type: Type.U32 },
            { type: Type.U32 },
            { type: Type.U32 },
            { type: Type.Register },
            { type: Type.U16 },
        ]
    ));
    // TODO: 3rd parameter is a string data reference.
    static readonly npc_action_string = (OPCODES[0xf8dc] = new Opcode(
        0xf8dc,
        "npc_action_string",
        [{ type: Type.Register }, { type: Type.Register }, { type: Type.DLabel }],
        false,
        []
    ));
    static readonly get_pad_cond = (OPCODES[0xf8dd] = new Opcode(
        0xf8dd,
        "get_pad_cond",
        [{ type: Type.Register }, { type: Type.Register }],
        false,
        []
    ));
    static readonly get_button_cond = (OPCODES[0xf8de] = new Opcode(
        0xf8de,
        "get_button_cond",
        [{ type: Type.Register }, { type: Type.Register }],
        false,
        []
    ));
    static readonly freeze_enemies = (OPCODES[0xf8df] = new Opcode(
        0xf8df,
        "freeze_enemies",
        [],
        false,
        []
    ));
    static readonly unfreeze_enemies = (OPCODES[0xf8e0] = new Opcode(
        0xf8e0,
        "unfreeze_enemies",
        [],
        false,
        []
    ));
    static readonly freeze_everything = (OPCODES[0xf8e1] = new Opcode(
        0xf8e1,
        "freeze_everything",
        [],
        false,
        []
    ));
    static readonly unfreeze_everything = (OPCODES[0xf8e2] = new Opcode(
        0xf8e2,
        "unfreeze_everything",
        [],
        false,
        []
    ));
    static readonly restore_hp = (OPCODES[0xf8e3] = new Opcode(
        0xf8e3,
        "restore_hp",
        [{ type: Type.Register }],
        false,
        []
    ));
    static readonly restore_tp = (OPCODES[0xf8e4] = new Opcode(
        0xf8e4,
        "restore_tp",
        [{ type: Type.Register }],
        false,
        []
    ));
    static readonly close_chat_bubble = (OPCODES[0xf8e5] = new Opcode(
        0xf8e5,
        "close_chat_bubble",
        [{ type: Type.Register }],
        false,
        []
    ));
    static readonly move_coords_object = (OPCODES[0xf8e6] = new Opcode(
        0xf8e6,
        "move_coords_object ",
        [{ type: Type.Register }, { type: Type.Register }],
        false,
        []
    ));
    static readonly at_coords_call_ex = (OPCODES[0xf8e7] = new Opcode(
        0xf8e7,
        "at_coords_call_ex",
        [{ type: Type.Register }, { type: Type.Register }],
        false,
        []
    ));
    static readonly unknown_f8e8 = (OPCODES[0xf8e8] = new Opcode(
        0xf8e8,
        "unknown_f8e8",
        [{ type: Type.Register }, { type: Type.Register }],
        false,
        []
    ));
    static readonly unknown_f8e9 = (OPCODES[0xf8e9] = new Opcode(
        0xf8e9,
        "unknown_f8e9",
        [{ type: Type.Register }, { type: Type.Register }],
        false,
        []
    ));
    static readonly unknown_f8ea = (OPCODES[0xf8ea] = new Opcode(
        0xf8ea,
        "unknown_f8ea",
        [{ type: Type.Register }, { type: Type.Register }],
        false,
        []
    ));
    static readonly unknown_f8eb = (OPCODES[0xf8eb] = new Opcode(
        0xf8eb,
        "unknown_f8eb",
        [{ type: Type.Register }, { type: Type.Register }],
        false,
        []
    ));
    static readonly unknown_f8ec = (OPCODES[0xf8ec] = new Opcode(
        0xf8ec,
        "unknown_f8ec",
        [{ type: Type.Register }, { type: Type.Register }],
        false,
        []
    ));
    static readonly animation_check = (OPCODES[0xf8ed] = new Opcode(
        0xf8ed,
        "animation_check",
        [{ type: Type.Register }, { type: Type.Register }],
        false,
        []
    ));
    static readonly call_image_data = (OPCODES[0xf8ee] = new Opcode(
        0xf8ee,
        "call_image_data",
        [],
        false,
        [{ type: Type.U32 }, { type: Type.U16 }]
    ));
    static readonly unknown_f8ef = (OPCODES[0xf8ef] = new Opcode(
        0xf8ef,
        "unknown_f8ef",
        [],
        false,
        []
    ));
    static readonly turn_off_bgm_p2 = (OPCODES[0xf8f0] = new Opcode(
        0xf8f0,
        "turn_off_bgm_p2",
        [],
        false,
        []
    ));
    static readonly turn_on_bgm_p2 = (OPCODES[0xf8f1] = new Opcode(
        0xf8f1,
        "turn_on_bgm_p2",
        [],
        false,
        []
    ));
    static readonly load_unk_data = (OPCODES[0xf8f2] = new Opcode(
        0xf8f2,
        "load_unk_data",
        [],
        false,
        [
            { type: Type.U32 },
            { type: Type.U32 },
            { type: Type.U32 },
            { type: Type.U32 },
            { type: Type.Register },
            { type: Type.DLabel },
        ]
    ));
    static readonly particle2 = (OPCODES[0xf8f3] = new Opcode(0xf8f3, "particle2", [], false, [
        { type: Type.Register },
        { type: Type.U32 },
        { type: Type.F32 },
    ]));
    static readonly unknown_f8f4 = (OPCODES[0xf8f4] = new Opcode(
        0xf8f4,
        "unknown_f8f4",
        [],
        false,
        []
    ));
    static readonly unknown_f8f5 = (OPCODES[0xf8f5] = new Opcode(
        0xf8f5,
        "unknown_f8f5",
        [],
        false,
        []
    ));
    static readonly unknown_f8f6 = (OPCODES[0xf8f6] = new Opcode(
        0xf8f6,
        "unknown_f8f6",
        [],
        false,
        []
    ));
    static readonly unknown_f8f7 = (OPCODES[0xf8f7] = new Opcode(
        0xf8f7,
        "unknown_f8f7",
        [],
        false,
        []
    ));
    static readonly unknown_f8f8 = (OPCODES[0xf8f8] = new Opcode(
        0xf8f8,
        "unknown_f8f8",
        [],
        false,
        []
    ));
    static readonly unknown_f8f9 = (OPCODES[0xf8f9] = new Opcode(
        0xf8f9,
        "unknown_f8f9",
        [],
        false,
        []
    ));
    static readonly unknown_f8fa = (OPCODES[0xf8fa] = new Opcode(
        0xf8fa,
        "unknown_f8fa",
        [],
        false,
        []
    ));
    static readonly unknown_f8fb = (OPCODES[0xf8fb] = new Opcode(
        0xf8fb,
        "unknown_f8fb",
        [],
        false,
        []
    ));
    static readonly unknown_f8fc = (OPCODES[0xf8fc] = new Opcode(
        0xf8fc,
        "unknown_f8fc",
        [],
        false,
        []
    ));
    static readonly unknown_f8fd = (OPCODES[0xf8fd] = new Opcode(
        0xf8fd,
        "unknown_f8fd",
        [],
        false,
        []
    ));
    static readonly unknown_f8fe = (OPCODES[0xf8fe] = new Opcode(
        0xf8fe,
        "unknown_f8fe",
        [],
        false,
        []
    ));
    static readonly unknown_f8ff = (OPCODES[0xf8ff] = new Opcode(
        0xf8ff,
        "unknown_f8ff",
        [],
        false,
        []
    ));
    static readonly unknown_f900 = (OPCODES[0xf900] = new Opcode(
        0xf900,
        "unknown_f900",
        [],
        false,
        []
    ));
    static readonly dec2float = (OPCODES[0xf901] = new Opcode(
        0xf901,
        "dec2float",
        [{ type: Type.Register }, { type: Type.Register }],
        false,
        []
    ));
    static readonly float2dec = (OPCODES[0xf902] = new Opcode(
        0xf902,
        "float2dec",
        [{ type: Type.Register }, { type: Type.Register }],
        false,
        []
    ));
    static readonly flet = (OPCODES[0xf903] = new Opcode(
        0xf903,
        "flet",
        [{ type: Type.Register }, { type: Type.Register }],
        false,
        []
    ));
    static readonly fleti = (OPCODES[0xf904] = new Opcode(
        0xf904,
        "fleti",
        [{ type: Type.Register }, { type: Type.F32 }],
        false,
        []
    ));
    static readonly unknown_f905 = (OPCODES[0xf905] = new Opcode(
        0xf905,
        "unknown_f905",
        [],
        false,
        []
    ));
    static readonly unknown_f906 = (OPCODES[0xf906] = new Opcode(
        0xf906,
        "unknown_f906",
        [],
        false,
        []
    ));
    static readonly unknown_f907 = (OPCODES[0xf907] = new Opcode(
        0xf907,
        "unknown_f907",
        [],
        false,
        []
    ));
    static readonly fadd = (OPCODES[0xf908] = new Opcode(
        0xf908,
        "fadd",
        [{ type: Type.Register }, { type: Type.Register }],
        false,
        []
    ));
    static readonly faddi = (OPCODES[0xf909] = new Opcode(
        0xf909,
        "faddi",
        [{ type: Type.Register }, { type: Type.F32 }],
        false,
        []
    ));
    static readonly fsub = (OPCODES[0xf90a] = new Opcode(
        0xf90a,
        "fsub",
        [{ type: Type.Register }, { type: Type.Register }],
        false,
        []
    ));
    static readonly fsubi = (OPCODES[0xf90b] = new Opcode(
        0xf90b,
        "fsubi",
        [{ type: Type.Register }, { type: Type.F32 }],
        false,
        []
    ));
    static readonly fmul = (OPCODES[0xf90c] = new Opcode(
        0xf90c,
        "fmul",
        [{ type: Type.Register }, { type: Type.Register }],
        false,
        []
    ));
    static readonly fmuli = (OPCODES[0xf90d] = new Opcode(
        0xf90d,
        "fmuli",
        [{ type: Type.Register }, { type: Type.F32 }],
        false,
        []
    ));
    static readonly fdiv = (OPCODES[0xf90e] = new Opcode(
        0xf90e,
        "fdiv",
        [{ type: Type.Register }, { type: Type.Register }],
        false,
        []
    ));
    static readonly fdivi = (OPCODES[0xf90f] = new Opcode(
        0xf90f,
        "fdivi",
        [{ type: Type.Register }, { type: Type.F32 }],
        false,
        []
    ));
    static readonly get_unknown_count = (OPCODES[0xf910] = new Opcode(
        0xf910,
        "get_unknown_count",
        [],
        false,
        [{ type: Type.U32 }, { type: Type.Register }]
    ));
    static readonly get_stackable_item_count = (OPCODES[0xf911] = new Opcode(
        0xf911,
        "get_stackable_item_count",
        [{ type: Type.Register }, { type: Type.Register }],
        false,
        []
    ));
    static readonly freeze_and_hide_equip = (OPCODES[0xf912] = new Opcode(
        0xf912,
        "freeze_and_hide_equip",
        [],
        false,
        []
    ));
    static readonly thaw_and_show_equip = (OPCODES[0xf913] = new Opcode(
        0xf913,
        "thaw_and_show_equip",
        [],
        false,
        []
    ));
    static readonly set_palettex_callback = (OPCODES[0xf914] = new Opcode(
        0xf914,
        "set_palettex_callback",
        [],
        false,
        [{ type: Type.Register }, { type: Type.ILabel }]
    ));
    static readonly activate_palettex = (OPCODES[0xf915] = new Opcode(
        0xf915,
        "activate_palettex",
        [],
        false,
        [{ type: Type.Register }]
    ));
    static readonly enable_palettex = (OPCODES[0xf916] = new Opcode(
        0xf916,
        "enable_palettex",
        [],
        false,
        [{ type: Type.Register }]
    ));
    static readonly restore_palettex = (OPCODES[0xf917] = new Opcode(
        0xf917,
        "restore_palettex",
        [],
        false,
        [{ type: Type.U32 }]
    ));
    static readonly disable_palettex = (OPCODES[0xf918] = new Opcode(
        0xf918,
        "disable_palettex",
        [],
        false,
        [{ type: Type.U32 }]
    ));
    static readonly get_palettex_activated = (OPCODES[0xf919] = new Opcode(
        0xf919,
        "get_palettex_activated",
        [],
        false,
        [{ type: Type.U32 }, { type: Type.Register }]
    ));
    static readonly get_unknown_palettex_status = (OPCODES[0xf91a] = new Opcode(
        0xf91a,
        "get_unknown_palettex_status",
        [],
        false,
        [{ type: Type.U32 }, { type: Type.Register }]
    ));
    static readonly disable_movement2 = (OPCODES[0xf91b] = new Opcode(
        0xf91b,
        "disable_movement2",
        [],
        false,
        [{ type: Type.Register }]
    ));
    static readonly enable_movement2 = (OPCODES[0xf91c] = new Opcode(
        0xf91c,
        "enable_movement2",
        [],
        false,
        [{ type: Type.Register }]
    ));
    static readonly get_time_played = (OPCODES[0xf91d] = new Opcode(
        0xf91d,
        "get_time_played",
        [{ type: Type.Register }],
        false,
        []
    ));
    static readonly get_guildcard_total = (OPCODES[0xf91e] = new Opcode(
        0xf91e,
        "get_guildcard_total",
        [{ type: Type.Register }],
        false,
        []
    ));
    static readonly get_slot_meseta = (OPCODES[0xf91f] = new Opcode(
        0xf91f,
        "get_slot_meseta",
        [{ type: Type.Register }],
        false,
        []
    ));
    static readonly get_player_level = (OPCODES[0xf920] = new Opcode(
        0xf920,
        "get_player_level",
        [],
        false,
        [{ type: Type.U32 }, { type: Type.Register }]
    ));
    static readonly get_section_id = (OPCODES[0xf921] = new Opcode(
        0xf921,
        "get_section_id",
        [],
        false,
        [{ type: Type.U32 }, { type: Type.Register }]
    ));
    static readonly get_player_hp = (OPCODES[0xf922] = new Opcode(
        0xf922,
        "get_player_hp",
        [],
        false,
        [{ type: Type.Register }, { type: Type.Register }]
    ));
    static readonly get_floor_number = (OPCODES[0xf923] = new Opcode(
        0xf923,
        "get_floor_number",
        [],
        false,
        [{ type: Type.Register }, { type: Type.Register }]
    ));
    static readonly get_coord_player_detect = (OPCODES[0xf924] = new Opcode(
        0xf924,
        "get_coord_player_detect",
        [{ type: Type.Register }, { type: Type.Register }],
        false,
        []
    ));
    static readonly read_global_flag = (OPCODES[0xf925] = new Opcode(
        0xf925,
        "read_global_flag",
        [],
        false,
        [{ type: Type.U8 }, { type: Type.Register }]
    ));
    static readonly write_global_flag = (OPCODES[0xf926] = new Opcode(
        0xf926,
        "write_global_flag",
        [],
        false,
        [{ type: Type.U8 }, { type: Type.Register }]
    ));
    static readonly unknown_f927 = (OPCODES[0xf927] = new Opcode(
        0xf927,
        "unknown_f927",
        [{ type: Type.Register }, { type: Type.Register }],
        false,
        []
    ));
    static readonly floor_player_detect = (OPCODES[0xf928] = new Opcode(
        0xf928,
        "floor_player_detect",
        [{ type: Type.Register }],
        false,
        []
    ));
    static readonly read_disk_file = (OPCODES[0xf929] = new Opcode(
        0xf929,
        "read_disk_file",
        [],
        false,
        [{ type: Type.String }]
    ));
    static readonly open_pack_select = (OPCODES[0xf92a] = new Opcode(
        0xf92a,
        "open_pack_select",
        [],
        false,
        []
    ));
    static readonly item_select = (OPCODES[0xf92b] = new Opcode(
        0xf92b,
        "item_select",
        [{ type: Type.Register }],
        false,
        []
    ));
    static readonly get_item_id = (OPCODES[0xf92c] = new Opcode(
        0xf92c,
        "get_item_id",
        [{ type: Type.Register }],
        false,
        []
    ));
    static readonly color_change = (OPCODES[0xf92d] = new Opcode(
        0xf92d,
        "color_change",
        [],
        false,
        [
            { type: Type.Register },
            { type: Type.Register },
            { type: Type.Register },
            { type: Type.Register },
            { type: Type.Register },
        ]
    ));
    static readonly send_statistic = (OPCODES[0xf92e] = new Opcode(
        0xf92e,
        "send_statistic",
        [],
        false,
        [
            { type: Type.U32 },
            { type: Type.U32 },
            { type: Type.U32 },
            { type: Type.U32 },
            { type: Type.U32 },
            { type: Type.U32 },
            { type: Type.U32 },
            { type: Type.U32 },
        ]
    ));
    static readonly unknown_f92f = (OPCODES[0xf92f] = new Opcode(
        0xf92f,
        "unknown_f92f",
        [],
        false,
        [{ type: Type.U32 }, { type: Type.U32 }]
    ));
    static readonly chat_box = (OPCODES[0xf930] = new Opcode(0xf930, "chat_box", [], false, [
        { type: Type.U32 },
        { type: Type.U32 },
        { type: Type.U32 },
        { type: Type.U32 },
        { type: Type.U32 },
        { type: Type.String },
    ]));
    static readonly chat_bubble = (OPCODES[0xf931] = new Opcode(0xf931, "chat_bubble", [], false, [
        { type: Type.U32 },
        { type: Type.String },
    ]));
    static readonly unknown_f932 = (OPCODES[0xf932] = new Opcode(
        0xf932,
        "unknown_f932",
        [],
        false,
        []
    ));
    static readonly unknown_f933 = (OPCODES[0xf933] = new Opcode(
        0xf933,
        "unknown_f933",
        [{ type: Type.Register }],
        false,
        []
    ));
    static readonly scroll_text = (OPCODES[0xf934] = new Opcode(0xf934, "scroll_text", [], false, [
        { type: Type.U32 },
        { type: Type.U32 },
        { type: Type.U32 },
        { type: Type.U32 },
        { type: Type.U32 },
        { type: Type.F32 },
        { type: Type.Register },
        { type: Type.String },
    ]));
    static readonly gba_unknown1 = (OPCODES[0xf935] = new Opcode(
        0xf935,
        "gba_unknown1",
        [],
        false,
        []
    ));
    static readonly gba_unknown2 = (OPCODES[0xf936] = new Opcode(
        0xf936,
        "gba_unknown2",
        [],
        false,
        []
    ));
    static readonly gba_unknown3 = (OPCODES[0xf937] = new Opcode(
        0xf937,
        "gba_unknown3",
        [],
        false,
        []
    ));
    static readonly add_damage_to = (OPCODES[0xf938] = new Opcode(
        0xf938,
        "add_damage_to",
        [],
        false,
        [{ type: Type.U32 }, { type: Type.U32 }]
    ));
    static readonly item_delete3 = (OPCODES[0xf939] = new Opcode(
        0xf939,
        "item_delete3",
        [],
        false,
        [{ type: Type.U32 }]
    ));
    static readonly get_item_info = (OPCODES[0xf93a] = new Opcode(
        0xf93a,
        "get_item_info",
        [],
        false,
        [{ type: Type.U32 }, { type: Type.Register }]
    ));
    static readonly item_packing1 = (OPCODES[0xf93b] = new Opcode(
        0xf93b,
        "item_packing1",
        [],
        false,
        [{ type: Type.U32 }]
    ));
    static readonly item_packing2 = (OPCODES[0xf93c] = new Opcode(
        0xf93c,
        "item_packing2",
        [],
        false,
        [{ type: Type.U32 }, { type: Type.U32 }]
    ));
    static readonly get_lang_setting = (OPCODES[0xf93d] = new Opcode(
        0xf93d,
        "get_lang_setting",
        [],
        false,
        [{ type: Type.Register }]
    ));
    static readonly prepare_statistic = (OPCODES[0xf93e] = new Opcode(
        0xf93e,
        "prepare_statistic",
        [],
        false,
        [{ type: Type.U32 }, { type: Type.ILabel }, { type: Type.ILabel }]
    ));
    static readonly keyword_detect = (OPCODES[0xf93f] = new Opcode(
        0xf93f,
        "keyword_detect",
        [],
        false,
        []
    ));
    static readonly keyword = (OPCODES[0xf940] = new Opcode(0xf940, "keyword", [], false, [
        { type: Type.Register },
        { type: Type.U32 },
        { type: Type.String },
    ]));
    static readonly get_guildcard_num = (OPCODES[0xf941] = new Opcode(
        0xf941,
        "get_guildcard_num",
        [],
        false,
        [{ type: Type.U32 }, { type: Type.Register }]
    ));
    static readonly unknown_f942 = (OPCODES[0xf942] = new Opcode(
        0xf942,
        "unknown_f942",
        [],
        false,
        []
    ));
    static readonly unknown_f943 = (OPCODES[0xf943] = new Opcode(
        0xf943,
        "unknown_f943",
        [],
        false,
        []
    ));
    static readonly get_wrap_status = (OPCODES[0xf944] = new Opcode(
        0xf944,
        "get_wrap_status",
        [],
        false,
        [{ type: Type.U32 }, { type: Type.Register }]
    ));
    static readonly initial_floor = (OPCODES[0xf945] = new Opcode(
        0xf945,
        "initial_floor",
        [],
        false,
        [{ type: Type.U32 }]
    ));
    static readonly sin = (OPCODES[0xf946] = new Opcode(0xf946, "sin", [], false, [
        { type: Type.Register },
        { type: Type.U32 },
    ]));
    static readonly cos = (OPCODES[0xf947] = new Opcode(0xf947, "cos", [], false, [
        { type: Type.Register },
        { type: Type.U32 },
    ]));
    static readonly unknown_f948 = (OPCODES[0xf948] = new Opcode(
        0xf948,
        "unknown_f948",
        [],
        false,
        []
    ));
    static readonly unknown_f949 = (OPCODES[0xf949] = new Opcode(
        0xf949,
        "unknown_f949",
        [],
        false,
        []
    ));
    static readonly boss_is_dead2 = (OPCODES[0xf94a] = new Opcode(
        0xf94a,
        "boss_is_dead2",
        [{ type: Type.Register }],
        false,
        []
    ));
    static readonly unknown_f94b = (OPCODES[0xf94b] = new Opcode(
        0xf94b,
        "unknown_f94b",
        [{ type: Type.Register }],
        false,
        []
    ));
    static readonly unknown_f94c = (OPCODES[0xf94c] = new Opcode(
        0xf94c,
        "unknown_f94c",
        [{ type: Type.Register }],
        false,
        []
    ));
    static readonly is_there_cardbattle = (OPCODES[0xf94d] = new Opcode(
        0xf94d,
        "is_there_cardbattle",
        [{ type: Type.Register }],
        false,
        []
    ));
    static readonly unknown_f94e = (OPCODES[0xf94e] = new Opcode(
        0xf94e,
        "unknown_f94e",
        [],
        false,
        []
    ));
    static readonly unknown_f94f = (OPCODES[0xf94f] = new Opcode(
        0xf94f,
        "unknown_f94f",
        [],
        false,
        []
    ));
    static readonly bb_p2_menu = (OPCODES[0xf950] = new Opcode(0xf950, "bb_p2_menu", [], false, [
        { type: Type.U32 },
    ]));
    static readonly bb_map_designate = (OPCODES[0xf951] = new Opcode(
        0xf951,
        "bb_map_designate",
        [{ type: Type.U8 }, { type: Type.U16 }, { type: Type.U8 }, { type: Type.U8 }],
        false,
        []
    ));
    static readonly bb_get_number_in_pack = (OPCODES[0xf952] = new Opcode(
        0xf952,
        "bb_get_number_in_pack",
        [{ type: Type.Register }],
        false,
        []
    ));
    static readonly bb_swap_item = (OPCODES[0xf953] = new Opcode(
        0xf953,
        "bb_swap_item",
        [],
        false,
        [
            { type: Type.U32 },
            { type: Type.U32 },
            { type: Type.U32 },
            { type: Type.U32 },
            { type: Type.U32 },
            { type: Type.U32 },
            { type: Type.ILabel },
            { type: Type.ILabel },
        ]
    ));
    static readonly bb_check_wrap = (OPCODES[0xf954] = new Opcode(
        0xf954,
        "bb_check_wrap",
        [],
        false,
        [{ type: Type.Register }, { type: Type.Register }]
    ));
    static readonly bb_exchange_pd_item = (OPCODES[0xf955] = new Opcode(
        0xf955,
        "bb_exchange_pd_item",
        [],
        false,
        [
            { type: Type.Register },
            { type: Type.Register },
            { type: Type.Register },
            { type: Type.ILabel },
            { type: Type.ILabel },
        ]
    ));
    static readonly bb_exchange_pd_srank = (OPCODES[0xf956] = new Opcode(
        0xf956,
        "bb_exchange_pd_srank",
        [],
        false,
        [
            { type: Type.Register },
            { type: Type.Register },
            { type: Type.Register },
            { type: Type.Register },
            { type: Type.Register },
            { type: Type.ILabel },
            { type: Type.ILabel },
        ]
    ));
    static readonly bb_exchange_pd_special = (OPCODES[0xf957] = new Opcode(
        0xf957,
        "bb_exchange_pd_special",
        [],
        false,
        [
            { type: Type.Register },
            { type: Type.Register },
            { type: Type.Register },
            { type: Type.Register },
            { type: Type.Register },
            { type: Type.U32 },
            { type: Type.ILabel },
            { type: Type.ILabel },
        ]
    ));
    static readonly bb_exchange_pd_percent = (OPCODES[0xf958] = new Opcode(
        0xf958,
        "bb_exchange_pd_percent",
        [],
        false,
        [
            { type: Type.Register },
            { type: Type.Register },
            { type: Type.Register },
            { type: Type.Register },
            { type: Type.Register },
            { type: Type.U32 },
            { type: Type.ILabel },
            { type: Type.ILabel },
        ]
    ));
    static readonly unknown_f959 = (OPCODES[0xf959] = new Opcode(
        0xf959,
        "unknown_f959",
        [],
        false,
        [{ type: Type.U32 }]
    ));
    static readonly unknown_f95a = (OPCODES[0xf95a] = new Opcode(
        0xf95a,
        "unknown_f95a",
        [],
        false,
        []
    ));
    static readonly unknown_f95b = (OPCODES[0xf95b] = new Opcode(
        0xf95b,
        "unknown_f95b",
        [],
        false,
        []
    ));
    static readonly bb_exchange_slt = (OPCODES[0xf95c] = new Opcode(
        0xf95c,
        "bb_exchange_slt",
        [],
        false,
        [{ type: Type.U32 }, { type: Type.Register }, { type: Type.ILabel }, { type: Type.ILabel }]
    ));
    static readonly bb_exchange_pc = (OPCODES[0xf95d] = new Opcode(
        0xf95d,
        "bb_exchange_pc",
        [],
        false,
        []
    ));
    static readonly bb_box_create_bp = (OPCODES[0xf95e] = new Opcode(
        0xf95e,
        "bb_box_create_bp",
        [],
        false,
        [{ type: Type.U32 }, { type: Type.F32 }, { type: Type.F32 }]
    ));
    static readonly bb_exchange_pt = (OPCODES[0xf95f] = new Opcode(
        0xf95f,
        "bb_exchange_pt",
        [],
        false,
        [
            { type: Type.Register },
            { type: Type.Register },
            { type: Type.U32 },
            { type: Type.ILabel },
            { type: Type.ILabel },
        ]
    ));
    static readonly unknown_f960 = (OPCODES[0xf960] = new Opcode(
        0xf960,
        "unknown_f960",
        [],
        false,
        [{ type: Type.U32 }]
    ));
    static readonly unknown_f961 = (OPCODES[0xf961] = new Opcode(
        0xf961,
        "unknown_f961",
        [],
        false,
        []
    ));
    static readonly unknown_f962 = (OPCODES[0xf962] = new Opcode(
        0xf962,
        "unknown_f962",
        [],
        false,
        []
    ));
    static readonly unknown_f963 = (OPCODES[0xf963] = new Opcode(
        0xf963,
        "unknown_f963",
        [],
        false,
        []
    ));
    static readonly unknown_f964 = (OPCODES[0xf964] = new Opcode(
        0xf964,
        "unknown_f964",
        [],
        false,
        []
    ));
    static readonly unknown_f965 = (OPCODES[0xf965] = new Opcode(
        0xf965,
        "unknown_f965",
        [],
        false,
        []
    ));
    static readonly unknown_f966 = (OPCODES[0xf966] = new Opcode(
        0xf966,
        "unknown_f966",
        [],
        false,
        []
    ));
    static readonly unknown_f967 = (OPCODES[0xf967] = new Opcode(
        0xf967,
        "unknown_f967",
        [],
        false,
        []
    ));
    static readonly unknown_f968 = (OPCODES[0xf968] = new Opcode(
        0xf968,
        "unknown_f968",
        [],
        false,
        []
    ));
    static readonly unknown_f969 = (OPCODES[0xf969] = new Opcode(
        0xf969,
        "unknown_f969",
        [],
        false,
        []
    ));
    static readonly unknown_f96a = (OPCODES[0xf96a] = new Opcode(
        0xf96a,
        "unknown_f96a",
        [],
        false,
        []
    ));
    static readonly unknown_f96b = (OPCODES[0xf96b] = new Opcode(
        0xf96b,
        "unknown_f96b",
        [],
        false,
        []
    ));
    static readonly unknown_f96c = (OPCODES[0xf96c] = new Opcode(
        0xf96c,
        "unknown_f96c",
        [],
        false,
        []
    ));
    static readonly unknown_f96d = (OPCODES[0xf96d] = new Opcode(
        0xf96d,
        "unknown_f96d",
        [],
        false,
        []
    ));
    static readonly unknown_f96e = (OPCODES[0xf96e] = new Opcode(
        0xf96e,
        "unknown_f96e",
        [],
        false,
        []
    ));
    static readonly unknown_f96f = (OPCODES[0xf96f] = new Opcode(
        0xf96f,
        "unknown_f96f",
        [],
        false,
        []
    ));
    static readonly unknown_f970 = (OPCODES[0xf970] = new Opcode(
        0xf970,
        "unknown_f970",
        [],
        false,
        []
    ));
    static readonly unknown_f971 = (OPCODES[0xf971] = new Opcode(
        0xf971,
        "unknown_f971",
        [],
        false,
        []
    ));
    static readonly unknown_f972 = (OPCODES[0xf972] = new Opcode(
        0xf972,
        "unknown_f972",
        [],
        false,
        []
    ));
    static readonly unknown_f973 = (OPCODES[0xf973] = new Opcode(
        0xf973,
        "unknown_f973",
        [],
        false,
        []
    ));
    static readonly unknown_f974 = (OPCODES[0xf974] = new Opcode(
        0xf974,
        "unknown_f974",
        [],
        false,
        []
    ));
    static readonly unknown_f975 = (OPCODES[0xf975] = new Opcode(
        0xf975,
        "unknown_f975",
        [],
        false,
        []
    ));
    static readonly unknown_f976 = (OPCODES[0xf976] = new Opcode(
        0xf976,
        "unknown_f976",
        [],
        false,
        []
    ));
    static readonly unknown_f977 = (OPCODES[0xf977] = new Opcode(
        0xf977,
        "unknown_f977",
        [],
        false,
        []
    ));
    static readonly unknown_f978 = (OPCODES[0xf978] = new Opcode(
        0xf978,
        "unknown_f978",
        [],
        false,
        []
    ));
    static readonly unknown_f979 = (OPCODES[0xf979] = new Opcode(
        0xf979,
        "unknown_f979",
        [],
        false,
        []
    ));
    static readonly unknown_f97a = (OPCODES[0xf97a] = new Opcode(
        0xf97a,
        "unknown_f97a",
        [],
        false,
        []
    ));
    static readonly unknown_f97b = (OPCODES[0xf97b] = new Opcode(
        0xf97b,
        "unknown_f97b",
        [],
        false,
        []
    ));
    static readonly unknown_f97c = (OPCODES[0xf97c] = new Opcode(
        0xf97c,
        "unknown_f97c",
        [],
        false,
        []
    ));
    static readonly unknown_f97d = (OPCODES[0xf97d] = new Opcode(
        0xf97d,
        "unknown_f97d",
        [],
        false,
        []
    ));
    static readonly unknown_f97e = (OPCODES[0xf97e] = new Opcode(
        0xf97e,
        "unknown_f97e",
        [],
        false,
        []
    ));
    static readonly unknown_f97f = (OPCODES[0xf97f] = new Opcode(
        0xf97f,
        "unknown_f97f",
        [],
        false,
        []
    ));
    static readonly unknown_f980 = (OPCODES[0xf980] = new Opcode(
        0xf980,
        "unknown_f980",
        [],
        false,
        []
    ));
    static readonly unknown_f981 = (OPCODES[0xf981] = new Opcode(
        0xf981,
        "unknown_f981",
        [],
        false,
        []
    ));
    static readonly unknown_f982 = (OPCODES[0xf982] = new Opcode(
        0xf982,
        "unknown_f982",
        [],
        false,
        []
    ));
    static readonly unknown_f983 = (OPCODES[0xf983] = new Opcode(
        0xf983,
        "unknown_f983",
        [],
        false,
        []
    ));
    static readonly unknown_f984 = (OPCODES[0xf984] = new Opcode(
        0xf984,
        "unknown_f984",
        [],
        false,
        []
    ));
    static readonly unknown_f985 = (OPCODES[0xf985] = new Opcode(
        0xf985,
        "unknown_f985",
        [],
        false,
        []
    ));
    static readonly unknown_f986 = (OPCODES[0xf986] = new Opcode(
        0xf986,
        "unknown_f986",
        [],
        false,
        []
    ));
    static readonly unknown_f987 = (OPCODES[0xf987] = new Opcode(
        0xf987,
        "unknown_f987",
        [],
        false,
        []
    ));
    static readonly unknown_f988 = (OPCODES[0xf988] = new Opcode(
        0xf988,
        "unknown_f988",
        [],
        false,
        []
    ));
    static readonly unknown_f989 = (OPCODES[0xf989] = new Opcode(
        0xf989,
        "unknown_f989",
        [],
        false,
        []
    ));
    static readonly unknown_f98a = (OPCODES[0xf98a] = new Opcode(
        0xf98a,
        "unknown_f98a",
        [],
        false,
        []
    ));
    static readonly unknown_f98b = (OPCODES[0xf98b] = new Opcode(
        0xf98b,
        "unknown_f98b",
        [],
        false,
        []
    ));
    static readonly unknown_f98c = (OPCODES[0xf98c] = new Opcode(
        0xf98c,
        "unknown_f98c",
        [],
        false,
        []
    ));
    static readonly unknown_f98d = (OPCODES[0xf98d] = new Opcode(
        0xf98d,
        "unknown_f98d",
        [],
        false,
        []
    ));
    static readonly unknown_f98e = (OPCODES[0xf98e] = new Opcode(
        0xf98e,
        "unknown_f98e",
        [],
        false,
        []
    ));
    static readonly unknown_f98f = (OPCODES[0xf98f] = new Opcode(
        0xf98f,
        "unknown_f98f",
        [],
        false,
        []
    ));
    static readonly unknown_f990 = (OPCODES[0xf990] = new Opcode(
        0xf990,
        "unknown_f990",
        [],
        false,
        []
    ));
    static readonly unknown_f991 = (OPCODES[0xf991] = new Opcode(
        0xf991,
        "unknown_f991",
        [],
        false,
        []
    ));
    static readonly unknown_f992 = (OPCODES[0xf992] = new Opcode(
        0xf992,
        "unknown_f992",
        [],
        false,
        []
    ));
    static readonly unknown_f993 = (OPCODES[0xf993] = new Opcode(
        0xf993,
        "unknown_f993",
        [],
        false,
        []
    ));
    static readonly unknown_f994 = (OPCODES[0xf994] = new Opcode(
        0xf994,
        "unknown_f994",
        [],
        false,
        []
    ));
    static readonly unknown_f995 = (OPCODES[0xf995] = new Opcode(
        0xf995,
        "unknown_f995",
        [],
        false,
        []
    ));
    static readonly unknown_f996 = (OPCODES[0xf996] = new Opcode(
        0xf996,
        "unknown_f996",
        [],
        false,
        []
    ));
    static readonly unknown_f997 = (OPCODES[0xf997] = new Opcode(
        0xf997,
        "unknown_f997",
        [],
        false,
        []
    ));
    static readonly unknown_f998 = (OPCODES[0xf998] = new Opcode(
        0xf998,
        "unknown_f998",
        [],
        false,
        []
    ));
    static readonly unknown_f999 = (OPCODES[0xf999] = new Opcode(
        0xf999,
        "unknown_f999",
        [],
        false,
        []
    ));
    static readonly unknown_f99a = (OPCODES[0xf99a] = new Opcode(
        0xf99a,
        "unknown_f99a",
        [],
        false,
        []
    ));
    static readonly unknown_f99b = (OPCODES[0xf99b] = new Opcode(
        0xf99b,
        "unknown_f99b",
        [],
        false,
        []
    ));
    static readonly unknown_f99c = (OPCODES[0xf99c] = new Opcode(
        0xf99c,
        "unknown_f99c",
        [],
        false,
        []
    ));
    static readonly unknown_f99d = (OPCODES[0xf99d] = new Opcode(
        0xf99d,
        "unknown_f99d",
        [],
        false,
        []
    ));
    static readonly unknown_f99e = (OPCODES[0xf99e] = new Opcode(
        0xf99e,
        "unknown_f99e",
        [],
        false,
        []
    ));
    static readonly unknown_f99f = (OPCODES[0xf99f] = new Opcode(
        0xf99f,
        "unknown_f99f",
        [],
        false,
        []
    ));
    static readonly unknown_f9a0 = (OPCODES[0xf9a0] = new Opcode(
        0xf9a0,
        "unknown_f9a0",
        [],
        false,
        []
    ));
    static readonly unknown_f9a1 = (OPCODES[0xf9a1] = new Opcode(
        0xf9a1,
        "unknown_f9a1",
        [],
        false,
        []
    ));
    static readonly unknown_f9a2 = (OPCODES[0xf9a2] = new Opcode(
        0xf9a2,
        "unknown_f9a2",
        [],
        false,
        []
    ));
    static readonly unknown_f9a3 = (OPCODES[0xf9a3] = new Opcode(
        0xf9a3,
        "unknown_f9a3",
        [],
        false,
        []
    ));
    static readonly unknown_f9a4 = (OPCODES[0xf9a4] = new Opcode(
        0xf9a4,
        "unknown_f9a4",
        [],
        false,
        []
    ));
    static readonly unknown_f9a5 = (OPCODES[0xf9a5] = new Opcode(
        0xf9a5,
        "unknown_f9a5",
        [],
        false,
        []
    ));
    static readonly unknown_f9a6 = (OPCODES[0xf9a6] = new Opcode(
        0xf9a6,
        "unknown_f9a6",
        [],
        false,
        []
    ));
    static readonly unknown_f9a7 = (OPCODES[0xf9a7] = new Opcode(
        0xf9a7,
        "unknown_f9a7",
        [],
        false,
        []
    ));
    static readonly unknown_f9a8 = (OPCODES[0xf9a8] = new Opcode(
        0xf9a8,
        "unknown_f9a8",
        [],
        false,
        []
    ));
    static readonly unknown_f9a9 = (OPCODES[0xf9a9] = new Opcode(
        0xf9a9,
        "unknown_f9a9",
        [],
        false,
        []
    ));
    static readonly unknown_f9aa = (OPCODES[0xf9aa] = new Opcode(
        0xf9aa,
        "unknown_f9aa",
        [],
        false,
        []
    ));
    static readonly unknown_f9ab = (OPCODES[0xf9ab] = new Opcode(
        0xf9ab,
        "unknown_f9ab",
        [],
        false,
        []
    ));
    static readonly unknown_f9ac = (OPCODES[0xf9ac] = new Opcode(
        0xf9ac,
        "unknown_f9ac",
        [],
        false,
        []
    ));
    static readonly unknown_f9ad = (OPCODES[0xf9ad] = new Opcode(
        0xf9ad,
        "unknown_f9ad",
        [],
        false,
        []
    ));
    static readonly unknown_f9ae = (OPCODES[0xf9ae] = new Opcode(
        0xf9ae,
        "unknown_f9ae",
        [],
        false,
        []
    ));
    static readonly unknown_f9af = (OPCODES[0xf9af] = new Opcode(
        0xf9af,
        "unknown_f9af",
        [],
        false,
        []
    ));
    static readonly unknown_f9b0 = (OPCODES[0xf9b0] = new Opcode(
        0xf9b0,
        "unknown_f9b0",
        [],
        false,
        []
    ));
    static readonly unknown_f9b1 = (OPCODES[0xf9b1] = new Opcode(
        0xf9b1,
        "unknown_f9b1",
        [],
        false,
        []
    ));
    static readonly unknown_f9b2 = (OPCODES[0xf9b2] = new Opcode(
        0xf9b2,
        "unknown_f9b2",
        [],
        false,
        []
    ));
    static readonly unknown_f9b3 = (OPCODES[0xf9b3] = new Opcode(
        0xf9b3,
        "unknown_f9b3",
        [],
        false,
        []
    ));
    static readonly unknown_f9b4 = (OPCODES[0xf9b4] = new Opcode(
        0xf9b4,
        "unknown_f9b4",
        [],
        false,
        []
    ));
    static readonly unknown_f9b5 = (OPCODES[0xf9b5] = new Opcode(
        0xf9b5,
        "unknown_f9b5",
        [],
        false,
        []
    ));
    static readonly unknown_f9b6 = (OPCODES[0xf9b6] = new Opcode(
        0xf9b6,
        "unknown_f9b6",
        [],
        false,
        []
    ));
    static readonly unknown_f9b7 = (OPCODES[0xf9b7] = new Opcode(
        0xf9b7,
        "unknown_f9b7",
        [],
        false,
        []
    ));
    static readonly unknown_f9b8 = (OPCODES[0xf9b8] = new Opcode(
        0xf9b8,
        "unknown_f9b8",
        [],
        false,
        []
    ));
    static readonly unknown_f9b9 = (OPCODES[0xf9b9] = new Opcode(
        0xf9b9,
        "unknown_f9b9",
        [],
        false,
        []
    ));
    static readonly unknown_f9ba = (OPCODES[0xf9ba] = new Opcode(
        0xf9ba,
        "unknown_f9ba",
        [],
        false,
        []
    ));
    static readonly unknown_f9bb = (OPCODES[0xf9bb] = new Opcode(
        0xf9bb,
        "unknown_f9bb",
        [],
        false,
        []
    ));
    static readonly unknown_f9bc = (OPCODES[0xf9bc] = new Opcode(
        0xf9bc,
        "unknown_f9bc",
        [],
        false,
        []
    ));
    static readonly unknown_f9bd = (OPCODES[0xf9bd] = new Opcode(
        0xf9bd,
        "unknown_f9bd",
        [],
        false,
        []
    ));
    static readonly unknown_f9be = (OPCODES[0xf9be] = new Opcode(
        0xf9be,
        "unknown_f9be",
        [],
        false,
        []
    ));
    static readonly unknown_f9bf = (OPCODES[0xf9bf] = new Opcode(
        0xf9bf,
        "unknown_f9bf",
        [],
        false,
        []
    ));
    static readonly unknown_f9c0 = (OPCODES[0xf9c0] = new Opcode(
        0xf9c0,
        "unknown_f9c0",
        [],
        false,
        []
    ));
    static readonly unknown_f9c1 = (OPCODES[0xf9c1] = new Opcode(
        0xf9c1,
        "unknown_f9c1",
        [],
        false,
        []
    ));
    static readonly unknown_f9c2 = (OPCODES[0xf9c2] = new Opcode(
        0xf9c2,
        "unknown_f9c2",
        [],
        false,
        []
    ));
    static readonly unknown_f9c3 = (OPCODES[0xf9c3] = new Opcode(
        0xf9c3,
        "unknown_f9c3",
        [],
        false,
        []
    ));
    static readonly unknown_f9c4 = (OPCODES[0xf9c4] = new Opcode(
        0xf9c4,
        "unknown_f9c4",
        [],
        false,
        []
    ));
    static readonly unknown_f9c5 = (OPCODES[0xf9c5] = new Opcode(
        0xf9c5,
        "unknown_f9c5",
        [],
        false,
        []
    ));
    static readonly unknown_f9c6 = (OPCODES[0xf9c6] = new Opcode(
        0xf9c6,
        "unknown_f9c6",
        [],
        false,
        []
    ));
    static readonly unknown_f9c7 = (OPCODES[0xf9c7] = new Opcode(
        0xf9c7,
        "unknown_f9c7",
        [],
        false,
        []
    ));
    static readonly unknown_f9c8 = (OPCODES[0xf9c8] = new Opcode(
        0xf9c8,
        "unknown_f9c8",
        [],
        false,
        []
    ));
    static readonly unknown_f9c9 = (OPCODES[0xf9c9] = new Opcode(
        0xf9c9,
        "unknown_f9c9",
        [],
        false,
        []
    ));
    static readonly unknown_f9ca = (OPCODES[0xf9ca] = new Opcode(
        0xf9ca,
        "unknown_f9ca",
        [],
        false,
        []
    ));
    static readonly unknown_f9cb = (OPCODES[0xf9cb] = new Opcode(
        0xf9cb,
        "unknown_f9cb",
        [],
        false,
        []
    ));
    static readonly unknown_f9cc = (OPCODES[0xf9cc] = new Opcode(
        0xf9cc,
        "unknown_f9cc",
        [],
        false,
        []
    ));
    static readonly unknown_f9cd = (OPCODES[0xf9cd] = new Opcode(
        0xf9cd,
        "unknown_f9cd",
        [],
        false,
        []
    ));
    static readonly unknown_f9ce = (OPCODES[0xf9ce] = new Opcode(
        0xf9ce,
        "unknown_f9ce",
        [],
        false,
        []
    ));
    static readonly unknown_f9cf = (OPCODES[0xf9cf] = new Opcode(
        0xf9cf,
        "unknown_f9cf",
        [],
        false,
        []
    ));
    static readonly unknown_f9d0 = (OPCODES[0xf9d0] = new Opcode(
        0xf9d0,
        "unknown_f9d0",
        [],
        false,
        []
    ));
    static readonly unknown_f9d1 = (OPCODES[0xf9d1] = new Opcode(
        0xf9d1,
        "unknown_f9d1",
        [],
        false,
        []
    ));
    static readonly unknown_f9d2 = (OPCODES[0xf9d2] = new Opcode(
        0xf9d2,
        "unknown_f9d2",
        [],
        false,
        []
    ));
    static readonly unknown_f9d3 = (OPCODES[0xf9d3] = new Opcode(
        0xf9d3,
        "unknown_f9d3",
        [],
        false,
        []
    ));
    static readonly unknown_f9d4 = (OPCODES[0xf9d4] = new Opcode(
        0xf9d4,
        "unknown_f9d4",
        [],
        false,
        []
    ));
    static readonly unknown_f9d5 = (OPCODES[0xf9d5] = new Opcode(
        0xf9d5,
        "unknown_f9d5",
        [],
        false,
        []
    ));
    static readonly unknown_f9d6 = (OPCODES[0xf9d6] = new Opcode(
        0xf9d6,
        "unknown_f9d6",
        [],
        false,
        []
    ));
    static readonly unknown_f9d7 = (OPCODES[0xf9d7] = new Opcode(
        0xf9d7,
        "unknown_f9d7",
        [],
        false,
        []
    ));
    static readonly unknown_f9d8 = (OPCODES[0xf9d8] = new Opcode(
        0xf9d8,
        "unknown_f9d8",
        [],
        false,
        []
    ));
    static readonly unknown_f9d9 = (OPCODES[0xf9d9] = new Opcode(
        0xf9d9,
        "unknown_f9d9",
        [],
        false,
        []
    ));
    static readonly unknown_f9da = (OPCODES[0xf9da] = new Opcode(
        0xf9da,
        "unknown_f9da",
        [],
        false,
        []
    ));
    static readonly unknown_f9db = (OPCODES[0xf9db] = new Opcode(
        0xf9db,
        "unknown_f9db",
        [],
        false,
        []
    ));
    static readonly unknown_f9dc = (OPCODES[0xf9dc] = new Opcode(
        0xf9dc,
        "unknown_f9dc",
        [],
        false,
        []
    ));
    static readonly unknown_f9dd = (OPCODES[0xf9dd] = new Opcode(
        0xf9dd,
        "unknown_f9dd",
        [],
        false,
        []
    ));
    static readonly unknown_f9de = (OPCODES[0xf9de] = new Opcode(
        0xf9de,
        "unknown_f9de",
        [],
        false,
        []
    ));
    static readonly unknown_f9df = (OPCODES[0xf9df] = new Opcode(
        0xf9df,
        "unknown_f9df",
        [],
        false,
        []
    ));
    static readonly unknown_f9e0 = (OPCODES[0xf9e0] = new Opcode(
        0xf9e0,
        "unknown_f9e0",
        [],
        false,
        []
    ));
    static readonly unknown_f9e1 = (OPCODES[0xf9e1] = new Opcode(
        0xf9e1,
        "unknown_f9e1",
        [],
        false,
        []
    ));
    static readonly unknown_f9e2 = (OPCODES[0xf9e2] = new Opcode(
        0xf9e2,
        "unknown_f9e2",
        [],
        false,
        []
    ));
    static readonly unknown_f9e3 = (OPCODES[0xf9e3] = new Opcode(
        0xf9e3,
        "unknown_f9e3",
        [],
        false,
        []
    ));
    static readonly unknown_f9e4 = (OPCODES[0xf9e4] = new Opcode(
        0xf9e4,
        "unknown_f9e4",
        [],
        false,
        []
    ));
    static readonly unknown_f9e5 = (OPCODES[0xf9e5] = new Opcode(
        0xf9e5,
        "unknown_f9e5",
        [],
        false,
        []
    ));
    static readonly unknown_f9e6 = (OPCODES[0xf9e6] = new Opcode(
        0xf9e6,
        "unknown_f9e6",
        [],
        false,
        []
    ));
    static readonly unknown_f9e7 = (OPCODES[0xf9e7] = new Opcode(
        0xf9e7,
        "unknown_f9e7",
        [],
        false,
        []
    ));
    static readonly unknown_f9e8 = (OPCODES[0xf9e8] = new Opcode(
        0xf9e8,
        "unknown_f9e8",
        [],
        false,
        []
    ));
    static readonly unknown_f9e9 = (OPCODES[0xf9e9] = new Opcode(
        0xf9e9,
        "unknown_f9e9",
        [],
        false,
        []
    ));
    static readonly unknown_f9ea = (OPCODES[0xf9ea] = new Opcode(
        0xf9ea,
        "unknown_f9ea",
        [],
        false,
        []
    ));
    static readonly unknown_f9eb = (OPCODES[0xf9eb] = new Opcode(
        0xf9eb,
        "unknown_f9eb",
        [],
        false,
        []
    ));
    static readonly unknown_f9ec = (OPCODES[0xf9ec] = new Opcode(
        0xf9ec,
        "unknown_f9ec",
        [],
        false,
        []
    ));
    static readonly unknown_f9ed = (OPCODES[0xf9ed] = new Opcode(
        0xf9ed,
        "unknown_f9ed",
        [],
        false,
        []
    ));
    static readonly unknown_f9ee = (OPCODES[0xf9ee] = new Opcode(
        0xf9ee,
        "unknown_f9ee",
        [],
        false,
        []
    ));
    static readonly unknown_f9ef = (OPCODES[0xf9ef] = new Opcode(
        0xf9ef,
        "unknown_f9ef",
        [],
        false,
        []
    ));
    static readonly unknown_f9f0 = (OPCODES[0xf9f0] = new Opcode(
        0xf9f0,
        "unknown_f9f0",
        [],
        false,
        []
    ));
    static readonly unknown_f9f1 = (OPCODES[0xf9f1] = new Opcode(
        0xf9f1,
        "unknown_f9f1",
        [],
        false,
        []
    ));
    static readonly unknown_f9f2 = (OPCODES[0xf9f2] = new Opcode(
        0xf9f2,
        "unknown_f9f2",
        [],
        false,
        []
    ));
    static readonly unknown_f9f3 = (OPCODES[0xf9f3] = new Opcode(
        0xf9f3,
        "unknown_f9f3",
        [],
        false,
        []
    ));
    static readonly unknown_f9f4 = (OPCODES[0xf9f4] = new Opcode(
        0xf9f4,
        "unknown_f9f4",
        [],
        false,
        []
    ));
    static readonly unknown_f9f5 = (OPCODES[0xf9f5] = new Opcode(
        0xf9f5,
        "unknown_f9f5",
        [],
        false,
        []
    ));
    static readonly unknown_f9f6 = (OPCODES[0xf9f6] = new Opcode(
        0xf9f6,
        "unknown_f9f6",
        [],
        false,
        []
    ));
    static readonly unknown_f9f7 = (OPCODES[0xf9f7] = new Opcode(
        0xf9f7,
        "unknown_f9f7",
        [],
        false,
        []
    ));
    static readonly unknown_f9f8 = (OPCODES[0xf9f8] = new Opcode(
        0xf9f8,
        "unknown_f9f8",
        [],
        false,
        []
    ));
    static readonly unknown_f9f9 = (OPCODES[0xf9f9] = new Opcode(
        0xf9f9,
        "unknown_f9f9",
        [],
        false,
        []
    ));
    static readonly unknown_f9fa = (OPCODES[0xf9fa] = new Opcode(
        0xf9fa,
        "unknown_f9fa",
        [],
        false,
        []
    ));
    static readonly unknown_f9fb = (OPCODES[0xf9fb] = new Opcode(
        0xf9fb,
        "unknown_f9fb",
        [],
        false,
        []
    ));
    static readonly unknown_f9fc = (OPCODES[0xf9fc] = new Opcode(
        0xf9fc,
        "unknown_f9fc",
        [],
        false,
        []
    ));
    static readonly unknown_f9fd = (OPCODES[0xf9fd] = new Opcode(
        0xf9fd,
        "unknown_f9fd",
        [],
        false,
        []
    ));
    static readonly unknown_f9fe = (OPCODES[0xf9fe] = new Opcode(
        0xf9fe,
        "unknown_f9fe",
        [],
        false,
        []
    ));
    static readonly unknown_f9ff = (OPCODES[0xf9ff] = new Opcode(
        0xf9ff,
        "unknown_f9ff",
        [],
        false,
        []
    ));
}

export const OPCODES_BY_MNEMONIC = new Map<string, Opcode>();

OPCODES.forEach(opcode => {
    OPCODES_BY_MNEMONIC.set(opcode.mnemonic, opcode);
});

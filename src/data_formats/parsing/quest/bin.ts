import Logger from "js-logger";
import { Cursor } from "../../cursor/Cursor";
import { WritableArrayBufferCursor } from "../../cursor/WritableArrayBufferCursor";
import { Endianness } from "../..";
import { ArrayBufferCursor } from "../../cursor/ArrayBufferCursor";

const logger = Logger.get("data_formats/parsing/quest/bin");

export type BinFile = {
    quest_id: number;
    language: number;
    quest_name: string;
    short_description: string;
    long_description: string;
    function_offsets: number[];
    instructions: Instruction[];
    object_code: ArrayBuffer;
    unknown: ArrayBuffer;
};

export function parse_bin(cursor: Cursor, lenient: boolean = false): BinFile {
    const object_code_offset = cursor.u32(); // Always 4652
    const function_offset_table_offset = cursor.u32(); // Relative offsets
    const size = cursor.u32();
    cursor.seek(4); // Always seems to be 0xFFFFFFFF
    const quest_id = cursor.u32();
    const language = cursor.u32();
    const quest_name = cursor.string_utf16(64, true, true);
    const short_description = cursor.string_utf16(256, true, true);
    const long_description = cursor.string_utf16(576, true, true);

    if (size !== cursor.size) {
        logger.warn(`Value ${size} in bin size field does not match actual size ${cursor.size}.`);
    }

    const unknown = cursor.take(object_code_offset - cursor.position).array_buffer();

    const object_code = cursor
        .seek_start(object_code_offset)
        .take(function_offset_table_offset - object_code_offset);

    const instructions = parse_object_code(object_code, lenient);

    const function_offset_count = Math.floor((cursor.size - function_offset_table_offset) / 4);

    cursor.seek_start(function_offset_table_offset);
    const function_offsets = [];

    for (let i = 0; i < function_offset_count; ++i) {
        function_offsets.push(cursor.i32());
    }

    return {
        quest_id,
        language,
        quest_name,
        short_description,
        long_description,
        function_offsets,
        instructions,
        object_code: object_code.seek_start(0).array_buffer(),
        unknown,
    };
}

export function write_bin(bin: BinFile): ArrayBuffer {
    const object_code_offset = 4652;
    const buffer = new ArrayBuffer(
        object_code_offset + bin.object_code.byteLength + 4 * bin.function_offsets.length
    );
    const cursor = new WritableArrayBufferCursor(buffer, Endianness.Little);

    cursor.write_u32(object_code_offset);
    cursor.write_u32(object_code_offset + bin.object_code.byteLength);
    cursor.write_u32(buffer.byteLength);
    cursor.write_u32(0xffffffff);
    cursor.write_u32(bin.quest_id);
    cursor.write_u32(bin.language);
    cursor.write_string_utf16(bin.quest_name, 64);
    cursor.write_string_utf16(bin.short_description, 256);
    cursor.write_string_utf16(bin.long_description, 576);

    cursor.write_cursor(new ArrayBufferCursor(bin.unknown, Endianness.Little));

    while (cursor.position < object_code_offset) {
        cursor.write_u8(0);
    }

    cursor.write_cursor(new ArrayBufferCursor(bin.object_code, Endianness.Little));

    for (const function_offset of bin.function_offsets) {
        cursor.write_i32(function_offset);
    }

    return buffer;
}

/**
 * Script object code instruction. Invoked by {@link Instruction}s.
 */
export class Opcode {
    /**
     * Byte size of the instruction code, either 1 or 2.
     */
    readonly code_size: number;

    constructor(
        /**
         * 1- Or 2-byte instruction code used to invoke this opcode.
         */
        readonly code: number,
        readonly mnemonic: string,
        /**
         * Directly passed in arguments.
         */
        readonly params: Type[],
        /**
         * If true, this opcode pushes arguments onto the stack.
         */
        readonly push_stack: boolean,
        /**
         * Arguments passed in via the stack.
         * These arguments are popped from the stack after the opcode has executed.
         */
        readonly stack_params: Type[]
    ) {
        const table = this.code >>> 8;
        this.code_size = table === 0 ? 1 : 2;
    }
}

/**
 * Instruction parameter types.
 */
export enum Type {
    U8,
    U16,
    U32,
    I32,
    F32,
    Register,
    SwitchData,
    JumpData,
    String,
}

/**
 * Instruction invocation.
 */
export class Instruction {
    /**
     * Byte size of the argument list.
     */
    readonly arg_size: number = 0;
    /**
     * Byte size of the entire instruction, i.e. the sum of the opcode size and all argument sizes.
     */
    readonly size: number;

    constructor(readonly opcode: Opcode, readonly args: Arg[]) {
        for (const { size } of args) {
            this.arg_size += size;
        }

        this.size = opcode.code_size + this.arg_size;
    }
}

export type Arg = {
    value: any;
    size: number;
};

function parse_object_code(cursor: Cursor, lenient: boolean): Instruction[] {
    const instructions: Instruction[] = [];

    try {
        while (cursor.bytes_left) {
            const main_opcode = cursor.u8();
            let code;
            let opcode_list;

            switch (main_opcode) {
                case 0xf8:
                    code = cursor.u8();
                    opcode_list = f8_opcodes;
                    break;
                case 0xf9:
                    code = cursor.u8();
                    opcode_list = f9_opcodes;
                    break;
                default:
                    code = main_opcode;
                    opcode_list = opcodes;
                    break;
            }

            let opcode = opcode_list[code];

            try {
                const args = parse_instruction_arguments(cursor, opcode);
                instructions.push(new Instruction(opcode, args));
            } catch (e) {
                instructions.push(new Instruction(opcode, []));
            }
        }
    } catch (e) {
        if (lenient) {
            logger.error("Couldn't fully parse object code.", e);
        } else {
            throw e;
        }
    }

    return instructions;
}

function parse_instruction_arguments(cursor: Cursor, opcode: Opcode): Arg[] {
    const args: Arg[] = [];

    for (const param of opcode.params) {
        let arg: any;
        const start_pos = cursor.position;

        switch (param) {
            case Type.U8:
                arg = cursor.u8();
                break;
            case Type.U16:
                arg = cursor.u16();
                break;
            case Type.U32:
                arg = cursor.u32();
                break;
            case Type.I32:
                arg = cursor.i32();
                break;
            case Type.F32:
                arg = cursor.f32();
                break;
            case Type.Register:
                arg = cursor.u8();
                break;
            case Type.SwitchData:
                {
                    const arg_size = 2 * cursor.u8();
                    arg = cursor.u8_array(arg_size);
                }
                break;
            case Type.JumpData:
                {
                    const arg_size = cursor.u8();
                    arg = cursor.u8_array(arg_size);
                }
                break;
            case Type.String:
                arg = cursor.string_utf16(1024, true, false);
                break;
            default:
                throw new Error(`Parameter type ${Type[param]} (${param}) not implemented.`);
        }

        args.push({ value: arg, size: cursor.position - start_pos });
    }

    return args;
}

const opcodes: Opcode[] = [
    new Opcode(0x00, "nop", [], false, []),
    new Opcode(0x01, "ret", [], false, []),
    new Opcode(0x02, "sync", [], false, []),
    new Opcode(0x03, "exit", [], false, [Type.U32]),
    new Opcode(0x04, "thread", [Type.U16], false, []),
    new Opcode(0x05, "va_start", [], false, []),
    new Opcode(0x06, "va_end", [], false, []),
    new Opcode(0x07, "va_call", [Type.U16], false, []),
    new Opcode(0x08, "let", [Type.Register, Type.Register], false, []),
    new Opcode(0x09, "leti", [Type.Register, Type.I32], false, []),
    new Opcode(0x0a, "unknown", [], false, []),
    new Opcode(0x0b, "unknown", [], false, []),
    new Opcode(0x0c, "unknown", [], false, []),
    new Opcode(0x0d, "unknown", [], false, []),
    new Opcode(0x0e, "unknown", [], false, []),
    new Opcode(0x0f, "unknown", [], false, []),
    new Opcode(0x10, "set", [Type.Register], false, []),
    new Opcode(0x11, "clear", [Type.Register], false, []),
    new Opcode(0x12, "rev", [Type.Register], false, []),
    new Opcode(0x13, "gset", [Type.U16], false, []),
    new Opcode(0x14, "gclear", [Type.U16], false, []),
    new Opcode(0x15, "grev", [Type.U16], false, []),
    new Opcode(0x16, "glet", [Type.U16], false, []),
    new Opcode(0x17, "gget", [Type.U16, Type.Register], false, []),
    new Opcode(0x18, "add", [Type.Register, Type.Register], false, []),
    new Opcode(0x19, "addi", [Type.Register, Type.I32], false, []),
    new Opcode(0x1a, "sub", [Type.Register, Type.Register], false, []),
    new Opcode(0x1b, "subi", [Type.Register, Type.I32], false, []),
    new Opcode(0x1c, "mul", [Type.Register, Type.Register], false, []),
    new Opcode(0x1d, "muli", [Type.Register, Type.I32], false, []),
    new Opcode(0x1e, "div", [Type.Register, Type.Register], false, []),
    new Opcode(0x1f, "divi", [Type.Register, Type.I32], false, []),
    new Opcode(0x20, "and", [Type.Register, Type.Register], false, []),
    new Opcode(0x21, "andi", [Type.Register, Type.I32], false, []),
    new Opcode(0x22, "or", [Type.Register, Type.Register], false, []),
    new Opcode(0x23, "ori", [Type.Register, Type.I32], false, []),
    new Opcode(0x24, "xor", [Type.Register, Type.Register], false, []),
    new Opcode(0x25, "xori", [Type.Register, Type.I32], false, []),
    new Opcode(0x26, "mod", [Type.Register, Type.Register], false, []),
    new Opcode(0x27, "modi", [Type.Register, Type.I32], false, []),
    new Opcode(0x28, "jmp", [Type.U16], false, []),
    new Opcode(0x29, "call", [Type.U16], false, []),
    new Opcode(0x2a, "jmp_on", [Type.U16, Type.JumpData], false, []),
    new Opcode(0x2b, "jmp_off", [Type.U16, Type.JumpData], false, []),
    new Opcode(0x2c, "jmp_=", [Type.Register, Type.Register, Type.U16], false, []),
    new Opcode(0x2d, "jmpi_=", [Type.Register, Type.I32, Type.U16], false, []),
    new Opcode(0x2e, "jmp_!=", [Type.Register, Type.Register, Type.U16], false, []),
    new Opcode(0x2f, "jmpi_!=", [Type.Register, Type.I32, Type.U16], false, []),
    new Opcode(0x30, "ujmp_>", [Type.Register, Type.Register, Type.U16], false, []),
    new Opcode(0x31, "ujmpi_>", [Type.Register, Type.U32, Type.U16], false, []),
    new Opcode(0x32, "jmp_>", [Type.Register, Type.Register, Type.U16], false, []),
    new Opcode(0x33, "jmpi_>", [Type.Register, Type.I32, Type.U16], false, []),
    new Opcode(0x34, "ujmp_<", [Type.Register, Type.Register, Type.U16], false, []),
    new Opcode(0x35, "ujmpi_<", [Type.Register, Type.U32, Type.U16], false, []),
    new Opcode(0x36, "jmp_<", [Type.Register, Type.Register, Type.U16], false, []),
    new Opcode(0x37, "jmpi_<", [Type.Register, Type.I32, Type.U16], false, []),
    new Opcode(0x38, "ujmp_>=", [Type.Register, Type.Register, Type.U16], false, []),
    new Opcode(0x39, "ujmpi_>=", [Type.Register, Type.U32, Type.U16], false, []),
    new Opcode(0x3a, "jmp_>=", [Type.Register, Type.Register, Type.U16], false, []),
    new Opcode(0x3b, "jmpi_>=", [Type.Register, Type.I32, Type.U16], false, []),
    new Opcode(0x3c, "ujmp_<=", [Type.Register, Type.Register, Type.U16], false, []),
    new Opcode(0x3d, "ujmpi_<=", [Type.Register, Type.U32, Type.U16], false, []),
    new Opcode(0x3e, "jmp_<=", [Type.Register, Type.Register, Type.U16], false, []),
    new Opcode(0x3f, "jmpi_<=", [Type.Register, Type.I32, Type.U16], false, []),
    new Opcode(0x40, "switch_jmp", [Type.Register, Type.SwitchData], false, []),
    new Opcode(0x41, "switch_call", [Type.Register, Type.SwitchData], false, []),
    new Opcode(0x42, "stack_push", [Type.Register], false, []),
    new Opcode(0x43, "stack_pop", [Type.Register], false, []),
    new Opcode(0x44, "stack_pushm", [Type.Register, Type.U32], false, []),
    new Opcode(0x45, "stack_popm", [Type.Register, Type.U32], false, []),
    new Opcode(0x46, "unknown", [], false, []),
    new Opcode(0x47, "unknown", [], false, []),
    new Opcode(0x48, "arg_pushr", [Type.Register], true, []),
    new Opcode(0x49, "arg_pushl", [Type.I32], true, []),
    new Opcode(0x4a, "arg_pushb", [Type.U8], true, []),
    new Opcode(0x4b, "arg_pushw", [Type.U16], true, []),
    new Opcode(0x4c, "unknown", [], false, []),
    new Opcode(0x4d, "unknown", [], false, []),
    new Opcode(0x4e, "arg_pushs", [Type.String], true, []),
    new Opcode(0x4f, "unknown4F", [Type.Register, Type.Register], false, []),
    new Opcode(0x50, "message", [], false, [Type.U32, Type.String]),
    new Opcode(0x51, "list", [], false, [Type.Register, Type.String]),
    new Opcode(0x52, "fadein", [], false, []),
    new Opcode(0x53, "fadeout", [], false, []),
    new Opcode(0x54, "se", [], false, [Type.U32]),
    new Opcode(0x55, "bgm", [], false, [Type.U32]),
    new Opcode(0x56, "unknown", [], false, []),
    new Opcode(0x57, "unknown", [], false, []),
    new Opcode(0x58, "enable", [], false, [Type.U32]),
    new Opcode(0x59, "disable", [], false, [Type.U32]),
    new Opcode(0x5a, "window_msg", [], false, [Type.String]),
    new Opcode(0x5b, "add_msg", [], false, [Type.String]),
    new Opcode(0x5c, "mesend", [], false, []),
    new Opcode(0x5d, "gettime", [Type.Register], false, []),
    new Opcode(0x5e, "winend", [], false, []),
    new Opcode(0x5f, "unknown", [], false, []),
    new Opcode(0x60, "npc_crt_V3", [Type.Register], false, []),
    new Opcode(0x61, "npc_stop", [], false, [Type.Register]),
    new Opcode(0x62, "npc_play", [], false, [Type.U32]),
    new Opcode(0x63, "npc_kill", [], false, [Type.Register]),
    new Opcode(0x64, "npc_nont", [], false, []),
    new Opcode(0x65, "npc_talk", [], false, []),
    new Opcode(0x66, "npc_crp_V3", [Type.Register], false, []),
    new Opcode(0x67, "unknown", [], false, []),
    new Opcode(0x68, "create_pipe", [], false, [Type.U32]),
    new Opcode(0x69, "p_hpstat_V3", [], false, [Type.Register, Type.U32]),
    new Opcode(0x6a, "p_dead_V3", [], false, [Type.Register, Type.U32]),
    new Opcode(0x6b, "p_disablewarp", [], false, []),
    new Opcode(0x6c, "p_enablewarp", [], false, []),
    new Opcode(0x6d, "p_move_V3", [Type.Register], false, []),
    new Opcode(0x6e, "p_look", [], false, [Type.U32]),
    new Opcode(0x6f, "unknown", [], false, []),
    new Opcode(0x70, "p_action_disable", [], false, []),
    new Opcode(0x71, "p_action_enable", [], false, []),
    new Opcode(0x72, "disable_movement1", [], false, [Type.Register]),
    new Opcode(0x73, "enable_movement1", [], false, [Type.Register]),
    new Opcode(0x74, "p_noncol", [], false, []),
    new Opcode(0x75, "p_col", [], false, []),
    new Opcode(0x76, "p_setpos", [], false, [Type.Register, Type.Register]),
    new Opcode(0x77, "p_return_guild", [], false, []),
    new Opcode(0x78, "p_talk_guild", [], false, [Type.U32]),
    new Opcode(0x79, "npc_talk_pl_V3", [Type.Register], false, []),
    new Opcode(0x7a, "npc_talk_kill", [], false, [Type.U32]),
    new Opcode(0x7b, "npc_crtpk_V3", [Type.Register], false, []),
    new Opcode(0x7c, "npc_crppk_V3", [Type.Register], false, []),
    new Opcode(0x7d, "npc_crptalk_v3", [Type.Register], false, []),
    new Opcode(0x7e, "p_look_at_V1", [], false, [Type.U32, Type.U32]),
    new Opcode(0x7f, "npc_crp_id_V3", [Type.Register], false, []),
    new Opcode(0x80, "cam_quake", [], false, []),
    new Opcode(0x81, "cam_adj", [], false, []),
    new Opcode(0x82, "cam_zmin", [], false, []),
    new Opcode(0x83, "cam_zmout", [], false, []),
    new Opcode(0x84, "cam_pan_V3", [Type.Register], false, []),
    new Opcode(0x85, "game_lev_super", [], false, []),
    new Opcode(0x86, "game_lev_reset", [], false, []),
    new Opcode(0x87, "pos_pipe_V3", [Type.Register], false, []),
    new Opcode(0x88, "if_zone_clear", [Type.Register, Type.Register], false, []),
    new Opcode(0x89, "chk_ene_num", [Type.Register], false, []),
    new Opcode(0x8a, "unhide_obj", [Type.Register], false, []),
    new Opcode(0x8b, "unhide_ene", [Type.Register], false, []),
    new Opcode(0x8c, "at_coords_call", [Type.Register], false, []),
    new Opcode(0x8d, "at_coords_talk", [Type.Register], false, []),
    new Opcode(0x8e, "col_npcin", [Type.Register], false, []),
    new Opcode(0x8f, "col_npcinr", [Type.Register], false, []),
    new Opcode(0x90, "switch_on", [], false, [Type.U32]),
    new Opcode(0x91, "switch_off", [], false, [Type.U32]),
    new Opcode(0x92, "playbgm_epi", [], false, [Type.U32]),
    new Opcode(0x93, "set_mainwarp", [], false, [Type.U32]),
    new Opcode(0x94, "set_obj_param", [Type.Register, Type.Register], false, []),
    new Opcode(0x95, "set_floor_handler", [], false, [Type.U32, Type.U16]),
    new Opcode(0x96, "clr_floor_handler", [], false, [Type.U32]),
    new Opcode(0x97, "col_plinaw", [Type.Register], false, []),
    new Opcode(0x98, "hud_hide", [], false, []),
    new Opcode(0x99, "hud_show", [], false, []),
    new Opcode(0x9a, "cine_enable", [], false, []),
    new Opcode(0x9b, "cine_disable", [], false, []),
    new Opcode(0x9c, "unknown", [], false, []),
    new Opcode(0x9d, "unknown", [], false, []),
    new Opcode(0x9e, "unknown", [], false, []),
    new Opcode(0x9f, "unknown", [], false, []),
    new Opcode(0xa0, "unknown", [], false, []),
    new Opcode(0xa1, "set_qt_failure", [Type.U16], false, []),
    new Opcode(0xa2, "set_qt_success", [Type.U16], false, []),
    new Opcode(0xa3, "clr_qt_failure", [], false, []),
    new Opcode(0xa4, "clr_qt_success", [], false, []),
    new Opcode(0xa5, "set_qt_cancel", [Type.U16], false, []),
    new Opcode(0xa6, "clr_qt_cancel", [], false, []),
    new Opcode(0xa7, "unknown", [], false, []),
    new Opcode(0xa8, "pl_walk_V3", [Type.Register], false, []),
    new Opcode(0xa9, "unknown", [], false, []),
    new Opcode(0xaa, "unknown", [], false, []),
    new Opcode(0xab, "unknown", [], false, []),
    new Opcode(0xac, "unknown", [], false, []),
    new Opcode(0xad, "unknown", [], false, []),
    new Opcode(0xae, "unknown", [], false, []),
    new Opcode(0xaf, "unknown", [], false, []),
    new Opcode(0xb0, "pl_add_meseta", [], false, [Type.U32, Type.U32]),
    new Opcode(0xb1, "thread_stg", [Type.U16], false, []),
    new Opcode(0xb2, "del_obj_param", [Type.Register], false, []),
    new Opcode(0xb3, "item_create", [Type.Register, Type.Register], false, []),
    new Opcode(0xb4, "item_create2", [Type.Register, Type.Register], false, []),
    new Opcode(0xb5, "item_delete", [Type.Register, Type.Register], false, []),
    new Opcode(0xb6, "item_delete2", [Type.Register, Type.Register], false, []),
    new Opcode(0xb7, "item_check", [Type.Register, Type.Register], false, []),
    new Opcode(0xb8, "setevt", [], false, [Type.U32]),
    new Opcode(0xb9, "get_difflvl", [Type.Register], false, []),
    new Opcode(0xba, "set_qt_exit", [Type.U16], false, []),
    new Opcode(0xbb, "clr_qt_exit", [], false, []),
    new Opcode(0xbc, "unknown", [], false, []),
    new Opcode(0xbd, "unknown", [], false, []),
    new Opcode(0xbe, "unknown", [], false, []),
    new Opcode(0xbf, "unknown", [], false, []),
    new Opcode(0xc0, "particle_V3", [Type.Register], false, []),
    new Opcode(0xc1, "npc_text", [], false, [Type.U32, Type.String]),
    new Opcode(0xc2, "npc_chkwarp", [], false, []),
    new Opcode(0xc3, "pl_pkoff", [], false, []),
    new Opcode(0xc4, "map_designate", [Type.Register], false, []),
    new Opcode(0xc5, "masterkey_on", [], false, []),
    new Opcode(0xc6, "masterkey_off", [], false, []),
    new Opcode(0xc7, "window_time", [], false, []),
    new Opcode(0xc8, "winend_time", [], false, []),
    new Opcode(0xc9, "winset_time", [Type.Register], false, []),
    new Opcode(0xca, "getmtime", [Type.Register], false, []),
    new Opcode(0xcb, "set_quest_board_handler", [], false, [Type.U32, Type.U16, Type.String]),
    new Opcode(0xcc, "clear_quest_board_handler", [], false, [Type.U32]),
    new Opcode(0xcd, "particle_id_V3", [Type.Register], false, []),
    new Opcode(0xce, "npc_crptalk_id_V3", [Type.Register], false, []),
    new Opcode(0xcf, "npc_lang_clean", [], false, []),
    new Opcode(0xd0, "pl_pkon", [], false, []),
    new Opcode(0xd1, "pl_chk_item2", [Type.Register, Type.Register], false, []),
    new Opcode(0xd2, "enable_mainmenu", [], false, []),
    new Opcode(0xd3, "disable_mainmenu", [], false, []),
    new Opcode(0xd4, "start_battlebgm", [], false, []),
    new Opcode(0xd5, "end_battlebgm", [], false, []),
    new Opcode(0xd6, "disp_msg_qb", [], false, [Type.String]),
    new Opcode(0xd7, "close_msg_qb", [], false, []),
    new Opcode(0xd8, "set_eventflag_v3", [], false, [Type.U32, Type.U32]),
    new Opcode(0xd9, "sync_leti", [], false, []),
    new Opcode(0xda, "set_returnhunter", [], false, []),
    new Opcode(0xdb, "set_returncity", [], false, []),
    new Opcode(0xdc, "load_pvr", [], false, []),
    new Opcode(0xdd, "load_midi", [], false, []),
    new Opcode(0xde, "unknown", [], false, []),
    new Opcode(0xdf, "npc_param_V3", [], false, [Type.Register, Type.U32]),
    new Opcode(0xe0, "pad_dragon", [], false, []),
    new Opcode(0xe1, "clear_mainwarp", [], false, [Type.U32]),
    new Opcode(0xe2, "pcam_param_V3", [Type.Register], false, []),
    new Opcode(0xe3, "start_setevt_v3", [], false, [Type.Register, Type.U32]),
    new Opcode(0xe4, "warp_on", [], false, []),
    new Opcode(0xe5, "warp_off", [], false, []),
    new Opcode(0xe6, "get_slotnumber", [Type.Register], false, []),
    new Opcode(0xe7, "get_servernumber", [Type.Register], false, []),
    new Opcode(0xe8, "set_eventflag2", [], false, [Type.U32, Type.Register]),
    new Opcode(0xe9, "res", [Type.Register, Type.Register], false, []),
    new Opcode(0xea, "unknownEA", [Type.Register, Type.U32], false, []),
    new Opcode(0xeb, "enable_bgmctrl", [], false, [Type.U32]),
    new Opcode(0xec, "sw_send", [Type.Register], false, []),
    new Opcode(0xed, "create_bgmctrl", [], false, []),
    new Opcode(0xee, "pl_add_meseta2", [], false, [Type.U32]),
    new Opcode(0xef, "sync_register", [], false, [Type.Register, Type.U32]),
    new Opcode(0xf0, "send_regwork", [], false, []),
    new Opcode(0xf1, "leti_fixed_camera_V3", [Type.Register], false, []),
    new Opcode(0xf2, "default_camera_pos1", [], false, []),
    new Opcode(0xf3, "unknown", [], false, []),
    new Opcode(0xf4, "unknown", [], false, []),
    new Opcode(0xf5, "unknown", [], false, []),
    new Opcode(0xf6, "unknown", [], false, []),
    new Opcode(0xf7, "unknown", [], false, []),
    new Opcode(0xf8, "unknownF8", [Type.Register], false, []),
    new Opcode(0xf9, "unknown", [], false, []),
    new Opcode(0xfa, "get_gc_number", [Type.Register], false, []),
    new Opcode(0xfb, "unknownFB", [Type.U16], false, []),
    new Opcode(0xfc, "unknown", [], false, []),
    new Opcode(0xfd, "unknown", [], false, []),
    new Opcode(0xfe, "unknown", [], false, []),
    new Opcode(0xff, "unknownFF", [], false, []),
];

const f8_opcodes = [
    new Opcode(0x00f8, "unknown", [], false, []),
    new Opcode(0x01f8, "set_chat_callback?", [], false, [Type.Register, Type.String]),
    new Opcode(0x02f8, "unknown", [], false, []),
    new Opcode(0x03f8, "unknown", [], false, []),
    new Opcode(0x04f8, "unknown", [], false, []),
    new Opcode(0x05f8, "unknown", [], false, []),
    new Opcode(0x06f8, "unknown", [], false, []),
    new Opcode(0x07f8, "unknown", [], false, []),
    new Opcode(0x08f8, "get_difficulty_level2", [Type.Register], false, []),
    new Opcode(0x09f8, "get_number_of_player1", [Type.Register], false, []),
    new Opcode(0x0af8, "get_coord_of_player", [Type.Register, Type.Register], false, []),
    new Opcode(0x0bf8, "unknownF80B", [], false, []),
    new Opcode(0x0cf8, "unknownF80C", [], false, []),
    new Opcode(0x0df8, "map_designate_ex", [Type.Register], false, []),
    new Opcode(0x0ef8, "unknownF80E", [], false, [Type.U32]),
    new Opcode(0x0ff8, "unknownF80F", [], false, [Type.U32]),
    new Opcode(0x10f8, "ba_initial_floor", [], false, [Type.U32]),
    new Opcode(0x11f8, "set_ba_rules", [], false, []),
    new Opcode(0x12f8, "unknownF812", [], false, [Type.U32]),
    new Opcode(0x13f8, "unknownF813", [], false, [Type.U32]),
    new Opcode(0x14f8, "unknownF814", [], false, [Type.U32]),
    new Opcode(0x15f8, "unknownF815", [], false, [Type.U32]),
    new Opcode(0x16f8, "unknownF816", [], false, [Type.U32]),
    new Opcode(0x17f8, "unknownF817", [], false, [Type.U32]),
    new Opcode(0x18f8, "unknownF818", [], false, [Type.U32]),
    new Opcode(0x19f8, "unknownF819", [], false, [Type.U32]),
    new Opcode(0x1af8, "unknownF81A", [], false, [Type.U32]),
    new Opcode(0x1bf8, "unknownF81B", [], false, [Type.U32]),
    new Opcode(0x1cf8, "ba_disp_msg", [], false, [Type.String]),
    new Opcode(0x1df8, "death_lvl_up", [], false, [Type.U32]),
    new Opcode(0x1ef8, "death_tech_lvl_up", [], false, [Type.U32]),
    new Opcode(0x1ff8, "unknown", [], false, []),
    new Opcode(0x20f8, "cmode_stage", [], false, [Type.U32]),
    new Opcode(0x21f8, "unknown", [], false, []),
    new Opcode(0x22f8, "unknown", [], false, []),
    new Opcode(0x23f8, "unknownF823", [], false, [Type.U32]),
    new Opcode(0x24f8, "unknownF824", [], false, [Type.U32]),
    new Opcode(0x25f8, "exp_multiplication", [Type.Register], false, []),
    new Opcode(0x26f8, "exp_division?", [Type.Register], false, []),
    new Opcode(0x27f8, "get_user_is_dead?", [Type.Register], false, []),
    new Opcode(0x28f8, "go_floor", [Type.Register, Type.Register], false, []),
    new Opcode(0x29f8, "unknown", [], false, []),
    new Opcode(0x2af8, "unknown", [], false, []),
    new Opcode(0x2bf8, "unlock_door2", [], false, [Type.U32, Type.U32]),
    new Opcode(0x2cf8, "lock_door2", [], false, [Type.U32, Type.U32]),
    new Opcode(0x2df8, "if_switch_not_pressed", [Type.Register], false, []),
    new Opcode(0x2ef8, "if_switch_pressed", [Type.Register], false, []),
    new Opcode(0x2ff8, "unknownF82F", [], false, [Type.U32, Type.U32]),
    new Opcode(0x30f8, "control_dragon", [Type.Register], false, []),
    new Opcode(0x31f8, "release_dragon", [], false, []),
    new Opcode(0x32f8, "unknown", [], false, []),
    new Opcode(0x33f8, "unknown", [], false, []),
    new Opcode(0x34f8, "unknown", [], false, []),
    new Opcode(0x35f8, "unknown", [], false, []),
    new Opcode(0x36f8, "unknown", [], false, []),
    new Opcode(0x37f8, "unknown", [], false, []),
    new Opcode(0x38f8, "shrink", [Type.Register], false, []),
    new Opcode(0x39f8, "unshrink", [Type.Register], false, []),
    new Opcode(0x3af8, "unknown", [], false, []),
    new Opcode(0x3bf8, "unknown", [], false, []),
    new Opcode(0x3cf8, "display_clock2?", [Type.Register], false, []),
    new Opcode(0x3df8, "unknownF83D", [], false, [Type.U32]),
    new Opcode(0x3ef8, "delete_area_title?", [], false, [Type.U32]),
    new Opcode(0x3ff8, "unknown", [], false, []),
    new Opcode(0x40f8, "load_npc_data", [], false, []),
    new Opcode(0x41f8, "get_npc_data", [Type.U16], false, []),
    new Opcode(0x42f8, "unknown", [], false, []),
    new Opcode(0x43f8, "unknown", [], false, []),
    new Opcode(0x44f8, "unknown", [], false, []),
    new Opcode(0x45f8, "unknown", [], false, []),
    new Opcode(0x46f8, "unknown", [], false, []),
    new Opcode(0x47f8, "unknown", [], false, []),
    new Opcode(0x48f8, "give_damage_score", [Type.Register], false, []),
    new Opcode(0x49f8, "take_damage_score", [Type.Register], false, []),
    new Opcode(0x4af8, "unk_score_F84A", [Type.Register], false, []),
    new Opcode(0x4bf8, "unk_score_F84B", [Type.Register], false, []),
    new Opcode(0x4cf8, "kill_score", [Type.Register], false, []),
    new Opcode(0x4df8, "death_score", [Type.Register], false, []),
    new Opcode(0x4ef8, "unk_score_F84E", [Type.Register], false, []),
    new Opcode(0x4ff8, "enemy_death_score", [Type.Register], false, []),
    new Opcode(0x50f8, "meseta_score", [Type.Register], false, []),
    new Opcode(0x51f8, "unknownF851", [Type.Register], false, []),
    new Opcode(0x52f8, "unknownF852", [], false, [Type.U32]),
    new Opcode(0x53f8, "reverse_warps", [], false, []),
    new Opcode(0x54f8, "unreverse_warps", [], false, []),
    new Opcode(0x55f8, "set_ult_map", [], false, []),
    new Opcode(0x56f8, "unset_ult_map", [], false, []),
    new Opcode(0x57f8, "set_area_title", [], false, [Type.String]),
    new Opcode(0x58f8, "unknownF858", [], false, []),
    new Opcode(0x59f8, "unknown", [], false, []),
    new Opcode(0x5af8, "equip_item", [Type.Register], false, []),
    new Opcode(0x5bf8, "unequip_item", [], false, [Type.U32, Type.U32]),
    new Opcode(0x5cf8, "unknown", [], false, []),
    new Opcode(0x5df8, "unknown", [], false, []),
    new Opcode(0x5ef8, "unknownF85E", [], false, [Type.U32]),
    new Opcode(0x5ff8, "unknownF85F", [], false, [Type.U32]),
    new Opcode(0x60f8, "unknownF860", [], false, []),
    new Opcode(0x61f8, "unknownF861", [], false, [Type.U32]),
    new Opcode(0x62f8, "unknown", [], false, []),
    new Opcode(0x63f8, "unknown", [], false, []),
    new Opcode(0x64f8, "cmode_rank", [], false, [Type.U32, Type.String]),
    new Opcode(0x65f8, "award_item_name?", [], false, []),
    new Opcode(0x66f8, "award_item_select?", [], false, []),
    new Opcode(0x67f8, "award_item_give_to?", [Type.Register], false, []),
    new Opcode(0x68f8, "unknownF868", [Type.Register, Type.Register], false, []),
    new Opcode(0x69f8, "unknownF869", [Type.Register, Type.Register], false, []),
    new Opcode(0x6af8, "item_create_cmode", [Type.Register, Type.Register], false, []),
    new Opcode(0x6bf8, "unknownF86B", [Type.Register], false, []),
    new Opcode(0x6cf8, "award_item_ok?", [Type.Register], false, []),
    new Opcode(0x6df8, "unknownF86D", [], false, []),
    new Opcode(0x6ef8, "unknownF86E", [], false, []),
    new Opcode(0x6ff8, "ba_set_lives", [], false, [Type.U32]),
    new Opcode(0x70f8, "ba_set_tech_lvl", [], false, [Type.U32]),
    new Opcode(0x71f8, "ba_set_lvl", [], false, [Type.U32]),
    new Opcode(0x72f8, "ba_set_time_limit", [], false, [Type.U32]),
    new Opcode(0x73f8, "boss_is_dead?", [Type.Register], false, []),
    new Opcode(0x74f8, "unknown", [], false, []),
    new Opcode(0x75f8, "unknown", [], false, []),
    new Opcode(0x76f8, "unknown", [], false, []),
    new Opcode(0x77f8, "enable_techs", [Type.Register], false, []),
    new Opcode(0x78f8, "disable_techs", [Type.Register], false, []),
    new Opcode(0x79f8, "get_gender", [Type.Register, Type.Register], false, []),
    new Opcode(0x7af8, "get_chara_class", [Type.Register, Type.Register], false, []),
    new Opcode(0x7bf8, "take_slot_meseta", [Type.Register, Type.Register], false, []),
    new Opcode(0x7cf8, "unknown", [], false, []),
    new Opcode(0x7df8, "unknown", [], false, []),
    new Opcode(0x7ef8, "unknown", [], false, []),
    new Opcode(0x7ff8, "read_guildcard_flag", [Type.Register, Type.Register], false, []),
    new Opcode(0x80f8, "unknownF880", [Type.Register], false, []),
    new Opcode(0x81f8, "get_pl_name?", [Type.Register], false, []),
    new Opcode(0x82f8, "unknown", [], false, []),
    new Opcode(0x83f8, "unknownF883", [Type.Register, Type.Register], false, []),
    new Opcode(0x84f8, "unknown", [], false, []),
    new Opcode(0x85f8, "unknown", [], false, []),
    new Opcode(0x86f8, "unknown", [], false, []),
    new Opcode(0x87f8, "unknown", [], false, []),
    new Opcode(0x88f8, "ba_close_msg", [], false, []),
    new Opcode(0x89f8, "unknown", [], false, []),
    new Opcode(0x8af8, "get_player_status", [Type.Register, Type.Register], false, []),
    new Opcode(0x8bf8, "send_mail", [], false, [Type.Register, Type.String]),
    new Opcode(0x8cf8, "online_check", [Type.Register], false, []),
    new Opcode(0x8df8, "chl_set_timerecord?", [Type.Register], false, []),
    new Opcode(0x8ef8, "chl_get_timerecord?", [Type.Register], false, []),
    new Opcode(0x8ff8, "unknownF88F", [Type.Register], false, []),
    new Opcode(0x90f8, "unknownF890", [], false, []),
    new Opcode(0x91f8, "load_enemy_data", [], false, [Type.U32]),
    new Opcode(0x92f8, "get_physical_data", [Type.U16], false, []),
    new Opcode(0x93f8, "get_attack_data", [Type.U16], false, []),
    new Opcode(0x94f8, "get_resist_data", [Type.U16], false, []),
    new Opcode(0x95f8, "get_movement_data", [Type.U16], false, []),
    new Opcode(0x96f8, "unknown", [], false, []),
    new Opcode(0x97f8, "unknown", [], false, []),
    new Opcode(0x98f8, "shift_left", [Type.Register, Type.Register], false, []),
    new Opcode(0x99f8, "shift_right", [Type.Register, Type.Register], false, []),
    new Opcode(0x9af8, "get_random", [Type.Register, Type.Register], false, []),
    new Opcode(0x9bf8, "reset_map", [], false, []),
    new Opcode(0x9cf8, "disp_chl_retry_menu", [Type.Register], false, []),
    new Opcode(0x9df8, "chl_reverser?", [], false, []),
    new Opcode(0x9ef8, "unknownF89E", [], false, [Type.U32]),
    new Opcode(0x9ff8, "unknownF89F", [Type.Register], false, []),
    new Opcode(0xa0f8, "unknownF8A0", [], false, []),
    new Opcode(0xa1f8, "unknownF8A1", [], false, []),
    new Opcode(0xa2f8, "unknown", [], false, []),
    new Opcode(0xa3f8, "unknown", [], false, []),
    new Opcode(0xa4f8, "unknown", [], false, []),
    new Opcode(0xa5f8, "unknown", [], false, []),
    new Opcode(0xa6f8, "unknown", [], false, []),
    new Opcode(0xa7f8, "unknown", [], false, []),
    new Opcode(0xa8f8, "unknownF8A8", [], false, [Type.U32]),
    new Opcode(0xa9f8, "unknownF8A9", [Type.Register], false, []),
    new Opcode(0xaaf8, "unknown", [], false, []),
    new Opcode(0xabf8, "unknown", [], false, []),
    new Opcode(0xacf8, "unknown", [], false, []),
    new Opcode(0xadf8, "get_number_of_player2", [Type.Register], false, []),
    new Opcode(0xaef8, "unknown", [], false, []),
    new Opcode(0xaff8, "unknown", [], false, []),
    new Opcode(0xb0f8, "unknown", [], false, []),
    new Opcode(0xb1f8, "unknown", [], false, []),
    new Opcode(0xb2f8, "unknown", [], false, []),
    new Opcode(0xb3f8, "unknown", [], false, []),
    new Opcode(0xb4f8, "unknown", [], false, []),
    new Opcode(0xb5f8, "unknown", [], false, []),
    new Opcode(0xb6f8, "unknown", [], false, []),
    new Opcode(0xb7f8, "unknown", [], false, []),
    new Opcode(0xb8f8, "unknownF8B8", [], false, []),
    new Opcode(0xb9f8, "chl_recovery?", [], false, []),
    new Opcode(0xbaf8, "unknown", [], false, []),
    new Opcode(0xbbf8, "unknown", [], false, []),
    new Opcode(0xbcf8, "set_episode", [Type.U32], false, []),
    new Opcode(0xbdf8, "unknown", [], false, []),
    new Opcode(0xbef8, "unknown", [], false, []),
    new Opcode(0xbff8, "unknown", [], false, []),
    new Opcode(0xc0f8, "file_dl_req", [], false, [Type.U32, Type.String]),
    new Opcode(0xc1f8, "get_dl_status", [Type.Register], false, []),
    new Opcode(0xc2f8, "gba_unknown4?", [], false, []),
    new Opcode(0xc3f8, "get_gba_state?", [Type.Register], false, []),
    new Opcode(0xc4f8, "unknownF8C4", [Type.Register], false, []),
    new Opcode(0xc5f8, "unknownF8C5", [Type.Register], false, []),
    new Opcode(0xc6f8, "QEXIT", [], false, []),
    new Opcode(0xc7f8, "use_animation", [Type.Register, Type.Register], false, []),
    new Opcode(0xc8f8, "stop_animation", [Type.Register], false, []),
    new Opcode(0xc9f8, "run_to_coord", [Type.Register, Type.Register], false, []),
    new Opcode(0xcaf8, "set_slot_invincible", [Type.Register, Type.Register], false, []),
    new Opcode(0xcbf8, "unknownF8CB", [Type.Register], false, []),
    new Opcode(0xccf8, "set_slot_poison", [Type.Register], false, []),
    new Opcode(0xcdf8, "set_slot_paralyze", [Type.Register], false, []),
    new Opcode(0xcef8, "set_slot_shock", [Type.Register], false, []),
    new Opcode(0xcff8, "set_slot_freeze", [Type.Register], false, []),
    new Opcode(0xd0f8, "set_slot_slow", [Type.Register], false, []),
    new Opcode(0xd1f8, "set_slot_confuse", [Type.Register], false, []),
    new Opcode(0xd2f8, "set_slot_shifta", [Type.Register], false, []),
    new Opcode(0xd3f8, "set_slot_deband", [Type.Register], false, []),
    new Opcode(0xd4f8, "set_slot_jellen", [Type.Register], false, []),
    new Opcode(0xd5f8, "set_slot_zalure", [Type.Register], false, []),
    new Opcode(0xd6f8, "fleti_fixed_camera", [], false, [Type.Register]),
    new Opcode(0xd7f8, "fleti_locked_camera", [], false, [Type.U32, Type.Register]),
    new Opcode(0xd8f8, "default_camera_pos2", [], false, []),
    new Opcode(0xd9f8, "set_motion_blur", [], false, []),
    new Opcode(0xdaf8, "set_screen_b&w", [], false, []),
    new Opcode(0xdbf8, "unknownF8DB", [], false, [
        Type.U32,
        Type.U32,
        Type.U32,
        Type.U32,
        Type.Register,
        Type.U16,
    ]),
    new Opcode(0xdcf8, "NPC_action_string", [Type.Register, Type.Register, Type.U16], false, []),
    new Opcode(0xddf8, "get_pad_cond", [Type.Register, Type.Register], false, []),
    new Opcode(0xdef8, "get_button_cond", [Type.Register, Type.Register], false, []),
    new Opcode(0xdff8, "freeze_enemies", [], false, []),
    new Opcode(0xe0f8, "unfreeze_enemies", [], false, []),
    new Opcode(0xe1f8, "freeze_everything", [], false, []),
    new Opcode(0xe2f8, "unfreeze_everything", [], false, []),
    new Opcode(0xe3f8, "restore_hp", [Type.Register], false, []),
    new Opcode(0xe4f8, "restore_tp", [Type.Register], false, []),
    new Opcode(0xe5f8, "close_chat_bubble", [Type.Register], false, []),
    new Opcode(0xe6f8, "unknownF8E6", [Type.Register, Type.Register], false, []),
    new Opcode(0xe7f8, "unknownF8E7", [Type.Register, Type.Register], false, []),
    new Opcode(0xe8f8, "unknownF8E8", [Type.Register, Type.Register], false, []),
    new Opcode(0xe9f8, "unknownF8E9", [Type.Register, Type.Register], false, []),
    new Opcode(0xeaf8, "unknownF8EA", [Type.Register, Type.Register], false, []),
    new Opcode(0xebf8, "unknownF8EB", [Type.Register, Type.Register], false, []),
    new Opcode(0xecf8, "unknownF8EC", [Type.Register, Type.Register], false, []),
    new Opcode(0xedf8, "animation_check", [Type.Register, Type.Register], false, []),
    new Opcode(0xeef8, "call_image_data", [], false, [Type.U32, Type.U16]),
    new Opcode(0xeff8, "unknownF8EF", [], false, []),
    new Opcode(0xf0f8, "turn_off_bgm_p2", [], false, []),
    new Opcode(0xf1f8, "turn_on_bgm_p2", [], false, []),
    new Opcode(0xf2f8, "load_unk_data", [], false, [
        Type.U32,
        Type.U32,
        Type.U32,
        Type.U32,
        Type.Register,
        Type.U16,
    ]),
    new Opcode(0xf3f8, "particle2", [], false, [Type.Register, Type.U32, Type.F32]),
    new Opcode(0xf4f8, "unknown", [], false, []),
    new Opcode(0xf5f8, "unknown", [], false, []),
    new Opcode(0xf6f8, "unknown", [], false, []),
    new Opcode(0xf7f8, "unknown", [], false, []),
    new Opcode(0xf8f8, "unknown", [], false, []),
    new Opcode(0xf9f8, "unknown", [], false, []),
    new Opcode(0xfaf8, "unknown", [], false, []),
    new Opcode(0xfbf8, "unknown", [], false, []),
    new Opcode(0xfcf8, "unknown", [], false, []),
    new Opcode(0xfdf8, "unknown", [], false, []),
    new Opcode(0xfef8, "unknown", [], false, []),
    new Opcode(0xfff8, "unknown", [], false, []),
];

const f9_opcodes = [
    new Opcode(0x00f9, "unknown", [], false, []),
    new Opcode(0x01f9, "dec2float", [Type.Register, Type.Register], false, []),
    new Opcode(0x02f9, "float2dec", [Type.Register, Type.Register], false, []),
    new Opcode(0x03f9, "flet", [Type.Register, Type.Register], false, []),
    new Opcode(0x04f9, "fleti", [Type.Register, Type.F32], false, []),
    new Opcode(0x05f9, "unknown", [], false, []),
    new Opcode(0x06f9, "unknown", [], false, []),
    new Opcode(0x07f9, "unknown", [], false, []),
    new Opcode(0x08f9, "fadd", [Type.Register, Type.Register], false, []),
    new Opcode(0x09f9, "faddi", [Type.Register, Type.F32], false, []),
    new Opcode(0x0af9, "fsub", [Type.Register, Type.Register], false, []),
    new Opcode(0x0bf9, "fsubi", [Type.Register, Type.F32], false, []),
    new Opcode(0x0cf9, "fmul", [Type.Register, Type.Register], false, []),
    new Opcode(0x0df9, "fmuli", [Type.Register, Type.F32], false, []),
    new Opcode(0x0ef9, "fdiv", [Type.Register, Type.Register], false, []),
    new Opcode(0x0ff9, "fdivi", [Type.Register, Type.F32], false, []),
    new Opcode(0x10f9, "get_unknown_count?", [], false, [Type.U32, Type.Register]),
    new Opcode(0x11f9, "get_stackable_item_count", [Type.Register, Type.Register], false, []),
    new Opcode(0x12f9, "freeze_and_hide_equip", [], false, []),
    new Opcode(0x13f9, "thaw_and_show_equip", [], false, []),
    new Opcode(0x14f9, "set_paletteX_callback", [], false, [Type.Register, Type.U16]),
    new Opcode(0x15f9, "activate_paletteX", [], false, [Type.Register]),
    new Opcode(0x16f9, "enable_paletteX", [], false, [Type.Register]),
    new Opcode(0x17f9, "restore_paletteX", [], false, [Type.U32]),
    new Opcode(0x18f9, "disable_paletteX", [], false, [Type.U32]),
    new Opcode(0x19f9, "get_paletteX_activated", [], false, [Type.U32, Type.Register]),
    new Opcode(0x1af9, "get_unknown_paletteX_status?", [], false, [Type.U32, Type.Register]),
    new Opcode(0x1bf9, "disable_movement2", [], false, [Type.Register]),
    new Opcode(0x1cf9, "enable_movement2", [], false, [Type.Register]),
    new Opcode(0x1df9, "get_time_played", [Type.Register], false, []),
    new Opcode(0x1ef9, "get_guildcard_total", [Type.Register], false, []),
    new Opcode(0x1ff9, "get_slot_meseta", [Type.Register], false, []),
    new Opcode(0x20f9, "get_player_level", [], false, [Type.U32, Type.Register]),
    new Opcode(0x21f9, "get_Section_ID", [], false, [Type.U32, Type.Register]),
    new Opcode(0x22f9, "get_player_hp", [], false, [Type.Register, Type.Register]),
    new Opcode(0x23f9, "get_floor_number", [], false, [Type.Register, Type.Register]),
    new Opcode(0x24f9, "get_coord_player_detect", [Type.Register, Type.Register], false, []),
    new Opcode(0x25f9, "read_global_flag", [], false, [Type.U8, Type.Register]),
    new Opcode(0x26f9, "write_global_flag", [], false, [Type.U8, Type.Register]),
    new Opcode(0x27f9, "unknownF927", [Type.Register, Type.Register], false, []),
    new Opcode(0x28f9, "floor_player_detect", [Type.Register], false, []),
    new Opcode(0x29f9, "read_disk_file?", [], false, [Type.String]),
    new Opcode(0x2af9, "open_pack_select", [], false, []),
    new Opcode(0x2bf9, "item_select", [Type.Register], false, []),
    new Opcode(0x2cf9, "get_item_id", [Type.Register], false, []),
    new Opcode(0x2df9, "color_change", [], false, [
        Type.Register,
        Type.Register,
        Type.Register,
        Type.Register,
        Type.Register,
    ]),
    new Opcode(0x2ef9, "send_statistic?", [], false, [
        Type.U32,
        Type.U32,
        Type.U32,
        Type.U32,
        Type.U32,
        Type.U32,
        Type.U32,
        Type.U32,
    ]),
    new Opcode(0x2ff9, "unknownF92F", [], false, [Type.U32, Type.U32]),
    new Opcode(0x30f9, "chat_box", [], false, [
        Type.U32,
        Type.U32,
        Type.U32,
        Type.U32,
        Type.U32,
        Type.String,
    ]),
    new Opcode(0x31f9, "chat_bubble", [], false, [Type.U32, Type.String]),
    new Opcode(0x32f9, "unknown", [], false, []),
    new Opcode(0x33f9, "unknownF933", [Type.Register], false, []),
    new Opcode(0x34f9, "scroll_text", [], false, [
        Type.U32,
        Type.U32,
        Type.U32,
        Type.U32,
        Type.U32,
        Type.F32,
        Type.Register,
        Type.String,
    ]),
    new Opcode(0x35f9, "gba_unknown1", [], false, []),
    new Opcode(0x36f9, "gba_unknown2", [], false, []),
    new Opcode(0x37f9, "gba_unknown3", [], false, []),
    new Opcode(0x38f9, "add_damage_to?", [], false, [Type.U32, Type.U32]),
    new Opcode(0x39f9, "item_delete2", [], false, [Type.U32]),
    new Opcode(0x3af9, "get_item_info", [], false, [Type.U32, Type.Register]),
    new Opcode(0x3bf9, "item_packing1", [], false, [Type.U32]),
    new Opcode(0x3cf9, "item_packing2", [], false, [Type.U32, Type.U32]),
    new Opcode(0x3df9, "get_lang_setting?", [], false, [Type.Register]),
    new Opcode(0x3ef9, "prepare_statistic?", [], false, [Type.U32, Type.U16, Type.U16]),
    new Opcode(0x3ff9, "keyword_detect", [], false, []),
    new Opcode(0x40f9, "Keyword", [], false, [Type.Register, Type.U32, Type.String]),
    new Opcode(0x41f9, "get_guildcard_num", [], false, [Type.U32, Type.Register]),
    new Opcode(0x42f9, "unknown", [], false, []),
    new Opcode(0x43f9, "unknown", [], false, []),
    new Opcode(0x44f9, "get_wrap_status", [], false, [Type.U32, Type.Register]),
    new Opcode(0x45f9, "initial_floor", [], false, [Type.U32]),
    new Opcode(0x46f9, "sin", [], false, [Type.Register, Type.U32]),
    new Opcode(0x47f9, "cos", [], false, [Type.Register, Type.U32]),
    new Opcode(0x48f9, "unknown", [], false, []),
    new Opcode(0x49f9, "unknown", [], false, []),
    new Opcode(0x4af9, "boss_is_dead2?", [Type.Register], false, []),
    new Opcode(0x4bf9, "unknownF94B", [Type.Register], false, []),
    new Opcode(0x4cf9, "unknownF94C", [Type.Register], false, []),
    new Opcode(0x4df9, "is_there_cardbattle", [Type.Register], false, []),
    new Opcode(0x4ef9, "unknown", [], false, []),
    new Opcode(0x4ff9, "unknown", [], false, []),
    new Opcode(0x50f9, "BB_p2_menu", [], false, [Type.U32]),
    new Opcode(0x51f9, "BB_Map_Designate", [Type.U8, Type.U16, Type.U8, Type.U8], false, []),
    new Opcode(0x52f9, "BB_get_number_in_pack", [Type.Register], false, []),
    new Opcode(0x53f9, "BB_swap_item", [], false, [
        Type.U32,
        Type.U32,
        Type.U32,
        Type.U32,
        Type.U32,
        Type.U32,
        Type.U16,
        Type.U16,
    ]),
    new Opcode(0x54f9, "BB_check_wrap", [], false, [Type.Register, Type.Register]),
    new Opcode(0x55f9, "BB_exchange_PD_item", [], false, [
        Type.Register,
        Type.Register,
        Type.Register,
        Type.U16,
        Type.U16,
    ]),
    new Opcode(0x56f9, "BB_exchange_PD_srank", [], false, [
        Type.Register,
        Type.Register,
        Type.Register,
        Type.Register,
        Type.Register,
        Type.U16,
        Type.U16,
    ]),
    new Opcode(0x57f9, "BB_exchange_PD_special", [], false, [
        Type.Register,
        Type.Register,
        Type.Register,
        Type.Register,
        Type.Register,
        Type.U32,
        Type.U16,
        Type.U16,
    ]),
    new Opcode(0x58f9, "BB_exchange_PD_percent", [], false, [
        Type.Register,
        Type.Register,
        Type.Register,
        Type.Register,
        Type.Register,
        Type.U32,
        Type.U16,
        Type.U16,
    ]),
    new Opcode(0x59f9, "unknownF959", [], false, [Type.U32]),
    new Opcode(0x5af9, "unknown", [], false, []),
    new Opcode(0x5bf9, "unknown", [], false, []),
    new Opcode(0x5cf9, "BB_exchange_SLT", [], false, [Type.U32, Type.Register, Type.U16, Type.U16]),
    new Opcode(0x5df9, "BB_exchange_PC", [], false, []),
    new Opcode(0x5ef9, "BB_box_create_BP", [], false, [Type.U32, Type.F32, Type.F32]),
    new Opcode(0x5ff9, "BB_exchange_PT", [], false, [
        Type.Register,
        Type.Register,
        Type.U32,
        Type.U16,
        Type.U16,
    ]),
    new Opcode(0x60f9, "unknownF960", [], false, [Type.U32]),
    new Opcode(0x61f9, "unknownF961", [], false, []),
    new Opcode(0x62f9, "unknown", [], false, []),
    new Opcode(0x63f9, "unknown", [], false, []),
    new Opcode(0x64f9, "unknown", [], false, []),
    new Opcode(0x65f9, "unknown", [], false, []),
    new Opcode(0x66f9, "unknown", [], false, []),
    new Opcode(0x67f9, "unknown", [], false, []),
    new Opcode(0x68f9, "unknown", [], false, []),
    new Opcode(0x69f9, "unknown", [], false, []),
    new Opcode(0x6af9, "unknown", [], false, []),
    new Opcode(0x6bf9, "unknown", [], false, []),
    new Opcode(0x6cf9, "unknown", [], false, []),
    new Opcode(0x6df9, "unknown", [], false, []),
    new Opcode(0x6ef9, "unknown", [], false, []),
    new Opcode(0x6ff9, "unknown", [], false, []),
    new Opcode(0x70f9, "unknown", [], false, []),
    new Opcode(0x71f9, "unknown", [], false, []),
    new Opcode(0x72f9, "unknown", [], false, []),
    new Opcode(0x73f9, "unknown", [], false, []),
    new Opcode(0x74f9, "unknown", [], false, []),
    new Opcode(0x75f9, "unknown", [], false, []),
    new Opcode(0x76f9, "unknown", [], false, []),
    new Opcode(0x77f9, "unknown", [], false, []),
    new Opcode(0x78f9, "unknown", [], false, []),
    new Opcode(0x79f9, "unknown", [], false, []),
    new Opcode(0x7af9, "unknown", [], false, []),
    new Opcode(0x7bf9, "unknown", [], false, []),
    new Opcode(0x7cf9, "unknown", [], false, []),
    new Opcode(0x7df9, "unknown", [], false, []),
    new Opcode(0x7ef9, "unknown", [], false, []),
    new Opcode(0x7ff9, "unknown", [], false, []),
    new Opcode(0x80f9, "unknown", [], false, []),
    new Opcode(0x81f9, "unknown", [], false, []),
    new Opcode(0x82f9, "unknown", [], false, []),
    new Opcode(0x83f9, "unknown", [], false, []),
    new Opcode(0x84f9, "unknown", [], false, []),
    new Opcode(0x85f9, "unknown", [], false, []),
    new Opcode(0x86f9, "unknown", [], false, []),
    new Opcode(0x87f9, "unknown", [], false, []),
    new Opcode(0x88f9, "unknown", [], false, []),
    new Opcode(0x89f9, "unknown", [], false, []),
    new Opcode(0x8af9, "unknown", [], false, []),
    new Opcode(0x8bf9, "unknown", [], false, []),
    new Opcode(0x8cf9, "unknown", [], false, []),
    new Opcode(0x8df9, "unknown", [], false, []),
    new Opcode(0x8ef9, "unknown", [], false, []),
    new Opcode(0x8ff9, "unknown", [], false, []),
    new Opcode(0x90f9, "unknown", [], false, []),
    new Opcode(0x91f9, "unknown", [], false, []),
    new Opcode(0x92f9, "unknown", [], false, []),
    new Opcode(0x93f9, "unknown", [], false, []),
    new Opcode(0x94f9, "unknown", [], false, []),
    new Opcode(0x95f9, "unknown", [], false, []),
    new Opcode(0x96f9, "unknown", [], false, []),
    new Opcode(0x97f9, "unknown", [], false, []),
    new Opcode(0x98f9, "unknown", [], false, []),
    new Opcode(0x99f9, "unknown", [], false, []),
    new Opcode(0x9af9, "unknown", [], false, []),
    new Opcode(0x9bf9, "unknown", [], false, []),
    new Opcode(0x9cf9, "unknown", [], false, []),
    new Opcode(0x9df9, "unknown", [], false, []),
    new Opcode(0x9ef9, "unknown", [], false, []),
    new Opcode(0x9ff9, "unknown", [], false, []),
    new Opcode(0xa0f9, "unknown", [], false, []),
    new Opcode(0xa1f9, "unknown", [], false, []),
    new Opcode(0xa2f9, "unknown", [], false, []),
    new Opcode(0xa3f9, "unknown", [], false, []),
    new Opcode(0xa4f9, "unknown", [], false, []),
    new Opcode(0xa5f9, "unknown", [], false, []),
    new Opcode(0xa6f9, "unknown", [], false, []),
    new Opcode(0xa7f9, "unknown", [], false, []),
    new Opcode(0xa8f9, "unknown", [], false, []),
    new Opcode(0xa9f9, "unknown", [], false, []),
    new Opcode(0xaaf9, "unknown", [], false, []),
    new Opcode(0xabf9, "unknown", [], false, []),
    new Opcode(0xacf9, "unknown", [], false, []),
    new Opcode(0xadf9, "unknown", [], false, []),
    new Opcode(0xaef9, "unknown", [], false, []),
    new Opcode(0xaff9, "unknown", [], false, []),
    new Opcode(0xb0f9, "unknown", [], false, []),
    new Opcode(0xb1f9, "unknown", [], false, []),
    new Opcode(0xb2f9, "unknown", [], false, []),
    new Opcode(0xb3f9, "unknown", [], false, []),
    new Opcode(0xb4f9, "unknown", [], false, []),
    new Opcode(0xb5f9, "unknown", [], false, []),
    new Opcode(0xb6f9, "unknown", [], false, []),
    new Opcode(0xb7f9, "unknown", [], false, []),
    new Opcode(0xb8f9, "unknown", [], false, []),
    new Opcode(0xb9f9, "unknown", [], false, []),
    new Opcode(0xbaf9, "unknown", [], false, []),
    new Opcode(0xbbf9, "unknown", [], false, []),
    new Opcode(0xbcf9, "unknown", [], false, []),
    new Opcode(0xbdf9, "unknown", [], false, []),
    new Opcode(0xbef9, "unknown", [], false, []),
    new Opcode(0xbff9, "unknown", [], false, []),
    new Opcode(0xc0f9, "unknown", [], false, []),
    new Opcode(0xc1f9, "unknown", [], false, []),
    new Opcode(0xc2f9, "unknown", [], false, []),
    new Opcode(0xc3f9, "unknown", [], false, []),
    new Opcode(0xc4f9, "unknown", [], false, []),
    new Opcode(0xc5f9, "unknown", [], false, []),
    new Opcode(0xc6f9, "unknown", [], false, []),
    new Opcode(0xc7f9, "unknown", [], false, []),
    new Opcode(0xc8f9, "unknown", [], false, []),
    new Opcode(0xc9f9, "unknown", [], false, []),
    new Opcode(0xcaf9, "unknown", [], false, []),
    new Opcode(0xcbf9, "unknown", [], false, []),
    new Opcode(0xccf9, "unknown", [], false, []),
    new Opcode(0xcdf9, "unknown", [], false, []),
    new Opcode(0xcef9, "unknown", [], false, []),
    new Opcode(0xcff9, "unknown", [], false, []),
    new Opcode(0xd0f9, "unknown", [], false, []),
    new Opcode(0xd1f9, "unknown", [], false, []),
    new Opcode(0xd2f9, "unknown", [], false, []),
    new Opcode(0xd3f9, "unknown", [], false, []),
    new Opcode(0xd4f9, "unknown", [], false, []),
    new Opcode(0xd5f9, "unknown", [], false, []),
    new Opcode(0xd6f9, "unknown", [], false, []),
    new Opcode(0xd7f9, "unknown", [], false, []),
    new Opcode(0xd8f9, "unknown", [], false, []),
    new Opcode(0xd9f9, "unknown", [], false, []),
    new Opcode(0xdaf9, "unknown", [], false, []),
    new Opcode(0xdbf9, "unknown", [], false, []),
    new Opcode(0xdcf9, "unknown", [], false, []),
    new Opcode(0xddf9, "unknown", [], false, []),
    new Opcode(0xdef9, "unknown", [], false, []),
    new Opcode(0xdff9, "unknown", [], false, []),
    new Opcode(0xe0f9, "unknown", [], false, []),
    new Opcode(0xe1f9, "unknown", [], false, []),
    new Opcode(0xe2f9, "unknown", [], false, []),
    new Opcode(0xe3f9, "unknown", [], false, []),
    new Opcode(0xe4f9, "unknown", [], false, []),
    new Opcode(0xe5f9, "unknown", [], false, []),
    new Opcode(0xe6f9, "unknown", [], false, []),
    new Opcode(0xe7f9, "unknown", [], false, []),
    new Opcode(0xe8f9, "unknown", [], false, []),
    new Opcode(0xe9f9, "unknown", [], false, []),
    new Opcode(0xeaf9, "unknown", [], false, []),
    new Opcode(0xebf9, "unknown", [], false, []),
    new Opcode(0xecf9, "unknown", [], false, []),
    new Opcode(0xedf9, "unknown", [], false, []),
    new Opcode(0xeef9, "unknown", [], false, []),
    new Opcode(0xeff9, "unknown", [], false, []),
    new Opcode(0xf0f9, "unknown", [], false, []),
    new Opcode(0xf1f9, "unknown", [], false, []),
    new Opcode(0xf2f9, "unknown", [], false, []),
    new Opcode(0xf3f9, "unknown", [], false, []),
    new Opcode(0xf4f9, "unknown", [], false, []),
    new Opcode(0xf5f9, "unknown", [], false, []),
    new Opcode(0xf6f9, "unknown", [], false, []),
    new Opcode(0xf7f9, "unknown", [], false, []),
    new Opcode(0xf8f9, "unknown", [], false, []),
    new Opcode(0xf9f9, "unknown", [], false, []),
    new Opcode(0xfaf9, "unknown", [], false, []),
    new Opcode(0xfbf9, "unknown", [], false, []),
    new Opcode(0xfcf9, "unknown", [], false, []),
    new Opcode(0xfdf9, "unknown", [], false, []),
    new Opcode(0xfef9, "unknown", [], false, []),
    new Opcode(0xfff9, "unknown", [], false, []),
];

export const OP_RET = opcodes[0x01];
export const SET_EPISODE = f8_opcodes[0xbc];
export const BB_MAP_DESIGNATE = f9_opcodes[0x51];

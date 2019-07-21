import Logger from "js-logger";
import { Endianness } from "../..";
import { ArrayBufferCursor } from "../../cursor/ArrayBufferCursor";
import { Cursor } from "../../cursor/Cursor";
import { WritableArrayBufferCursor } from "../../cursor/WritableArrayBufferCursor";

const logger = Logger.get("data_formats/parsing/quest/bin");

export class BinFile {
    constructor(
        readonly quest_id: number,
        readonly language: number,
        readonly quest_name: string,
        readonly short_description: string,
        readonly long_description: string,
        /**
         * Map of labels to instruction indices.
         */
        readonly labels: Map<number, number>,
        readonly instructions: Instruction[],
        readonly object_code: ArrayBuffer,
        readonly unknown: ArrayBuffer
    ) {}

    get_label_instructions(label: number): Instruction[] | undefined {
        const index = this.labels.get(label);

        if (index == null || index > this.instructions.length) return undefined;

        const instructions: Instruction[] = [];

        for (let i = index; i < this.instructions.length; i++) {
            const instruction = this.instructions[i];
            instructions.push(instruction);

            if (instruction.opcode === OP_RET) {
                break;
            }
        }

        return instructions;
    }
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

export function parse_bin(cursor: Cursor, lenient: boolean = false): BinFile {
    const object_code_offset = cursor.u32(); // Always 4652
    const label_offset_table_offset = cursor.u32(); // Relative offsets
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
        .take(label_offset_table_offset - object_code_offset);

    const instructions = parse_object_code(object_code, lenient);

    let instruction_size = 0;

    for (const instruction of instructions) {
        instruction_size += instruction.size;
    }

    if (object_code.size !== instruction_size) {
        throw new Error(
            `Expected to parse ${object_code.size} bytes but parsed ${instruction_size} instead.`
        );
    }

    const label_offset_count = Math.floor((cursor.size - label_offset_table_offset) / 4);
    cursor.seek_start(label_offset_table_offset);

    const labels = new Map<number, number>();

    for (let label = 0; label < label_offset_count; ++label) {
        const offset = cursor.i32();

        if (offset >= 0) {
            let size = 0;
            let index = 0;

            for (const instruction of instructions) {
                if (offset === size) {
                    break;
                } else if (offset < size) {
                    logger.warn(
                        `Label ${label} offset ${offset} does not point to the start of an instruction.`
                    );
                    break;
                }

                size += instruction.size;
                index++;
            }

            labels.set(label, index);
        }
    }

    return new BinFile(
        quest_id,
        language,
        quest_name,
        short_description,
        long_description,
        labels,
        instructions,
        object_code.seek_start(0).array_buffer(),
        unknown
    );
}

export function write_bin(bin: BinFile): ArrayBuffer {
    const labels: number[] = [...bin.labels.entries()].reduce((ls, [l, i]) => {
        ls[l] = i;
        return ls;
    }, new Array<number>());

    const object_code_offset = 4652;
    const buffer = new ArrayBuffer(
        object_code_offset + bin.object_code.byteLength + 4 * labels.length
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

    for (let label = 0; label < labels.length; label++) {
        const index = labels[label];

        if (index == null) {
            cursor.write_i32(-1);
        } else {
            let offset = 0;

            for (let j = 0; j < bin.instructions.length; j++) {
                const instruction = bin.instructions[j];

                if (j === index) {
                    break;
                } else {
                    offset += instruction.size;
                }
            }

            cursor.write_i32(offset);
        }
    }

    return buffer;
}

function parse_object_code(cursor: Cursor, lenient: boolean): Instruction[] {
    const instructions: Instruction[] = [];

    try {
        while (cursor.bytes_left) {
            const main_opcode = cursor.u8();
            let opcode_index;

            switch (main_opcode) {
                case 0xf8:
                    opcode_index = 0x100 + cursor.u8();
                    break;
                case 0xf9:
                    opcode_index = 0x200 + cursor.u8();
                    break;
                default:
                    opcode_index = main_opcode;
                    break;
            }

            let opcode = OPCODES[opcode_index];

            try {
                const args = parse_instruction_arguments(cursor, opcode);
                instructions.push(new Instruction(opcode, args));
            } catch (e) {
                logger.warn(
                    `Exception occurred while parsing arguments for instruction ${opcode.mnemonic}.`,
                    e
                );
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
                    const arg_size = cursor.u8();
                    arg = cursor.u16_array(arg_size);
                }
                break;
            case Type.JumpData:
                {
                    const arg_size = cursor.u8();
                    arg = cursor.u8_array(arg_size);
                }
                break;
            case Type.String:
                arg = cursor.string_utf16(Math.min(4096, cursor.bytes_left), true, false);
                break;
            default:
                throw new Error(`Parameter type ${Type[param]} (${param}) not implemented.`);
        }

        args.push({ value: arg, size: cursor.position - start_pos });
    }

    return args;
}

export const OPCODES: Opcode[] = [
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
    new Opcode(0x0a, "unknown_0a", [], false, []),
    new Opcode(0x0b, "unknown_0b", [], false, []),
    new Opcode(0x0c, "unknown_0c", [], false, []),
    new Opcode(0x0d, "unknown_0d", [], false, []),
    new Opcode(0x0e, "unknown_0e", [], false, []),
    new Opcode(0x0f, "unknown_0f", [], false, []),
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
    new Opcode(0x46, "unknown_46", [], false, []),
    new Opcode(0x47, "unknown_47", [], false, []),
    new Opcode(0x48, "arg_pushr", [Type.Register], true, []),
    new Opcode(0x49, "arg_pushl", [Type.I32], true, []),
    new Opcode(0x4a, "arg_pushb", [Type.U8], true, []),
    new Opcode(0x4b, "arg_pushw", [Type.U16], true, []),
    new Opcode(0x4c, "unknown_4c", [], false, []),
    new Opcode(0x4d, "unknown_4d", [], false, []),
    new Opcode(0x4e, "arg_pushs", [Type.String], true, []),
    new Opcode(0x4f, "unknown_4f", [Type.Register, Type.Register], false, []),
    new Opcode(0x50, "message", [], false, [Type.U32, Type.String]),
    new Opcode(0x51, "list", [], false, [Type.Register, Type.String]),
    new Opcode(0x52, "fadein", [], false, []),
    new Opcode(0x53, "fadeout", [], false, []),
    new Opcode(0x54, "se", [], false, [Type.U32]),
    new Opcode(0x55, "bgm", [], false, [Type.U32]),
    new Opcode(0x56, "unknown_56", [], false, []),
    new Opcode(0x57, "unknown_57", [], false, []),
    new Opcode(0x58, "enable", [], false, [Type.U32]),
    new Opcode(0x59, "disable", [], false, [Type.U32]),
    new Opcode(0x5a, "window_msg", [], false, [Type.String]),
    new Opcode(0x5b, "add_msg", [], false, [Type.String]),
    new Opcode(0x5c, "mesend", [], false, []),
    new Opcode(0x5d, "gettime", [Type.Register], false, []),
    new Opcode(0x5e, "winend", [], false, []),
    new Opcode(0x5f, "unknown_5f", [], false, []),
    new Opcode(0x60, "npc_crt_v3", [Type.Register], false, []),
    new Opcode(0x61, "npc_stop", [], false, [Type.Register]),
    new Opcode(0x62, "npc_play", [], false, [Type.U32]),
    new Opcode(0x63, "npc_kill", [], false, [Type.Register]),
    new Opcode(0x64, "npc_nont", [], false, []),
    new Opcode(0x65, "npc_talk", [], false, []),
    new Opcode(0x66, "npc_crp_v3", [Type.Register], false, []),
    new Opcode(0x67, "unknown_67", [], false, []),
    new Opcode(0x68, "create_pipe", [], false, [Type.U32]),
    new Opcode(0x69, "p_hpstat_v3", [], false, [Type.Register, Type.U32]),
    new Opcode(0x6a, "p_dead_v3", [], false, [Type.Register, Type.U32]),
    new Opcode(0x6b, "p_disablewarp", [], false, []),
    new Opcode(0x6c, "p_enablewarp", [], false, []),
    new Opcode(0x6d, "p_move_v3", [Type.Register], false, []),
    new Opcode(0x6e, "p_look", [], false, [Type.U32]),
    new Opcode(0x6f, "unknown_6f", [], false, []),
    new Opcode(0x70, "p_action_disable", [], false, []),
    new Opcode(0x71, "p_action_enable", [], false, []),
    new Opcode(0x72, "disable_movement1", [], false, [Type.Register]),
    new Opcode(0x73, "enable_movement1", [], false, [Type.Register]),
    new Opcode(0x74, "p_noncol", [], false, []),
    new Opcode(0x75, "p_col", [], false, []),
    new Opcode(0x76, "p_setpos", [], false, [Type.Register, Type.Register]),
    new Opcode(0x77, "p_return_guild", [], false, []),
    new Opcode(0x78, "p_talk_guild", [], false, [Type.U32]),
    new Opcode(0x79, "npc_talk_pl_v3", [Type.Register], false, []),
    new Opcode(0x7a, "npc_talk_kill", [], false, [Type.U32]),
    new Opcode(0x7b, "npc_crtpk_v3", [Type.Register], false, []),
    new Opcode(0x7c, "npc_crppk_v3", [Type.Register], false, []),
    new Opcode(0x7d, "npc_crptalk_v3", [Type.Register], false, []),
    new Opcode(0x7e, "p_look_at_v1", [], false, [Type.U32, Type.U32]),
    new Opcode(0x7f, "npc_crp_id_v3", [Type.Register], false, []),
    new Opcode(0x80, "cam_quake", [], false, []),
    new Opcode(0x81, "cam_adj", [], false, []),
    new Opcode(0x82, "cam_zmin", [], false, []),
    new Opcode(0x83, "cam_zmout", [], false, []),
    new Opcode(0x84, "cam_pan_v3", [Type.Register], false, []),
    new Opcode(0x85, "game_lev_super", [], false, []),
    new Opcode(0x86, "game_lev_reset", [], false, []),
    new Opcode(0x87, "pos_pipe_v3", [Type.Register], false, []),
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
    new Opcode(0x9c, "unknown_9c", [], false, []),
    new Opcode(0x9d, "unknown_9d", [], false, []),
    new Opcode(0x9e, "unknown_9e", [], false, []),
    new Opcode(0x9f, "unknown_9f", [], false, []),
    new Opcode(0xa0, "unknown_a0", [], false, []),
    new Opcode(0xa1, "set_qt_failure", [Type.U16], false, []),
    new Opcode(0xa2, "set_qt_success", [Type.U16], false, []),
    new Opcode(0xa3, "clr_qt_failure", [], false, []),
    new Opcode(0xa4, "clr_qt_success", [], false, []),
    new Opcode(0xa5, "set_qt_cancel", [Type.U16], false, []),
    new Opcode(0xa6, "clr_qt_cancel", [], false, []),
    new Opcode(0xa7, "unknown_a7", [], false, []),
    new Opcode(0xa8, "pl_walk_v3", [Type.Register], false, []),
    new Opcode(0xa9, "unknown_a9", [], false, []),
    new Opcode(0xaa, "unknown_aa", [], false, []),
    new Opcode(0xab, "unknown_ab", [], false, []),
    new Opcode(0xac, "unknown_ac", [], false, []),
    new Opcode(0xad, "unknown_ad", [], false, []),
    new Opcode(0xae, "unknown_ae", [], false, []),
    new Opcode(0xaf, "unknown_af", [], false, []),
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
    new Opcode(0xbc, "unknown_bc", [], false, []),
    new Opcode(0xbd, "unknown_bd", [], false, []),
    new Opcode(0xbe, "unknown_be", [], false, []),
    new Opcode(0xbf, "unknown_bf", [], false, []),
    new Opcode(0xc0, "particle_v3", [Type.Register], false, []),
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
    new Opcode(0xcd, "particle_id_v3", [Type.Register], false, []),
    new Opcode(0xce, "npc_crptalk_id_v3", [Type.Register], false, []),
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
    new Opcode(0xde, "unknown_de", [], false, []),
    new Opcode(0xdf, "npc_param_v3", [], false, [Type.Register, Type.U32]),
    new Opcode(0xe0, "pad_dragon", [], false, []),
    new Opcode(0xe1, "clear_mainwarp", [], false, [Type.U32]),
    new Opcode(0xe2, "pcam_param_v3", [Type.Register], false, []),
    new Opcode(0xe3, "start_setevt_v3", [], false, [Type.Register, Type.U32]),
    new Opcode(0xe4, "warp_on", [], false, []),
    new Opcode(0xe5, "warp_off", [], false, []),
    new Opcode(0xe6, "get_slotnumber", [Type.Register], false, []),
    new Opcode(0xe7, "get_servernumber", [Type.Register], false, []),
    new Opcode(0xe8, "set_eventflag2", [], false, [Type.U32, Type.Register]),
    new Opcode(0xe9, "res", [Type.Register, Type.Register], false, []),
    new Opcode(0xea, "unknown_ea", [Type.Register, Type.U32], false, []),
    new Opcode(0xeb, "enable_bgmctrl", [], false, [Type.U32]),
    new Opcode(0xec, "sw_send", [Type.Register], false, []),
    new Opcode(0xed, "create_bgmctrl", [], false, []),
    new Opcode(0xee, "pl_add_meseta2", [], false, [Type.U32]),
    new Opcode(0xef, "sync_register", [], false, [Type.Register, Type.U32]),
    new Opcode(0xf0, "send_regwork", [], false, []),
    new Opcode(0xf1, "leti_fixed_camera_v3", [Type.Register], false, []),
    new Opcode(0xf2, "default_camera_pos1", [], false, []),
    new Opcode(0xf3, "unknown_f3", [], false, []),
    new Opcode(0xf4, "unknown_f4", [], false, []),
    new Opcode(0xf5, "unknown_f5", [], false, []),
    new Opcode(0xf6, "unknown_f6", [], false, []),
    new Opcode(0xf7, "unknown_f7", [], false, []),
    new Opcode(0xf8, "unknown_f8", [Type.Register], false, []),
    new Opcode(0xf9, "unknown_f9", [], false, []),
    new Opcode(0xfa, "get_gc_number", [Type.Register], false, []),
    new Opcode(0xfb, "unknown_fb", [Type.U16], false, []),
    new Opcode(0xfc, "unknown_fc", [], false, []),
    new Opcode(0xfd, "unknown_fd", [], false, []),
    new Opcode(0xfe, "unknown_fe", [], false, []),
    new Opcode(0xff, "unknown_ff", [], false, []),
    new Opcode(0xf800, "unknown_f800", [], false, []),
    new Opcode(0xf801, "set_chat_callback", [], false, [Type.Register, Type.String]),
    new Opcode(0xf802, "unknown_f802", [], false, []),
    new Opcode(0xf803, "unknown_f803", [], false, []),
    new Opcode(0xf804, "unknown_f804", [], false, []),
    new Opcode(0xf805, "unknown_f805", [], false, []),
    new Opcode(0xf806, "unknown_f806", [], false, []),
    new Opcode(0xf807, "unknown_f807", [], false, []),
    new Opcode(0xf808, "get_difficulty_level2", [Type.Register], false, []),
    new Opcode(0xf809, "get_number_of_player1", [Type.Register], false, []),
    new Opcode(0xf80a, "get_coord_of_player", [Type.Register, Type.Register], false, []),
    new Opcode(0xf80b, "unknown_f80b", [], false, []),
    new Opcode(0xf80c, "unknown_f80c", [], false, []),
    new Opcode(0xf80d, "map_designate_ex", [Type.Register], false, []),
    new Opcode(0xf80e, "unknown_f80e", [], false, [Type.U32]),
    new Opcode(0xf80f, "unknown_f80f", [], false, [Type.U32]),
    new Opcode(0xf810, "ba_initial_floor", [], false, [Type.U32]),
    new Opcode(0xf811, "set_ba_rules", [], false, []),
    new Opcode(0xf812, "unknown_f812", [], false, [Type.U32]),
    new Opcode(0xf813, "unknown_f813", [], false, [Type.U32]),
    new Opcode(0xf814, "unknown_f814", [], false, [Type.U32]),
    new Opcode(0xf815, "unknown_f815", [], false, [Type.U32]),
    new Opcode(0xf816, "unknown_f816", [], false, [Type.U32]),
    new Opcode(0xf817, "unknown_f817", [], false, [Type.U32]),
    new Opcode(0xf818, "unknown_f818", [], false, [Type.U32]),
    new Opcode(0xf819, "unknown_f819", [], false, [Type.U32]),
    new Opcode(0xf81a, "unknown_f81a", [], false, [Type.U32]),
    new Opcode(0xf81b, "unknown_f81b", [], false, [Type.U32]),
    new Opcode(0xf81c, "ba_disp_msg", [], false, [Type.String]),
    new Opcode(0xf81d, "death_lvl_up", [], false, [Type.U32]),
    new Opcode(0xf81e, "death_tech_lvl_up", [], false, [Type.U32]),
    new Opcode(0xf81f, "unknown_f81f", [], false, []),
    new Opcode(0xf820, "cmode_stage", [], false, [Type.U32]),
    new Opcode(0xf821, "unknown_f821", [], false, []),
    new Opcode(0xf822, "unknown_f822", [], false, []),
    new Opcode(0xf823, "unknown_f823", [], false, [Type.U32]),
    new Opcode(0xf824, "unknown_f824", [], false, [Type.U32]),
    new Opcode(0xf825, "exp_multiplication", [Type.Register], false, []),
    new Opcode(0xf826, "exp_division", [Type.Register], false, []),
    new Opcode(0xf827, "get_user_is_dead", [Type.Register], false, []),
    new Opcode(0xf828, "go_floor", [Type.Register, Type.Register], false, []),
    new Opcode(0xf829, "unknown_f829", [], false, []),
    new Opcode(0xf82a, "unknown_f82a", [], false, []),
    new Opcode(0xf82b, "unlock_door2", [], false, [Type.U32, Type.U32]),
    new Opcode(0xf82c, "lock_door2", [], false, [Type.U32, Type.U32]),
    new Opcode(0xf82d, "if_switch_not_pressed", [Type.Register], false, []),
    new Opcode(0xf82e, "if_switch_pressed", [Type.Register], false, []),
    new Opcode(0xf82f, "unknown_f82f", [], false, [Type.U32, Type.U32]),
    new Opcode(0xf830, "control_dragon", [Type.Register], false, []),
    new Opcode(0xf831, "release_dragon", [], false, []),
    new Opcode(0xf832, "unknown_f832", [], false, []),
    new Opcode(0xf833, "unknown_f833", [], false, []),
    new Opcode(0xf834, "unknown_f834", [], false, []),
    new Opcode(0xf835, "unknown_f835", [], false, []),
    new Opcode(0xf836, "unknown_f836", [], false, []),
    new Opcode(0xf837, "unknown_f837", [], false, []),
    new Opcode(0xf838, "shrink", [Type.Register], false, []),
    new Opcode(0xf839, "unshrink", [Type.Register], false, []),
    new Opcode(0xf83a, "unknown_f83a", [], false, []),
    new Opcode(0xf83b, "unknown_f83b", [], false, []),
    new Opcode(0xf83c, "display_clock2", [Type.Register], false, []),
    new Opcode(0xf83d, "unknown_f83d", [], false, [Type.U32]),
    new Opcode(0xf83e, "delete_area_title", [], false, [Type.U32]),
    new Opcode(0xf83f, "unknown_f83f", [], false, []),
    new Opcode(0xf840, "load_npc_data", [], false, []),
    new Opcode(0xf841, "get_npc_data", [Type.U16], false, []),
    new Opcode(0xf842, "unknown_f842", [], false, []),
    new Opcode(0xf843, "unknown_f843", [], false, []),
    new Opcode(0xf844, "unknown_f844", [], false, []),
    new Opcode(0xf845, "unknown_f845", [], false, []),
    new Opcode(0xf846, "unknown_f846", [], false, []),
    new Opcode(0xf847, "unknown_f847", [], false, []),
    new Opcode(0xf848, "give_damage_score", [Type.Register], false, []),
    new Opcode(0xf849, "take_damage_score", [Type.Register], false, []),
    new Opcode(0xf84a, "unk_score_f84a", [Type.Register], false, []),
    new Opcode(0xf84b, "unk_score_f84b", [Type.Register], false, []),
    new Opcode(0xf84c, "kill_score", [Type.Register], false, []),
    new Opcode(0xf84d, "death_score", [Type.Register], false, []),
    new Opcode(0xf84e, "unk_score_f84e", [Type.Register], false, []),
    new Opcode(0xf84f, "enemy_death_score", [Type.Register], false, []),
    new Opcode(0xf850, "meseta_score", [Type.Register], false, []),
    new Opcode(0xf851, "unknown_f851", [Type.Register], false, []),
    new Opcode(0xf852, "unknown_f852", [], false, [Type.U32]),
    new Opcode(0xf853, "reverse_warps", [], false, []),
    new Opcode(0xf854, "unreverse_warps", [], false, []),
    new Opcode(0xf855, "set_ult_map", [], false, []),
    new Opcode(0xf856, "unset_ult_map", [], false, []),
    new Opcode(0xf857, "set_area_title", [], false, [Type.String]),
    new Opcode(0xf858, "unknown_f858", [], false, []),
    new Opcode(0xf859, "unknown_f859", [], false, []),
    new Opcode(0xf85a, "equip_item", [Type.Register], false, []),
    new Opcode(0xf85b, "unequip_item", [], false, [Type.U32, Type.U32]),
    new Opcode(0xf85c, "unknown_f85c", [], false, []),
    new Opcode(0xf85d, "unknown_f85d", [], false, []),
    new Opcode(0xf85e, "unknown_f85e", [], false, [Type.U32]),
    new Opcode(0xf85f, "unknown_f85f", [], false, [Type.U32]),
    new Opcode(0xf860, "unknown_f860", [], false, []),
    new Opcode(0xf861, "unknown_f861", [], false, [Type.U32]),
    new Opcode(0xf862, "unknown_f862", [], false, []),
    new Opcode(0xf863, "unknown_f863", [], false, []),
    new Opcode(0xf864, "cmode_rank", [], false, [Type.U32, Type.String]),
    new Opcode(0xf865, "award_item_name", [], false, []),
    new Opcode(0xf866, "award_item_select", [], false, []),
    new Opcode(0xf867, "award_item_give_to", [Type.Register], false, []),
    new Opcode(0xf868, "unknown_f868", [Type.Register, Type.Register], false, []),
    new Opcode(0xf869, "unknown_f869", [Type.Register, Type.Register], false, []),
    new Opcode(0xf86a, "item_create_cmode", [Type.Register, Type.Register], false, []),
    new Opcode(0xf86b, "unknown_f86b", [Type.Register], false, []),
    new Opcode(0xf86c, "award_item_ok", [Type.Register], false, []),
    new Opcode(0xf86d, "unknown_f86d", [], false, []),
    new Opcode(0xf86e, "unknown_f86e", [], false, []),
    new Opcode(0xf86f, "ba_set_lives", [], false, [Type.U32]),
    new Opcode(0xf870, "ba_set_tech_lvl", [], false, [Type.U32]),
    new Opcode(0xf871, "ba_set_lvl", [], false, [Type.U32]),
    new Opcode(0xf872, "ba_set_time_limit", [], false, [Type.U32]),
    new Opcode(0xf873, "boss_is_dead", [Type.Register], false, []),
    new Opcode(0xf874, "unknown_f874", [], false, []),
    new Opcode(0xf875, "unknown_f875", [], false, []),
    new Opcode(0xf876, "unknown_f876", [], false, []),
    new Opcode(0xf877, "enable_techs", [Type.Register], false, []),
    new Opcode(0xf878, "disable_techs", [Type.Register], false, []),
    new Opcode(0xf879, "get_gender", [Type.Register, Type.Register], false, []),
    new Opcode(0xf87a, "get_chara_class", [Type.Register, Type.Register], false, []),
    new Opcode(0xf87b, "take_slot_meseta", [Type.Register, Type.Register], false, []),
    new Opcode(0xf87c, "unknown_f87c", [], false, []),
    new Opcode(0xf87d, "unknown_f87d", [], false, []),
    new Opcode(0xf87e, "unknown_f87e", [], false, []),
    new Opcode(0xf87f, "read_guildcard_flag", [Type.Register, Type.Register], false, []),
    new Opcode(0xf880, "unknown_f880", [Type.Register], false, []),
    new Opcode(0xf881, "get_pl_name", [Type.Register], false, []),
    new Opcode(0xf882, "unknown_f882", [], false, []),
    new Opcode(0xf883, "unknown_f883", [Type.Register, Type.Register], false, []),
    new Opcode(0xf884, "unknown_f884", [], false, []),
    new Opcode(0xf885, "unknown_f885", [], false, []),
    new Opcode(0xf886, "unknown_f886", [], false, []),
    new Opcode(0xf887, "unknown_f887", [], false, []),
    new Opcode(0xf888, "ba_close_msg", [], false, []),
    new Opcode(0xf889, "unknown_f889", [], false, []),
    new Opcode(0xf88a, "get_player_status", [Type.Register, Type.Register], false, []),
    new Opcode(0xf88b, "send_mail", [], false, [Type.Register, Type.String]),
    new Opcode(0xf88c, "online_check", [Type.Register], false, []),
    new Opcode(0xf88d, "chl_set_timerecord", [Type.Register], false, []),
    new Opcode(0xf88e, "chl_get_timerecord", [Type.Register], false, []),
    new Opcode(0xf88f, "unknown_f88f", [Type.Register], false, []),
    new Opcode(0xf890, "unknown_f890", [], false, []),
    new Opcode(0xf891, "load_enemy_data", [], false, [Type.U32]),
    new Opcode(0xf892, "get_physical_data", [Type.U16], false, []),
    new Opcode(0xf893, "get_attack_data", [Type.U16], false, []),
    new Opcode(0xf894, "get_resist_data", [Type.U16], false, []),
    new Opcode(0xf895, "get_movement_data", [Type.U16], false, []),
    new Opcode(0xf896, "unknown_f896", [], false, []),
    new Opcode(0xf897, "unknown_f897", [], false, []),
    new Opcode(0xf898, "shift_left", [Type.Register, Type.Register], false, []),
    new Opcode(0xf899, "shift_right", [Type.Register, Type.Register], false, []),
    new Opcode(0xf89a, "get_random", [Type.Register, Type.Register], false, []),
    new Opcode(0xf89b, "reset_map", [], false, []),
    new Opcode(0xf89c, "disp_chl_retry_menu", [Type.Register], false, []),
    new Opcode(0xf89d, "chl_reverser", [], false, []),
    new Opcode(0xf89e, "unknown_f89e", [], false, [Type.U32]),
    new Opcode(0xf89f, "unknown_f89f", [Type.Register], false, []),
    new Opcode(0xf8a0, "unknown_f8a0", [], false, []),
    new Opcode(0xf8a1, "unknown_f8a1", [], false, []),
    new Opcode(0xf8a2, "unknown_f8a2", [], false, []),
    new Opcode(0xf8a3, "unknown_f8a3", [], false, []),
    new Opcode(0xf8a4, "unknown_f8a4", [], false, []),
    new Opcode(0xf8a5, "unknown_f8a5", [], false, []),
    new Opcode(0xf8a6, "unknown_f8a6", [], false, []),
    new Opcode(0xf8a7, "unknown_f8a7", [], false, []),
    new Opcode(0xf8a8, "unknown_f8a8", [], false, [Type.U32]),
    new Opcode(0xf8a9, "unknown_f8a9", [Type.Register], false, []),
    new Opcode(0xf8aa, "unknown_f8aa", [], false, []),
    new Opcode(0xf8ab, "unknown_f8ab", [], false, []),
    new Opcode(0xf8ac, "unknown_f8ac", [], false, []),
    new Opcode(0xf8ad, "get_number_of_player2", [Type.Register], false, []),
    new Opcode(0xf8ae, "unknown_f8ae", [], false, []),
    new Opcode(0xf8af, "unknown_f8af", [], false, []),
    new Opcode(0xf8b0, "unknown_f8b0", [], false, []),
    new Opcode(0xf8b1, "unknown_f8b1", [], false, []),
    new Opcode(0xf8b2, "unknown_f8b2", [], false, []),
    new Opcode(0xf8b3, "unknown_f8b3", [], false, []),
    new Opcode(0xf8b4, "unknown_f8b4", [], false, []),
    new Opcode(0xf8b5, "unknown_f8b5", [], false, []),
    new Opcode(0xf8b6, "unknown_f8b6", [], false, []),
    new Opcode(0xf8b7, "unknown_f8b7", [], false, []),
    new Opcode(0xf8b8, "unknown_f8b8", [], false, []),
    new Opcode(0xf8b9, "chl_recovery", [], false, []),
    new Opcode(0xf8ba, "unknown_f8ba", [], false, []),
    new Opcode(0xf8bb, "unknown_f8bb", [], false, []),
    new Opcode(0xf8bc, "set_episode", [Type.U32], false, []),
    new Opcode(0xf8bd, "unknown_f8bd", [], false, []),
    new Opcode(0xf8be, "unknown_f8be", [], false, []),
    new Opcode(0xf8bf, "unknown_f8bf", [], false, []),
    new Opcode(0xf8c0, "file_dl_req", [], false, [Type.U32, Type.String]),
    new Opcode(0xf8c1, "get_dl_status", [Type.Register], false, []),
    new Opcode(0xf8c2, "gba_unknown4", [], false, []),
    new Opcode(0xf8c3, "get_gba_state", [Type.Register], false, []),
    new Opcode(0xf8c4, "unknown_f8c4", [Type.Register], false, []),
    new Opcode(0xf8c5, "unknown_f8c5", [Type.Register], false, []),
    new Opcode(0xf8c6, "qexit", [], false, []),
    new Opcode(0xf8c7, "use_animation", [Type.Register, Type.Register], false, []),
    new Opcode(0xf8c8, "stop_animation", [Type.Register], false, []),
    new Opcode(0xf8c9, "run_to_coord", [Type.Register, Type.Register], false, []),
    new Opcode(0xf8ca, "set_slot_invincible", [Type.Register, Type.Register], false, []),
    new Opcode(0xf8cb, "unknown_f8cb", [Type.Register], false, []),
    new Opcode(0xf8cc, "set_slot_poison", [Type.Register], false, []),
    new Opcode(0xf8cd, "set_slot_paralyze", [Type.Register], false, []),
    new Opcode(0xf8ce, "set_slot_shock", [Type.Register], false, []),
    new Opcode(0xf8cf, "set_slot_freeze", [Type.Register], false, []),
    new Opcode(0xf8d0, "set_slot_slow", [Type.Register], false, []),
    new Opcode(0xf8d1, "set_slot_confuse", [Type.Register], false, []),
    new Opcode(0xf8d2, "set_slot_shifta", [Type.Register], false, []),
    new Opcode(0xf8d3, "set_slot_deband", [Type.Register], false, []),
    new Opcode(0xf8d4, "set_slot_jellen", [Type.Register], false, []),
    new Opcode(0xf8d5, "set_slot_zalure", [Type.Register], false, []),
    new Opcode(0xf8d6, "fleti_fixed_camera", [], false, [Type.Register]),
    new Opcode(0xf8d7, "fleti_locked_camera", [], false, [Type.U32, Type.Register]),
    new Opcode(0xf8d8, "default_camera_pos2", [], false, []),
    new Opcode(0xf8d9, "set_motion_blur", [], false, []),
    new Opcode(0xf8da, "set_screen_bw", [], false, []),
    new Opcode(0xf8db, "unknown_f8db", [], false, [
        Type.U32,
        Type.U32,
        Type.U32,
        Type.U32,
        Type.Register,
        Type.U16,
    ]),
    new Opcode(0xf8dc, "npc_action_string", [Type.Register, Type.Register, Type.U16], false, []),
    new Opcode(0xf8dd, "get_pad_cond", [Type.Register, Type.Register], false, []),
    new Opcode(0xf8de, "get_button_cond", [Type.Register, Type.Register], false, []),
    new Opcode(0xf8df, "freeze_enemies", [], false, []),
    new Opcode(0xf8e0, "unfreeze_enemies", [], false, []),
    new Opcode(0xf8e1, "freeze_everything", [], false, []),
    new Opcode(0xf8e2, "unfreeze_everything", [], false, []),
    new Opcode(0xf8e3, "restore_hp", [Type.Register], false, []),
    new Opcode(0xf8e4, "restore_tp", [Type.Register], false, []),
    new Opcode(0xf8e5, "close_chat_bubble", [Type.Register], false, []),
    new Opcode(0xf8e6, "unknown_f8e6", [Type.Register, Type.Register], false, []),
    new Opcode(0xf8e7, "unknown_f8e7", [Type.Register, Type.Register], false, []),
    new Opcode(0xf8e8, "unknown_f8e8", [Type.Register, Type.Register], false, []),
    new Opcode(0xf8e9, "unknown_f8e9", [Type.Register, Type.Register], false, []),
    new Opcode(0xf8ea, "unknown_f8ea", [Type.Register, Type.Register], false, []),
    new Opcode(0xf8eb, "unknown_f8eb", [Type.Register, Type.Register], false, []),
    new Opcode(0xf8ec, "unknown_f8ec", [Type.Register, Type.Register], false, []),
    new Opcode(0xf8ed, "animation_check", [Type.Register, Type.Register], false, []),
    new Opcode(0xf8ee, "call_image_data", [], false, [Type.U32, Type.U16]),
    new Opcode(0xf8ef, "unknown_f8ef", [], false, []),
    new Opcode(0xf8f0, "turn_off_bgm_p2", [], false, []),
    new Opcode(0xf8f1, "turn_on_bgm_p2", [], false, []),
    new Opcode(0xf8f2, "load_unk_data", [], false, [
        Type.U32,
        Type.U32,
        Type.U32,
        Type.U32,
        Type.Register,
        Type.U16,
    ]),
    new Opcode(0xf8f3, "particle2", [], false, [Type.Register, Type.U32, Type.F32]),
    new Opcode(0xf8f4, "unknown_f8f4", [], false, []),
    new Opcode(0xf8f5, "unknown_f8f5", [], false, []),
    new Opcode(0xf8f6, "unknown_f8f6", [], false, []),
    new Opcode(0xf8f7, "unknown_f8f7", [], false, []),
    new Opcode(0xf8f8, "unknown_f8f8", [], false, []),
    new Opcode(0xf8f9, "unknown_f8f9", [], false, []),
    new Opcode(0xf8fa, "unknown_f8fa", [], false, []),
    new Opcode(0xf8fb, "unknown_f8fb", [], false, []),
    new Opcode(0xf8fc, "unknown_f8fc", [], false, []),
    new Opcode(0xf8fd, "unknown_f8fd", [], false, []),
    new Opcode(0xf8fe, "unknown_f8fe", [], false, []),
    new Opcode(0xf8ff, "unknown_f8ff", [], false, []),
    new Opcode(0xf900, "unknown_f900", [], false, []),
    new Opcode(0xf901, "dec2float", [Type.Register, Type.Register], false, []),
    new Opcode(0xf902, "float2dec", [Type.Register, Type.Register], false, []),
    new Opcode(0xf903, "flet", [Type.Register, Type.Register], false, []),
    new Opcode(0xf904, "fleti", [Type.Register, Type.F32], false, []),
    new Opcode(0xf905, "unknown_f905", [], false, []),
    new Opcode(0xf906, "unknown_f906", [], false, []),
    new Opcode(0xf907, "unknown_f907", [], false, []),
    new Opcode(0xf908, "fadd", [Type.Register, Type.Register], false, []),
    new Opcode(0xf909, "faddi", [Type.Register, Type.F32], false, []),
    new Opcode(0xf90a, "fsub", [Type.Register, Type.Register], false, []),
    new Opcode(0xf90b, "fsubi", [Type.Register, Type.F32], false, []),
    new Opcode(0xf90c, "fmul", [Type.Register, Type.Register], false, []),
    new Opcode(0xf90d, "fmuli", [Type.Register, Type.F32], false, []),
    new Opcode(0xf90e, "fdiv", [Type.Register, Type.Register], false, []),
    new Opcode(0xf90f, "fdivi", [Type.Register, Type.F32], false, []),
    new Opcode(0xf910, "get_unknown_count", [], false, [Type.U32, Type.Register]),
    new Opcode(0xf911, "get_stackable_item_count", [Type.Register, Type.Register], false, []),
    new Opcode(0xf912, "freeze_and_hide_equip", [], false, []),
    new Opcode(0xf913, "thaw_and_show_equip", [], false, []),
    new Opcode(0xf914, "set_palettex_callback", [], false, [Type.Register, Type.U16]),
    new Opcode(0xf915, "activate_palettex", [], false, [Type.Register]),
    new Opcode(0xf916, "enable_palettex", [], false, [Type.Register]),
    new Opcode(0xf917, "restore_palettex", [], false, [Type.U32]),
    new Opcode(0xf918, "disable_palettex", [], false, [Type.U32]),
    new Opcode(0xf919, "get_palettex_activated", [], false, [Type.U32, Type.Register]),
    new Opcode(0xf91a, "get_unknown_palettex_status", [], false, [Type.U32, Type.Register]),
    new Opcode(0xf91b, "disable_movement2", [], false, [Type.Register]),
    new Opcode(0xf91c, "enable_movement2", [], false, [Type.Register]),
    new Opcode(0xf91d, "get_time_played", [Type.Register], false, []),
    new Opcode(0xf91e, "get_guildcard_total", [Type.Register], false, []),
    new Opcode(0xf91f, "get_slot_meseta", [Type.Register], false, []),
    new Opcode(0xf920, "get_player_level", [], false, [Type.U32, Type.Register]),
    new Opcode(0xf921, "get_section_id", [], false, [Type.U32, Type.Register]),
    new Opcode(0xf922, "get_player_hp", [], false, [Type.Register, Type.Register]),
    new Opcode(0xf923, "get_floor_number", [], false, [Type.Register, Type.Register]),
    new Opcode(0xf924, "get_coord_player_detect", [Type.Register, Type.Register], false, []),
    new Opcode(0xf925, "read_global_flag", [], false, [Type.U8, Type.Register]),
    new Opcode(0xf926, "write_global_flag", [], false, [Type.U8, Type.Register]),
    new Opcode(0xf927, "unknown_f927", [Type.Register, Type.Register], false, []),
    new Opcode(0xf928, "floor_player_detect", [Type.Register], false, []),
    new Opcode(0xf929, "read_disk_file", [], false, [Type.String]),
    new Opcode(0xf92a, "open_pack_select", [], false, []),
    new Opcode(0xf92b, "item_select", [Type.Register], false, []),
    new Opcode(0xf92c, "get_item_id", [Type.Register], false, []),
    new Opcode(0xf92d, "color_change", [], false, [
        Type.Register,
        Type.Register,
        Type.Register,
        Type.Register,
        Type.Register,
    ]),
    new Opcode(0xf92e, "send_statistic", [], false, [
        Type.U32,
        Type.U32,
        Type.U32,
        Type.U32,
        Type.U32,
        Type.U32,
        Type.U32,
        Type.U32,
    ]),
    new Opcode(0xf92f, "unknown_f92f", [], false, [Type.U32, Type.U32]),
    new Opcode(0xf930, "chat_box", [], false, [
        Type.U32,
        Type.U32,
        Type.U32,
        Type.U32,
        Type.U32,
        Type.String,
    ]),
    new Opcode(0xf931, "chat_bubble", [], false, [Type.U32, Type.String]),
    new Opcode(0xf932, "unknown_f932", [], false, []),
    new Opcode(0xf933, "unknown_f933", [Type.Register], false, []),
    new Opcode(0xf934, "scroll_text", [], false, [
        Type.U32,
        Type.U32,
        Type.U32,
        Type.U32,
        Type.U32,
        Type.F32,
        Type.Register,
        Type.String,
    ]),
    new Opcode(0xf935, "gba_unknown1", [], false, []),
    new Opcode(0xf936, "gba_unknown2", [], false, []),
    new Opcode(0xf937, "gba_unknown3", [], false, []),
    new Opcode(0xf938, "add_damage_to", [], false, [Type.U32, Type.U32]),
    new Opcode(0xf939, "item_delete2", [], false, [Type.U32]),
    new Opcode(0xf93a, "get_item_info", [], false, [Type.U32, Type.Register]),
    new Opcode(0xf93b, "item_packing1", [], false, [Type.U32]),
    new Opcode(0xf93c, "item_packing2", [], false, [Type.U32, Type.U32]),
    new Opcode(0xf93d, "get_lang_setting", [], false, [Type.Register]),
    new Opcode(0xf93e, "prepare_statistic", [], false, [Type.U32, Type.U16, Type.U16]),
    new Opcode(0xf93f, "keyword_detect", [], false, []),
    new Opcode(0xf940, "keyword", [], false, [Type.Register, Type.U32, Type.String]),
    new Opcode(0xf941, "get_guildcard_num", [], false, [Type.U32, Type.Register]),
    new Opcode(0xf942, "unknown_f942", [], false, []),
    new Opcode(0xf943, "unknown_f943", [], false, []),
    new Opcode(0xf944, "get_wrap_status", [], false, [Type.U32, Type.Register]),
    new Opcode(0xf945, "initial_floor", [], false, [Type.U32]),
    new Opcode(0xf946, "sin", [], false, [Type.Register, Type.U32]),
    new Opcode(0xf947, "cos", [], false, [Type.Register, Type.U32]),
    new Opcode(0xf948, "unknown_f948", [], false, []),
    new Opcode(0xf949, "unknown_f949", [], false, []),
    new Opcode(0xf94a, "boss_is_dead2", [Type.Register], false, []),
    new Opcode(0xf94b, "unknown_f94b", [Type.Register], false, []),
    new Opcode(0xf94c, "unknown_f94c", [Type.Register], false, []),
    new Opcode(0xf94d, "is_there_cardbattle", [Type.Register], false, []),
    new Opcode(0xf94e, "unknown_f94e", [], false, []),
    new Opcode(0xf94f, "unknown_f94f", [], false, []),
    new Opcode(0xf950, "bb_p2_menu", [], false, [Type.U32]),
    new Opcode(0xf951, "bb_map_designate", [Type.U8, Type.U16, Type.U8, Type.U8], false, []),
    new Opcode(0xf952, "bb_get_number_in_pack", [Type.Register], false, []),
    new Opcode(0xf953, "bb_swap_item", [], false, [
        Type.U32,
        Type.U32,
        Type.U32,
        Type.U32,
        Type.U32,
        Type.U32,
        Type.U16,
        Type.U16,
    ]),
    new Opcode(0xf954, "bb_check_wrap", [], false, [Type.Register, Type.Register]),
    new Opcode(0xf955, "bb_exchange_pd_item", [], false, [
        Type.Register,
        Type.Register,
        Type.Register,
        Type.U16,
        Type.U16,
    ]),
    new Opcode(0xf956, "bb_exchange_pd_srank", [], false, [
        Type.Register,
        Type.Register,
        Type.Register,
        Type.Register,
        Type.Register,
        Type.U16,
        Type.U16,
    ]),
    new Opcode(0xf957, "bb_exchange_pd_special", [], false, [
        Type.Register,
        Type.Register,
        Type.Register,
        Type.Register,
        Type.Register,
        Type.U32,
        Type.U16,
        Type.U16,
    ]),
    new Opcode(0xf958, "bb_exchange_pd_percent", [], false, [
        Type.Register,
        Type.Register,
        Type.Register,
        Type.Register,
        Type.Register,
        Type.U32,
        Type.U16,
        Type.U16,
    ]),
    new Opcode(0xf959, "unknown_f959", [], false, [Type.U32]),
    new Opcode(0xf95a, "unknown_f95a", [], false, []),
    new Opcode(0xf95b, "unknown_f95b", [], false, []),
    new Opcode(0xf95c, "bb_exchange_slt", [], false, [Type.U32, Type.Register, Type.U16, Type.U16]),
    new Opcode(0xf95d, "bb_exchange_pc", [], false, []),
    new Opcode(0xf95e, "bb_box_create_bp", [], false, [Type.U32, Type.F32, Type.F32]),
    new Opcode(0xf95f, "bb_exchange_pt", [], false, [
        Type.Register,
        Type.Register,
        Type.U32,
        Type.U16,
        Type.U16,
    ]),
    new Opcode(0xf960, "unknown_f960", [], false, [Type.U32]),
    new Opcode(0xf961, "unknown_f961", [], false, []),
    new Opcode(0xf962, "unknown_f962", [], false, []),
    new Opcode(0xf963, "unknown_f963", [], false, []),
    new Opcode(0xf964, "unknown_f964", [], false, []),
    new Opcode(0xf965, "unknown_f965", [], false, []),
    new Opcode(0xf966, "unknown_f966", [], false, []),
    new Opcode(0xf967, "unknown_f967", [], false, []),
    new Opcode(0xf968, "unknown_f968", [], false, []),
    new Opcode(0xf969, "unknown_f969", [], false, []),
    new Opcode(0xf96a, "unknown_f96a", [], false, []),
    new Opcode(0xf96b, "unknown_f96b", [], false, []),
    new Opcode(0xf96c, "unknown_f96c", [], false, []),
    new Opcode(0xf96d, "unknown_f96d", [], false, []),
    new Opcode(0xf96e, "unknown_f96e", [], false, []),
    new Opcode(0xf96f, "unknown_f96f", [], false, []),
    new Opcode(0xf970, "unknown_f970", [], false, []),
    new Opcode(0xf971, "unknown_f971", [], false, []),
    new Opcode(0xf972, "unknown_f972", [], false, []),
    new Opcode(0xf973, "unknown_f973", [], false, []),
    new Opcode(0xf974, "unknown_f974", [], false, []),
    new Opcode(0xf975, "unknown_f975", [], false, []),
    new Opcode(0xf976, "unknown_f976", [], false, []),
    new Opcode(0xf977, "unknown_f977", [], false, []),
    new Opcode(0xf978, "unknown_f978", [], false, []),
    new Opcode(0xf979, "unknown_f979", [], false, []),
    new Opcode(0xf97a, "unknown_f97a", [], false, []),
    new Opcode(0xf97b, "unknown_f97b", [], false, []),
    new Opcode(0xf97c, "unknown_f97c", [], false, []),
    new Opcode(0xf97d, "unknown_f97d", [], false, []),
    new Opcode(0xf97e, "unknown_f97e", [], false, []),
    new Opcode(0xf97f, "unknown_f97f", [], false, []),
    new Opcode(0xf980, "unknown_f980", [], false, []),
    new Opcode(0xf981, "unknown_f981", [], false, []),
    new Opcode(0xf982, "unknown_f982", [], false, []),
    new Opcode(0xf983, "unknown_f983", [], false, []),
    new Opcode(0xf984, "unknown_f984", [], false, []),
    new Opcode(0xf985, "unknown_f985", [], false, []),
    new Opcode(0xf986, "unknown_f986", [], false, []),
    new Opcode(0xf987, "unknown_f987", [], false, []),
    new Opcode(0xf988, "unknown_f988", [], false, []),
    new Opcode(0xf989, "unknown_f989", [], false, []),
    new Opcode(0xf98a, "unknown_f98a", [], false, []),
    new Opcode(0xf98b, "unknown_f98b", [], false, []),
    new Opcode(0xf98c, "unknown_f98c", [], false, []),
    new Opcode(0xf98d, "unknown_f98d", [], false, []),
    new Opcode(0xf98e, "unknown_f98e", [], false, []),
    new Opcode(0xf98f, "unknown_f98f", [], false, []),
    new Opcode(0xf990, "unknown_f990", [], false, []),
    new Opcode(0xf991, "unknown_f991", [], false, []),
    new Opcode(0xf992, "unknown_f992", [], false, []),
    new Opcode(0xf993, "unknown_f993", [], false, []),
    new Opcode(0xf994, "unknown_f994", [], false, []),
    new Opcode(0xf995, "unknown_f995", [], false, []),
    new Opcode(0xf996, "unknown_f996", [], false, []),
    new Opcode(0xf997, "unknown_f997", [], false, []),
    new Opcode(0xf998, "unknown_f998", [], false, []),
    new Opcode(0xf999, "unknown_f999", [], false, []),
    new Opcode(0xf99a, "unknown_f99a", [], false, []),
    new Opcode(0xf99b, "unknown_f99b", [], false, []),
    new Opcode(0xf99c, "unknown_f99c", [], false, []),
    new Opcode(0xf99d, "unknown_f99d", [], false, []),
    new Opcode(0xf99e, "unknown_f99e", [], false, []),
    new Opcode(0xf99f, "unknown_f99f", [], false, []),
    new Opcode(0xf9a0, "unknown_f9a0", [], false, []),
    new Opcode(0xf9a1, "unknown_f9a1", [], false, []),
    new Opcode(0xf9a2, "unknown_f9a2", [], false, []),
    new Opcode(0xf9a3, "unknown_f9a3", [], false, []),
    new Opcode(0xf9a4, "unknown_f9a4", [], false, []),
    new Opcode(0xf9a5, "unknown_f9a5", [], false, []),
    new Opcode(0xf9a6, "unknown_f9a6", [], false, []),
    new Opcode(0xf9a7, "unknown_f9a7", [], false, []),
    new Opcode(0xf9a8, "unknown_f9a8", [], false, []),
    new Opcode(0xf9a9, "unknown_f9a9", [], false, []),
    new Opcode(0xf9aa, "unknown_f9aa", [], false, []),
    new Opcode(0xf9ab, "unknown_f9ab", [], false, []),
    new Opcode(0xf9ac, "unknown_f9ac", [], false, []),
    new Opcode(0xf9ad, "unknown_f9ad", [], false, []),
    new Opcode(0xf9ae, "unknown_f9ae", [], false, []),
    new Opcode(0xf9af, "unknown_f9af", [], false, []),
    new Opcode(0xf9b0, "unknown_f9b0", [], false, []),
    new Opcode(0xf9b1, "unknown_f9b1", [], false, []),
    new Opcode(0xf9b2, "unknown_f9b2", [], false, []),
    new Opcode(0xf9b3, "unknown_f9b3", [], false, []),
    new Opcode(0xf9b4, "unknown_f9b4", [], false, []),
    new Opcode(0xf9b5, "unknown_f9b5", [], false, []),
    new Opcode(0xf9b6, "unknown_f9b6", [], false, []),
    new Opcode(0xf9b7, "unknown_f9b7", [], false, []),
    new Opcode(0xf9b8, "unknown_f9b8", [], false, []),
    new Opcode(0xf9b9, "unknown_f9b9", [], false, []),
    new Opcode(0xf9ba, "unknown_f9ba", [], false, []),
    new Opcode(0xf9bb, "unknown_f9bb", [], false, []),
    new Opcode(0xf9bc, "unknown_f9bc", [], false, []),
    new Opcode(0xf9bd, "unknown_f9bd", [], false, []),
    new Opcode(0xf9be, "unknown_f9be", [], false, []),
    new Opcode(0xf9bf, "unknown_f9bf", [], false, []),
    new Opcode(0xf9c0, "unknown_f9c0", [], false, []),
    new Opcode(0xf9c1, "unknown_f9c1", [], false, []),
    new Opcode(0xf9c2, "unknown_f9c2", [], false, []),
    new Opcode(0xf9c3, "unknown_f9c3", [], false, []),
    new Opcode(0xf9c4, "unknown_f9c4", [], false, []),
    new Opcode(0xf9c5, "unknown_f9c5", [], false, []),
    new Opcode(0xf9c6, "unknown_f9c6", [], false, []),
    new Opcode(0xf9c7, "unknown_f9c7", [], false, []),
    new Opcode(0xf9c8, "unknown_f9c8", [], false, []),
    new Opcode(0xf9c9, "unknown_f9c9", [], false, []),
    new Opcode(0xf9ca, "unknown_f9ca", [], false, []),
    new Opcode(0xf9cb, "unknown_f9cb", [], false, []),
    new Opcode(0xf9cc, "unknown_f9cc", [], false, []),
    new Opcode(0xf9cd, "unknown_f9cd", [], false, []),
    new Opcode(0xf9ce, "unknown_f9ce", [], false, []),
    new Opcode(0xf9cf, "unknown_f9cf", [], false, []),
    new Opcode(0xf9d0, "unknown_f9d0", [], false, []),
    new Opcode(0xf9d1, "unknown_f9d1", [], false, []),
    new Opcode(0xf9d2, "unknown_f9d2", [], false, []),
    new Opcode(0xf9d3, "unknown_f9d3", [], false, []),
    new Opcode(0xf9d4, "unknown_f9d4", [], false, []),
    new Opcode(0xf9d5, "unknown_f9d5", [], false, []),
    new Opcode(0xf9d6, "unknown_f9d6", [], false, []),
    new Opcode(0xf9d7, "unknown_f9d7", [], false, []),
    new Opcode(0xf9d8, "unknown_f9d8", [], false, []),
    new Opcode(0xf9d9, "unknown_f9d9", [], false, []),
    new Opcode(0xf9da, "unknown_f9da", [], false, []),
    new Opcode(0xf9db, "unknown_f9db", [], false, []),
    new Opcode(0xf9dc, "unknown_f9dc", [], false, []),
    new Opcode(0xf9dd, "unknown_f9dd", [], false, []),
    new Opcode(0xf9de, "unknown_f9de", [], false, []),
    new Opcode(0xf9df, "unknown_f9df", [], false, []),
    new Opcode(0xf9e0, "unknown_f9e0", [], false, []),
    new Opcode(0xf9e1, "unknown_f9e1", [], false, []),
    new Opcode(0xf9e2, "unknown_f9e2", [], false, []),
    new Opcode(0xf9e3, "unknown_f9e3", [], false, []),
    new Opcode(0xf9e4, "unknown_f9e4", [], false, []),
    new Opcode(0xf9e5, "unknown_f9e5", [], false, []),
    new Opcode(0xf9e6, "unknown_f9e6", [], false, []),
    new Opcode(0xf9e7, "unknown_f9e7", [], false, []),
    new Opcode(0xf9e8, "unknown_f9e8", [], false, []),
    new Opcode(0xf9e9, "unknown_f9e9", [], false, []),
    new Opcode(0xf9ea, "unknown_f9ea", [], false, []),
    new Opcode(0xf9eb, "unknown_f9eb", [], false, []),
    new Opcode(0xf9ec, "unknown_f9ec", [], false, []),
    new Opcode(0xf9ed, "unknown_f9ed", [], false, []),
    new Opcode(0xf9ee, "unknown_f9ee", [], false, []),
    new Opcode(0xf9ef, "unknown_f9ef", [], false, []),
    new Opcode(0xf9f0, "unknown_f9f0", [], false, []),
    new Opcode(0xf9f1, "unknown_f9f1", [], false, []),
    new Opcode(0xf9f2, "unknown_f9f2", [], false, []),
    new Opcode(0xf9f3, "unknown_f9f3", [], false, []),
    new Opcode(0xf9f4, "unknown_f9f4", [], false, []),
    new Opcode(0xf9f5, "unknown_f9f5", [], false, []),
    new Opcode(0xf9f6, "unknown_f9f6", [], false, []),
    new Opcode(0xf9f7, "unknown_f9f7", [], false, []),
    new Opcode(0xf9f8, "unknown_f9f8", [], false, []),
    new Opcode(0xf9f9, "unknown_f9f9", [], false, []),
    new Opcode(0xf9fa, "unknown_f9fa", [], false, []),
    new Opcode(0xf9fb, "unknown_f9fb", [], false, []),
    new Opcode(0xf9fc, "unknown_f9fc", [], false, []),
    new Opcode(0xf9fd, "unknown_f9fd", [], false, []),
    new Opcode(0xf9fe, "unknown_f9fe", [], false, []),
    new Opcode(0xf9ff, "unknown_f9ff", [], false, []),
];

export const OP_RET = OPCODES[0x01];
export const SET_EPISODE = OPCODES[0x01bc];
export const BB_MAP_DESIGNATE = OPCODES[0x0251];

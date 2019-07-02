import { BufferCursor } from "../../BufferCursor";
import Logger from "js-logger";

const logger = Logger.get("data_formats/parsing/quest/bin");

export interface BinFile {
    quest_id: number;
    language: number;
    quest_name: string;
    short_description: string;
    long_description: string;
    function_offsets: number[];
    instructions: Instruction[];
    data: BufferCursor;
}

export function parse_bin(cursor: BufferCursor, lenient: boolean = false): BinFile {
    const object_code_offset = cursor.u32();
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

    const function_offset_count = Math.floor((cursor.size - function_offset_table_offset) / 4);

    cursor.seek_start(function_offset_table_offset);
    const function_offsets = [];

    for (let i = 0; i < function_offset_count; ++i) {
        function_offsets.push(cursor.i32());
    }

    const instructions = parse_object_code(
        cursor
            .seek_start(object_code_offset)
            .take(function_offset_table_offset - object_code_offset),
        lenient
    );

    return {
        quest_id,
        language,
        quest_name,
        short_description,
        long_description,
        function_offsets,
        instructions,
        data: cursor.seek_start(0).take(cursor.size),
    };
}

export function write_bin({ data }: { data: BufferCursor }): BufferCursor {
    return data.seek_start(0);
}

export interface Instruction {
    opcode: number;
    mnemonic: string;
    args: any[];
    size: number;
}

function parse_object_code(cursor: BufferCursor, lenient: boolean): Instruction[] {
    const instructions = [];

    try {
        while (cursor.bytes_left) {
            const main_opcode = cursor.u8();
            let opcode;
            let opsize;
            let list;

            switch (main_opcode) {
                case 0xf8:
                    opcode = cursor.u8();
                    opsize = 2;
                    list = f8_opcode_list;
                    break;
                case 0xf9:
                    opcode = cursor.u8();
                    opsize = 2;
                    list = f9_opcode_list;
                    break;
                default:
                    opcode = main_opcode;
                    opsize = 1;
                    list = opcode_list;
                    break;
            }

            let [, mnemonic, mask] = list[opcode];

            if (mask == null) {
                let full_opcode = main_opcode;

                if (main_opcode === 0xf8 || main_opcode === 0xf9) {
                    full_opcode = (full_opcode << 8) | opcode;
                }

                logger.warn(
                    `Parameters unknown for opcode 0x${full_opcode
                        .toString(16)
                        .toUpperCase()}, assuming 0.`
                );

                instructions.push({
                    opcode,
                    mnemonic,
                    args: [],
                    size: opsize,
                });
            } else {
                try {
                    const opargs = parse_instruction_arguments(cursor, mask);

                    instructions.push({
                        opcode,
                        mnemonic,
                        args: opargs.args,
                        size: opsize + opargs.size,
                    });
                } catch (e) {
                    instructions.push({
                        opcode,
                        mnemonic,
                        args: [],
                        size: opsize,
                    });
                }
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

function parse_instruction_arguments(
    cursor: BufferCursor,
    mask: string
): { args: any[]; size: number } {
    const old_pos = cursor.position;
    const args = [];
    let args_size: number;

    outer: for (let i = 0; i < mask.length; ++i) {
        switch (mask.charAt(i)) {
            // Pushes something on the stack
            case "p":
                break;
            // Pops the stack (no increments)
            case "a":
                break outer;

            // Unsigned integers
            case "B":
                args.push(cursor.u8());
                break;
            case "W":
                args.push(cursor.u16());
                break;
            case "L":
                args.push(cursor.u32());
                break;

            // Signed integers
            case "I":
                args.push(cursor.i32());
                break;

            // Floats
            case "f":
            case "F":
                args.push(cursor.f32());
                break;

            // Registers?
            case "R":
            case "r":
                cursor.seek(1);
                break;

            // Registers with unsigned integers?
            case "b":
                args.push(cursor.u8());
                break;
            case "w":
                args.push(cursor.u16());
                break;
            case "l":
                args.push(cursor.u32());
                break;

            // Registers with signed integers?
            case "i":
                args.push(cursor.i32());
                break;

            // Variably sized data?
            case "j":
            case "J":
                args_size = 2 * cursor.u8();
                cursor.seek(args_size);
                break;
            case "t":
            case "T":
                args_size = cursor.u8();
                cursor.seek(args_size);
                break;

            // Strings
            case "s":
            case "S":
                while (cursor.u16()) {}
                break;

            default:
                throw new Error(`Unknown mask part ${mask.charAt(i)}.`);
        }
    }

    return { args, size: cursor.position - old_pos };
}

const opcode_list: Array<[number, string, string | null]> = [
    [0x00, "nop", ""],
    [0x01, "ret", ""],
    [0x02, "sync", ""],
    [0x03, "exit", "aL"],
    [0x04, "thread", "W"],
    [0x05, "va_start", ""],
    [0x06, "va_end", ""],
    [0x07, "va_call", "W"],
    [0x08, "let", "RR"],
    [0x09, "leti", "RI"],
    [0x0a, "unknown", null],
    [0x0b, "unknown", null],
    [0x0c, "unknown", null],
    [0x0d, "unknown", null],
    [0x0e, "unknown", null],
    [0x0f, "unknown", null],
    [0x10, "set", "R"],
    [0x11, "clear", "R"],
    [0x12, "rev", "R"],
    [0x13, "gset", "w"],
    [0x14, "gclear", "w"],
    [0x15, "grev", "w"],
    [0x16, "glet", "w"],
    [0x17, "gget", "wR"],
    [0x18, "add", "RR"],
    [0x19, "addi", "RI"],
    [0x1a, "sub", "RR"],
    [0x1b, "subi", "RI"],
    [0x1c, "mul", "RR"],
    [0x1d, "muli", "RI"],
    [0x1e, "div", "RR"],
    [0x1f, "divi", "RI"],
    [0x20, "and", "RR"],
    [0x21, "andi", "RI"],
    [0x22, "or", "RR"],
    [0x23, "ori", "RI"],
    [0x24, "xor", "RR"],
    [0x25, "xori", "RI"],
    [0x26, "mod", "RR"],
    [0x27, "modi", "RI"],
    [0x28, "jmp", "W"],
    [0x29, "call", "W"],
    [0x2a, "jmp_on", "Wt"],
    [0x2b, "jmp_off", "Wt"],
    [0x2c, "jmp_=", "RRW"],
    [0x2d, "jmpi_=", "RIW"],
    [0x2e, "jmp_!=", "RRW"],
    [0x2f, "jmpi_!=", "RIW"],
    [0x30, "ujmp_>", "RRW"],
    [0x31, "ujmpi_>", "RLW"],
    [0x32, "jmp_>", "RRW"],
    [0x33, "jmpi_>", "RIW"],
    [0x34, "ujmp_<", "RRW"],
    [0x35, "ujmpi_<", "RLW"],
    [0x36, "jmp_<", "RRW"],
    [0x37, "jmpi_<", "RIW"],
    [0x38, "ujmp_>=", "RRW"],
    [0x39, "ujmpi_>=", "RLW"],
    [0x3a, "jmp_>=", "RRW"],
    [0x3b, "jmpi_>=", "RIW"],
    [0x3c, "ujmp_<=", "RRW"],
    [0x3d, "ujmpi_<=", "RLW"],
    [0x3e, "jmp_<=", "RRW"],
    [0x3f, "jmpi_<=", "RIW"],
    [0x40, "switch_jmp", "Rj"],
    [0x41, "switch_call", "Rj"],
    [0x42, "stack_push", "R"],
    [0x43, "stack_pop", "R"],
    [0x44, "stack_pushm", "RL"],
    [0x45, "stack_popm", "RL"],
    [0x46, "unknown", null],
    [0x47, "unknown", null],
    [0x48, "arg_pushr", "pR"],
    [0x49, "arg_pushl", "pI"],
    [0x4a, "arg_pushb", "pB"],
    [0x4b, "arg_pushw", "pW"],
    [0x4c, "unknown", null],
    [0x4d, "unknown", null],
    [0x4e, "arg_pushs", "ps"],
    [0x4f, "unknown4F", "RR"],
    [0x50, "message", "aLs"],
    [0x51, "list", "aRs"],
    [0x52, "fadein", ""],
    [0x53, "fadeout", ""],
    [0x54, "se", "aL"],
    [0x55, "bgm", "aL"],
    [0x56, "unknown", null],
    [0x57, "unknown", null],
    [0x58, "enable", "aL"],
    [0x59, "disable", "aL"],
    [0x5a, "window_msg", "as"],
    [0x5b, "add_msg", "as"],
    [0x5c, "mesend", ""],
    [0x5d, "gettime", "R"],
    [0x5e, "winend", ""],
    [0x5f, "unknown", null],
    //[ 0x60, 'npc_crt_V1', null ],
    [0x60, "npc_crt_V3", "R"],
    [0x61, "npc_stop", "aR"],
    [0x62, "npc_play", "aL"],
    [0x63, "npc_kill", "aR"],
    [0x64, "npc_nont", ""],
    [0x65, "npc_talk", ""],
    //[ 0x66, 'npc_crp_V1', null ],
    [0x66, "npc_crp_V3", "R"],
    [0x67, "unknown", null],
    [0x68, "create_pipe", "aL"],
    //[ 0x69, 'p_hpstat_V1', null ],
    [0x69, "p_hpstat_V3", "aRL"],
    //[ 0x6A, 'p_dead_V1', null ],
    [0x6a, "p_dead_V3", "aRL"],
    [0x6b, "p_disablewarp", ""],
    [0x6c, "p_enablewarp", ""],
    //[ 0x6D, 'p_move_V1', null ],
    [0x6d, "p_move_V3", "R"],
    [0x6e, "p_look", "aL"],
    [0x6f, "unknown", null],
    [0x70, "p_action_disable", ""],
    [0x71, "p_action_enable", ""],
    [0x72, "disable_movement1", "aR"],
    [0x73, "enable_movement1", "aR"],
    [0x74, "p_noncol", ""],
    [0x75, "p_col", ""],
    [0x76, "p_setpos", "aRR"],
    [0x77, "p_return_guild", ""],
    [0x78, "p_talk_guild", "aL"],
    //[ 0x79, 'npc_talk_pl_V1', null ],
    [0x79, "npc_talk_pl_V3", "R"],
    [0x7a, "npc_talk_kill", "aL"],
    //[ 0x7B, 'npc_crtpk_V1', null ],
    [0x7b, "npc_crtpk_V3", "R"],
    //[ 0x7C, 'npc_crppk_V1', null ],
    [0x7c, "npc_crppk_V3", "R"],
    //[ 0x7D, 'npc_crptalk_v1', null ],
    [0x7d, "npc_crptalk_v3", "R"],
    [0x7e, "p_look_at_V1", "aLL"],
    //[ 0x7F, 'npc_crp_id_V1', null ],
    [0x7f, "npc_crp_id_V3", "R"],
    [0x80, "cam_quake", ""],
    [0x81, "cam_adj", ""],
    [0x82, "cam_zmin", ""],
    [0x83, "cam_zmout", ""],
    //[ 0x84, 'cam_pan_V1', null ],
    [0x84, "cam_pan_V3", "R"],
    [0x85, "game_lev_super", ""],
    [0x86, "game_lev_reset", ""],
    //[ 0x87, 'pos_pipe_V1', null ],
    [0x87, "pos_pipe_V3", "R"],
    [0x88, "if_zone_clear", "RR"],
    [0x89, "chk_ene_num", "R"],
    [0x8a, "unhide_obj", "R"],
    [0x8b, "unhide_ene", "R"],
    [0x8c, "at_coords_call", "R"],
    [0x8d, "at_coords_talk", "R"],
    [0x8e, "col_npcin", "R"],
    [0x8f, "col_npcinr", "R"],
    [0x90, "switch_on", "aL"],
    [0x91, "switch_off", "aL"],
    [0x92, "playbgm_epi", "aL"],
    [0x93, "set_mainwarp", "aL"],
    [0x94, "set_obj_param", "RR"],
    [0x95, "set_floor_handler", "aLW"],
    [0x96, "clr_floor_handler", "aL"],
    [0x97, "col_plinaw", "R"],
    [0x98, "hud_hide", ""],
    [0x99, "hud_show", ""],
    [0x9a, "cine_enable", ""],
    [0x9b, "cine_disable", ""],
    [0x9c, "unknown", null],
    [0x9d, "unknown", null],
    [0x9e, "unknown", null],
    [0x9f, "unknown", null],
    [0xa0, "unknown", null],
    [0xa1, "set_qt_failure", "W"],
    [0xa2, "set_qt_success", "W"],
    [0xa3, "clr_qt_failure", ""],
    [0xa4, "clr_qt_success", ""],
    [0xa5, "set_qt_cancel", "W"],
    [0xa6, "clr_qt_cancel", ""],
    [0xa7, "unknown", null],
    //[ 0xA8, 'pl_walk_V1', null ],
    [0xa8, "pl_walk_V3", "R"],
    [0xa9, "unknown", null],
    [0xaa, "unknown", null],
    [0xab, "unknown", null],
    [0xac, "unknown", null],
    [0xad, "unknown", null],
    [0xae, "unknown", null],
    [0xaf, "unknown", null],
    [0xb0, "pl_add_meseta", "aLL"],
    [0xb1, "thread_stg", "W"],
    [0xb2, "del_obj_param", "R"],
    [0xb3, "item_create", "RR"],
    [0xb4, "item_create2", "RR"],
    [0xb5, "item_delete", "RR"],
    [0xb6, "item_delete2", "RR"],
    [0xb7, "item_check", "RR"],
    [0xb8, "setevt", "aL"],
    [0xb9, "get_difflvl", "R"],
    [0xba, "set_qt_exit", "W"],
    [0xbb, "clr_qt_exit", ""],
    [0xbc, "unknown", null],
    [0xbd, "unknown", null],
    [0xbe, "unknown", null],
    [0xbf, "unknown", null],
    //[ 0xC0, 'particle_V1', null ],
    [0xc0, "particle_V3", "R"],
    [0xc1, "npc_text", "aLs"],
    [0xc2, "npc_chkwarp", ""],
    [0xc3, "pl_pkoff", ""],
    [0xc4, "map_designate", "R"],
    [0xc5, "masterkey_on", ""],
    [0xc6, "masterkey_off", ""],
    [0xc7, "window_time", ""],
    [0xc8, "winend_time", ""],
    [0xc9, "winset_time", "R"],
    [0xca, "getmtime", "R"],
    [0xcb, "set_quest_board_handler", "aLWs"],
    [0xcc, "clear_quest_board_handler", "aL"],
    //[ 0xCD, 'particle_id_V1', null ],
    [0xcd, "particle_id_V3", "R"],
    //[ 0xCE, 'npc_crptalk_id_V1', null ],
    [0xce, "npc_crptalk_id_V3", "R"],
    [0xcf, "npc_lang_clean", ""],
    [0xd0, "pl_pkon", ""],
    [0xd1, "pl_chk_item2", "RR"],
    [0xd2, "enable_mainmenu", ""],
    [0xd3, "disable_mainmenu", ""],
    [0xd4, "start_battlebgm", ""],
    [0xd5, "end_battlebgm", ""],
    [0xd6, "disp_msg_qb", "as"],
    [0xd7, "close_msg_qb", ""],
    //[ 0xD8, 'set_eventflag_v1', null ],
    [0xd8, "set_eventflag_v3", "aLL"],
    [0xd9, "sync_leti", null],
    [0xda, "set_returnhunter", ""],
    [0xdb, "set_returncity", ""],
    [0xdc, "load_pvr", ""],
    [0xdd, "load_midi", ""],
    [0xde, "unknown", null],
    //[ 0xDF, 'npc_param_V1', null ],
    [0xdf, "npc_param_V3", "aRL"],
    [0xe0, "pad_dragon", ""],
    [0xe1, "clear_mainwarp", "aL"],
    //[ 0xE2, 'pcam_param_V1', null ],
    [0xe2, "pcam_param_V3", "R"],
    //[ 0xE3, 'start_setevt_v1', null ],
    [0xe3, "start_setevt_v3", "aRL"],
    [0xe4, "warp_on", ""],
    [0xe5, "warp_off", ""],
    [0xe6, "get_slotnumber", "R"],
    [0xe7, "get_servernumber", "R"],
    [0xe8, "set_eventflag2", "aLR"],
    [0xe9, "res", "RR"],
    [0xea, "unknownEA", "RL"],
    [0xeb, "enable_bgmctrl", "aL"],
    [0xec, "sw_send", "R"],
    [0xed, "create_bgmctrl", ""],
    [0xee, "pl_add_meseta2", "aL"],
    //[ 0xEF, 'sync_let', null ],
    [0xef, "sync_register", "aRL"],
    [0xf0, "send_regwork", null],
    //[ 0xF1, 'leti_fixed_camera_V1', null ],
    [0xf1, "leti_fixed_camera_V3", "R"],
    [0xf2, "default_camera_pos1", ""],
    [0xf3, "unknown", null],
    [0xf4, "unknown", null],
    [0xf5, "unknown", null],
    [0xf6, "unknown", null],
    [0xf7, "unknown", null],
    [0xf8, "unknownF8", "R"],
    [0xf9, "unknown", null],
    [0xfa, "get_gc_number", "R"],
    [0xfb, "unknownFB", "W"],
    [0xfc, "unknown", null],
    [0xfd, "unknown", null],
    [0xfe, "unknown", null],
    [0xff, "unknownFF", ""],
];

const f8_opcode_list: Array<[number, string, string | null]> = [
    [0x00, "unknown", null],
    [0x01, "set_chat_callback?", "aRs"],
    [0x02, "unknown", null],
    [0x03, "unknown", null],
    [0x04, "unknown", null],
    [0x05, "unknown", null],
    [0x06, "unknown", null],
    [0x07, "unknown", null],
    [0x08, "get_difficulty_level2", "R"],
    [0x09, "get_number_of_player1", "R"],
    [0x0a, "get_coord_of_player", "RR"],
    [0x0b, "unknownF80B", ""],
    [0x0c, "unknownF80C", ""],
    [0x0d, "map_designate_ex", "R"],
    [0x0e, "unknownF80E", "aL"],
    [0x0f, "unknownF80F", "aL"],
    [0x10, "ba_initial_floor", "aL"],
    [0x11, "set_ba_rules", ""],
    [0x12, "unknownF812", "aL"],
    [0x13, "unknownF813", "aL"],
    [0x14, "unknownF814", "aL"],
    [0x15, "unknownF815", "aL"],
    [0x16, "unknownF816", "aL"],
    [0x17, "unknownF817", "aL"],
    [0x18, "unknownF818", "aL"],
    [0x19, "unknownF819", "aL"],
    [0x1a, "unknownF81A", "aL"],
    [0x1b, "unknownF81B", "aL"],
    [0x1c, "ba_disp_msg", "as"],
    [0x1d, "death_lvl_up", "aL"],
    [0x1e, "death_tech_lvl_up", "aL"],
    [0x1f, "unknown", null],
    [0x20, "cmode_stage", "aL"],
    [0x21, "unknown", null],
    [0x22, "unknown", null],
    [0x23, "unknownF823", "aL"],
    [0x24, "unknownF824", "aL"],
    [0x25, "exp_multiplication", "R"],
    [0x26, "exp_division?", "R"],
    [0x27, "get_user_is_dead?", "R"],
    [0x28, "go_floor", "RR"],
    [0x29, "unknown", null],
    [0x2a, "unknown", null],
    [0x2b, "unlock_door2", "aLL"],
    [0x2c, "lock_door2", "aLL"],
    [0x2d, "if_switch_not_pressed", "R"],
    [0x2e, "if_switch_pressed", "R"],
    [0x2f, "unknownF82F", "aLL"],
    [0x30, "control_dragon", "R"],
    [0x31, "release_dragon", ""],
    [0x32, "unknown", null],
    [0x33, "unknown", null],
    [0x34, "unknown", null],
    [0x35, "unknown", null],
    [0x36, "unknown", null],
    [0x37, "unknown", null],
    [0x38, "shrink", "R"],
    [0x39, "unshrink", "R"],
    [0x3a, "unknown", null],
    [0x3b, "unknown", null],
    [0x3c, "display_clock2?", "R"],
    [0x3d, "unknownF83D", "aL"],
    [0x3e, "delete_area_title?", "aL"],
    [0x3f, "unknown", null],
    [0x40, "load_npc_data", ""],
    [0x41, "get_npc_data", "W"],
    [0x42, "unknown", null],
    [0x43, "unknown", null],
    [0x44, "unknown", null],
    [0x45, "unknown", null],
    [0x46, "unknown", null],
    [0x47, "unknown", null],
    [0x48, "give_damage_score", "R"],
    [0x49, "take_damage_score", "R"],
    [0x4a, "unk_score_F84A", "R"],
    [0x4b, "unk_score_F84B", "R"],
    [0x4c, "kill_score", "R"],
    [0x4d, "death_score", "R"],
    [0x4e, "unk_score_F84E", "R"],
    [0x4f, "enemy_death_score", "R"],
    [0x50, "meseta_score", "R"],
    [0x51, "unknownF851", "R"],
    [0x52, "unknownF852", "aL"],
    [0x53, "reverse_warps", ""],
    [0x54, "unreverse_warps", ""],
    [0x55, "set_ult_map", ""],
    [0x56, "unset_ult_map", ""],
    [0x57, "set_area_title", "as"],
    [0x58, "unknownF858", ""],
    [0x59, "unknown", null],
    [0x5a, "equip_item", "R"],
    [0x5b, "unequip_item", "aLL"],
    [0x5c, "unknown", null],
    [0x5d, "unknown", null],
    [0x5e, "unknownF85E", "aL"],
    [0x5f, "unknownF85F", "aL"],
    [0x60, "unknownF860", ""],
    [0x61, "unknownF861", "aL"],
    [0x62, "unknown", null],
    [0x63, "unknown", null],
    [0x64, "cmode_rank", "aLs"],
    [0x65, "award_item_name?", ""],
    [0x66, "award_item_select?", ""],
    [0x67, "award_item_give_to?", "R"],
    [0x68, "unknownF868", "RR"],
    [0x69, "unknownF869", "RR"],
    [0x6a, "item_create_cmode", "RR"],
    [0x6b, "unknownF86B", "R"],
    [0x6c, "award_item_ok?", "R"],
    [0x6d, "unknownF86D", ""],
    [0x6e, "unknownF86E", ""],
    [0x6f, "ba_set_lives", "aL"],
    [0x70, "ba_set_tech_lvl", "aL"],
    [0x71, "ba_set_lvl", "aL"],
    [0x72, "ba_set_time_limit", "aL"],
    [0x73, "boss_is_dead?", "R"],
    [0x74, "unknown", null],
    [0x75, "unknown", null],
    [0x76, "unknown", null],
    [0x77, "enable_techs", "R"],
    [0x78, "disable_techs", "R"],
    [0x79, "get_gender", "RR"],
    [0x7a, "get_chara_class", "RR"],
    [0x7b, "take_slot_meseta", "RR"],
    [0x7c, "unknown", null],
    [0x7d, "unknown", null],
    [0x7e, "unknown", null],
    [0x7f, "read_guildcard_flag", "RR"],
    [0x80, "unknownF880", "R"],
    [0x81, "get_pl_name?", "R"],
    [0x82, "unknown", null],
    [0x83, "unknownF883", "RR"],
    [0x84, "unknown", null],
    [0x85, "unknown", null],
    [0x86, "unknown", null],
    [0x87, "unknown", null],
    [0x88, "ba_close_msg", ""],
    [0x89, "unknown", null],
    [0x8a, "get_player_status", "RR"],
    [0x8b, "send_mail", "aRs"],
    [0x8c, "online_check", "R"],
    [0x8d, "chl_set_timerecord?", "R"],
    [0x8e, "chl_get_timerecord?", "R"],
    [0x8f, "unknownF88F", "R"],
    [0x90, "unknownF890", ""],
    [0x91, "load_enemy_data", "aL"],
    [0x92, "get_physical_data", "W"],
    [0x93, "get_attack_data", "W"],
    [0x94, "get_resist_data", "W"],
    [0x95, "get_movement_data", "W"],
    [0x96, "unknown", null],
    [0x97, "unknown", null],
    [0x98, "shift_left", "RR"],
    [0x99, "shift_right", "RR"],
    [0x9a, "get_random", "RR"],
    [0x9b, "reset_map", ""],
    [0x9c, "disp_chl_retry_menu", "R"],
    [0x9d, "chl_reverser?", ""],
    [0x9e, "unknownF89E", "aL"],
    [0x9f, "unknownF89F", "R"],
    [0xa0, "unknownF8A0", ""],
    [0xa1, "unknownF8A1", ""],
    [0xa2, "unknown", null],
    [0xa3, "unknown", null],
    [0xa4, "unknown", null],
    [0xa5, "unknown", null],
    [0xa6, "unknown", null],
    [0xa7, "unknown", null],
    [0xa8, "unknownF8A8", "aL"],
    [0xa9, "unknownF8A9", "R"],
    [0xaa, "unknown", null],
    [0xab, "unknown", null],
    [0xac, "unknown", null],
    [0xad, "get_number_of_player2", "R"],
    [0xae, "unknown", null],
    [0xaf, "unknown", null],
    [0xb0, "unknown", null],
    [0xb1, "unknown", null],
    [0xb2, "unknown", null],
    [0xb3, "unknown", null],
    [0xb4, "unknown", null],
    [0xb5, "unknown", null],
    [0xb6, "unknown", null],
    [0xb7, "unknown", null],
    [0xb8, "unknownF8B8", ""],
    [0xb9, "chl_recovery?", ""],
    [0xba, "unknown", null],
    [0xbb, "unknown", null],
    [0xbc, "set_episode", "L"],
    [0xbd, "unknown", null],
    [0xbe, "unknown", null],
    [0xbf, "unknown", null],
    [0xc0, "file_dl_req", "aLs"],
    [0xc1, "get_dl_status", "R"],
    [0xc2, "gba_unknown4?", ""],
    [0xc3, "get_gba_state?", "R"],
    [0xc4, "unknownF8C4", "R"],
    [0xc5, "unknownF8C5", "R"],
    [0xc6, "QEXIT", ""],
    [0xc7, "use_animation", "RR"],
    [0xc8, "stop_animation", "R"],
    [0xc9, "run_to_coord", "RR"],
    [0xca, "set_slot_invincible", "RR"],
    [0xcb, "unknownF8CB", "R"],
    [0xcc, "set_slot_poison", "R"],
    [0xcd, "set_slot_paralyze", "R"],
    [0xce, "set_slot_shock", "R"],
    [0xcf, "set_slot_freeze", "R"],
    [0xd0, "set_slot_slow", "R"],
    [0xd1, "set_slot_confuse", "R"],
    [0xd2, "set_slot_shifta", "R"],
    [0xd3, "set_slot_deband", "R"],
    [0xd4, "set_slot_jellen", "R"],
    [0xd5, "set_slot_zalure", "R"],
    [0xd6, "fleti_fixed_camera", "aR"],
    [0xd7, "fleti_locked_camera", "aLR"],
    [0xd8, "default_camera_pos2", ""],
    [0xd9, "set_motion_blur", ""],
    [0xda, "set_screen_b&w", ""],
    [0xdb, "unknownF8DB", "aLLLLRW"],
    [0xdc, "NPC_action_string", "RRW"],
    [0xdd, "get_pad_cond", "RR"],
    [0xde, "get_button_cond", "RR"],
    [0xdf, "freeze_enemies", ""],
    [0xe0, "unfreeze_enemies", ""],
    [0xe1, "freeze_everything", ""],
    [0xe2, "unfreeze_everything", ""],
    [0xe3, "restore_hp", "R"],
    [0xe4, "restore_tp", "R"],
    [0xe5, "close_chat_bubble", "R"],
    [0xe6, "unknownF8E6", "RR"],
    [0xe7, "unknownF8E7", "RR"],
    [0xe8, "unknownF8E8", "RR"],
    [0xe9, "unknownF8E9", "RR"],
    [0xea, "unknownF8EA", "RR"],
    [0xeb, "unknownF8EB", "RR"],
    [0xec, "unknownF8EC", "RR"],
    [0xed, "animation_check", "RR"],
    [0xee, "call_image_data", "aLW"],
    [0xef, "unknownF8EF", ""],
    [0xf0, "turn_off_bgm_p2", ""],
    [0xf1, "turn_on_bgm_p2", ""],
    [0xf2, "load_unk_data", "aLLLLRW"],
    [0xf3, "particle2", "aRLf"],
    [0xf4, "unknown", null],
    [0xf5, "unknown", null],
    [0xf6, "unknown", null],
    [0xf7, "unknown", null],
    [0xf8, "unknown", null],
    [0xf9, "unknown", null],
    [0xfa, "unknown", null],
    [0xfb, "unknown", null],
    [0xfc, "unknown", null],
    [0xfd, "unknown", null],
    [0xfe, "unknown", null],
    [0xff, "unknown", null],
];

const f9_opcode_list: Array<[number, string, string | null]> = [
    [0x00, "unknown", null],
    [0x01, "dec2float", "RR"],
    [0x02, "float2dec", "RR"],
    [0x03, "flet", "RR"],
    [0x04, "fleti", "RF"],
    [0x05, "unknown", null],
    [0x06, "unknown", null],
    [0x07, "unknown", null],
    [0x08, "fadd", "RR"],
    [0x09, "faddi", "RF"],
    [0x0a, "fsub", "RR"],
    [0x0b, "fsubi", "RF"],
    [0x0c, "fmul", "RR"],
    [0x0d, "fmuli", "RF"],
    [0x0e, "fdiv", "RR"],
    [0x0f, "fdivi", "RF"],
    [0x10, "get_unknown_count?", "aLR"],
    [0x11, "get_stackable_item_count", "RR"],
    [0x12, "freeze_and_hide_equip", ""],
    [0x13, "thaw_and_show_equip", ""],
    [0x14, "set_paletteX_callback", "aRW"],
    [0x15, "activate_paletteX", "aR"],
    [0x16, "enable_paletteX", "aR"],
    [0x17, "restore_paletteX", "aL"],
    [0x18, "disable_paletteX", "aL"],
    [0x19, "get_paletteX_activated", "aLR"],
    [0x1a, "get_unknown_paletteX_status?", "aLR"],
    [0x1b, "disable_movement2", "aR"],
    [0x1c, "enable_movement2", "aR"],
    [0x1d, "get_time_played", "R"],
    [0x1e, "get_guildcard_total", "R"],
    [0x1f, "get_slot_meseta", "R"],
    [0x20, "get_player_level", "aLR"],
    [0x21, "get_Section_ID", "aLR"],
    [0x22, "get_player_hp", "aRR"],
    [0x23, "get_floor_number", "aRR"],
    [0x24, "get_coord_player_detect", "RR"],
    [0x25, "read_global_flag", "abR"],
    [0x26, "write_global_flag", "abR"],
    [0x27, "unknownF927", "RR"],
    [0x28, "floor_player_detect", "R"],
    [0x29, "read_disk_file?", "as"],
    [0x2a, "open_pack_select", ""],
    [0x2b, "item_select", "R"],
    [0x2c, "get_item_id", "R"],
    [0x2d, "color_change", "aRRRRR"],
    [0x2e, "send_statistic?", "aLLLLLLLL"],
    [0x2f, "unknownF92F", "aLL"],
    [0x30, "chat_box", "aLLLLLs"],
    [0x31, "chat_bubble", "aLs"],
    [0x32, "unknown", null],
    [0x33, "unknownF933", "R"],
    [0x34, "scroll_text", "aLLLLLfRs"],
    [0x35, "gba_unknown1", ""],
    [0x36, "gba_unknown2", ""],
    [0x37, "gba_unknown3", ""],
    [0x38, "add_damage_to?", "aLL"],
    [0x39, "item_delete2", "aL"],
    [0x3a, "get_item_info", "aLR"],
    [0x3b, "item_packing1", "aL"],
    [0x3c, "item_packing2", "aLL"],
    [0x3d, "get_lang_setting?", "aR"],
    [0x3e, "prepare_statistic?", "aLWW"],
    [0x3f, "keyword_detect", ""],
    [0x40, "Keyword", "aRLs"],
    [0x41, "get_guildcard_num", "aLR"],
    [0x42, "unknown", null],
    [0x43, "unknown", null],
    [0x44, "get_wrap_status", "aLR"],
    [0x45, "initial_floor", "aL"],
    [0x46, "sin", "aRL"],
    [0x47, "cos", "aRL"],
    [0x48, "unknown", null],
    [0x49, "unknown", null],
    [0x4a, "boss_is_dead2?", "R"],
    [0x4b, "unknownF94B", "R"],
    [0x4c, "unknownF94C", "R"],
    [0x4d, "is_there_cardbattle", "R"],
    [0x4e, "unknown", null],
    [0x4f, "unknown", null],
    [0x50, "BB_p2_menu", "aL"],
    [0x51, "BB_Map_Designate", "BWBB"],
    [0x52, "BB_get_number_in_pack", "R"],
    [0x53, "BB_swap_item", "aLLLLLLWW"],
    [0x54, "BB_check_wrap", "aRR"],
    [0x55, "BB_exchange_PD_item", "aRRRWW"],
    [0x56, "BB_exchange_PD_srank", "aRRRRRWW"],
    [0x57, "BB_exchange_PD_special", "aRRRRRLWW"],
    [0x58, "BB_exchange_PD_percent", "aRRRRRLWW"],
    [0x59, "unknownF959", "aL"],
    [0x5a, "unknown", null],
    [0x5b, "unknown", null],
    [0x5c, "BB_exchange_SLT", "aLRWW"],
    [0x5d, "BB_exchange_PC", ""],
    [0x5e, "BB_box_create_BP", "aLff"],
    [0x5f, "BB_exchange_PT", "aRRLWW"],
    [0x60, "unknownF960", "aL"],
    [0x61, "unknownF961", ""],
    [0x62, "unknown", null],
    [0x63, "unknown", null],
    [0x64, "unknown", null],
    [0x65, "unknown", null],
    [0x66, "unknown", null],
    [0x67, "unknown", null],
    [0x68, "unknown", null],
    [0x69, "unknown", null],
    [0x6a, "unknown", null],
    [0x6b, "unknown", null],
    [0x6c, "unknown", null],
    [0x6d, "unknown", null],
    [0x6e, "unknown", null],
    [0x6f, "unknown", null],
    [0x70, "unknown", null],
    [0x71, "unknown", null],
    [0x72, "unknown", null],
    [0x73, "unknown", null],
    [0x74, "unknown", null],
    [0x75, "unknown", null],
    [0x76, "unknown", null],
    [0x77, "unknown", null],
    [0x78, "unknown", null],
    [0x79, "unknown", null],
    [0x7a, "unknown", null],
    [0x7b, "unknown", null],
    [0x7c, "unknown", null],
    [0x7d, "unknown", null],
    [0x7e, "unknown", null],
    [0x7f, "unknown", null],
    [0x80, "unknown", null],
    [0x81, "unknown", null],
    [0x82, "unknown", null],
    [0x83, "unknown", null],
    [0x84, "unknown", null],
    [0x85, "unknown", null],
    [0x86, "unknown", null],
    [0x87, "unknown", null],
    [0x88, "unknown", null],
    [0x89, "unknown", null],
    [0x8a, "unknown", null],
    [0x8b, "unknown", null],
    [0x8c, "unknown", null],
    [0x8d, "unknown", null],
    [0x8e, "unknown", null],
    [0x8f, "unknown", null],
    [0x90, "unknown", null],
    [0x91, "unknown", null],
    [0x92, "unknown", null],
    [0x93, "unknown", null],
    [0x94, "unknown", null],
    [0x95, "unknown", null],
    [0x96, "unknown", null],
    [0x97, "unknown", null],
    [0x98, "unknown", null],
    [0x99, "unknown", null],
    [0x9a, "unknown", null],
    [0x9b, "unknown", null],
    [0x9c, "unknown", null],
    [0x9d, "unknown", null],
    [0x9e, "unknown", null],
    [0x9f, "unknown", null],
    [0xa0, "unknown", null],
    [0xa1, "unknown", null],
    [0xa2, "unknown", null],
    [0xa3, "unknown", null],
    [0xa4, "unknown", null],
    [0xa5, "unknown", null],
    [0xa6, "unknown", null],
    [0xa7, "unknown", null],
    [0xa8, "unknown", null],
    [0xa9, "unknown", null],
    [0xaa, "unknown", null],
    [0xab, "unknown", null],
    [0xac, "unknown", null],
    [0xad, "unknown", null],
    [0xae, "unknown", null],
    [0xaf, "unknown", null],
    [0xb0, "unknown", null],
    [0xb1, "unknown", null],
    [0xb2, "unknown", null],
    [0xb3, "unknown", null],
    [0xb4, "unknown", null],
    [0xb5, "unknown", null],
    [0xb6, "unknown", null],
    [0xb7, "unknown", null],
    [0xb8, "unknown", null],
    [0xb9, "unknown", null],
    [0xba, "unknown", null],
    [0xbb, "unknown", null],
    [0xbc, "unknown", null],
    [0xbd, "unknown", null],
    [0xbe, "unknown", null],
    [0xbf, "unknown", null],
    [0xc0, "unknown", null],
    [0xc1, "unknown", null],
    [0xc2, "unknown", null],
    [0xc3, "unknown", null],
    [0xc4, "unknown", null],
    [0xc5, "unknown", null],
    [0xc6, "unknown", null],
    [0xc7, "unknown", null],
    [0xc8, "unknown", null],
    [0xc9, "unknown", null],
    [0xca, "unknown", null],
    [0xcb, "unknown", null],
    [0xcc, "unknown", null],
    [0xcd, "unknown", null],
    [0xce, "unknown", null],
    [0xcf, "unknown", null],
    [0xd0, "unknown", null],
    [0xd1, "unknown", null],
    [0xd2, "unknown", null],
    [0xd3, "unknown", null],
    [0xd4, "unknown", null],
    [0xd5, "unknown", null],
    [0xd6, "unknown", null],
    [0xd7, "unknown", null],
    [0xd8, "unknown", null],
    [0xd9, "unknown", null],
    [0xda, "unknown", null],
    [0xdb, "unknown", null],
    [0xdc, "unknown", null],
    [0xdd, "unknown", null],
    [0xde, "unknown", null],
    [0xdf, "unknown", null],
    [0xe0, "unknown", null],
    [0xe1, "unknown", null],
    [0xe2, "unknown", null],
    [0xe3, "unknown", null],
    [0xe4, "unknown", null],
    [0xe5, "unknown", null],
    [0xe6, "unknown", null],
    [0xe7, "unknown", null],
    [0xe8, "unknown", null],
    [0xe9, "unknown", null],
    [0xea, "unknown", null],
    [0xeb, "unknown", null],
    [0xec, "unknown", null],
    [0xed, "unknown", null],
    [0xee, "unknown", null],
    [0xef, "unknown", null],
    [0xf0, "unknown", null],
    [0xf1, "unknown", null],
    [0xf2, "unknown", null],
    [0xf3, "unknown", null],
    [0xf4, "unknown", null],
    [0xf5, "unknown", null],
    [0xf6, "unknown", null],
    [0xf7, "unknown", null],
    [0xf8, "unknown", null],
    [0xf9, "unknown", null],
    [0xfa, "unknown", null],
    [0xfb, "unknown", null],
    [0xfc, "unknown", null],
    [0xfd, "unknown", null],
    [0xfe, "unknown", null],
    [0xff, "unknown", null],
];

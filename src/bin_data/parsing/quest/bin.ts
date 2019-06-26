import { BufferCursor } from '../../BufferCursor';
import Logger from 'js-logger';

const logger = Logger.get('bin_data/parsing/quest/bin');

export interface BinFile {
    questNumber: number;
    language: number;
    questName: string;
    shortDescription: string;
    longDescription: string;
    functionOffsets: number[];
    instructions: Instruction[];
    data: BufferCursor;
}

export function parseBin(cursor: BufferCursor, lenient: boolean = false): BinFile {
    const objectCodeOffset = cursor.u32();
    const functionOffsetTableOffset = cursor.u32(); // Relative offsets
    const size = cursor.u32();
    cursor.seek(4); // Always seems to be 0xFFFFFFFF
    const questNumber = cursor.u32();
    const language = cursor.u32();
    const questName = cursor.string_utf16(64, true, true);
    const shortDescription = cursor.string_utf16(256, true, true);
    const longDescription = cursor.string_utf16(576, true, true);

    if (size !== cursor.size) {
        logger.warn(`Value ${size} in bin size field does not match actual size ${cursor.size}.`);
    }

    const functionOffsetCount = Math.floor(
        (cursor.size - functionOffsetTableOffset) / 4);

    cursor.seek_start(functionOffsetTableOffset);
    const functionOffsets = [];

    for (let i = 0; i < functionOffsetCount; ++i) {
        functionOffsets.push(cursor.i32());
    }

    const instructions = parseObjectCode(
        cursor.seek_start(objectCodeOffset).take(functionOffsetTableOffset - objectCodeOffset),
        lenient
    );

    return {
        questNumber,
        language,
        questName,
        shortDescription,
        longDescription,
        functionOffsets,
        instructions,
        data: cursor.seek_start(0).take(cursor.size)
    };
}

export function writeBin({ data }: { data: BufferCursor }): BufferCursor {
    return data.seek_start(0);
}

export interface Instruction {
    opcode: number;
    mnemonic: string;
    args: any[];
    size: number;
}

function parseObjectCode(cursor: BufferCursor, lenient: boolean): Instruction[] {
    const instructions = [];

    try {
        while (cursor.bytes_left) {
            const mainOpcode = cursor.u8();
            let opcode;
            let opsize;
            let list;

            switch (mainOpcode) {
                case 0xF8:
                    opcode = cursor.u8();
                    opsize = 2;
                    list = F8opcodeList;
                    break;
                case 0xF9:
                    opcode = cursor.u8();
                    opsize = 2;
                    list = F9opcodeList;
                    break;
                default:
                    opcode = mainOpcode;
                    opsize = 1;
                    list = opcodeList;
                    break;
            }

            let [, mnemonic, mask] = list[opcode];

            if (mask == null) {
                let fullOpcode = mainOpcode;

                if (mainOpcode === 0xF8 || mainOpcode === 0xF9) {
                    fullOpcode = (fullOpcode << 8) | opcode;
                }

                logger.warn(`Parameters unknown for opcode 0x${fullOpcode.toString(16).toUpperCase()}, assuming 0.`);

                instructions.push({
                    opcode,
                    mnemonic,
                    args: [],
                    size: opsize
                });
            } else {
                try {
                    const opargs = parseInstructionArguments(cursor, mask);

                    instructions.push({
                        opcode,
                        mnemonic,
                        args: opargs.args,
                        size: opsize + opargs.size
                    });
                } catch (e) {
                    instructions.push({
                        opcode,
                        mnemonic,
                        args: [],
                        size: opsize
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

function parseInstructionArguments(
    cursor: BufferCursor,
    mask: string
): { args: any[], size: number } {
    const oldPos = cursor.position;
    const args = [];
    let argsSize: number;

    outer:
    for (let i = 0; i < mask.length; ++i) {
        switch (mask.charAt(i)) {
            // Pushes something on the stack
            case 'p':
                break;
            // Pops the stack (no increments)
            case 'a':
                break outer;

            // Unsigned integers
            case 'B':
                args.push(cursor.u8());
                break;
            case 'W':
                args.push(cursor.u16());
                break;
            case 'L':
                args.push(cursor.u32());
                break;

            // Signed integers
            case 'I':
                args.push(cursor.i32());
                break;

            // Floats
            case 'f':
            case 'F':
                args.push(cursor.f32());
                break;

            // Registers?
            case 'R':
            case 'r':
                cursor.seek(1);
                break;

            // Registers with unsigned integers?
            case 'b':
                args.push(cursor.u8());
                break;
            case 'w':
                args.push(cursor.u16());
                break;
            case 'l':
                args.push(cursor.u32());
                break;

            // Registers with signed integers?
            case 'i':
                args.push(cursor.i32());
                break;

            // Variably sized data?
            case 'j':
            case 'J':
                argsSize = 2 * cursor.u8();
                cursor.seek(argsSize);
                break;
            case 't':
            case 'T':
                argsSize = cursor.u8();
                cursor.seek(argsSize);
                break;

            // Strings
            case 's':
            case 'S':
                while (cursor.u16()) { }
                break;

            default:
                throw new Error(`Unknown mask part ${mask.charAt(i)}.`);
        }
    }

    return { args, size: cursor.position - oldPos };
}

const opcodeList: Array<[number, string, string | null]> = [
    [0x00, 'nop', ''],
    [0x01, 'ret', ''],
    [0x02, 'sync', ''],
    [0x03, 'exit', 'aL'],
    [0x04, 'thread', 'W'],
    [0x05, 'va_start', ''],
    [0x06, 'va_end', ''],
    [0x07, 'va_call', 'W'],
    [0x08, 'let', 'RR'],
    [0x09, 'leti', 'RI'],
    [0x0A, 'unknown', null],
    [0x0B, 'unknown', null],
    [0x0C, 'unknown', null],
    [0x0D, 'unknown', null],
    [0x0E, 'unknown', null],
    [0x0F, 'unknown', null],
    [0x10, 'set', 'R'],
    [0x11, 'clear', 'R'],
    [0x12, 'rev', 'R'],
    [0x13, 'gset', 'w'],
    [0x14, 'gclear', 'w'],
    [0x15, 'grev', 'w'],
    [0x16, 'glet', 'w'],
    [0x17, 'gget', 'wR'],
    [0x18, 'add', 'RR'],
    [0x19, 'addi', 'RI'],
    [0x1A, 'sub', 'RR'],
    [0x1B, 'subi', 'RI'],
    [0x1C, 'mul', 'RR'],
    [0x1D, 'muli', 'RI'],
    [0x1E, 'div', 'RR'],
    [0x1F, 'divi', 'RI'],
    [0x20, 'and', 'RR'],
    [0x21, 'andi', 'RI'],
    [0x22, 'or', 'RR'],
    [0x23, 'ori', 'RI'],
    [0x24, 'xor', 'RR'],
    [0x25, 'xori', 'RI'],
    [0x26, 'mod', 'RR'],
    [0x27, 'modi', 'RI'],
    [0x28, 'jmp', 'W'],
    [0x29, 'call', 'W'],
    [0x2A, 'jmp_on', 'Wt'],
    [0x2B, 'jmp_off', 'Wt'],
    [0x2C, 'jmp_=', 'RRW'],
    [0x2D, 'jmpi_=', 'RIW'],
    [0x2E, 'jmp_!=', 'RRW'],
    [0x2F, 'jmpi_!=', 'RIW'],
    [0x30, 'ujmp_>', 'RRW'],
    [0x31, 'ujmpi_>', 'RLW'],
    [0x32, 'jmp_>', 'RRW'],
    [0x33, 'jmpi_>', 'RIW'],
    [0x34, 'ujmp_<', 'RRW'],
    [0x35, 'ujmpi_<', 'RLW'],
    [0x36, 'jmp_<', 'RRW'],
    [0x37, 'jmpi_<', 'RIW'],
    [0x38, 'ujmp_>=', 'RRW'],
    [0x39, 'ujmpi_>=', 'RLW'],
    [0x3A, 'jmp_>=', 'RRW'],
    [0x3B, 'jmpi_>=', 'RIW'],
    [0x3C, 'ujmp_<=', 'RRW'],
    [0x3D, 'ujmpi_<=', 'RLW'],
    [0x3E, 'jmp_<=', 'RRW'],
    [0x3F, 'jmpi_<=', 'RIW'],
    [0x40, 'switch_jmp', 'Rj'],
    [0x41, 'switch_call', 'Rj'],
    [0x42, 'stack_push', 'R'],
    [0x43, 'stack_pop', 'R'],
    [0x44, 'stack_pushm', 'RL'],
    [0x45, 'stack_popm', 'RL'],
    [0x46, 'unknown', null],
    [0x47, 'unknown', null],
    [0x48, 'arg_pushr', 'pR'],
    [0x49, 'arg_pushl', 'pI'],
    [0x4A, 'arg_pushb', 'pB'],
    [0x4B, 'arg_pushw', 'pW'],
    [0x4C, 'unknown', null],
    [0x4D, 'unknown', null],
    [0x4E, 'arg_pushs', 'ps'],
    [0x4F, 'unknown4F', 'RR'],
    [0x50, 'message', 'aLs'],
    [0x51, 'list', 'aRs'],
    [0x52, 'fadein', ''],
    [0x53, 'fadeout', ''],
    [0x54, 'se', 'aL'],
    [0x55, 'bgm', 'aL'],
    [0x56, 'unknown', null],
    [0x57, 'unknown', null],
    [0x58, 'enable', 'aL'],
    [0x59, 'disable', 'aL'],
    [0x5A, 'window_msg', 'as'],
    [0x5B, 'add_msg', 'as'],
    [0x5C, 'mesend', ''],
    [0x5D, 'gettime', 'R'],
    [0x5E, 'winend', ''],
    [0x5F, 'unknown', null],
    //[ 0x60, 'npc_crt_V1', null ],
    [0x60, 'npc_crt_V3', 'R'],
    [0x61, 'npc_stop', 'aR'],
    [0x62, 'npc_play', 'aL'],
    [0x63, 'npc_kill', 'aR'],
    [0x64, 'npc_nont', ''],
    [0x65, 'npc_talk', ''],
    //[ 0x66, 'npc_crp_V1', null ],
    [0x66, 'npc_crp_V3', 'R'],
    [0x67, 'unknown', null],
    [0x68, 'create_pipe', 'aL'],
    //[ 0x69, 'p_hpstat_V1', null ],
    [0x69, 'p_hpstat_V3', 'aRL'],
    //[ 0x6A, 'p_dead_V1', null ],
    [0x6A, 'p_dead_V3', 'aRL'],
    [0x6B, 'p_disablewarp', ''],
    [0x6C, 'p_enablewarp', ''],
    //[ 0x6D, 'p_move_V1', null ],
    [0x6D, 'p_move_V3', 'R'],
    [0x6E, 'p_look', 'aL'],
    [0x6F, 'unknown', null],
    [0x70, 'p_action_disable', ''],
    [0x71, 'p_action_enable', ''],
    [0x72, 'disable_movement1', 'aR'],
    [0x73, 'enable_movement1', 'aR'],
    [0x74, 'p_noncol', ''],
    [0x75, 'p_col', ''],
    [0x76, 'p_setpos', 'aRR'],
    [0x77, 'p_return_guild', ''],
    [0x78, 'p_talk_guild', 'aL'],
    //[ 0x79, 'npc_talk_pl_V1', null ],
    [0x79, 'npc_talk_pl_V3', 'R'],
    [0x7A, 'npc_talk_kill', 'aL'],
    //[ 0x7B, 'npc_crtpk_V1', null ],
    [0x7B, 'npc_crtpk_V3', 'R'],
    //[ 0x7C, 'npc_crppk_V1', null ],
    [0x7C, 'npc_crppk_V3', 'R'],
    //[ 0x7D, 'npc_crptalk_v1', null ],
    [0x7D, 'npc_crptalk_v3', 'R'],
    [0x7E, 'p_look_at_V1', 'aLL'],
    //[ 0x7F, 'npc_crp_id_V1', null ],
    [0x7F, 'npc_crp_id_V3', 'R'],
    [0x80, 'cam_quake', ''],
    [0x81, 'cam_adj', ''],
    [0x82, 'cam_zmin', ''],
    [0x83, 'cam_zmout', ''],
    //[ 0x84, 'cam_pan_V1', null ],
    [0x84, 'cam_pan_V3', 'R'],
    [0x85, 'game_lev_super', ''],
    [0x86, 'game_lev_reset', ''],
    //[ 0x87, 'pos_pipe_V1', null ],
    [0x87, 'pos_pipe_V3', 'R'],
    [0x88, 'if_zone_clear', 'RR'],
    [0x89, 'chk_ene_num', 'R'],
    [0x8A, 'unhide_obj', 'R'],
    [0x8B, 'unhide_ene', 'R'],
    [0x8C, 'at_coords_call', 'R'],
    [0x8D, 'at_coords_talk', 'R'],
    [0x8E, 'col_npcin', 'R'],
    [0x8F, 'col_npcinr', 'R'],
    [0x90, 'switch_on', 'aL'],
    [0x91, 'switch_off', 'aL'],
    [0x92, 'playbgm_epi', 'aL'],
    [0x93, 'set_mainwarp', 'aL'],
    [0x94, 'set_obj_param', 'RR'],
    [0x95, 'set_floor_handler', 'aLW'],
    [0x96, 'clr_floor_handler', 'aL'],
    [0x97, 'col_plinaw', 'R'],
    [0x98, 'hud_hide', ''],
    [0x99, 'hud_show', ''],
    [0x9A, 'cine_enable', ''],
    [0x9B, 'cine_disable', ''],
    [0x9C, 'unknown', null],
    [0x9D, 'unknown', null],
    [0x9E, 'unknown', null],
    [0x9F, 'unknown', null],
    [0xA0, 'unknown', null],
    [0xA1, 'set_qt_failure', 'W'],
    [0xA2, 'set_qt_success', 'W'],
    [0xA3, 'clr_qt_failure', ''],
    [0xA4, 'clr_qt_success', ''],
    [0xA5, 'set_qt_cancel', 'W'],
    [0xA6, 'clr_qt_cancel', ''],
    [0xA7, 'unknown', null],
    //[ 0xA8, 'pl_walk_V1', null ],
    [0xA8, 'pl_walk_V3', 'R'],
    [0xA9, 'unknown', null],
    [0xAA, 'unknown', null],
    [0xAB, 'unknown', null],
    [0xAC, 'unknown', null],
    [0xAD, 'unknown', null],
    [0xAE, 'unknown', null],
    [0xAF, 'unknown', null],
    [0xB0, 'pl_add_meseta', 'aLL'],
    [0xB1, 'thread_stg', 'W'],
    [0xB2, 'del_obj_param', 'R'],
    [0xB3, 'item_create', 'RR'],
    [0xB4, 'item_create2', 'RR'],
    [0xB5, 'item_delete', 'RR'],
    [0xB6, 'item_delete2', 'RR'],
    [0xB7, 'item_check', 'RR'],
    [0xB8, 'setevt', 'aL'],
    [0xB9, 'get_difflvl', 'R'],
    [0xBA, 'set_qt_exit', 'W'],
    [0xBB, 'clr_qt_exit', ''],
    [0xBC, 'unknown', null],
    [0xBD, 'unknown', null],
    [0xBE, 'unknown', null],
    [0xBF, 'unknown', null],
    //[ 0xC0, 'particle_V1', null ],
    [0xC0, 'particle_V3', 'R'],
    [0xC1, 'npc_text', 'aLs'],
    [0xC2, 'npc_chkwarp', ''],
    [0xC3, 'pl_pkoff', ''],
    [0xC4, 'map_designate', 'R'],
    [0xC5, 'masterkey_on', ''],
    [0xC6, 'masterkey_off', ''],
    [0xC7, 'window_time', ''],
    [0xC8, 'winend_time', ''],
    [0xC9, 'winset_time', 'R'],
    [0xCA, 'getmtime', 'R'],
    [0xCB, 'set_quest_board_handler', 'aLWs'],
    [0xCC, 'clear_quest_board_handler', 'aL'],
    //[ 0xCD, 'particle_id_V1', null ],
    [0xCD, 'particle_id_V3', 'R'],
    //[ 0xCE, 'npc_crptalk_id_V1', null ],
    [0xCE, 'npc_crptalk_id_V3', 'R'],
    [0xCF, 'npc_lang_clean', ''],
    [0xD0, 'pl_pkon', ''],
    [0xD1, 'pl_chk_item2', 'RR'],
    [0xD2, 'enable_mainmenu', ''],
    [0xD3, 'disable_mainmenu', ''],
    [0xD4, 'start_battlebgm', ''],
    [0xD5, 'end_battlebgm', ''],
    [0xD6, 'disp_msg_qb', 'as'],
    [0xD7, 'close_msg_qb', ''],
    //[ 0xD8, 'set_eventflag_v1', null ],
    [0xD8, 'set_eventflag_v3', 'aLL'],
    [0xD9, 'sync_leti', null],
    [0xDA, 'set_returnhunter', ''],
    [0xDB, 'set_returncity', ''],
    [0xDC, 'load_pvr', ''],
    [0xDD, 'load_midi', ''],
    [0xDE, 'unknown', null],
    //[ 0xDF, 'npc_param_V1', null ],
    [0xDF, 'npc_param_V3', 'aRL'],
    [0xE0, 'pad_dragon', ''],
    [0xE1, 'clear_mainwarp', 'aL'],
    //[ 0xE2, 'pcam_param_V1', null ],
    [0xE2, 'pcam_param_V3', 'R'],
    //[ 0xE3, 'start_setevt_v1', null ],
    [0xE3, 'start_setevt_v3', 'aRL'],
    [0xE4, 'warp_on', ''],
    [0xE5, 'warp_off', ''],
    [0xE6, 'get_slotnumber', 'R'],
    [0xE7, 'get_servernumber', 'R'],
    [0xE8, 'set_eventflag2', 'aLR'],
    [0xE9, 'res', 'RR'],
    [0xEA, 'unknownEA', 'RL'],
    [0xEB, 'enable_bgmctrl', 'aL'],
    [0xEC, 'sw_send', 'R'],
    [0xED, 'create_bgmctrl', ''],
    [0xEE, 'pl_add_meseta2', 'aL'],
    //[ 0xEF, 'sync_let', null ],
    [0xEF, 'sync_register', 'aRL'],
    [0xF0, 'send_regwork', null],
    //[ 0xF1, 'leti_fixed_camera_V1', null ],
    [0xF1, 'leti_fixed_camera_V3', 'R'],
    [0xF2, 'default_camera_pos1', ''],
    [0xF3, 'unknown', null],
    [0xF4, 'unknown', null],
    [0xF5, 'unknown', null],
    [0xF6, 'unknown', null],
    [0xF7, 'unknown', null],
    [0xF8, 'unknownF8', 'R'],
    [0xF9, 'unknown', null],
    [0xFA, 'get_gc_number', 'R'],
    [0xFB, 'unknownFB', 'W'],
    [0xFC, 'unknown', null],
    [0xFD, 'unknown', null],
    [0xFE, 'unknown', null],
    [0xFF, 'unknownFF', ''],
];

const F8opcodeList: Array<[number, string, string | null]> = [
    [0x00, 'unknown', null],
    [0x01, 'set_chat_callback?', 'aRs'],
    [0x02, 'unknown', null],
    [0x03, 'unknown', null],
    [0x04, 'unknown', null],
    [0x05, 'unknown', null],
    [0x06, 'unknown', null],
    [0x07, 'unknown', null],
    [0x08, 'get_difficulty_level2', 'R'],
    [0x09, 'get_number_of_player1', 'R'],
    [0x0A, 'get_coord_of_player', 'RR'],
    [0x0B, 'unknownF80B', ''],
    [0x0C, 'unknownF80C', ''],
    [0x0D, 'map_designate_ex', 'R'],
    [0x0E, 'unknownF80E', 'aL'],
    [0x0F, 'unknownF80F', 'aL'],
    [0x10, 'ba_initial_floor', 'aL'],
    [0x11, 'set_ba_rules', ''],
    [0x12, 'unknownF812', 'aL'],
    [0x13, 'unknownF813', 'aL'],
    [0x14, 'unknownF814', 'aL'],
    [0x15, 'unknownF815', 'aL'],
    [0x16, 'unknownF816', 'aL'],
    [0x17, 'unknownF817', 'aL'],
    [0x18, 'unknownF818', 'aL'],
    [0x19, 'unknownF819', 'aL'],
    [0x1A, 'unknownF81A', 'aL'],
    [0x1B, 'unknownF81B', 'aL'],
    [0x1C, 'ba_disp_msg', 'as'],
    [0x1D, 'death_lvl_up', 'aL'],
    [0x1E, 'death_tech_lvl_up', 'aL'],
    [0x1F, 'unknown', null],
    [0x20, 'cmode_stage', 'aL'],
    [0x21, 'unknown', null],
    [0x22, 'unknown', null],
    [0x23, 'unknownF823', 'aL'],
    [0x24, 'unknownF824', 'aL'],
    [0x25, 'exp_multiplication', 'R'],
    [0x26, 'exp_division?', 'R'],
    [0x27, 'get_user_is_dead?', 'R'],
    [0x28, 'go_floor', 'RR'],
    [0x29, 'unknown', null],
    [0x2A, 'unknown', null],
    [0x2B, 'unlock_door2', 'aLL'],
    [0x2C, 'lock_door2', 'aLL'],
    [0x2D, 'if_switch_not_pressed', 'R'],
    [0x2E, 'if_switch_pressed', 'R'],
    [0x2F, 'unknownF82F', 'aLL'],
    [0x30, 'control_dragon', 'R'],
    [0x31, 'release_dragon', ''],
    [0x32, 'unknown', null],
    [0x33, 'unknown', null],
    [0x34, 'unknown', null],
    [0x35, 'unknown', null],
    [0x36, 'unknown', null],
    [0x37, 'unknown', null],
    [0x38, 'shrink', 'R'],
    [0x39, 'unshrink', 'R'],
    [0x3A, 'unknown', null],
    [0x3B, 'unknown', null],
    [0x3C, 'display_clock2?', 'R'],
    [0x3D, 'unknownF83D', 'aL'],
    [0x3E, 'delete_area_title?', 'aL'],
    [0x3F, 'unknown', null],
    [0x40, 'load_npc_data', ''],
    [0x41, 'get_npc_data', 'W'],
    [0x42, 'unknown', null],
    [0x43, 'unknown', null],
    [0x44, 'unknown', null],
    [0x45, 'unknown', null],
    [0x46, 'unknown', null],
    [0x47, 'unknown', null],
    [0x48, 'give_damage_score', 'R'],
    [0x49, 'take_damage_score', 'R'],
    [0x4A, 'unk_score_F84A', 'R'],
    [0x4B, 'unk_score_F84B', 'R'],
    [0x4C, 'kill_score', 'R'],
    [0x4D, 'death_score', 'R'],
    [0x4E, 'unk_score_F84E', 'R'],
    [0x4F, 'enemy_death_score', 'R'],
    [0x50, 'meseta_score', 'R'],
    [0x51, 'unknownF851', 'R'],
    [0x52, 'unknownF852', 'aL'],
    [0x53, 'reverse_warps', ''],
    [0x54, 'unreverse_warps', ''],
    [0x55, 'set_ult_map', ''],
    [0x56, 'unset_ult_map', ''],
    [0x57, 'set_area_title', 'as'],
    [0x58, 'unknownF858', ''],
    [0x59, 'unknown', null],
    [0x5A, 'equip_item', 'R'],
    [0x5B, 'unequip_item', 'aLL'],
    [0x5C, 'unknown', null],
    [0x5D, 'unknown', null],
    [0x5E, 'unknownF85E', 'aL'],
    [0x5F, 'unknownF85F', 'aL'],
    [0x60, 'unknownF860', ''],
    [0x61, 'unknownF861', 'aL'],
    [0x62, 'unknown', null],
    [0x63, 'unknown', null],
    [0x64, 'cmode_rank', 'aLs'],
    [0x65, 'award_item_name?', ''],
    [0x66, 'award_item_select?', ''],
    [0x67, 'award_item_give_to?', 'R'],
    [0x68, 'unknownF868', 'RR'],
    [0x69, 'unknownF869', 'RR'],
    [0x6A, 'item_create_cmode', 'RR'],
    [0x6B, 'unknownF86B', 'R'],
    [0x6C, 'award_item_ok?', 'R'],
    [0x6D, 'unknownF86D', ''],
    [0x6E, 'unknownF86E', ''],
    [0x6F, 'ba_set_lives', 'aL'],
    [0x70, 'ba_set_tech_lvl', 'aL'],
    [0x71, 'ba_set_lvl', 'aL'],
    [0x72, 'ba_set_time_limit', 'aL'],
    [0x73, 'boss_is_dead?', 'R'],
    [0x74, 'unknown', null],
    [0x75, 'unknown', null],
    [0x76, 'unknown', null],
    [0x77, 'enable_techs', 'R'],
    [0x78, 'disable_techs', 'R'],
    [0x79, 'get_gender', 'RR'],
    [0x7A, 'get_chara_class', 'RR'],
    [0x7B, 'take_slot_meseta', 'RR'],
    [0x7C, 'unknown', null],
    [0x7D, 'unknown', null],
    [0x7E, 'unknown', null],
    [0x7F, 'read_guildcard_flag', 'RR'],
    [0x80, 'unknownF880', 'R'],
    [0x81, 'get_pl_name?', 'R'],
    [0x82, 'unknown', null],
    [0x83, 'unknownF883', 'RR'],
    [0x84, 'unknown', null],
    [0x85, 'unknown', null],
    [0x86, 'unknown', null],
    [0x87, 'unknown', null],
    [0x88, 'ba_close_msg', ''],
    [0x89, 'unknown', null],
    [0x8A, 'get_player_status', 'RR'],
    [0x8B, 'send_mail', 'aRs'],
    [0x8C, 'online_check', 'R'],
    [0x8D, 'chl_set_timerecord?', 'R'],
    [0x8E, 'chl_get_timerecord?', 'R'],
    [0x8F, 'unknownF88F', 'R'],
    [0x90, 'unknownF890', ''],
    [0x91, 'load_enemy_data', 'aL'],
    [0x92, 'get_physical_data', 'W'],
    [0x93, 'get_attack_data', 'W'],
    [0x94, 'get_resist_data', 'W'],
    [0x95, 'get_movement_data', 'W'],
    [0x96, 'unknown', null],
    [0x97, 'unknown', null],
    [0x98, 'shift_left', 'RR'],
    [0x99, 'shift_right', 'RR'],
    [0x9A, 'get_random', 'RR'],
    [0x9B, 'reset_map', ''],
    [0x9C, 'disp_chl_retry_menu', 'R'],
    [0x9D, 'chl_reverser?', ''],
    [0x9E, 'unknownF89E', 'aL'],
    [0x9F, 'unknownF89F', 'R'],
    [0xA0, 'unknownF8A0', ''],
    [0xA1, 'unknownF8A1', ''],
    [0xA2, 'unknown', null],
    [0xA3, 'unknown', null],
    [0xA4, 'unknown', null],
    [0xA5, 'unknown', null],
    [0xA6, 'unknown', null],
    [0xA7, 'unknown', null],
    [0xA8, 'unknownF8A8', 'aL'],
    [0xA9, 'unknownF8A9', 'R'],
    [0xAA, 'unknown', null],
    [0xAB, 'unknown', null],
    [0xAC, 'unknown', null],
    [0xAD, 'get_number_of_player2', 'R'],
    [0xAE, 'unknown', null],
    [0xAF, 'unknown', null],
    [0xB0, 'unknown', null],
    [0xB1, 'unknown', null],
    [0xB2, 'unknown', null],
    [0xB3, 'unknown', null],
    [0xB4, 'unknown', null],
    [0xB5, 'unknown', null],
    [0xB6, 'unknown', null],
    [0xB7, 'unknown', null],
    [0xB8, 'unknownF8B8', ''],
    [0xB9, 'chl_recovery?', ''],
    [0xBA, 'unknown', null],
    [0xBB, 'unknown', null],
    [0xBC, 'set_episode', 'L'],
    [0xBD, 'unknown', null],
    [0xBE, 'unknown', null],
    [0xBF, 'unknown', null],
    [0xC0, 'file_dl_req', 'aLs'],
    [0xC1, 'get_dl_status', 'R'],
    [0xC2, 'gba_unknown4?', ''],
    [0xC3, 'get_gba_state?', 'R'],
    [0xC4, 'unknownF8C4', 'R'],
    [0xC5, 'unknownF8C5', 'R'],
    [0xC6, 'QEXIT', ''],
    [0xC7, 'use_animation', 'RR'],
    [0xC8, 'stop_animation', 'R'],
    [0xC9, 'run_to_coord', 'RR'],
    [0xCA, 'set_slot_invincible', 'RR'],
    [0xCB, 'unknownF8CB', 'R'],
    [0xCC, 'set_slot_poison', 'R'],
    [0xCD, 'set_slot_paralyze', 'R'],
    [0xCE, 'set_slot_shock', 'R'],
    [0xCF, 'set_slot_freeze', 'R'],
    [0xD0, 'set_slot_slow', 'R'],
    [0xD1, 'set_slot_confuse', 'R'],
    [0xD2, 'set_slot_shifta', 'R'],
    [0xD3, 'set_slot_deband', 'R'],
    [0xD4, 'set_slot_jellen', 'R'],
    [0xD5, 'set_slot_zalure', 'R'],
    [0xD6, 'fleti_fixed_camera', 'aR'],
    [0xD7, 'fleti_locked_camera', 'aLR'],
    [0xD8, 'default_camera_pos2', ''],
    [0xD9, 'set_motion_blur', ''],
    [0xDA, 'set_screen_b&w', ''],
    [0xDB, 'unknownF8DB', 'aLLLLRW'],
    [0xDC, 'NPC_action_string', 'RRW'],
    [0xDD, 'get_pad_cond', 'RR'],
    [0xDE, 'get_button_cond', 'RR'],
    [0xDF, 'freeze_enemies', ''],
    [0xE0, 'unfreeze_enemies', ''],
    [0xE1, 'freeze_everything', ''],
    [0xE2, 'unfreeze_everything', ''],
    [0xE3, 'restore_hp', 'R'],
    [0xE4, 'restore_tp', 'R'],
    [0xE5, 'close_chat_bubble', 'R'],
    [0xE6, 'unknownF8E6', 'RR'],
    [0xE7, 'unknownF8E7', 'RR'],
    [0xE8, 'unknownF8E8', 'RR'],
    [0xE9, 'unknownF8E9', 'RR'],
    [0xEA, 'unknownF8EA', 'RR'],
    [0xEB, 'unknownF8EB', 'RR'],
    [0xEC, 'unknownF8EC', 'RR'],
    [0xED, 'animation_check', 'RR'],
    [0xEE, 'call_image_data', 'aLW'],
    [0xEF, 'unknownF8EF', ''],
    [0xF0, 'turn_off_bgm_p2', ''],
    [0xF1, 'turn_on_bgm_p2', ''],
    [0xF2, 'load_unk_data', 'aLLLLRW'],
    [0xF3, 'particle2', 'aRLf'],
    [0xF4, 'unknown', null],
    [0xF5, 'unknown', null],
    [0xF6, 'unknown', null],
    [0xF7, 'unknown', null],
    [0xF8, 'unknown', null],
    [0xF9, 'unknown', null],
    [0xFA, 'unknown', null],
    [0xFB, 'unknown', null],
    [0xFC, 'unknown', null],
    [0xFD, 'unknown', null],
    [0xFE, 'unknown', null],
    [0xFF, 'unknown', null],
];

const F9opcodeList: Array<[number, string, string | null]> = [
    [0x00, 'unknown', null],
    [0x01, 'dec2float', 'RR'],
    [0x02, 'float2dec', 'RR'],
    [0x03, 'flet', 'RR'],
    [0x04, 'fleti', 'RF'],
    [0x05, 'unknown', null],
    [0x06, 'unknown', null],
    [0x07, 'unknown', null],
    [0x08, 'fadd', 'RR'],
    [0x09, 'faddi', 'RF'],
    [0x0A, 'fsub', 'RR'],
    [0x0B, 'fsubi', 'RF'],
    [0x0C, 'fmul', 'RR'],
    [0x0D, 'fmuli', 'RF'],
    [0x0E, 'fdiv', 'RR'],
    [0x0F, 'fdivi', 'RF'],
    [0x10, 'get_unknown_count?', 'aLR'],
    [0x11, 'get_stackable_item_count', 'RR'],
    [0x12, 'freeze_and_hide_equip', ''],
    [0x13, 'thaw_and_show_equip', ''],
    [0x14, 'set_paletteX_callback', 'aRW'],
    [0x15, 'activate_paletteX', 'aR'],
    [0x16, 'enable_paletteX', 'aR'],
    [0x17, 'restore_paletteX', 'aL'],
    [0x18, 'disable_paletteX', 'aL'],
    [0x19, 'get_paletteX_activated', 'aLR'],
    [0x1A, 'get_unknown_paletteX_status?', 'aLR'],
    [0x1B, 'disable_movement2', 'aR'],
    [0x1C, 'enable_movement2', 'aR'],
    [0x1D, 'get_time_played', 'R'],
    [0x1E, 'get_guildcard_total', 'R'],
    [0x1F, 'get_slot_meseta', 'R'],
    [0x20, 'get_player_level', 'aLR'],
    [0x21, 'get_Section_ID', 'aLR'],
    [0x22, 'get_player_hp', 'aRR'],
    [0x23, 'get_floor_number', 'aRR'],
    [0x24, 'get_coord_player_detect', 'RR'],
    [0x25, 'read_global_flag', 'abR'],
    [0x26, 'write_global_flag', 'abR'],
    [0x27, 'unknownF927', 'RR'],
    [0x28, 'floor_player_detect', 'R'],
    [0x29, 'read_disk_file?', 'as'],
    [0x2A, 'open_pack_select', ''],
    [0x2B, 'item_select', 'R'],
    [0x2C, 'get_item_id', 'R'],
    [0x2D, 'color_change', 'aRRRRR'],
    [0x2E, 'send_statistic?', 'aLLLLLLLL'],
    [0x2F, 'unknownF92F', 'aLL'],
    [0x30, 'chat_box', 'aLLLLLs'],
    [0x31, 'chat_bubble', 'aLs'],
    [0x32, 'unknown', null],
    [0x33, 'unknownF933', 'R'],
    [0x34, 'scroll_text', 'aLLLLLfRs'],
    [0x35, 'gba_unknown1', ''],
    [0x36, 'gba_unknown2', ''],
    [0x37, 'gba_unknown3', ''],
    [0x38, 'add_damage_to?', 'aLL'],
    [0x39, 'item_delete2', 'aL'],
    [0x3A, 'get_item_info', 'aLR'],
    [0x3B, 'item_packing1', 'aL'],
    [0x3C, 'item_packing2', 'aLL'],
    [0x3D, 'get_lang_setting?', 'aR'],
    [0x3E, 'prepare_statistic?', 'aLWW'],
    [0x3F, 'keyword_detect', ''],
    [0x40, 'Keyword', 'aRLs'],
    [0x41, 'get_guildcard_num', 'aLR'],
    [0x42, 'unknown', null],
    [0x43, 'unknown', null],
    [0x44, 'get_wrap_status', 'aLR'],
    [0x45, 'initial_floor', 'aL'],
    [0x46, 'sin', 'aRL'],
    [0x47, 'cos', 'aRL'],
    [0x48, 'unknown', null],
    [0x49, 'unknown', null],
    [0x4A, 'boss_is_dead2?', 'R'],
    [0x4B, 'unknownF94B', 'R'],
    [0x4C, 'unknownF94C', 'R'],
    [0x4D, 'is_there_cardbattle', 'R'],
    [0x4E, 'unknown', null],
    [0x4F, 'unknown', null],
    [0x50, 'BB_p2_menu', 'aL'],
    [0x51, 'BB_Map_Designate', 'BWBB'],
    [0x52, 'BB_get_number_in_pack', 'R'],
    [0x53, 'BB_swap_item', 'aLLLLLLWW'],
    [0x54, 'BB_check_wrap', 'aRR'],
    [0x55, 'BB_exchange_PD_item', 'aRRRWW'],
    [0x56, 'BB_exchange_PD_srank', 'aRRRRRWW'],
    [0x57, 'BB_exchange_PD_special', 'aRRRRRLWW'],
    [0x58, 'BB_exchange_PD_percent', 'aRRRRRLWW'],
    [0x59, 'unknownF959', 'aL'],
    [0x5A, 'unknown', null],
    [0x5B, 'unknown', null],
    [0x5C, 'BB_exchange_SLT', 'aLRWW'],
    [0x5D, 'BB_exchange_PC', ''],
    [0x5E, 'BB_box_create_BP', 'aLff'],
    [0x5F, 'BB_exchange_PT', 'aRRLWW'],
    [0x60, 'unknownF960', 'aL'],
    [0x61, 'unknownF961', ''],
    [0x62, 'unknown', null],
    [0x63, 'unknown', null],
    [0x64, 'unknown', null],
    [0x65, 'unknown', null],
    [0x66, 'unknown', null],
    [0x67, 'unknown', null],
    [0x68, 'unknown', null],
    [0x69, 'unknown', null],
    [0x6A, 'unknown', null],
    [0x6B, 'unknown', null],
    [0x6C, 'unknown', null],
    [0x6D, 'unknown', null],
    [0x6E, 'unknown', null],
    [0x6F, 'unknown', null],
    [0x70, 'unknown', null],
    [0x71, 'unknown', null],
    [0x72, 'unknown', null],
    [0x73, 'unknown', null],
    [0x74, 'unknown', null],
    [0x75, 'unknown', null],
    [0x76, 'unknown', null],
    [0x77, 'unknown', null],
    [0x78, 'unknown', null],
    [0x79, 'unknown', null],
    [0x7A, 'unknown', null],
    [0x7B, 'unknown', null],
    [0x7C, 'unknown', null],
    [0x7D, 'unknown', null],
    [0x7E, 'unknown', null],
    [0x7F, 'unknown', null],
    [0x80, 'unknown', null],
    [0x81, 'unknown', null],
    [0x82, 'unknown', null],
    [0x83, 'unknown', null],
    [0x84, 'unknown', null],
    [0x85, 'unknown', null],
    [0x86, 'unknown', null],
    [0x87, 'unknown', null],
    [0x88, 'unknown', null],
    [0x89, 'unknown', null],
    [0x8A, 'unknown', null],
    [0x8B, 'unknown', null],
    [0x8C, 'unknown', null],
    [0x8D, 'unknown', null],
    [0x8E, 'unknown', null],
    [0x8F, 'unknown', null],
    [0x90, 'unknown', null],
    [0x91, 'unknown', null],
    [0x92, 'unknown', null],
    [0x93, 'unknown', null],
    [0x94, 'unknown', null],
    [0x95, 'unknown', null],
    [0x96, 'unknown', null],
    [0x97, 'unknown', null],
    [0x98, 'unknown', null],
    [0x99, 'unknown', null],
    [0x9A, 'unknown', null],
    [0x9B, 'unknown', null],
    [0x9C, 'unknown', null],
    [0x9D, 'unknown', null],
    [0x9E, 'unknown', null],
    [0x9F, 'unknown', null],
    [0xA0, 'unknown', null],
    [0xA1, 'unknown', null],
    [0xA2, 'unknown', null],
    [0xA3, 'unknown', null],
    [0xA4, 'unknown', null],
    [0xA5, 'unknown', null],
    [0xA6, 'unknown', null],
    [0xA7, 'unknown', null],
    [0xA8, 'unknown', null],
    [0xA9, 'unknown', null],
    [0xAA, 'unknown', null],
    [0xAB, 'unknown', null],
    [0xAC, 'unknown', null],
    [0xAD, 'unknown', null],
    [0xAE, 'unknown', null],
    [0xAF, 'unknown', null],
    [0xB0, 'unknown', null],
    [0xB1, 'unknown', null],
    [0xB2, 'unknown', null],
    [0xB3, 'unknown', null],
    [0xB4, 'unknown', null],
    [0xB5, 'unknown', null],
    [0xB6, 'unknown', null],
    [0xB7, 'unknown', null],
    [0xB8, 'unknown', null],
    [0xB9, 'unknown', null],
    [0xBA, 'unknown', null],
    [0xBB, 'unknown', null],
    [0xBC, 'unknown', null],
    [0xBD, 'unknown', null],
    [0xBE, 'unknown', null],
    [0xBF, 'unknown', null],
    [0xC0, 'unknown', null],
    [0xC1, 'unknown', null],
    [0xC2, 'unknown', null],
    [0xC3, 'unknown', null],
    [0xC4, 'unknown', null],
    [0xC5, 'unknown', null],
    [0xC6, 'unknown', null],
    [0xC7, 'unknown', null],
    [0xC8, 'unknown', null],
    [0xC9, 'unknown', null],
    [0xCA, 'unknown', null],
    [0xCB, 'unknown', null],
    [0xCC, 'unknown', null],
    [0xCD, 'unknown', null],
    [0xCE, 'unknown', null],
    [0xCF, 'unknown', null],
    [0xD0, 'unknown', null],
    [0xD1, 'unknown', null],
    [0xD2, 'unknown', null],
    [0xD3, 'unknown', null],
    [0xD4, 'unknown', null],
    [0xD5, 'unknown', null],
    [0xD6, 'unknown', null],
    [0xD7, 'unknown', null],
    [0xD8, 'unknown', null],
    [0xD9, 'unknown', null],
    [0xDA, 'unknown', null],
    [0xDB, 'unknown', null],
    [0xDC, 'unknown', null],
    [0xDD, 'unknown', null],
    [0xDE, 'unknown', null],
    [0xDF, 'unknown', null],
    [0xE0, 'unknown', null],
    [0xE1, 'unknown', null],
    [0xE2, 'unknown', null],
    [0xE3, 'unknown', null],
    [0xE4, 'unknown', null],
    [0xE5, 'unknown', null],
    [0xE6, 'unknown', null],
    [0xE7, 'unknown', null],
    [0xE8, 'unknown', null],
    [0xE9, 'unknown', null],
    [0xEA, 'unknown', null],
    [0xEB, 'unknown', null],
    [0xEC, 'unknown', null],
    [0xED, 'unknown', null],
    [0xEE, 'unknown', null],
    [0xEF, 'unknown', null],
    [0xF0, 'unknown', null],
    [0xF1, 'unknown', null],
    [0xF2, 'unknown', null],
    [0xF3, 'unknown', null],
    [0xF4, 'unknown', null],
    [0xF5, 'unknown', null],
    [0xF6, 'unknown', null],
    [0xF7, 'unknown', null],
    [0xF8, 'unknown', null],
    [0xF9, 'unknown', null],
    [0xFA, 'unknown', null],
    [0xFB, 'unknown', null],
    [0xFC, 'unknown', null],
    [0xFD, 'unknown', null],
    [0xFE, 'unknown', null],
    [0xFF, 'unknown', null],
];
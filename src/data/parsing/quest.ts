import { ArrayBufferCursor } from '../ArrayBufferCursor';
import * as prs from '../compression/prs';
import { parse_dat, write_dat } from './dat';
import { parse_bin, write_bin, Instruction } from './bin';
import { parse_qst, write_qst } from './qst';
import {
    Vec3,
    AreaVariant,
    QuestNpc,
    QuestObject,
    Quest,
    ObjectType,
    NpcType
} from '../../domain';
import { area_store } from '../../store';

/**
 * High level parsing function that delegates to lower level parsing functions.
 * 
 * Always delegates to parse_qst at the moment.
 */
export function parse_quest(cursor: ArrayBufferCursor): Quest | null {
    const qst = parse_qst(cursor);

    if (!qst) {
        return null;
    }

    let dat_file = null;
    let bin_file = null;

    for (const file of qst.files) {
        if (file.name.endsWith('.dat')) {
            dat_file = file;
        } else if (file.name.endsWith('.bin')) {
            bin_file = file;
        }
    }

    // TODO: deal with missing/multiple DAT or BIN file.

    if (!dat_file || !bin_file) {
        return null;
    }

    const dat = parse_dat(prs.decompress(dat_file.data));
    const bin = parse_bin(prs.decompress(bin_file.data));
    let episode = 1;
    let area_variants: AreaVariant[] = [];

    if (bin.function_offsets.length) {
        const func_0_ops = get_func_operations(bin.instructions, bin.function_offsets[0]);

        if (func_0_ops) {
            episode = get_episode(func_0_ops);
            area_variants = get_area_variants(episode, func_0_ops);
        } else {
            console.warn(`Function 0 offset ${bin.function_offsets[0]} is invalid.`);
        }
    } else {
        console.warn('File contains no functions.');
    }

    return new Quest(
        bin.quest_name,
        bin.short_description,
        bin.long_description,
        dat_file.quest_no,
        episode,
        area_variants,
        parse_obj_data(dat.objs),
        parse_npc_data(episode, dat.npcs),
        { unknowns: dat.unknowns },
        bin.data
    );
}

export function write_quest_qst(quest: Quest, file_name: string): ArrayBufferCursor {
    const dat = write_dat({
        objs: objects_to_dat_data(quest.episode, quest.objects),
        npcs: npcs_to_dat_data(quest.episode, quest.npcs),
        unknowns: quest.dat.unknowns
    });
    const bin = write_bin({ data: quest.bin });
    const ext_start = file_name.lastIndexOf('.');
    const base_file_name = ext_start === -1 ? file_name : file_name.slice(0, ext_start);

    return write_qst({
        files: [
            {
                name: base_file_name + '.dat',
                quest_no: quest.quest_no,
                data: prs.compress(dat)
            },
            {
                name: base_file_name + '.bin',
                quest_no: quest.quest_no,
                data: prs.compress(bin)
            }
        ]
    });
}

/**
 * Defaults to episode I.
 */
function get_episode(func_0_ops: Instruction[]): number {
    const set_episode = func_0_ops.find(op => op.mnemonic === 'set_episode');

    if (set_episode) {
        switch (set_episode.args[0]) {
            default:
            case 0: return 1;
            case 1: return 2;
            case 2: return 4;
        }
    } else {
        console.warn('Function 0 has no set_episode instruction.');
        return 1;
    }
}

function get_area_variants(episode: number, func_0_ops: Instruction[]): AreaVariant[] {
    const area_variants = new Map();
    const bb_maps = func_0_ops.filter(op => op.mnemonic === 'BB_Map_Designate');

    for (const bb_map of bb_maps) {
        const area_id = bb_map.args[0];
        const variant_id = bb_map.args[2];
        area_variants.set(area_id, variant_id);
    }

    // Sort by area order and then variant id.
    return (
        Array.from(area_variants)
            .map(([area_id, variant_id]) =>
                area_store.get_variant(episode, area_id, variant_id))
            .sort((a, b) => a.area.order - b.area.order || a.id - b.id)
    );
}

function get_func_operations(operations: Instruction[], func_offset: number) {
    let position = 0;
    let func_found = false;
    const func_ops = [];

    for (const operation of operations) {
        if (position === func_offset) {
            func_found = true;
        }

        if (func_found) {
            func_ops.push(operation);

            // Break when ret is encountered.
            if (operation.opcode === 1) {
                break;
            }
        }

        position += operation.size;
    }

    return func_found ? func_ops : null;
}

function parse_obj_data(objs: any[]): QuestObject[] {
    return objs.map(obj_data => {
        const { x, y, z } = obj_data.position;
        const rot = obj_data.rotation;
        return new QuestObject(
            obj_data.area_id,
            obj_data.section_id,
            new Vec3(x, y, z),
            new Vec3(rot.x, rot.y, rot.z),
            ObjectType.from_pso_id(obj_data.type_id),
            obj_data
        );
    });
}

function parse_npc_data(episode: number, npcs: any[]): QuestNpc[] {
    return npcs.map(npc_data => {
        const { x, y, z } = npc_data.position;
        const rot = npc_data.rotation;
        return new QuestNpc(
            npc_data.area_id,
            npc_data.section_id,
            new Vec3(x, y, z),
            new Vec3(rot.x, rot.y, rot.z),
            get_npc_type(episode, npc_data),
            npc_data
        );
    });
}

function get_npc_type(episode: number, { type_id, unknown, skin, area_id }: any): NpcType {
    const regular = (unknown[2][18] & 0x80) === 0;

    switch (`${type_id}, ${skin % 3}, ${episode}`) {
        case `${0x044}, 0, 1`: return NpcType.Booma;
        case `${0x044}, 1, 1`: return NpcType.Gobooma;
        case `${0x044}, 2, 1`: return NpcType.Gigobooma;

        case `${0x063}, 0, 1`: return NpcType.EvilShark;
        case `${0x063}, 1, 1`: return NpcType.PalShark;
        case `${0x063}, 2, 1`: return NpcType.GuilShark;

        case `${0x0A6}, 0, 1`: return NpcType.Dimenian;
        case `${0x0A6}, 0, 2`: return NpcType.Dimenian2;
        case `${0x0A6}, 1, 1`: return NpcType.LaDimenian;
        case `${0x0A6}, 1, 2`: return NpcType.LaDimenian2;
        case `${0x0A6}, 2, 1`: return NpcType.SoDimenian;
        case `${0x0A6}, 2, 2`: return NpcType.SoDimenian2;

        case `${0x0D6}, 0, 2`: return NpcType.Mericarol;
        case `${0x0D6}, 1, 2`: return NpcType.Mericus;
        case `${0x0D6}, 2, 2`: return NpcType.Merikle;

        case `${0x115}, 0, 4`: return NpcType.Boota;
        case `${0x115}, 1, 4`: return NpcType.ZeBoota;
        case `${0x115}, 2, 4`: return NpcType.BaBoota;
        case `${0x117}, 0, 4`: return NpcType.Goran;
        case `${0x117}, 1, 4`: return NpcType.PyroGoran;
        case `${0x117}, 2, 4`: return NpcType.GoranDetonator;
    }

    switch (`${type_id}, ${skin % 2}, ${episode}`) {
        case `${0x040}, 0, 1`: return NpcType.Hildebear;
        case `${0x040}, 0, 2`: return NpcType.Hildebear2;
        case `${0x040}, 1, 1`: return NpcType.Hildeblue;
        case `${0x040}, 1, 2`: return NpcType.Hildeblue2;
        case `${0x041}, 0, 1`: return NpcType.RagRappy;
        case `${0x041}, 0, 2`: return NpcType.RagRappy2;
        case `${0x041}, 0, 4`: return NpcType.SandRappy;
        case `${0x041}, 1, 1`: return NpcType.AlRappy;
        case `${0x041}, 1, 2`: return NpcType.LoveRappy;
        case `${0x041}, 1, 4`: return NpcType.DelRappy;

        case `${0x061}, 0, 1`: return area_id > 15 ? NpcType.DelLily : NpcType.PoisonLily;
        case `${0x061}, 0, 2`: return area_id > 15 ? NpcType.DelLily : NpcType.PoisonLily2;
        case `${0x061}, 1, 1`: return area_id > 15 ? NpcType.DelLily : NpcType.NarLily;
        case `${0x061}, 1, 2`: return area_id > 15 ? NpcType.DelLily : NpcType.NarLily2;

        case `${0x080}, 0, 1`: return NpcType.Dubchic;
        case `${0x080}, 0, 2`: return NpcType.Dubchic2;
        case `${0x080}, 1, 1`: return NpcType.Gilchic;
        case `${0x080}, 1, 2`: return NpcType.Gilchic2;

        case `${0x0D4}, 0, 2`: return NpcType.SinowBerill;
        case `${0x0D4}, 1, 2`: return NpcType.SinowSpigell;
        case `${0x0D5}, 0, 2`: return NpcType.Merillia;
        case `${0x0D5}, 1, 2`: return NpcType.Meriltas;
        case `${0x0D7}, 0, 2`: return NpcType.UlGibbon;
        case `${0x0D7}, 1, 2`: return NpcType.ZolGibbon;

        case `${0x0DD}, 0, 2`: return NpcType.Dolmolm;
        case `${0x0DD}, 1, 2`: return NpcType.Dolmdarl;
        case `${0x0E0}, 0, 2`: return area_id > 15 ? NpcType.Epsilon : NpcType.SinowZoa;
        case `${0x0E0}, 1, 2`: return area_id > 15 ? NpcType.Epsilon : NpcType.SinowZele;

        case `${0x112}, 0, 4`: return NpcType.MerissaA;
        case `${0x112}, 1, 4`: return NpcType.MerissaAA;
        case `${0x114}, 0, 4`: return NpcType.Zu;
        case `${0x114}, 1, 4`: return NpcType.Pazuzu;
        case `${0x116}, 0, 4`: return NpcType.Dorphon;
        case `${0x116}, 1, 4`: return NpcType.DorphonEclair;
        case `${0x119}, 0, 4`: return regular ? NpcType.SaintMillion : NpcType.Kondrieu;
        case `${0x119}, 1, 4`: return regular ? NpcType.Shambertin : NpcType.Kondrieu;
    }

    switch (`${type_id}, ${episode}`) {
        case `${0x042}, 1`: return NpcType.Monest;
        case `${0x042}, 2`: return NpcType.Monest2;
        case `${0x043}, 1`: return regular ? NpcType.SavageWolf : NpcType.BarbarousWolf;
        case `${0x043}, 2`: return regular ? NpcType.SavageWolf2 : NpcType.BarbarousWolf2;

        case `${0x060}, 1`: return NpcType.GrassAssassin;
        case `${0x060}, 2`: return NpcType.GrassAssassin2;
        case `${0x062}, 1`: return NpcType.NanoDragon;
        case `${0x064}, 1`: return regular ? NpcType.PofuillySlime : NpcType.PouillySlime;
        case `${0x065}, 1`: return NpcType.PanArms;
        case `${0x065}, 2`: return NpcType.PanArms2;

        case `${0x081}, 1`: return NpcType.Garanz;
        case `${0x081}, 2`: return NpcType.Garanz2;
        case `${0x082}, 1`: return regular ? NpcType.SinowBeat : NpcType.SinowGold;
        case `${0x083}, 1`: return NpcType.Canadine;
        case `${0x084}, 1`: return NpcType.Canane;
        case `${0x085}, 1`: return NpcType.Dubswitch;
        case `${0x085}, 2`: return NpcType.Dubswitch2;

        case `${0x0A0}, 1`: return NpcType.Delsaber;
        case `${0x0A0}, 2`: return NpcType.Delsaber2;
        case `${0x0A1}, 1`: return NpcType.ChaosSorcerer;
        case `${0x0A1}, 2`: return NpcType.ChaosSorcerer2;
        case `${0x0A2}, 1`: return NpcType.DarkGunner;
        case `${0x0A4}, 1`: return NpcType.ChaosBringer;
        case `${0x0A5}, 1`: return NpcType.DarkBelra;
        case `${0x0A5}, 2`: return NpcType.DarkBelra2;
        case `${0x0A7}, 1`: return NpcType.Bulclaw;
        case `${0x0A8}, 1`: return NpcType.Claw;

        case `${0x0C0}, 1`: return NpcType.Dragon;
        case `${0x0C0}, 2`: return NpcType.GalGryphon;
        case `${0x0C1}, 1`: return NpcType.DeRolLe;
        // TODO:
        // case `${0x0C2}, 1`: return NpcType.VolOptPart1;
        case `${0x0C5}, 1`: return NpcType.VolOpt;
        case `${0x0C8}, 1`: return NpcType.DarkFalz;
        case `${0x0CA}, 2`: return NpcType.OlgaFlow;
        case `${0x0CB}, 2`: return NpcType.BarbaRay;
        case `${0x0CC}, 2`: return NpcType.GolDragon;

        case `${0x0D8}, 2`: return NpcType.Gibbles;
        case `${0x0D9}, 2`: return NpcType.Gee;
        case `${0x0DA}, 2`: return NpcType.GiGue;

        case `${0x0DB}, 2`: return NpcType.Deldepth;
        case `${0x0DC}, 2`: return NpcType.Delbiter;
        case `${0x0DE}, 2`: return NpcType.Morfos;
        case `${0x0DF}, 2`: return NpcType.Recobox;
        case `${0x0E1}, 2`: return NpcType.IllGill;

        case `${0x110}, 4`: return NpcType.Astark;
        case `${0x111}, 4`: return regular ? NpcType.SatelliteLizard : NpcType.Yowie;
        case `${0x113}, 4`: return NpcType.Girtablulu;
    }

    switch (type_id) {
        case 0x004: return NpcType.FemaleFat;
        case 0x005: return NpcType.FemaleMacho;
        case 0x007: return NpcType.FemaleTall;
        case 0x00A: return NpcType.MaleDwarf;
        case 0x00B: return NpcType.MaleFat;
        case 0x00C: return NpcType.MaleMacho;
        case 0x00D: return NpcType.MaleOld;
        case 0x019: return NpcType.BlueSoldier;
        case 0x01A: return NpcType.RedSoldier;
        case 0x01B: return NpcType.Principal;
        case 0x01C: return NpcType.Tekker;
        case 0x01D: return NpcType.GuildLady;
        case 0x01E: return NpcType.Scientist;
        case 0x01F: return NpcType.Nurse;
        case 0x020: return NpcType.Irene;
        case 0x0F1: return NpcType.ItemShop;
        case 0x0FE: return NpcType.Nurse2;
    }

    // TODO: remove log statement:
    console.log(`Unknown type ID: ${type_id} (0x${type_id.toString(16)}).`);
    return NpcType.Unknown;
}

function objects_to_dat_data(episode: number, objects: QuestObject[]): any[] {
    return objects.map(object => ({
        type_id: object.type.pso_id,
        section_id: object.section_id,
        position: object.section_position,
        rotation: object.rotation,
        area_id: object.area_id,
        unknown: object.dat.unknown
    }));
}

function npcs_to_dat_data(episode: number, npcs: QuestNpc[]): any[] {
    return npcs.map(npc => {
        // If the type is unknown, type_data will be null and we use the raw data from the DAT file.
        const type_data = npc_type_to_dat_data(npc.type);

        if (type_data) {
            npc.dat.unknown[2][18] = (npc.dat.unknown[2][18] & ~0x80) | (type_data.regular ? 0 : 0x80);
        }

        return {
            type_id: type_data ? type_data.type_id : npc.dat.type_id,
            section_id: npc.section_id,
            position: npc.section_position,
            rotation: npc.rotation,
            skin: type_data ? type_data.skin : npc.dat.skin,
            area_id: npc.area_id,
            unknown: npc.dat.unknown
        };
    });
}

function npc_type_to_dat_data(
    type: NpcType
): { type_id: number, skin: number, regular: boolean } | null {
    switch (type) {
        default: throw new Error(`Unexpected type ${type.code}.`);

        case NpcType.Unknown: return null;

        case NpcType.FemaleFat: return { type_id: 0x004, skin: 0, regular: true };
        case NpcType.FemaleMacho: return { type_id: 0x005, skin: 0, regular: true };
        case NpcType.FemaleTall: return { type_id: 0x007, skin: 0, regular: true };
        case NpcType.MaleDwarf: return { type_id: 0x00A, skin: 0, regular: true };
        case NpcType.MaleFat: return { type_id: 0x00B, skin: 0, regular: true };
        case NpcType.MaleMacho: return { type_id: 0x00C, skin: 0, regular: true };
        case NpcType.MaleOld: return { type_id: 0x00D, skin: 0, regular: true };
        case NpcType.BlueSoldier: return { type_id: 0x019, skin: 0, regular: true };
        case NpcType.RedSoldier: return { type_id: 0x01A, skin: 0, regular: true };
        case NpcType.Principal: return { type_id: 0x01B, skin: 0, regular: true };
        case NpcType.Tekker: return { type_id: 0x01C, skin: 0, regular: true };
        case NpcType.GuildLady: return { type_id: 0x01D, skin: 0, regular: true };
        case NpcType.Scientist: return { type_id: 0x01E, skin: 0, regular: true };
        case NpcType.Nurse: return { type_id: 0x01F, skin: 0, regular: true };
        case NpcType.Irene: return { type_id: 0x020, skin: 0, regular: true };
        case NpcType.ItemShop: return { type_id: 0x0F1, skin: 0, regular: true };
        case NpcType.Nurse2: return { type_id: 0x0FE, skin: 0, regular: true };

        case NpcType.Hildebear: return { type_id: 0x040, skin: 0, regular: true };
        case NpcType.Hildeblue: return { type_id: 0x040, skin: 1, regular: true };
        case NpcType.RagRappy: return { type_id: 0x041, skin: 0, regular: true };
        case NpcType.AlRappy: return { type_id: 0x041, skin: 1, regular: true };
        case NpcType.Monest: return { type_id: 0x042, skin: 0, regular: true };
        case NpcType.SavageWolf: return { type_id: 0x043, skin: 0, regular: true };
        case NpcType.BarbarousWolf: return { type_id: 0x043, skin: 0, regular: false };
        case NpcType.Booma: return { type_id: 0x044, skin: 0, regular: true };
        case NpcType.Gobooma: return { type_id: 0x044, skin: 1, regular: true };
        case NpcType.Gigobooma: return { type_id: 0x044, skin: 2, regular: true };
        case NpcType.Dragon: return { type_id: 0x0C0, skin: 0, regular: true };

        case NpcType.GrassAssassin: return { type_id: 0x060, skin: 0, regular: true };
        case NpcType.PoisonLily: return { type_id: 0x061, skin: 0, regular: true };
        case NpcType.NarLily: return { type_id: 0x061, skin: 1, regular: true };
        case NpcType.NanoDragon: return { type_id: 0x062, skin: 0, regular: true };
        case NpcType.EvilShark: return { type_id: 0x063, skin: 0, regular: true };
        case NpcType.PalShark: return { type_id: 0x063, skin: 1, regular: true };
        case NpcType.GuilShark: return { type_id: 0x063, skin: 2, regular: true };
        case NpcType.PofuillySlime: return { type_id: 0x064, skin: 0, regular: true };
        case NpcType.PouillySlime: return { type_id: 0x064, skin: 0, regular: false };
        case NpcType.PanArms: return { type_id: 0x065, skin: 0, regular: true };
        case NpcType.DeRolLe: return { type_id: 0x0C1, skin: 0, regular: true };

        case NpcType.Dubchic: return { type_id: 0x080, skin: 0, regular: true };
        case NpcType.Gilchic: return { type_id: 0x080, skin: 1, regular: true };
        case NpcType.Garanz: return { type_id: 0x081, skin: 0, regular: true };
        case NpcType.SinowBeat: return { type_id: 0x082, skin: 0, regular: true };
        case NpcType.SinowGold: return { type_id: 0x082, skin: 0, regular: false };
        case NpcType.Canadine: return { type_id: 0x083, skin: 0, regular: true };
        case NpcType.Canane: return { type_id: 0x084, skin: 0, regular: true };
        case NpcType.Dubswitch: return { type_id: 0x085, skin: 0, regular: true };
        case NpcType.VolOpt: return { type_id: 0x0C5, skin: 0, regular: true };

        case NpcType.Delsaber: return { type_id: 0x0A0, skin: 0, regular: true };
        case NpcType.ChaosSorcerer: return { type_id: 0x0A1, skin: 0, regular: true };
        case NpcType.DarkGunner: return { type_id: 0x0A2, skin: 0, regular: true };
        case NpcType.ChaosBringer: return { type_id: 0x0A4, skin: 0, regular: true };
        case NpcType.DarkBelra: return { type_id: 0x0A5, skin: 0, regular: true };
        case NpcType.Dimenian: return { type_id: 0x0A6, skin: 0, regular: true };
        case NpcType.LaDimenian: return { type_id: 0x0A6, skin: 1, regular: true };
        case NpcType.SoDimenian: return { type_id: 0x0A6, skin: 2, regular: true };
        case NpcType.Bulclaw: return { type_id: 0x0A7, skin: 0, regular: true };
        case NpcType.Claw: return { type_id: 0x0A8, skin: 0, regular: true };
        case NpcType.DarkFalz: return { type_id: 0x0C8, skin: 0, regular: true };

        case NpcType.Hildebear2: return { type_id: 0x040, skin: 0, regular: true };
        case NpcType.Hildeblue2: return { type_id: 0x040, skin: 1, regular: true };
        case NpcType.RagRappy2: return { type_id: 0x041, skin: 0, regular: true };
        case NpcType.LoveRappy: return { type_id: 0x041, skin: 1, regular: true };
        case NpcType.Monest2: return { type_id: 0x042, skin: 0, regular: true };
        case NpcType.PoisonLily2: return { type_id: 0x061, skin: 0, regular: true };
        case NpcType.NarLily2: return { type_id: 0x061, skin: 1, regular: true };
        case NpcType.GrassAssassin2: return { type_id: 0x060, skin: 0, regular: true };
        case NpcType.Dimenian2: return { type_id: 0x0A6, skin: 0, regular: true };
        case NpcType.LaDimenian2: return { type_id: 0x0A6, skin: 1, regular: true };
        case NpcType.SoDimenian2: return { type_id: 0x0A6, skin: 2, regular: true };
        case NpcType.DarkBelra2: return { type_id: 0x0A5, skin: 0, regular: true };
        case NpcType.BarbaRay: return { type_id: 0x0CB, skin: 0, regular: true };

        case NpcType.SavageWolf2: return { type_id: 0x043, skin: 0, regular: true };
        case NpcType.BarbarousWolf2: return { type_id: 0x043, skin: 0, regular: false };
        case NpcType.PanArms2: return { type_id: 0x065, skin: 0, regular: true };
        case NpcType.Dubchic2: return { type_id: 0x080, skin: 0, regular: true };
        case NpcType.Gilchic2: return { type_id: 0x080, skin: 1, regular: true };
        case NpcType.Garanz2: return { type_id: 0x081, skin: 0, regular: true };
        case NpcType.Dubswitch2: return { type_id: 0x085, skin: 0, regular: true };
        case NpcType.Delsaber2: return { type_id: 0x0A0, skin: 0, regular: true };
        case NpcType.ChaosSorcerer2: return { type_id: 0x0A1, skin: 0, regular: true };
        case NpcType.GolDragon: return { type_id: 0x0CC, skin: 0, regular: true };

        case NpcType.SinowBerill: return { type_id: 0x0D4, skin: 0, regular: true };
        case NpcType.SinowSpigell: return { type_id: 0x0D4, skin: 1, regular: true };
        case NpcType.Merillia: return { type_id: 0x0D5, skin: 0, regular: true };
        case NpcType.Meriltas: return { type_id: 0x0D5, skin: 1, regular: true };
        case NpcType.Mericarol: return { type_id: 0x0D6, skin: 0, regular: true };
        case NpcType.Mericus: return { type_id: 0x0D6, skin: 1, regular: true };
        case NpcType.Merikle: return { type_id: 0x0D6, skin: 2, regular: true };
        case NpcType.UlGibbon: return { type_id: 0x0D7, skin: 0, regular: true };
        case NpcType.ZolGibbon: return { type_id: 0x0D7, skin: 1, regular: true };
        case NpcType.Gibbles: return { type_id: 0x0D8, skin: 0, regular: true };
        case NpcType.Gee: return { type_id: 0x0D9, skin: 0, regular: true };
        case NpcType.GiGue: return { type_id: 0x0DA, skin: 0, regular: true };
        case NpcType.GalGryphon: return { type_id: 0x0C0, skin: 0, regular: true };

        case NpcType.Deldepth: return { type_id: 0x0DB, skin: 0, regular: true };
        case NpcType.Delbiter: return { type_id: 0x0DC, skin: 0, regular: true };
        case NpcType.Dolmolm: return { type_id: 0x0DD, skin: 0, regular: true };
        case NpcType.Dolmdarl: return { type_id: 0x0DD, skin: 1, regular: true };
        case NpcType.Morfos: return { type_id: 0x0DE, skin: 0, regular: true };
        case NpcType.Recobox: return { type_id: 0x0DF, skin: 0, regular: true };
        case NpcType.Epsilon: return { type_id: 0x0E0, skin: 0, regular: true };
        case NpcType.SinowZoa: return { type_id: 0x0E0, skin: 0, regular: true };
        case NpcType.SinowZele: return { type_id: 0x0E0, skin: 1, regular: true };
        case NpcType.IllGill: return { type_id: 0x0E1, skin: 0, regular: true };
        case NpcType.DelLily: return { type_id: 0x061, skin: 0, regular: true };
        case NpcType.OlgaFlow: return { type_id: 0x0CA, skin: 0, regular: true };

        case NpcType.SandRappy: return { type_id: 0x041, skin: 0, regular: true };
        case NpcType.DelRappy: return { type_id: 0x041, skin: 1, regular: true };
        case NpcType.Astark: return { type_id: 0x110, skin: 0, regular: true };
        case NpcType.SatelliteLizard: return { type_id: 0x111, skin: 0, regular: true };
        case NpcType.Yowie: return { type_id: 0x111, skin: 0, regular: false };
        case NpcType.MerissaA: return { type_id: 0x112, skin: 0, regular: true };
        case NpcType.MerissaAA: return { type_id: 0x112, skin: 1, regular: true };
        case NpcType.Girtablulu: return { type_id: 0x113, skin: 0, regular: true };
        case NpcType.Zu: return { type_id: 0x114, skin: 0, regular: true };
        case NpcType.Pazuzu: return { type_id: 0x114, skin: 1, regular: true };
        case NpcType.Boota: return { type_id: 0x115, skin: 0, regular: true };
        case NpcType.ZeBoota: return { type_id: 0x115, skin: 1, regular: true };
        case NpcType.BaBoota: return { type_id: 0x115, skin: 2, regular: true };
        case NpcType.Dorphon: return { type_id: 0x116, skin: 0, regular: true };
        case NpcType.DorphonEclair: return { type_id: 0x116, skin: 1, regular: true };
        case NpcType.Goran: return { type_id: 0x117, skin: 0, regular: true };
        case NpcType.PyroGoran: return { type_id: 0x117, skin: 1, regular: true };
        case NpcType.GoranDetonator: return { type_id: 0x117, skin: 2, regular: true };
        case NpcType.SaintMillion: return { type_id: 0x119, skin: 0, regular: true };
        case NpcType.Shambertin: return { type_id: 0x119, skin: 1, regular: true };
        case NpcType.Kondrieu: return { type_id: 0x119, skin: 0, regular: false };
    }
}

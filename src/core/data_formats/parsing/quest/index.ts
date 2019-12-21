import Logger from "js-logger";
import {
    Instruction,
    InstructionSegment,
    Segment,
    SegmentType,
} from "../../../../quest_editor/scripting/instructions";
import { OP_BB_MAP_DESIGNATE, OP_SET_EPISODE } from "../../../../quest_editor/scripting/opcodes";
import { prs_compress } from "../../compression/prs/compress";
import { prs_decompress } from "../../compression/prs/decompress";
import { ArrayBufferCursor } from "../../cursor/ArrayBufferCursor";
import { Cursor } from "../../cursor/Cursor";
import { ResizableBufferCursor } from "../../cursor/ResizableBufferCursor";
import { Endianness } from "../../Endianness";
import { parse_bin, write_bin } from "./bin";
import { DatFile, DatNpc, DatObject, DatUnknown, parse_dat, write_dat } from "./dat";
import { QuestNpc, QuestObject, QuestEvent } from "./entities";
import { Episode } from "./Episode";
import { object_data, ObjectType, pso_id_to_object_type } from "./object_types";
import { parse_qst, QstContainedFile, write_qst } from "./qst";
import { npc_data, NpcType } from "./npc_types";
import { reinterpret_f32_as_i32, reinterpret_i32_as_f32 } from "../../../primitive_conversion";

const logger = Logger.get("core/data_formats/parsing/quest");

export type Quest = {
    readonly id: number;
    readonly language: number;
    readonly name: string;
    readonly short_description: string;
    readonly long_description: string;
    readonly episode: Episode;
    readonly objects: readonly QuestObject[];
    readonly npcs: readonly QuestNpc[];
    readonly events: readonly QuestEvent[];
    /**
     * (Partial) raw DAT data that can't be parsed yet by Phantasmal.
     */
    readonly dat_unknowns: readonly DatUnknown[];
    readonly object_code: readonly Segment[];
    readonly shop_items: readonly number[];
    readonly map_designations: Map<number, number>;
};

/**
 * High level parsing function that delegates to lower level parsing functions.
 *
 * Always delegates to parse_qst at the moment.
 */
export function parse_quest(cursor: Cursor, lenient: boolean = false): Quest | undefined {
    // Extract contained .dat and .bin files.
    const qst = parse_qst(cursor);

    if (!qst) {
        return;
    }

    let dat_file: QstContainedFile | undefined;
    let bin_file: QstContainedFile | undefined;

    for (const file of qst.files) {
        const file_name = file.name.trim().toLowerCase();

        if (file_name.endsWith(".dat")) {
            dat_file = file;
        } else if (file_name.endsWith(".bin")) {
            bin_file = file;
        }
    }

    if (!dat_file) {
        logger.error("File contains no DAT file.");
        return;
    }

    if (!bin_file) {
        logger.error("File contains no BIN file.");
        return;
    }

    // Decompress and parse contained files.
    const dat_decompressed = prs_decompress(
        new ArrayBufferCursor(dat_file.data, Endianness.Little),
    );
    const dat = parse_dat(dat_decompressed);
    const objects = parse_obj_data(dat.objs);

    const bin_decompressed = prs_decompress(
        new ArrayBufferCursor(bin_file.data, Endianness.Little),
    );
    const bin = parse_bin(
        bin_decompressed,
        extract_script_entry_points(objects, dat.npcs),
        lenient,
    );

    // Extract episode and map designations from object code.
    let episode = Episode.I;
    let map_designations: Map<number, number> = new Map();

    if (bin.object_code.length) {
        let label_0_segment: InstructionSegment | undefined;

        for (const segment of bin.object_code) {
            if (segment.type === SegmentType.Instructions && segment.labels.includes(0)) {
                label_0_segment = segment;
                break;
            }
        }

        if (label_0_segment) {
            episode = get_episode(label_0_segment.instructions);
            map_designations = extract_map_designations(dat, episode, label_0_segment.instructions);
        } else {
            logger.warn(`No instruction for label 0 found.`);
        }
    } else {
        logger.warn("File contains no instruction labels.");
    }

    return {
        id: bin.quest_id,
        language: bin.language,
        name: bin.quest_name,
        short_description: bin.short_description,
        long_description: bin.long_description,
        episode,
        objects,
        npcs: parse_npc_data(episode, dat.npcs),
        events: dat.waves,
        dat_unknowns: dat.unknowns,
        object_code: bin.object_code,
        shop_items: bin.shop_items,
        map_designations,
    };
}

export function write_quest_qst(quest: Quest, file_name: string): ArrayBuffer {
    const dat = write_dat({
        objs: objects_to_dat_data(quest.objects),
        npcs: npcs_to_dat_data(quest.npcs),
        waves: quest.events,
        unknowns: quest.dat_unknowns,
    });
    const bin = write_bin({
        quest_id: quest.id,
        language: quest.language,
        quest_name: quest.name,
        short_description: quest.short_description,
        long_description: quest.long_description,
        object_code: quest.object_code,
        shop_items: quest.shop_items,
    });
    const ext_start = file_name.lastIndexOf(".");
    const base_file_name =
        ext_start === -1 ? file_name.slice(0, 11) : file_name.slice(0, Math.min(11, ext_start));

    return write_qst({
        files: [
            {
                name: base_file_name + ".dat",
                id: quest.id,
                data: prs_compress(
                    new ResizableBufferCursor(dat, Endianness.Little),
                ).array_buffer(),
            },
            {
                name: base_file_name + ".bin",
                id: quest.id,
                data: prs_compress(new ArrayBufferCursor(bin, Endianness.Little)).array_buffer(),
            },
        ],
    });
}

/**
 * Defaults to episode I.
 */
function get_episode(func_0_instructions: Instruction[]): Episode {
    const set_episode = func_0_instructions.find(
        instruction => instruction.opcode.code === OP_SET_EPISODE.code,
    );

    if (set_episode) {
        switch (set_episode.args[0].value) {
            default:
            case 0:
                return Episode.I;
            case 1:
                return Episode.II;
            case 2:
                return Episode.IV;
        }
    } else {
        logger.debug("Function 0 has no set_episode instruction.");
        return Episode.I;
    }
}

function extract_map_designations(
    dat: DatFile,
    episode: Episode,
    func_0_instructions: Instruction[],
): Map<number, number> {
    const map_designations = new Map<number, number>();

    for (const inst of func_0_instructions) {
        if (inst.opcode.code === OP_BB_MAP_DESIGNATE.code) {
            map_designations.set(inst.args[0].value, inst.args[2].value);
        }
    }

    return map_designations;
}

function extract_script_entry_points(
    objects: readonly QuestObject[],
    npcs: readonly DatNpc[],
): number[] {
    const entry_points = new Set([0]);

    for (const obj of objects) {
        const entry_point = obj.properties.get("script_label");

        if (entry_point != undefined) {
            entry_points.add(entry_point);
        }

        const entry_point_2 = obj.properties.get("script_label_2");

        if (entry_point_2 != undefined) {
            entry_points.add(entry_point_2);
        }
    }

    for (const npc of npcs) {
        entry_points.add(Math.round(npc.script_label));
    }

    return [...entry_points];
}

function parse_obj_data(objs: readonly DatObject[]): QuestObject[] {
    return objs.map(obj_data => {
        const type = pso_id_to_object_type(obj_data.type_id);

        return {
            type,
            id: obj_data.id,
            group_id: obj_data.group_id,
            area_id: obj_data.area_id,
            section_id: obj_data.section_id,
            position: obj_data.position,
            rotation: obj_data.rotation,
            properties: new Map(
                obj_data.properties.map((value, index) => {
                    if (
                        index === 3 &&
                        (type === ObjectType.ScriptCollision ||
                            type === ObjectType.ForestConsole ||
                            type === ObjectType.TalkLinkToSupport)
                    ) {
                        return ["script_label", value];
                    } else if (index === 4 && type === ObjectType.RicoMessagePod) {
                        return ["script_label", value];
                    } else if (index === 5 && type === ObjectType.RicoMessagePod) {
                        return ["script_label_2", value];
                    } else {
                        return [`property_${index}`, value];
                    }
                }),
            ),
            unknown: obj_data.unknown,
        };
    });
}

function parse_npc_data(episode: number, npcs: readonly DatNpc[]): QuestNpc[] {
    return npcs.map(npc_data => {
        return {
            type: get_npc_type(episode, npc_data),
            area_id: npc_data.area_id,
            section_id: npc_data.section_id,
            wave: npc_data.wave,
            pso_wave2: npc_data.wave2,
            position: npc_data.position,
            rotation: npc_data.rotation,
            scale: npc_data.scale,
            unknown: npc_data.unknown,
            pso_type_id: npc_data.type_id,
            npc_id: npc_data.npc_id,
            script_label: Math.round(npc_data.script_label),
            pso_roaming: npc_data.roaming,
        };
    });
}

// TODO: detect Mothmant, St. Rappy, Hallo Rappy, Egg Rappy, Death Gunner, Bulk and Recon.
function get_npc_type(episode: number, { type_id, scale, roaming, area_id }: DatNpc): NpcType {
    const regular = Math.abs(scale.y - 1) > 0.00001;

    switch (`${type_id}, ${roaming % 3}, ${episode}`) {
        case `${0x044}, 0, 1`:
            return NpcType.Booma;
        case `${0x044}, 1, 1`:
            return NpcType.Gobooma;
        case `${0x044}, 2, 1`:
            return NpcType.Gigobooma;

        case `${0x063}, 0, 1`:
            return NpcType.EvilShark;
        case `${0x063}, 1, 1`:
            return NpcType.PalShark;
        case `${0x063}, 2, 1`:
            return NpcType.GuilShark;

        case `${0x0a6}, 0, 1`:
            return NpcType.Dimenian;
        case `${0x0a6}, 0, 2`:
            return NpcType.Dimenian2;
        case `${0x0a6}, 1, 1`:
            return NpcType.LaDimenian;
        case `${0x0a6}, 1, 2`:
            return NpcType.LaDimenian2;
        case `${0x0a6}, 2, 1`:
            return NpcType.SoDimenian;
        case `${0x0a6}, 2, 2`:
            return NpcType.SoDimenian2;

        case `${0x0d6}, 0, 2`:
            return NpcType.Mericarol;
        case `${0x0d6}, 1, 2`:
            return NpcType.Mericus;
        case `${0x0d6}, 2, 2`:
            return NpcType.Merikle;

        case `${0x115}, 0, 4`:
            return NpcType.Boota;
        case `${0x115}, 1, 4`:
            return NpcType.ZeBoota;
        case `${0x115}, 2, 4`:
            return NpcType.BaBoota;
        case `${0x117}, 0, 4`:
            return NpcType.Goran;
        case `${0x117}, 1, 4`:
            return NpcType.PyroGoran;
        case `${0x117}, 2, 4`:
            return NpcType.GoranDetonator;
    }

    switch (`${type_id}, ${roaming % 2}, ${episode}`) {
        case `${0x040}, 0, 1`:
            return NpcType.Hildebear;
        case `${0x040}, 0, 2`:
            return NpcType.Hildebear2;
        case `${0x040}, 1, 1`:
            return NpcType.Hildeblue;
        case `${0x040}, 1, 2`:
            return NpcType.Hildeblue2;
        case `${0x041}, 0, 1`:
            return NpcType.RagRappy;
        case `${0x041}, 0, 2`:
            return NpcType.RagRappy2;
        case `${0x041}, 0, 4`:
            return NpcType.SandRappy;
        case `${0x041}, 1, 1`:
            return NpcType.AlRappy;
        case `${0x041}, 1, 2`:
            return NpcType.LoveRappy;
        case `${0x041}, 1, 4`:
            return NpcType.DelRappy;

        case `${0x080}, 0, 1`:
            return NpcType.Dubchic;
        case `${0x080}, 0, 2`:
            return NpcType.Dubchic2;
        case `${0x080}, 1, 1`:
            return NpcType.Gilchic;
        case `${0x080}, 1, 2`:
            return NpcType.Gilchic2;

        case `${0x0d4}, 0, 2`:
            return NpcType.SinowBerill;
        case `${0x0d4}, 1, 2`:
            return NpcType.SinowSpigell;
        case `${0x0d5}, 0, 2`:
            return NpcType.Merillia;
        case `${0x0d5}, 1, 2`:
            return NpcType.Meriltas;
        case `${0x0d7}, 0, 2`:
            return NpcType.UlGibbon;
        case `${0x0d7}, 1, 2`:
            return NpcType.ZolGibbon;

        case `${0x0dd}, 0, 2`:
            return NpcType.Dolmolm;
        case `${0x0dd}, 1, 2`:
            return NpcType.Dolmdarl;
        case `${0x0e0}, 0, 2`:
            return area_id > 15 ? NpcType.Epsilon : NpcType.SinowZoa;
        case `${0x0e0}, 1, 2`:
            return area_id > 15 ? NpcType.Epsilon : NpcType.SinowZele;

        case `${0x112}, 0, 4`:
            return NpcType.MerissaA;
        case `${0x112}, 1, 4`:
            return NpcType.MerissaAA;
        case `${0x114}, 0, 4`:
            return NpcType.Zu;
        case `${0x114}, 1, 4`:
            return NpcType.Pazuzu;
        case `${0x116}, 0, 4`:
            return NpcType.Dorphon;
        case `${0x116}, 1, 4`:
            return NpcType.DorphonEclair;
        case `${0x119}, 0, 4`:
            return regular ? NpcType.SaintMilion : NpcType.Kondrieu;
        case `${0x119}, 1, 4`:
            return regular ? NpcType.Shambertin : NpcType.Kondrieu;
    }

    switch (`${type_id}, ${episode}`) {
        case `${0x042}, 1`:
            return NpcType.Monest;
        case `${0x042}, 2`:
            return NpcType.Monest2;
        case `${0x043}, 1`:
            return regular ? NpcType.SavageWolf : NpcType.BarbarousWolf;
        case `${0x043}, 2`:
            return regular ? NpcType.SavageWolf2 : NpcType.BarbarousWolf2;

        case `${0x060}, 1`:
            return NpcType.GrassAssassin;
        case `${0x060}, 2`:
            return NpcType.GrassAssassin2;
        case `${0x061}, 1`:
            return area_id > 15 ? NpcType.DelLily : regular ? NpcType.PoisonLily : NpcType.NarLily;
        case `${0x061}, 2`:
            return area_id > 15
                ? NpcType.DelLily
                : regular
                ? NpcType.PoisonLily2
                : NpcType.NarLily2;
        case `${0x062}, 1`:
            return NpcType.NanoDragon;
        case `${0x064}, 1`:
            return regular ? NpcType.PofuillySlime : NpcType.PouillySlime;
        case `${0x065}, 1`:
            return NpcType.PanArms;
        case `${0x065}, 2`:
            return NpcType.PanArms2;

        case `${0x081}, 1`:
            return NpcType.Garanz;
        case `${0x081}, 2`:
            return NpcType.Garanz2;
        case `${0x082}, 1`:
            return regular ? NpcType.SinowBeat : NpcType.SinowGold;
        case `${0x083}, 1`:
            return NpcType.Canadine;
        case `${0x084}, 1`:
            return NpcType.Canane;
        case `${0x085}, 1`:
            return NpcType.Dubswitch;
        case `${0x085}, 2`:
            return NpcType.Dubswitch2;

        case `${0x0a0}, 1`:
            return NpcType.Delsaber;
        case `${0x0a0}, 2`:
            return NpcType.Delsaber2;
        case `${0x0a1}, 1`:
            return NpcType.ChaosSorcerer;
        case `${0x0a1}, 2`:
            return NpcType.ChaosSorcerer2;
        case `${0x0a2}, 1`:
            return NpcType.DarkGunner;
        case `${0x0a4}, 1`:
            return NpcType.ChaosBringer;
        case `${0x0a5}, 1`:
            return NpcType.DarkBelra;
        case `${0x0a5}, 2`:
            return NpcType.DarkBelra2;
        case `${0x0a7}, 1`:
            return NpcType.Bulclaw;
        case `${0x0a8}, 1`:
            return NpcType.Claw;

        case `${0x0c0}, 1`:
            return NpcType.Dragon;
        case `${0x0c0}, 2`:
            return NpcType.GalGryphon;
        case `${0x0c1}, 1`:
            return NpcType.DeRolLe;
        // TODO:
        // case `${0x0C2}, 1`: return NpcType.VolOptPart1;
        case `${0x0c5}, 1`:
            return NpcType.VolOpt;
        case `${0x0c8}, 1`:
            return NpcType.DarkFalz;
        case `${0x0ca}, 2`:
            return NpcType.OlgaFlow;
        case `${0x0cb}, 2`:
            return NpcType.BarbaRay;
        case `${0x0cc}, 2`:
            return NpcType.GolDragon;

        case `${0x0d8}, 2`:
            return NpcType.Gibbles;
        case `${0x0d9}, 2`:
            return NpcType.Gee;
        case `${0x0da}, 2`:
            return NpcType.GiGue;

        case `${0x0db}, 2`:
            return NpcType.Deldepth;
        case `${0x0dc}, 2`:
            return NpcType.Delbiter;
        case `${0x0de}, 2`:
            return NpcType.Morfos;
        case `${0x0df}, 2`:
            return NpcType.Recobox;
        case `${0x0e1}, 2`:
            return NpcType.IllGill;

        case `${0x110}, 4`:
            return NpcType.Astark;
        case `${0x111}, 4`:
            return regular ? NpcType.SatelliteLizard : NpcType.Yowie;
        case `${0x113}, 4`:
            return NpcType.Girtablulu;
    }

    switch (type_id) {
        case 0x004:
            return NpcType.FemaleFat;
        case 0x005:
            return NpcType.FemaleMacho;
        case 0x007:
            return NpcType.FemaleTall;
        case 0x00a:
            return NpcType.MaleDwarf;
        case 0x00b:
            return NpcType.MaleFat;
        case 0x00c:
            return NpcType.MaleMacho;
        case 0x00d:
            return NpcType.MaleOld;
        case 0x019:
            return NpcType.BlueSoldier;
        case 0x01a:
            return NpcType.RedSoldier;
        case 0x01b:
            return NpcType.Principal;
        case 0x01c:
            return NpcType.Tekker;
        case 0x01d:
            return NpcType.GuildLady;
        case 0x01e:
            return NpcType.Scientist;
        case 0x01f:
            return NpcType.Nurse;
        case 0x020:
            return NpcType.Irene;
        case 0x0f1:
            return NpcType.ItemShop;
        case 0x0fe:
            return NpcType.Nurse2;
    }

    return NpcType.Unknown;
}

function objects_to_dat_data(objects: readonly QuestObject[]): DatObject[] {
    return objects.map(object => ({
        type_id: object_data(object.type).pso_id!,
        id: object.id,
        group_id: object.group_id,
        section_id: object.section_id,
        position: object.position,
        rotation: object.rotation,
        properties: [...object.properties.values()],
        area_id: object.area_id,
        unknown: object.unknown,
    }));
}

function npcs_to_dat_data(npcs: readonly QuestNpc[]): DatNpc[] {
    return npcs.map(npc => {
        const type_data = npc_data(npc.type);
        const type_id =
            type_data.pso_type_id == undefined ? npc.pso_type_id : type_data.pso_type_id;
        const roaming =
            type_data.pso_roaming == undefined ? npc.pso_roaming : type_data.pso_roaming;
        const regular = type_data.pso_regular == undefined ? true : type_data.pso_regular;

        const scale_y = reinterpret_i32_as_f32(
            (reinterpret_f32_as_i32(npc.scale.y) & ~0x800000) | (regular ? 0 : 0x800000),
        );

        const scale = { x: npc.scale.x, y: scale_y, z: npc.scale.z };

        return {
            type_id,
            wave: npc.wave,
            wave2: npc.pso_wave2,
            section_id: npc.section_id,
            position: npc.position,
            rotation: npc.rotation,
            scale,
            npc_id: npc.npc_id,
            script_label: npc.script_label,
            roaming,
            area_id: npc.area_id,
            unknown: npc.unknown,
        };
    });
}

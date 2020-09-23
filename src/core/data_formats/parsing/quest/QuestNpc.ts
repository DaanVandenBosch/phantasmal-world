import { npc_data, NpcType } from "./npc_types";
import { Vec3 } from "../../vector";
import { Episode } from "./Episode";
import { NPC_BYTE_SIZE } from "./dat";
import { assert } from "../../../util";
import { angle_to_rad, rad_to_angle } from "../ninja/angle";

const DEFAULT_SCALE: Vec3 = Object.freeze({ x: 1, y: 1, z: 1 });

export type QuestNpc = {
    episode: Episode;
    area_id: number;
    readonly data: ArrayBuffer;
    readonly view: DataView;
};

export function create_quest_npc(type: NpcType, area_id: number, wave: number): QuestNpc {
    const data = new ArrayBuffer(NPC_BYTE_SIZE);
    const npc: QuestNpc = {
        episode: Episode.I,
        area_id,
        data,
        view: new DataView(data),
    };

    // Set scale before type, because set_npc_type will change it.
    set_npc_scale(npc, DEFAULT_SCALE);
    set_npc_type(npc, type);
    // Set area_id after type, because you might want to overwrite the area_id that type has
    // determined.
    npc.area_id = area_id;
    set_npc_wave(npc, wave);
    set_npc_wave_2(npc, wave);

    return npc;
}

export function data_to_quest_npc(episode: Episode, area_id: number, data: ArrayBuffer): QuestNpc {
    assert(
        data.byteLength === NPC_BYTE_SIZE,
        () => `Data byteLength should be ${NPC_BYTE_SIZE} but was ${data.byteLength}.`,
    );

    return {
        episode,
        area_id,
        data,
        view: new DataView(data),
    };
}

//
// Simple properties that directly map to a part of the data block.
//

export function get_npc_type_id(npc: QuestNpc): number {
    return npc.view.getUint16(0, true);
}

export function set_npc_type_id(npc: QuestNpc, type_id: number): void {
    npc.view.setUint16(0, type_id, true);
}

export function get_npc_section_id(npc: QuestNpc): number {
    return npc.view.getUint16(12, true);
}

export function set_npc_section_id(npc: QuestNpc, section_id: number): void {
    npc.view.setUint16(12, section_id, true);
}

export function get_npc_wave(npc: QuestNpc): number {
    return npc.view.getUint16(14, true);
}

export function set_npc_wave(npc: QuestNpc, wave: number): void {
    npc.view.setUint16(14, wave, true);
}

export function get_npc_wave_2(npc: QuestNpc): number {
    return npc.view.getUint32(16, true);
}

export function set_npc_wave_2(npc: QuestNpc, wave_2: number): void {
    npc.view.setUint32(16, wave_2, true);
}

/**
 * Section-relative position.
 */
export function get_npc_position(npc: QuestNpc): Vec3 {
    return {
        x: npc.view.getFloat32(20, true),
        y: npc.view.getFloat32(24, true),
        z: npc.view.getFloat32(28, true),
    };
}

export function set_npc_position(npc: QuestNpc, position: Vec3): void {
    npc.view.setFloat32(20, position.x, true);
    npc.view.setFloat32(24, position.y, true);
    npc.view.setFloat32(28, position.z, true);
}

export function get_npc_rotation(npc: QuestNpc): Vec3 {
    return {
        x: angle_to_rad(npc.view.getInt32(32, true)),
        y: angle_to_rad(npc.view.getInt32(36, true)),
        z: angle_to_rad(npc.view.getInt32(40, true)),
    };
}

export function set_npc_rotation(npc: QuestNpc, rotation: Vec3): void {
    npc.view.setInt32(32, rad_to_angle(rotation.x), true);
    npc.view.setInt32(36, rad_to_angle(rotation.y), true);
    npc.view.setInt32(40, rad_to_angle(rotation.z), true);
}

/**
 * Seemingly 3 floats, not sure what they represent.
 * The y component is used to help determine what the NpcType is.
 */
export function get_npc_scale(npc: QuestNpc): Vec3 {
    return {
        x: npc.view.getFloat32(44, true),
        y: npc.view.getFloat32(48, true),
        z: npc.view.getFloat32(52, true),
    };
}

export function set_npc_scale(npc: QuestNpc, scale: Vec3): void {
    npc.view.setFloat32(44, scale.x, true);
    npc.view.setFloat32(48, scale.y, true);
    npc.view.setFloat32(52, scale.z, true);
}

export function get_npc_id(npc: QuestNpc): number {
    return npc.view.getFloat32(56, true);
}

/**
 * Only seems to be valid for non-enemies.
 */
export function get_npc_script_label(npc: QuestNpc): number {
    return Math.round(npc.view.getFloat32(60, true));
}

export function get_npc_skin(npc: QuestNpc): number {
    return npc.view.getUint32(64, true);
}

export function set_npc_skin(npc: QuestNpc, skin: number): void {
    npc.view.setUint32(64, skin, true);
}

//
// Complex properties that use multiple parts of the data block and possible other properties.
//

// TODO: detect Mothmant, St. Rappy, Hallo Rappy, Egg Rappy, Death Gunner, Bulk and Recon.
export function get_npc_type(npc: QuestNpc): NpcType {
    const episode = npc.episode;
    const type_id = get_npc_type_id(npc);
    const regular = is_npc_regular(npc);
    const skin = get_npc_skin(npc);
    const area_id = npc.area_id;

    switch (`${type_id}, ${skin % 3}, ${episode}`) {
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

    switch (`${type_id}, ${skin % 2}, ${episode}`) {
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
        case `${0x0c2}, 1`:
            return NpcType.VolOptPart1;
        case `${0x0c5}, 1`:
            return NpcType.VolOptPart2;
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

export function set_npc_type(npc: QuestNpc, type: NpcType): void {
    const data = npc_data(type);

    if (data.episode != undefined) {
        npc.episode = data.episode;
    }

    set_npc_type_id(npc, data.type_id ?? 0);
    set_npc_regular(npc, data.regular ?? true);
    set_npc_skin(npc, data.skin ?? 0);

    if (data.area_ids.length > 0 && !data.area_ids.includes(npc.area_id)) {
        npc.area_id = data.area_ids[0];
    }
}

export function is_npc_regular(npc: QuestNpc): boolean {
    return Math.abs(npc.view.getFloat32(48, true) - 1) > 0.00001;
}

export function set_npc_regular(npc: QuestNpc, regular: boolean): void {
    npc.view.setInt32(
        48,
        (npc.view.getInt32(48, true) & ~0x800000) | (regular ? 0 : 0x800000),
        true,
    );
}

import { ArrayBufferCursor } from '../ArrayBufferCursor';
import * as prs from '../compression/prs';
import { parseDat, writeDat, DatObject, DatNpc } from './dat';
import { parseBin, writeBin, Instruction } from './bin';
import { parseQst, writeQst } from './qst';
import {
    Vec3,
    AreaVariant,
    QuestNpc,
    QuestObject,
    Quest,
    ObjectType,
    NpcType
} from '../../domain';
import { areaStore } from '../../stores/AreaStore';

/**
 * High level parsing function that delegates to lower level parsing functions.
 * 
 * Always delegates to parseQst at the moment.
 */
export function parseQuest(cursor: ArrayBufferCursor): Quest | undefined {
    const qst = parseQst(cursor);

    if (!qst) {
        return;
    }

    let datFile = null;
    let binFile = null;

    for (const file of qst.files) {
        if (file.name.endsWith('.dat')) {
            datFile = file;
        } else if (file.name.endsWith('.bin')) {
            binFile = file;
        }
    }

    // TODO: deal with missing/multiple DAT or BIN file.

    if (!datFile) {
        console.error('File contains no DAT file.');
        return;
    }

    if (!binFile) {
        console.error('File contains no BIN file.');
        return;
    }

    const dat = parseDat(prs.decompress(datFile.data));
    const bin = parseBin(prs.decompress(binFile.data));
    let episode = 1;
    let areaVariants: AreaVariant[] = [];

    if (bin.functionOffsets.length) {
        const func0Ops = getFuncOperations(bin.instructions, bin.functionOffsets[0]);

        if (func0Ops) {
            episode = getEpisode(func0Ops);
            areaVariants = getAreaVariants(episode, func0Ops);
        } else {
            console.warn(`Function 0 offset ${bin.functionOffsets[0]} is invalid.`);
        }
    } else {
        console.warn('File contains no functions.');
    }

    return new Quest(
        bin.questName,
        bin.shortDescription,
        bin.longDescription,
        datFile.questNo,
        episode,
        areaVariants,
        parseObjData(dat.objs),
        parseNpcData(episode, dat.npcs),
        dat.unknowns,
        bin.data
    );
}

export function writeQuestQst(quest: Quest, fileName: string): ArrayBufferCursor {
    const dat = writeDat({
        objs: objectsToDatData(quest.objects),
        npcs: npcsToDatData(quest.npcs),
        unknowns: quest.datUnkowns
    });
    const bin = writeBin({ data: quest.binData });
    const extStart = fileName.lastIndexOf('.');
    const baseFileName = extStart === -1 ? fileName : fileName.slice(0, extStart);

    return writeQst({
        files: [
            {
                name: baseFileName + '.dat',
                questNo: quest.questNo,
                data: prs.compress(dat)
            },
            {
                name: baseFileName + '.bin',
                questNo: quest.questNo,
                data: prs.compress(bin)
            }
        ]
    });
}

/**
 * Defaults to episode I.
 */
function getEpisode(func0Ops: Instruction[]): number {
    const setEpisode = func0Ops.find(op => op.mnemonic === 'set_episode');

    if (setEpisode) {
        switch (setEpisode.args[0]) {
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

function getAreaVariants(episode: number, func0Ops: Instruction[]): AreaVariant[] {
    const areaVariants = new Map();
    const bbMaps = func0Ops.filter(op => op.mnemonic === 'BB_Map_Designate');

    for (const bbMap of bbMaps) {
        const areaId = bbMap.args[0];
        const variantId = bbMap.args[2];
        areaVariants.set(areaId, variantId);
    }

    // Sort by area order and then variant id.
    return (
        Array.from(areaVariants)
            .map(([areaId, variantId]) =>
                areaStore.getVariant(episode, areaId, variantId))
            .sort((a, b) => a.area.order - b.area.order || a.id - b.id)
    );
}

function getFuncOperations(operations: Instruction[], funcOffset: number) {
    let position = 0;
    let funcFound = false;
    const funcOps: Instruction[] = [];

    for (const operation of operations) {
        if (position === funcOffset) {
            funcFound = true;
        }

        if (funcFound) {
            funcOps.push(operation);

            // Break when ret is encountered.
            if (operation.opcode === 1) {
                break;
            }
        }

        position += operation.size;
    }

    return funcFound ? funcOps : null;
}

function parseObjData(objs: DatObject[]): QuestObject[] {
    return objs.map(objData => {
        const { x, y, z } = objData.position;
        const rot = objData.rotation;
        return new QuestObject(
            objData.areaId,
            objData.sectionId,
            new Vec3(x, y, z),
            new Vec3(rot.x, rot.y, rot.z),
            ObjectType.fromPsoId(objData.typeId),
            objData
        );
    });
}

function parseNpcData(episode: number, npcs: DatNpc[]): QuestNpc[] {
    return npcs.map(npcData => {
        const { x, y, z } = npcData.position;
        const rot = npcData.rotation;
        return new QuestNpc(
            npcData.areaId,
            npcData.sectionId,
            new Vec3(x, y, z),
            new Vec3(rot.x, rot.y, rot.z),
            getNpcType(episode, npcData),
            npcData
        );
    });
}

// TODO: detect Mothmant, St. Rappy, Hallo Rappy, Egg Rappy, Death Gunner, Bulk and Recon.
function getNpcType(episode: number, { typeId, unknown, skin, areaId }: DatNpc): NpcType {
    const regular = (unknown[2][18] & 0x80) === 0;

    switch (`${typeId}, ${skin % 3}, ${episode}`) {
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

    switch (`${typeId}, ${skin % 2}, ${episode}`) {
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

        case `${0x061}, 0, 1`: return areaId > 15 ? NpcType.DelLily : NpcType.PoisonLily;
        case `${0x061}, 0, 2`: return areaId > 15 ? NpcType.DelLily : NpcType.PoisonLily2;
        case `${0x061}, 1, 1`: return areaId > 15 ? NpcType.DelLily : NpcType.NarLily;
        case `${0x061}, 1, 2`: return areaId > 15 ? NpcType.DelLily : NpcType.NarLily2;

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
        case `${0x0E0}, 0, 2`: return areaId > 15 ? NpcType.Epsilon : NpcType.SinowZoa;
        case `${0x0E0}, 1, 2`: return areaId > 15 ? NpcType.Epsilon : NpcType.SinowZele;

        case `${0x112}, 0, 4`: return NpcType.MerissaA;
        case `${0x112}, 1, 4`: return NpcType.MerissaAA;
        case `${0x114}, 0, 4`: return NpcType.Zu;
        case `${0x114}, 1, 4`: return NpcType.Pazuzu;
        case `${0x116}, 0, 4`: return NpcType.Dorphon;
        case `${0x116}, 1, 4`: return NpcType.DorphonEclair;
        case `${0x119}, 0, 4`: return regular ? NpcType.SaintMilion : NpcType.Kondrieu;
        case `${0x119}, 1, 4`: return regular ? NpcType.Shambertin : NpcType.Kondrieu;
    }

    switch (`${typeId}, ${episode}`) {
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

    switch (typeId) {
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

    return NpcType.Unknown;
}

function objectsToDatData(objects: QuestObject[]): DatObject[] {
    return objects.map(object => ({
        typeId: object.type.psoId!,
        sectionId: object.sectionId,
        position: object.sectionPosition,
        rotation: object.rotation,
        areaId: object.areaId,
        unknown: object.dat.unknown
    }));
}

function npcsToDatData(npcs: QuestNpc[]): DatNpc[] {
    return npcs.map(npc => {
        // If the type is unknown, typeData will be null and we use the raw data from the DAT file.
        const typeData = npcTypeToDatData(npc.type);

        if (typeData) {
            npc.dat.unknown[2][18] = (npc.dat.unknown[2][18] & ~0x80) | (typeData.regular ? 0 : 0x80);
        }

        return {
            typeId: typeData ? typeData.typeId : npc.dat.typeId,
            sectionId: npc.sectionId,
            position: npc.sectionPosition,
            rotation: npc.rotation,
            skin: typeData ? typeData.skin : npc.dat.skin,
            areaId: npc.areaId,
            unknown: npc.dat.unknown
        };
    });
}

function npcTypeToDatData(
    type: NpcType
): { typeId: number, skin: number, regular: boolean } | null {
    switch (type) {
        default: throw new Error(`Unexpected type ${type.code}.`);

        case NpcType.Unknown: return null;

        case NpcType.FemaleFat: return { typeId: 0x004, skin: 0, regular: true };
        case NpcType.FemaleMacho: return { typeId: 0x005, skin: 0, regular: true };
        case NpcType.FemaleTall: return { typeId: 0x007, skin: 0, regular: true };
        case NpcType.MaleDwarf: return { typeId: 0x00A, skin: 0, regular: true };
        case NpcType.MaleFat: return { typeId: 0x00B, skin: 0, regular: true };
        case NpcType.MaleMacho: return { typeId: 0x00C, skin: 0, regular: true };
        case NpcType.MaleOld: return { typeId: 0x00D, skin: 0, regular: true };
        case NpcType.BlueSoldier: return { typeId: 0x019, skin: 0, regular: true };
        case NpcType.RedSoldier: return { typeId: 0x01A, skin: 0, regular: true };
        case NpcType.Principal: return { typeId: 0x01B, skin: 0, regular: true };
        case NpcType.Tekker: return { typeId: 0x01C, skin: 0, regular: true };
        case NpcType.GuildLady: return { typeId: 0x01D, skin: 0, regular: true };
        case NpcType.Scientist: return { typeId: 0x01E, skin: 0, regular: true };
        case NpcType.Nurse: return { typeId: 0x01F, skin: 0, regular: true };
        case NpcType.Irene: return { typeId: 0x020, skin: 0, regular: true };
        case NpcType.ItemShop: return { typeId: 0x0F1, skin: 0, regular: true };
        case NpcType.Nurse2: return { typeId: 0x0FE, skin: 0, regular: true };

        case NpcType.Hildebear: return { typeId: 0x040, skin: 0, regular: true };
        case NpcType.Hildeblue: return { typeId: 0x040, skin: 1, regular: true };
        case NpcType.RagRappy: return { typeId: 0x041, skin: 0, regular: true };
        case NpcType.AlRappy: return { typeId: 0x041, skin: 1, regular: true };
        case NpcType.Monest: return { typeId: 0x042, skin: 0, regular: true };
        case NpcType.SavageWolf: return { typeId: 0x043, skin: 0, regular: true };
        case NpcType.BarbarousWolf: return { typeId: 0x043, skin: 0, regular: false };
        case NpcType.Booma: return { typeId: 0x044, skin: 0, regular: true };
        case NpcType.Gobooma: return { typeId: 0x044, skin: 1, regular: true };
        case NpcType.Gigobooma: return { typeId: 0x044, skin: 2, regular: true };
        case NpcType.Dragon: return { typeId: 0x0C0, skin: 0, regular: true };

        case NpcType.GrassAssassin: return { typeId: 0x060, skin: 0, regular: true };
        case NpcType.PoisonLily: return { typeId: 0x061, skin: 0, regular: true };
        case NpcType.NarLily: return { typeId: 0x061, skin: 1, regular: true };
        case NpcType.NanoDragon: return { typeId: 0x062, skin: 0, regular: true };
        case NpcType.EvilShark: return { typeId: 0x063, skin: 0, regular: true };
        case NpcType.PalShark: return { typeId: 0x063, skin: 1, regular: true };
        case NpcType.GuilShark: return { typeId: 0x063, skin: 2, regular: true };
        case NpcType.PofuillySlime: return { typeId: 0x064, skin: 0, regular: true };
        case NpcType.PouillySlime: return { typeId: 0x064, skin: 0, regular: false };
        case NpcType.PanArms: return { typeId: 0x065, skin: 0, regular: true };
        case NpcType.DeRolLe: return { typeId: 0x0C1, skin: 0, regular: true };

        case NpcType.Dubchic: return { typeId: 0x080, skin: 0, regular: true };
        case NpcType.Gilchic: return { typeId: 0x080, skin: 1, regular: true };
        case NpcType.Garanz: return { typeId: 0x081, skin: 0, regular: true };
        case NpcType.SinowBeat: return { typeId: 0x082, skin: 0, regular: true };
        case NpcType.SinowGold: return { typeId: 0x082, skin: 0, regular: false };
        case NpcType.Canadine: return { typeId: 0x083, skin: 0, regular: true };
        case NpcType.Canane: return { typeId: 0x084, skin: 0, regular: true };
        case NpcType.Dubswitch: return { typeId: 0x085, skin: 0, regular: true };
        case NpcType.VolOpt: return { typeId: 0x0C5, skin: 0, regular: true };

        case NpcType.Delsaber: return { typeId: 0x0A0, skin: 0, regular: true };
        case NpcType.ChaosSorcerer: return { typeId: 0x0A1, skin: 0, regular: true };
        case NpcType.DarkGunner: return { typeId: 0x0A2, skin: 0, regular: true };
        case NpcType.ChaosBringer: return { typeId: 0x0A4, skin: 0, regular: true };
        case NpcType.DarkBelra: return { typeId: 0x0A5, skin: 0, regular: true };
        case NpcType.Dimenian: return { typeId: 0x0A6, skin: 0, regular: true };
        case NpcType.LaDimenian: return { typeId: 0x0A6, skin: 1, regular: true };
        case NpcType.SoDimenian: return { typeId: 0x0A6, skin: 2, regular: true };
        case NpcType.Bulclaw: return { typeId: 0x0A7, skin: 0, regular: true };
        case NpcType.Claw: return { typeId: 0x0A8, skin: 0, regular: true };
        case NpcType.DarkFalz: return { typeId: 0x0C8, skin: 0, regular: true };

        case NpcType.Hildebear2: return { typeId: 0x040, skin: 0, regular: true };
        case NpcType.Hildeblue2: return { typeId: 0x040, skin: 1, regular: true };
        case NpcType.RagRappy2: return { typeId: 0x041, skin: 0, regular: true };
        case NpcType.LoveRappy: return { typeId: 0x041, skin: 1, regular: true };
        case NpcType.Monest2: return { typeId: 0x042, skin: 0, regular: true };
        case NpcType.PoisonLily2: return { typeId: 0x061, skin: 0, regular: true };
        case NpcType.NarLily2: return { typeId: 0x061, skin: 1, regular: true };
        case NpcType.GrassAssassin2: return { typeId: 0x060, skin: 0, regular: true };
        case NpcType.Dimenian2: return { typeId: 0x0A6, skin: 0, regular: true };
        case NpcType.LaDimenian2: return { typeId: 0x0A6, skin: 1, regular: true };
        case NpcType.SoDimenian2: return { typeId: 0x0A6, skin: 2, regular: true };
        case NpcType.DarkBelra2: return { typeId: 0x0A5, skin: 0, regular: true };
        case NpcType.BarbaRay: return { typeId: 0x0CB, skin: 0, regular: true };

        case NpcType.SavageWolf2: return { typeId: 0x043, skin: 0, regular: true };
        case NpcType.BarbarousWolf2: return { typeId: 0x043, skin: 0, regular: false };
        case NpcType.PanArms2: return { typeId: 0x065, skin: 0, regular: true };
        case NpcType.Dubchic2: return { typeId: 0x080, skin: 0, regular: true };
        case NpcType.Gilchic2: return { typeId: 0x080, skin: 1, regular: true };
        case NpcType.Garanz2: return { typeId: 0x081, skin: 0, regular: true };
        case NpcType.Dubswitch2: return { typeId: 0x085, skin: 0, regular: true };
        case NpcType.Delsaber2: return { typeId: 0x0A0, skin: 0, regular: true };
        case NpcType.ChaosSorcerer2: return { typeId: 0x0A1, skin: 0, regular: true };
        case NpcType.GolDragon: return { typeId: 0x0CC, skin: 0, regular: true };

        case NpcType.SinowBerill: return { typeId: 0x0D4, skin: 0, regular: true };
        case NpcType.SinowSpigell: return { typeId: 0x0D4, skin: 1, regular: true };
        case NpcType.Merillia: return { typeId: 0x0D5, skin: 0, regular: true };
        case NpcType.Meriltas: return { typeId: 0x0D5, skin: 1, regular: true };
        case NpcType.Mericarol: return { typeId: 0x0D6, skin: 0, regular: true };
        case NpcType.Mericus: return { typeId: 0x0D6, skin: 1, regular: true };
        case NpcType.Merikle: return { typeId: 0x0D6, skin: 2, regular: true };
        case NpcType.UlGibbon: return { typeId: 0x0D7, skin: 0, regular: true };
        case NpcType.ZolGibbon: return { typeId: 0x0D7, skin: 1, regular: true };
        case NpcType.Gibbles: return { typeId: 0x0D8, skin: 0, regular: true };
        case NpcType.Gee: return { typeId: 0x0D9, skin: 0, regular: true };
        case NpcType.GiGue: return { typeId: 0x0DA, skin: 0, regular: true };
        case NpcType.GalGryphon: return { typeId: 0x0C0, skin: 0, regular: true };

        case NpcType.Deldepth: return { typeId: 0x0DB, skin: 0, regular: true };
        case NpcType.Delbiter: return { typeId: 0x0DC, skin: 0, regular: true };
        case NpcType.Dolmolm: return { typeId: 0x0DD, skin: 0, regular: true };
        case NpcType.Dolmdarl: return { typeId: 0x0DD, skin: 1, regular: true };
        case NpcType.Morfos: return { typeId: 0x0DE, skin: 0, regular: true };
        case NpcType.Recobox: return { typeId: 0x0DF, skin: 0, regular: true };
        case NpcType.Epsilon: return { typeId: 0x0E0, skin: 0, regular: true };
        case NpcType.SinowZoa: return { typeId: 0x0E0, skin: 0, regular: true };
        case NpcType.SinowZele: return { typeId: 0x0E0, skin: 1, regular: true };
        case NpcType.IllGill: return { typeId: 0x0E1, skin: 0, regular: true };
        case NpcType.DelLily: return { typeId: 0x061, skin: 0, regular: true };
        case NpcType.OlgaFlow: return { typeId: 0x0CA, skin: 0, regular: true };

        case NpcType.SandRappy: return { typeId: 0x041, skin: 0, regular: true };
        case NpcType.DelRappy: return { typeId: 0x041, skin: 1, regular: true };
        case NpcType.Astark: return { typeId: 0x110, skin: 0, regular: true };
        case NpcType.SatelliteLizard: return { typeId: 0x111, skin: 0, regular: true };
        case NpcType.Yowie: return { typeId: 0x111, skin: 0, regular: false };
        case NpcType.MerissaA: return { typeId: 0x112, skin: 0, regular: true };
        case NpcType.MerissaAA: return { typeId: 0x112, skin: 1, regular: true };
        case NpcType.Girtablulu: return { typeId: 0x113, skin: 0, regular: true };
        case NpcType.Zu: return { typeId: 0x114, skin: 0, regular: true };
        case NpcType.Pazuzu: return { typeId: 0x114, skin: 1, regular: true };
        case NpcType.Boota: return { typeId: 0x115, skin: 0, regular: true };
        case NpcType.ZeBoota: return { typeId: 0x115, skin: 1, regular: true };
        case NpcType.BaBoota: return { typeId: 0x115, skin: 2, regular: true };
        case NpcType.Dorphon: return { typeId: 0x116, skin: 0, regular: true };
        case NpcType.DorphonEclair: return { typeId: 0x116, skin: 1, regular: true };
        case NpcType.Goran: return { typeId: 0x117, skin: 0, regular: true };
        case NpcType.PyroGoran: return { typeId: 0x117, skin: 1, regular: true };
        case NpcType.GoranDetonator: return { typeId: 0x117, skin: 2, regular: true };
        case NpcType.SaintMilion: return { typeId: 0x119, skin: 0, regular: true };
        case NpcType.Shambertin: return { typeId: 0x119, skin: 1, regular: true };
        case NpcType.Kondrieu: return { typeId: 0x119, skin: 0, regular: false };
    }
}

import { check_episode, Episode } from "./Episode";

// Make sure ObjectType does not overlap NpcType.
export enum NpcType {
    //
    // Unknown NPCs
    //

    Unknown,

    //
    // Friendly NPCs
    //

    FemaleFat,
    FemaleMacho,
    FemaleTall,
    MaleDwarf,
    MaleFat,
    MaleMacho,
    MaleOld,
    BlueSoldier,
    RedSoldier,
    Principal,
    Tekker,
    GuildLady,
    Scientist,
    Nurse,
    Irene,
    ItemShop,
    Nurse2,

    //
    // Enemy NPCs
    //

    // Episode I Forest

    Hildebear,
    Hildeblue,
    RagRappy,
    AlRappy,
    Monest,
    Mothmant,
    SavageWolf,
    BarbarousWolf,
    Booma,
    Gobooma,
    Gigobooma,
    Dragon,

    // Episode I Caves

    GrassAssassin,
    PoisonLily,
    NarLily,
    NanoDragon,
    EvilShark,
    PalShark,
    GuilShark,
    PofuillySlime,
    PouillySlime,
    PanArms,
    Migium,
    Hidoom,
    DeRolLe,

    // Episode I Mines

    Dubchic,
    Gilchic,
    Garanz,
    SinowBeat,
    SinowGold,
    Canadine,
    Canane,
    Dubswitch,
    VolOpt,

    // Episode I Ruins

    Delsaber,
    ChaosSorcerer,
    DarkGunner,
    DeathGunner,
    ChaosBringer,
    DarkBelra,
    Dimenian,
    LaDimenian,
    SoDimenian,
    Bulclaw,
    Bulk,
    Claw,
    DarkFalz,

    // Episode II VR Temple

    Hildebear2,
    Hildeblue2,
    RagRappy2,
    LoveRappy,
    StRappy,
    HalloRappy,
    EggRappy,
    Monest2,
    Mothmant2,
    PoisonLily2,
    NarLily2,
    GrassAssassin2,
    Dimenian2,
    LaDimenian2,
    SoDimenian2,
    DarkBelra2,
    BarbaRay,

    // Episode II VR Spaceship

    SavageWolf2,
    BarbarousWolf2,
    PanArms2,
    Migium2,
    Hidoom2,
    Dubchic2,
    Gilchic2,
    Garanz2,
    Dubswitch2,
    Delsaber2,
    ChaosSorcerer2,
    GolDragon,

    // Episode II Central Control Area

    SinowBerill,
    SinowSpigell,
    Merillia,
    Meriltas,
    Mericarol,
    Mericus,
    Merikle,
    UlGibbon,
    ZolGibbon,
    Gibbles,
    Gee,
    GiGue,
    IllGill,
    DelLily,
    Epsilon,
    GalGryphon,

    // Episode II Seabed

    Deldepth,
    Delbiter,
    Dolmolm,
    Dolmdarl,
    Morfos,
    Recobox,
    Recon,
    SinowZoa,
    SinowZele,
    OlgaFlow,

    // Episode IV

    SandRappy,
    DelRappy,
    Astark,
    SatelliteLizard,
    Yowie,
    MerissaA,
    MerissaAA,
    Girtablulu,
    Zu,
    Pazuzu,
    Boota,
    ZeBoota,
    BaBoota,
    Dorphon,
    DorphonEclair,
    Goran,
    PyroGoran,
    GoranDetonator,
    SaintMilion,
    Shambertin,
    Kondrieu,
}

export type NpcTypeData = {
    /**
     * Unique name. E.g. an episode II Delsaber would have (Ep. II) appended to its name.
     */
    readonly name: string;
    /**
     * Name used in the game.
     * Might conflict with other NPC names (e.g. Delsaber from ep. I and ep. II).
     */
    readonly simple_name: string;
    readonly ultimate_name: string;
    readonly episode?: Episode;
    readonly enemy: boolean;
    readonly rare_type?: NpcType;
    /**
     * Type ID used by the game.
     */
    readonly pso_type_id?: number;
    /**
     * Roaming value used by the game.
     */
    readonly pso_roaming?: number;
    /**
     * Boolean specifying whether an NPC is the regular or special variant. The game uses a single
     * bit in the y component of the NPC's scale vector for this value.
     * Sometimes signifies a variant (e.g. Barbarous Wolf), sometimes a rare variant (e.g. Pouilly
     * Slime).
     */
    readonly pso_regular?: boolean;
};

export const NPC_TYPES: NpcType[] = [];
export const ENEMY_NPC_TYPES: NpcType[] = [];

export function npc_data(type: NpcType): NpcTypeData {
    return NPC_TYPE_DATA[type];
}

/**
 * Uniquely identifies an NPC. Tries to match on simple_name and ultimate_name.
 */
export function name_and_episode_to_npc_type(name: string, episode: Episode): NpcType | undefined {
    check_episode(episode);
    return EP_AND_NAME_TO_NPC_TYPE[episode]!.get(name);
}

const EP_AND_NAME_TO_NPC_TYPE = [
    undefined,
    new Map<string, NpcType>(),
    new Map<string, NpcType>(),
    undefined,
    new Map<string, NpcType>(),
];

const NPC_TYPE_DATA: NpcTypeData[] = [];

function define_npc_type_data(
    npc_type: NpcType,
    name: string,
    simple_name: string,
    ultimate_name: string,
    episode: Episode | undefined,
    enemy: boolean,
    rare_type: NpcType | undefined,
    pso_type_id: number | undefined,
    pso_roaming: number | undefined,
    pso_regular: boolean | undefined,
): void {
    NPC_TYPES.push(npc_type);

    if (enemy) {
        ENEMY_NPC_TYPES.push(npc_type);
    }

    NPC_TYPE_DATA[npc_type] = {
        name,
        simple_name,
        ultimate_name,
        episode,
        enemy,
        rare_type,
        pso_type_id,
        pso_roaming,
        pso_regular,
    };

    if (episode) {
        const map = EP_AND_NAME_TO_NPC_TYPE[episode];

        if (map) {
            map.set(simple_name, npc_type);
            map.set(ultimate_name, npc_type);
        }
    }
}

//
// Unknown NPCs
//

define_npc_type_data(
    NpcType.Unknown,
    "Unknown",
    "Unknown",
    "Unknown",
    undefined,
    false,
    undefined,
    undefined,
    undefined,
    undefined,
);

//
// Friendly NPCs
//

define_npc_type_data(
    NpcType.FemaleFat,
    "Female Fat",
    "Female Fat",
    "Female Fat",
    undefined,
    false,
    undefined,
    0x004,
    0,
    true,
);
define_npc_type_data(
    NpcType.FemaleMacho,
    "Female Macho",
    "Female Macho",
    "Female Macho",
    undefined,
    false,
    undefined,
    0x005,
    0,
    true,
);
define_npc_type_data(
    NpcType.FemaleTall,
    "Female Tall",
    "Female Tall",
    "Female Tall",
    undefined,
    false,
    undefined,
    0x007,
    0,
    true,
);
define_npc_type_data(
    NpcType.MaleDwarf,
    "Male Dwarf",
    "Male Dwarf",
    "Male Dwarf",
    undefined,
    false,
    undefined,
    0x00a,
    0,
    true,
);
define_npc_type_data(
    NpcType.MaleFat,
    "Male Fat",
    "Male Fat",
    "Male Fat",
    undefined,
    false,
    undefined,
    0x00b,
    0,
    true,
);
define_npc_type_data(
    NpcType.MaleMacho,
    "Male Macho",
    "Male Macho",
    "Male Macho",
    undefined,
    false,
    undefined,
    0x00c,
    0,
    true,
);
define_npc_type_data(
    NpcType.MaleOld,
    "Male Old",
    "Male Old",
    "Male Old",
    undefined,
    false,
    undefined,
    0x00d,
    0,
    true,
);
define_npc_type_data(
    NpcType.BlueSoldier,
    "Blue Soldier",
    "Blue Soldier",
    "Blue Soldier",
    undefined,
    false,
    undefined,
    0x019,
    0,
    true,
);
define_npc_type_data(
    NpcType.RedSoldier,
    "Red Soldier",
    "Red Soldier",
    "Red Soldier",
    undefined,
    false,
    undefined,
    0x01a,
    0,
    true,
);
define_npc_type_data(
    NpcType.Principal,
    "Principal",
    "Principal",
    "Principal",
    undefined,
    false,
    undefined,
    0x01b,
    0,
    true,
);
define_npc_type_data(
    NpcType.Tekker,
    "Tekker",
    "Tekker",
    "Tekker",
    undefined,
    false,
    undefined,
    0x01c,
    0,
    true,
);
define_npc_type_data(
    NpcType.GuildLady,
    "Guild Lady",
    "Guild Lady",
    "Guild Lady",
    undefined,
    false,
    undefined,
    0x01d,
    0,
    true,
);
define_npc_type_data(
    NpcType.Scientist,
    "Scientist",
    "Scientist",
    "Scientist",
    undefined,
    false,
    undefined,
    0x01e,
    0,
    true,
);
define_npc_type_data(
    NpcType.Nurse,
    "Nurse",
    "Nurse",
    "Nurse",
    undefined,
    false,
    undefined,
    0x01f,
    0,
    true,
);
define_npc_type_data(
    NpcType.Irene,
    "Irene",
    "Irene",
    "Irene",
    undefined,
    false,
    undefined,
    0x020,
    0,
    true,
);
define_npc_type_data(
    NpcType.ItemShop,
    "Item Shop",
    "Item Shop",
    "Item Shop",
    undefined,
    false,
    undefined,
    0x0f1,
    0,
    true,
);
define_npc_type_data(
    NpcType.Nurse2,
    "Nurse (Ep. II)",
    "Nurse",
    "Nurse",
    2,
    false,
    undefined,
    0x0fe,
    0,
    true,
);

//
// Enemy NPCs
//

// Episode I Forest

define_npc_type_data(
    NpcType.Hildebear,
    "Hildebear",
    "Hildebear",
    "Hildelt",
    1,
    true,
    NpcType.Hildeblue,
    0x040,
    0,
    true,
);
define_npc_type_data(
    NpcType.Hildeblue,
    "Hildeblue",
    "Hildeblue",
    "Hildetorr",
    1,
    true,
    undefined,
    0x040,
    1,
    true,
);
define_npc_type_data(
    NpcType.RagRappy,
    "Rag Rappy",
    "Rag Rappy",
    "El Rappy",
    1,
    true,
    NpcType.AlRappy,
    0x041,
    0,
    true,
);
define_npc_type_data(
    NpcType.AlRappy,
    "Al Rappy",
    "Al Rappy",
    "Pal Rappy",
    1,
    true,
    undefined,
    0x041,
    1,
    true,
);
define_npc_type_data(
    NpcType.Monest,
    "Monest",
    "Monest",
    "Mothvist",
    1,
    true,
    undefined,
    0x042,
    0,
    true,
);
define_npc_type_data(
    NpcType.Mothmant,
    "Mothmant",
    "Mothmant",
    "Mothvert",
    1,
    true,
    undefined,
    undefined,
    undefined,
    undefined,
);
define_npc_type_data(
    NpcType.SavageWolf,
    "Savage Wolf",
    "Savage Wolf",
    "Gulgus",
    1,
    true,
    undefined,
    0x043,
    0,
    true,
);
define_npc_type_data(
    NpcType.BarbarousWolf,
    "Barbarous Wolf",
    "Barbarous Wolf",
    "Gulgus-Gue",
    1,
    true,
    undefined,
    0x043,
    0,
    false,
);
define_npc_type_data(NpcType.Booma, "Booma", "Booma", "Bartle", 1, true, undefined, 0x044, 0, true);
define_npc_type_data(
    NpcType.Gobooma,
    "Gobooma",
    "Gobooma",
    "Barble",
    1,
    true,
    undefined,
    0x044,
    1,
    true,
);
define_npc_type_data(
    NpcType.Gigobooma,
    "Gigobooma",
    "Gigobooma",
    "Tollaw",
    1,
    true,
    undefined,
    0x044,
    2,
    true,
);
define_npc_type_data(
    NpcType.Dragon,
    "Dragon",
    "Dragon",
    "Sil Dragon",
    1,
    true,
    undefined,
    0x0c0,
    0,
    true,
);

// Episode I Caves

define_npc_type_data(
    NpcType.GrassAssassin,
    "Grass Assassin",
    "Grass Assassin",
    "Crimson Assassin",
    1,
    true,
    undefined,
    0x060,
    0,
    true,
);
define_npc_type_data(
    NpcType.PoisonLily,
    "Poison Lily",
    "Poison Lily",
    "Ob Lily",
    1,
    true,
    NpcType.NarLily,
    0x061,
    0,
    true,
);
define_npc_type_data(
    NpcType.NarLily,
    "Nar Lily",
    "Nar Lily",
    "Mil Lily",
    1,
    true,
    undefined,
    0x061,
    1,
    true,
);
define_npc_type_data(
    NpcType.NanoDragon,
    "Nano Dragon",
    "Nano Dragon",
    "Nano Dragon",
    1,
    true,
    undefined,
    0x062,
    0,
    true,
);
define_npc_type_data(
    NpcType.EvilShark,
    "Evil Shark",
    "Evil Shark",
    "Vulmer",
    1,
    true,
    undefined,
    0x063,
    0,
    true,
);
define_npc_type_data(
    NpcType.PalShark,
    "Pal Shark",
    "Pal Shark",
    "Govulmer",
    1,
    true,
    undefined,
    0x063,
    1,
    true,
);
define_npc_type_data(
    NpcType.GuilShark,
    "Guil Shark",
    "Guil Shark",
    "Melqueek",
    1,
    true,
    undefined,
    0x063,
    2,
    true,
);
define_npc_type_data(
    NpcType.PofuillySlime,
    "Pofuilly Slime",
    "Pofuilly Slime",
    "Pofuilly Slime",
    1,
    true,
    NpcType.PouillySlime,
    0x064,
    0,
    true,
);
define_npc_type_data(
    NpcType.PouillySlime,
    "Pouilly Slime",
    "Pouilly Slime",
    "Pouilly Slime",
    1,
    true,
    undefined,
    0x064,
    0,
    false,
);
define_npc_type_data(
    NpcType.PanArms,
    "Pan Arms",
    "Pan Arms",
    "Pan Arms",
    1,
    true,
    undefined,
    0x065,
    0,
    true,
);
define_npc_type_data(
    NpcType.Migium,
    "Migium",
    "Migium",
    "Migium",
    1,
    true,
    undefined,
    undefined,
    undefined,
    undefined,
);
define_npc_type_data(
    NpcType.Hidoom,
    "Hidoom",
    "Hidoom",
    "Hidoom",
    1,
    true,
    undefined,
    undefined,
    undefined,
    undefined,
);
define_npc_type_data(
    NpcType.DeRolLe,
    "De Rol Le",
    "De Rol Le",
    "Dal Ra Lie",
    1,
    true,
    undefined,
    0x0c1,
    0,
    true,
);

// Episode I Mines

define_npc_type_data(
    NpcType.Dubchic,
    "Dubchic",
    "Dubchic",
    "Dubchich",
    1,
    true,
    undefined,
    0x080,
    0,
    true,
);
define_npc_type_data(
    NpcType.Gilchic,
    "Gilchic",
    "Gilchic",
    "Gilchich",
    1,
    true,
    undefined,
    0x080,
    1,
    true,
);
define_npc_type_data(
    NpcType.Garanz,
    "Garanz",
    "Garanz",
    "Baranz",
    1,
    true,
    undefined,
    0x081,
    0,
    true,
);
define_npc_type_data(
    NpcType.SinowBeat,
    "Sinow Beat",
    "Sinow Beat",
    "Sinow Blue",
    1,
    true,
    undefined,
    0x082,
    0,
    true,
);
define_npc_type_data(
    NpcType.SinowGold,
    "Sinow Gold",
    "Sinow Gold",
    "Sinow Red",
    1,
    true,
    undefined,
    0x082,
    0,
    false,
);
define_npc_type_data(
    NpcType.Canadine,
    "Canadine",
    "Canadine",
    "Canabin",
    1,
    true,
    undefined,
    0x083,
    0,
    true,
);
define_npc_type_data(
    NpcType.Canane,
    "Canane",
    "Canane",
    "Canune",
    1,
    true,
    undefined,
    0x084,
    0,
    true,
);
define_npc_type_data(
    NpcType.Dubswitch,
    "Dubswitch",
    "Dubswitch",
    "Dubswitch",
    1,
    true,
    undefined,
    0x085,
    0,
    true,
);
define_npc_type_data(
    NpcType.VolOpt,
    "Vol Opt",
    "Vol Opt",
    "Vol Opt ver.2",
    1,
    true,
    undefined,
    0x0c5,
    0,
    true,
);

// Episode I Ruins

define_npc_type_data(
    NpcType.Delsaber,
    "Delsaber",
    "Delsaber",
    "Delsaber",
    1,
    true,
    undefined,
    0x0a0,
    0,
    true,
);
define_npc_type_data(
    NpcType.ChaosSorcerer,
    "Chaos Sorcerer",
    "Chaos Sorcerer",
    "Gran Sorcerer",
    1,
    true,
    undefined,
    0x0a1,
    0,
    true,
);
define_npc_type_data(
    NpcType.DarkGunner,
    "Dark Gunner",
    "Dark Gunner",
    "Dark Gunner",
    1,
    true,
    undefined,
    0x0a2,
    0,
    true,
);
define_npc_type_data(
    NpcType.DeathGunner,
    "Death Gunner",
    "Death Gunner",
    "Death Gunner",
    1,
    true,
    undefined,
    undefined,
    undefined,
    undefined,
);
define_npc_type_data(
    NpcType.ChaosBringer,
    "Chaos Bringer",
    "Chaos Bringer",
    "Dark Bringer",
    1,
    true,
    undefined,
    0x0a4,
    0,
    true,
);
define_npc_type_data(
    NpcType.DarkBelra,
    "Dark Belra",
    "Dark Belra",
    "Indi Belra",
    1,
    true,
    undefined,
    0x0a5,
    0,
    true,
);
define_npc_type_data(
    NpcType.Dimenian,
    "Dimenian",
    "Dimenian",
    "Arlan",
    1,
    true,
    undefined,
    0x0a6,
    0,
    true,
);
define_npc_type_data(
    NpcType.LaDimenian,
    "La Dimenian",
    "La Dimenian",
    "Merlan",
    1,
    true,
    undefined,
    0x0a6,
    1,
    true,
);
define_npc_type_data(
    NpcType.SoDimenian,
    "So Dimenian",
    "So Dimenian",
    "Del-D",
    1,
    true,
    undefined,
    0x0a6,
    2,
    true,
);
define_npc_type_data(
    NpcType.Bulclaw,
    "Bulclaw",
    "Bulclaw",
    "Bulclaw",
    1,
    true,
    undefined,
    0x0a7,
    0,
    true,
);
define_npc_type_data(
    NpcType.Bulk,
    "Bulk",
    "Bulk",
    "Bulk",
    1,
    true,
    undefined,
    undefined,
    undefined,
    undefined,
);
define_npc_type_data(NpcType.Claw, "Claw", "Claw", "Claw", 1, true, undefined, 0x0a8, 0, true);
define_npc_type_data(
    NpcType.DarkFalz,
    "Dark Falz",
    "Dark Falz",
    "Dark Falz",
    1,
    true,
    undefined,
    0x0c8,
    0,
    true,
);

// Episode II VR Temple

define_npc_type_data(
    NpcType.Hildebear2,
    "Hildebear (Ep. II)",
    "Hildebear",
    "Hildelt",
    2,
    true,
    NpcType.Hildeblue2,
    0x040,
    0,
    true,
);
define_npc_type_data(
    NpcType.Hildeblue2,
    "Hildeblue (Ep. II)",
    "Hildeblue",
    "Hildetorr",
    2,
    true,
    undefined,
    0x040,
    1,
    true,
);
define_npc_type_data(
    NpcType.RagRappy2,
    "Rag Rappy (Ep. II)",
    "Rag Rappy",
    "El Rappy",
    2,
    true,
    NpcType.LoveRappy,
    0x041,
    0,
    true,
);
define_npc_type_data(
    NpcType.LoveRappy,
    "Love Rappy",
    "Love Rappy",
    "Love Rappy",
    2,
    true,
    undefined,
    0x041,
    1,
    true,
);
define_npc_type_data(
    NpcType.StRappy,
    "St. Rappy",
    "St. Rappy",
    "St. Rappy",
    2,
    true,
    undefined,
    undefined,
    undefined,
    undefined,
);
define_npc_type_data(
    NpcType.HalloRappy,
    "Hallo Rappy",
    "Hallo Rappy",
    "Hallo Rappy",
    2,
    true,
    undefined,
    undefined,
    undefined,
    undefined,
);
define_npc_type_data(
    NpcType.EggRappy,
    "Egg Rappy",
    "Egg Rappy",
    "Egg Rappy",
    2,
    true,
    undefined,
    undefined,
    undefined,
    undefined,
);
define_npc_type_data(
    NpcType.Monest2,
    "Monest (Ep. II)",
    "Monest",
    "Mothvist",
    2,
    true,
    undefined,
    0x042,
    0,
    true,
);
define_npc_type_data(
    NpcType.Mothmant2,
    "Mothmant",
    "Mothmant",
    "Mothvert",
    2,
    true,
    undefined,
    undefined,
    undefined,
    undefined,
);
define_npc_type_data(
    NpcType.PoisonLily2,
    "Poison Lily (Ep. II)",
    "Poison Lily",
    "Ob Lily",
    2,
    true,
    NpcType.NarLily2,
    0x061,
    0,
    true,
);
define_npc_type_data(
    NpcType.NarLily2,
    "Nar Lily (Ep. II)",
    "Nar Lily",
    "Mil Lily",
    2,
    true,
    undefined,
    0x061,
    1,
    true,
);
define_npc_type_data(
    NpcType.GrassAssassin2,
    "Grass Assassin (Ep. II)",
    "Grass Assassin",
    "Crimson Assassin",
    2,
    true,
    undefined,
    0x060,
    0,
    true,
);
define_npc_type_data(
    NpcType.Dimenian2,
    "Dimenian (Ep. II)",
    "Dimenian",
    "Arlan",
    2,
    true,
    undefined,
    0x0a6,
    0,
    true,
);
define_npc_type_data(
    NpcType.LaDimenian2,
    "La Dimenian (Ep. II)",
    "La Dimenian",
    "Merlan",
    2,
    true,
    undefined,
    0x0a6,
    1,
    true,
);
define_npc_type_data(
    NpcType.SoDimenian2,
    "So Dimenian (Ep. II)",
    "So Dimenian",
    "Del-D",
    2,
    true,
    undefined,
    0x0a6,
    2,
    true,
);
define_npc_type_data(
    NpcType.DarkBelra2,
    "Dark Belra (Ep. II)",
    "Dark Belra",
    "Indi Belra",
    2,
    true,
    undefined,
    0x0a5,
    0,
    true,
);
define_npc_type_data(
    NpcType.BarbaRay,
    "Barba Ray",
    "Barba Ray",
    "Barba Ray",
    2,
    true,
    undefined,
    0x0cb,
    0,
    true,
);

// Episode II VR Spaceship

define_npc_type_data(
    NpcType.SavageWolf2,
    "Savage Wolf (Ep. II)",
    "Savage Wolf",
    "Gulgus",
    2,
    true,
    undefined,
    0x043,
    0,
    true,
);
define_npc_type_data(
    NpcType.BarbarousWolf2,
    "Barbarous Wolf (Ep. II)",
    "Barbarous Wolf",
    "Gulgus-Gue",
    2,
    true,
    undefined,
    0x043,
    0,
    false,
);
define_npc_type_data(
    NpcType.PanArms2,
    "Pan Arms (Ep. II)",
    "Pan Arms",
    "Pan Arms",
    2,
    true,
    undefined,
    0x065,
    0,
    true,
);
define_npc_type_data(
    NpcType.Migium2,
    "Migium (Ep. II)",
    "Migium",
    "Migium",
    2,
    true,
    undefined,
    undefined,
    undefined,
    undefined,
);
define_npc_type_data(
    NpcType.Hidoom2,
    "Hidoom (Ep. II)",
    "Hidoom",
    "Hidoom",
    2,
    true,
    undefined,
    undefined,
    undefined,
    undefined,
);
define_npc_type_data(
    NpcType.Dubchic2,
    "Dubchic (Ep. II)",
    "Dubchic",
    "Dubchich",
    2,
    true,
    undefined,
    0x080,
    0,
    true,
);
define_npc_type_data(
    NpcType.Gilchic2,
    "Gilchic (Ep. II)",
    "Gilchic",
    "Gilchich",
    2,
    true,
    undefined,
    0x080,
    1,
    true,
);
define_npc_type_data(
    NpcType.Garanz2,
    "Garanz (Ep. II)",
    "Garanz",
    "Baranz",
    2,
    true,
    undefined,
    0x081,
    0,
    true,
);
define_npc_type_data(
    NpcType.Dubswitch2,
    "Dubswitch (Ep. II)",
    "Dubswitch",
    "Dubswitch",
    2,
    true,
    undefined,
    0x085,
    0,
    true,
);
define_npc_type_data(
    NpcType.Delsaber2,
    "Delsaber (Ep. II)",
    "Delsaber",
    "Delsaber",
    2,
    true,
    undefined,
    0x0a0,
    0,
    true,
);
define_npc_type_data(
    NpcType.ChaosSorcerer2,
    "Chaos Sorcerer (Ep. II)",
    "Chaos Sorcerer",
    "Gran Sorcerer",
    2,
    true,
    undefined,
    0x0a1,
    0,
    true,
);
define_npc_type_data(
    NpcType.GolDragon,
    "Gol Dragon",
    "Gol Dragon",
    "Gol Dragon",
    2,
    true,
    undefined,
    0x0cc,
    0,
    true,
);

// Episode II Central Control Area

define_npc_type_data(
    NpcType.SinowBerill,
    "Sinow Berill",
    "Sinow Berill",
    "Sinow Berill",
    2,
    true,
    undefined,
    0x0d4,
    0,
    true,
);
define_npc_type_data(
    NpcType.SinowSpigell,
    "Sinow Spigell",
    "Sinow Spigell",
    "Sinow Spigell",
    2,
    true,
    undefined,
    0x0d4,
    1,
    true,
);
define_npc_type_data(
    NpcType.Merillia,
    "Merillia",
    "Merillia",
    "Merillia",
    2,
    true,
    undefined,
    0x0d5,
    0,
    true,
);
define_npc_type_data(
    NpcType.Meriltas,
    "Meriltas",
    "Meriltas",
    "Meriltas",
    2,
    true,
    undefined,
    0x0d5,
    1,
    true,
);
define_npc_type_data(
    NpcType.Mericarol,
    "Mericarol",
    "Mericarol",
    "Mericarol",
    2,
    true,
    undefined,
    0x0d6,
    0,
    true,
);
define_npc_type_data(
    NpcType.Mericus,
    "Mericus",
    "Mericus",
    "Mericus",
    2,
    true,
    undefined,
    0x0d6,
    1,
    true,
);
define_npc_type_data(
    NpcType.Merikle,
    "Merikle",
    "Merikle",
    "Merikle",
    2,
    true,
    undefined,
    0x0d6,
    2,
    true,
);
define_npc_type_data(
    NpcType.UlGibbon,
    "Ul Gibbon",
    "Ul Gibbon",
    "Ul Gibbon",
    2,
    true,
    undefined,
    0x0d7,
    0,
    true,
);
define_npc_type_data(
    NpcType.ZolGibbon,
    "Zol Gibbon",
    "Zol Gibbon",
    "Zol Gibbon",
    2,
    true,
    undefined,
    0x0d7,
    1,
    true,
);
define_npc_type_data(
    NpcType.Gibbles,
    "Gibbles",
    "Gibbles",
    "Gibbles",
    2,
    true,
    undefined,
    0x0d8,
    0,
    true,
);
define_npc_type_data(NpcType.Gee, "Gee", "Gee", "Gee", 2, true, undefined, 0x0d9, 0, true);
define_npc_type_data(
    NpcType.GiGue,
    "Gi Gue",
    "Gi Gue",
    "Gi Gue",
    2,
    true,
    undefined,
    0x0da,
    0,
    true,
);
define_npc_type_data(
    NpcType.IllGill,
    "Ill Gill",
    "Ill Gill",
    "Ill Gill",
    2,
    true,
    undefined,
    0x0e1,
    0,
    true,
);
define_npc_type_data(
    NpcType.DelLily,
    "Del Lily",
    "Del Lily",
    "Del Lily",
    2,
    true,
    undefined,
    0x061,
    0,
    true,
);
define_npc_type_data(
    NpcType.Epsilon,
    "Epsilon",
    "Epsilon",
    "Epsilon",
    2,
    true,
    undefined,
    0x0e0,
    0,
    true,
);
define_npc_type_data(
    NpcType.GalGryphon,
    "Gal Gryphon",
    "Gal Gryphon",
    "Gal Gryphon",
    2,
    true,
    undefined,
    0x0c0,
    0,
    true,
);

// Episode II Seabed

define_npc_type_data(
    NpcType.Deldepth,
    "Deldepth",
    "Deldepth",
    "Deldepth",
    2,
    true,
    undefined,
    0x0db,
    0,
    true,
);
define_npc_type_data(
    NpcType.Delbiter,
    "Delbiter",
    "Delbiter",
    "Delbiter",
    2,
    true,
    undefined,
    0x0dc,
    0,
    true,
);
define_npc_type_data(
    NpcType.Dolmolm,
    "Dolmolm",
    "Dolmolm",
    "Dolmolm",
    2,
    true,
    undefined,
    0x0dd,
    0,
    true,
);
define_npc_type_data(
    NpcType.Dolmdarl,
    "Dolmdarl",
    "Dolmdarl",
    "Dolmdarl",
    2,
    true,
    undefined,
    0x0dd,
    1,
    true,
);
define_npc_type_data(
    NpcType.Morfos,
    "Morfos",
    "Morfos",
    "Morfos",
    2,
    true,
    undefined,
    0x0de,
    0,
    true,
);
define_npc_type_data(
    NpcType.Recobox,
    "Recobox",
    "Recobox",
    "Recobox",
    2,
    true,
    undefined,
    0x0df,
    0,
    true,
);
define_npc_type_data(
    NpcType.Recon,
    "Recon",
    "Recon",
    "Recon",
    2,
    true,
    undefined,
    undefined,
    undefined,
    undefined,
);
define_npc_type_data(
    NpcType.SinowZoa,
    "Sinow Zoa",
    "Sinow Zoa",
    "Sinow Zoa",
    2,
    true,
    undefined,
    0x0e0,
    0,
    true,
);
define_npc_type_data(
    NpcType.SinowZele,
    "Sinow Zele",
    "Sinow Zele",
    "Sinow Zele",
    2,
    true,
    undefined,
    0x0e0,
    1,
    true,
);
define_npc_type_data(
    NpcType.OlgaFlow,
    "Olga Flow",
    "Olga Flow",
    "Olga Flow",
    2,
    true,
    undefined,
    0x0ca,
    0,
    true,
);

// Episode IV

define_npc_type_data(
    NpcType.SandRappy,
    "Sand Rappy",
    "Sand Rappy",
    "Sand Rappy",
    4,
    true,
    NpcType.DelRappy,
    0x041,
    0,
    true,
);
define_npc_type_data(
    NpcType.DelRappy,
    "Del Rappy",
    "Del Rappy",
    "Del Rappy",
    4,
    true,
    undefined,
    0x041,
    1,
    true,
);
define_npc_type_data(
    NpcType.Astark,
    "Astark",
    "Astark",
    "Astark",
    4,
    true,
    undefined,
    0x110,
    0,
    true,
);
define_npc_type_data(
    NpcType.SatelliteLizard,
    "Satellite Lizard",
    "Satellite Lizard",
    "Satellite Lizard",
    4,
    true,
    undefined,
    0x111,
    0,
    true,
);
define_npc_type_data(NpcType.Yowie, "Yowie", "Yowie", "Yowie", 4, true, undefined, 0x111, 0, false);
define_npc_type_data(
    NpcType.MerissaA,
    "Merissa A",
    "Merissa A",
    "Merissa A",
    4,
    true,
    NpcType.MerissaAA,
    0x112,
    0,
    true,
);
define_npc_type_data(
    NpcType.MerissaAA,
    "Merissa AA",
    "Merissa AA",
    "Merissa AA",
    4,
    true,
    undefined,
    0x112,
    1,
    true,
);
define_npc_type_data(
    NpcType.Girtablulu,
    "Girtablulu",
    "Girtablulu",
    "Girtablulu",
    4,
    true,
    undefined,
    0x113,
    0,
    true,
);
define_npc_type_data(NpcType.Zu, "Zu", "Zu", "Zu", 4, true, NpcType.Pazuzu, 0x114, 0, true);
define_npc_type_data(
    NpcType.Pazuzu,
    "Pazuzu",
    "Pazuzu",
    "Pazuzu",
    4,
    true,
    undefined,
    0x114,
    1,
    true,
);
define_npc_type_data(NpcType.Boota, "Boota", "Boota", "Boota", 4, true, undefined, 0x115, 0, true);
define_npc_type_data(
    NpcType.ZeBoota,
    "Ze Boota",
    "Ze Boota",
    "Ze Boota",
    4,
    true,
    undefined,
    0x115,
    1,
    true,
);
define_npc_type_data(
    NpcType.BaBoota,
    "Ba Boota",
    "Ba Boota",
    "Ba Boota",
    4,
    true,
    undefined,
    0x115,
    2,
    true,
);
define_npc_type_data(
    NpcType.Dorphon,
    "Dorphon",
    "Dorphon",
    "Dorphon",
    4,
    true,
    NpcType.DorphonEclair,
    0x116,
    0,
    true,
);
define_npc_type_data(
    NpcType.DorphonEclair,
    "Dorphon Eclair",
    "Dorphon Eclair",
    "Dorphon Eclair",
    4,
    true,
    undefined,
    0x116,
    1,
    true,
);
define_npc_type_data(NpcType.Goran, "Goran", "Goran", "Goran", 4, true, undefined, 0x117, 0, true);
define_npc_type_data(
    NpcType.PyroGoran,
    "Pyro Goran",
    "Pyro Goran",
    "Pyro Goran",
    4,
    true,
    undefined,
    0x117,
    1,
    true,
);
define_npc_type_data(
    NpcType.GoranDetonator,
    "Goran Detonator",
    "Goran Detonator",
    "Goran Detonator",
    4,
    true,
    undefined,
    0x117,
    2,
    true,
);
define_npc_type_data(
    NpcType.SaintMilion,
    "Saint-Milion",
    "Saint-Milion",
    "Saint-Milion",
    4,
    true,
    NpcType.Kondrieu,
    0x119,
    0,
    true,
);
define_npc_type_data(
    NpcType.Shambertin,
    "Shambertin",
    "Shambertin",
    "Shambertin",
    4,
    true,
    NpcType.Kondrieu,
    0x119,
    1,
    true,
);
define_npc_type_data(
    NpcType.Kondrieu,
    "Kondrieu",
    "Kondrieu",
    "Kondrieu",
    4,
    true,
    undefined,
    0x119,
    0,
    false,
);

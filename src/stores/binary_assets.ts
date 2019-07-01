import { NpcType, ObjectType } from '../domain';

export function get_area_render_data(
    episode: number,
    area_id: number,
    area_version: number
): Promise<ArrayBuffer> {
    return get_area_asset(episode, area_id, area_version, 'render');
}

export function get_area_collision_data(
    episode: number,
    area_id: number,
    area_version: number
): Promise<ArrayBuffer> {
    return get_area_asset(episode, area_id, area_version, 'collision');
}

export async function get_npc_data(npc_type: NpcType): Promise<{ url: string, data: ArrayBuffer }> {
    const url = npc_type_to_url(npc_type);
    const data = await get_asset(url);
    return { url, data };
}

export async function get_object_data(object_type: ObjectType): Promise<{ url: string, data: ArrayBuffer }> {
    const url = object_type_to_url(object_type);
    const data = await get_asset(url);
    return { url, data };
}

export async function get_player_data(
    player_class: string,
    body_part: string,
    no?: number
): Promise<ArrayBuffer> {
    return await get_asset(player_class_to_url(player_class, body_part, no));
}

function get_asset(url: string): Promise<ArrayBuffer> {
    const base_url = process.env.PUBLIC_URL;
    const promise = fetch(base_url + url).then(r => r.arrayBuffer());
    return promise;
}

const area_base_names = [
    [
        ['city00_00', 1],
        ['forest01', 1],
        ['forest02', 1],
        ['cave01_', 6],
        ['cave02_', 5],
        ['cave03_', 6],
        ['machine01_', 6],
        ['machine02_', 6],
        ['ancient01_', 5],
        ['ancient02_', 5],
        ['ancient03_', 5],
        ['boss01', 1],
        ['boss02', 1],
        ['boss03', 1],
        ['darkfalz00', 1]
    ],
    [
        ['labo00_00', 1],
        ['ruins01_', 3],
        ['ruins02_', 3],
        ['space01_', 3],
        ['space02_', 3],
        ['jungle01_00', 1],
        ['jungle02_00', 1],
        ['jungle03_00', 1],
        ['jungle04_', 3],
        ['jungle05_00', 1],
        ['seabed01_', 3],
        ['seabed02_', 3],
        ['boss05', 1],
        ['boss06', 1],
        ['boss07', 1],
        ['boss08', 1],
        ['jungle06_00', 1],
        ['jungle07_', 5]
    ],
    [
        // Don't remove this empty array, see usage of areaBaseNames in areaVersionToBaseUrl.
    ],
    [
        ['city02_00', 1],
        ['wilds01_00', 1],
        ['wilds01_01', 1],
        ['wilds01_02', 1],
        ['wilds01_03', 1],
        ['crater01_00', 1],
        ['desert01_', 3],
        ['desert02_', 3],
        ['desert03_', 3],
        ['boss09_00', 1]
    ]
];

function area_version_to_base_url(
    episode: number,
    area_id: number,
    area_variant: number
): string {
    const episode_base_names = area_base_names[episode - 1];

    if (0 <= area_id && area_id < episode_base_names.length) {
        const [base_name, variants] = episode_base_names[area_id];

        if (0 <= area_variant && area_variant < variants) {
            let variant: string;

            if (variants === 1) {
                variant = '';
            } else {
                variant = String(area_variant);
                while (variant.length < 2) variant = '0' + variant;
            }

            return `/maps/map_${base_name}${variant}`;
        } else {
            throw new Error(`Unknown variant ${area_variant} of area ${area_id} in episode ${episode}.`);
        }
    } else {
        throw new Error(`Unknown episode ${episode} area ${area_id}.`);
    }
}

type AreaAssetType = 'render' | 'collision';

function get_area_asset(
    episode: number,
    area_id: number,
    area_variant: number,
    type: AreaAssetType
): Promise<ArrayBuffer> {
    try {
        const base_url = area_version_to_base_url(episode, area_id, area_variant);
        const suffix = type === 'render' ? 'n.rel' : 'c.rel';
        return get_asset(base_url + suffix);
    } catch (e) {
        return Promise.reject(e);
    }
}

function npc_type_to_url(npc_type: NpcType): string {
    switch (npc_type) {
        // The dubswitch model is in XJ format.
        case NpcType.Dubswitch: return `/npcs/${npc_type.code}.xj`;

        // Episode II VR Temple

        case NpcType.Hildebear2: return npc_type_to_url(NpcType.Hildebear);
        case NpcType.Hildeblue2: return npc_type_to_url(NpcType.Hildeblue);
        case NpcType.RagRappy2: return npc_type_to_url(NpcType.RagRappy);
        case NpcType.Monest2: return npc_type_to_url(NpcType.Monest);
        case NpcType.PoisonLily2: return npc_type_to_url(NpcType.PoisonLily);
        case NpcType.NarLily2: return npc_type_to_url(NpcType.NarLily);
        case NpcType.GrassAssassin2: return npc_type_to_url(NpcType.GrassAssassin);
        case NpcType.Dimenian2: return npc_type_to_url(NpcType.Dimenian);
        case NpcType.LaDimenian2: return npc_type_to_url(NpcType.LaDimenian);
        case NpcType.SoDimenian2: return npc_type_to_url(NpcType.SoDimenian);
        case NpcType.DarkBelra2: return npc_type_to_url(NpcType.DarkBelra);

        // Episode II VR Spaceship

        case NpcType.SavageWolf2: return npc_type_to_url(NpcType.SavageWolf);
        case NpcType.BarbarousWolf2: return npc_type_to_url(NpcType.BarbarousWolf);
        case NpcType.PanArms2: return npc_type_to_url(NpcType.PanArms);
        case NpcType.Dubchic2: return npc_type_to_url(NpcType.Dubchic);
        case NpcType.Gilchic2: return npc_type_to_url(NpcType.Gilchic);
        case NpcType.Garanz2: return npc_type_to_url(NpcType.Garanz);
        case NpcType.Dubswitch2: return npc_type_to_url(NpcType.Dubswitch);
        case NpcType.Delsaber2: return npc_type_to_url(NpcType.Delsaber);
        case NpcType.ChaosSorcerer2: return npc_type_to_url(NpcType.ChaosSorcerer);

        default: return `/npcs/${npc_type.code}.nj`;
    }
}

function object_type_to_url(object_type: ObjectType): string {
    switch (object_type) {
        case ObjectType.EasterEgg:
        case ObjectType.ChristmasTree:
        case ObjectType.ChristmasWreath:
        case ObjectType.TwentyFirstCentury:
        case ObjectType.Sonic:
        case ObjectType.WelcomeBoard:
        case ObjectType.FloatingJelifish:
        case ObjectType.RuinsSeal:
        case ObjectType.Dolphin:
        case ObjectType.Cacti:
        case ObjectType.BigBrownRock:
        case ObjectType.PoisonPlant:
        case ObjectType.BigBlackRocks:
        case ObjectType.FallingRock:
        case ObjectType.DesertFixedTypeBoxBreakableCrystals:
        case ObjectType.BeeHive:
            return `/objects/${object_type.pso_id}.nj`;

        default:
            return `/objects/${object_type.pso_id}.xj`;
    }
}

function player_class_to_url(player_class: string, body_part: string, no?: number): string {
    return `/player/${player_class}${body_part}${no == null ? '' : no}.nj`;
}

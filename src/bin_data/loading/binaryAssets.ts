import { NpcType, ObjectType } from '../../domain';

export function getAreaRenderData(
    episode: number,
    areaId: number,
    areaVersion: number
): Promise<ArrayBuffer> {
    return getAreaAsset(episode, areaId, areaVersion, 'render');
}

export function getAreaCollisionData(
    episode: number,
    areaId: number,
    areaVersion: number
): Promise<ArrayBuffer> {
    return getAreaAsset(episode, areaId, areaVersion, 'collision');
}

export async function getNpcData(npcType: NpcType): Promise<{ url: string, data: ArrayBuffer }> {
    const url = npcTypeToUrl(npcType);
    const data = await getAsset(url);
    return ({ url, data });
}

export async function getObjectData(objectType: ObjectType): Promise<{ url: string, data: ArrayBuffer }> {
    const url = objectTypeToUrl(objectType);
    const data = await getAsset(url);
    return ({ url, data });
}

/**
 * Cache for the binary data.
 */
const bufferCache: Map<string, Promise<ArrayBuffer>> = new Map();

function getAsset(url: string): Promise<ArrayBuffer> {
    const promise = bufferCache.get(url);

    if (promise) {
        return promise;
    } else {
        const baseUrl = process.env.PUBLIC_URL;
        const promise = fetch(baseUrl + url).then(r => r.arrayBuffer());
        bufferCache.set(url, promise);
        return promise;
    }
}

const areaBaseNames = [
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

function areaVersionToBaseUrl(
    episode: number,
    areaId: number,
    areaVariant: number
): string {
    const episodeBaseNames = areaBaseNames[episode - 1];

    if (0 <= areaId && areaId < episodeBaseNames.length) {
        const [baseName, variants] = episodeBaseNames[areaId];

        if (0 <= areaVariant && areaVariant < variants) {
            let variant: string;

            if (variants === 1) {
                variant = '';
            } else {
                variant = String(areaVariant);
                while (variant.length < 2) variant = '0' + variant;
            }

            return `/maps/map_${baseName}${variant}`;
        } else {
            throw new Error(`Unknown variant ${areaVariant} of area ${areaId} in episode ${episode}.`);
        }
    } else {
        throw new Error(`Unknown episode ${episode} area ${areaId}.`);
    }
}

type AreaAssetType = 'render' | 'collision';

function getAreaAsset(
    episode: number,
    areaId: number,
    areaVariant: number,
    type: AreaAssetType
): Promise<ArrayBuffer> {
    try {
        const baseUrl = areaVersionToBaseUrl(episode, areaId, areaVariant);
        const suffix = type === 'render' ? 'n.rel' : 'c.rel';
        return getAsset(baseUrl + suffix);
    } catch (e) {
        return Promise.reject(e);
    }
}

function npcTypeToUrl(npcType: NpcType): string {
    switch (npcType) {
        // The dubswitch model in in XJ format.
        case NpcType.Dubswitch: return `/npcs/${npcType.code}.xj`;

        // Episode II VR Temple

        case NpcType.Hildebear2: return npcTypeToUrl(NpcType.Hildebear);
        case NpcType.Hildeblue2: return npcTypeToUrl(NpcType.Hildeblue);
        case NpcType.RagRappy2: return npcTypeToUrl(NpcType.RagRappy);
        case NpcType.Monest2: return npcTypeToUrl(NpcType.Monest);
        case NpcType.PoisonLily2: return npcTypeToUrl(NpcType.PoisonLily);
        case NpcType.NarLily2: return npcTypeToUrl(NpcType.NarLily);
        case NpcType.GrassAssassin2: return npcTypeToUrl(NpcType.GrassAssassin);
        case NpcType.Dimenian2: return npcTypeToUrl(NpcType.Dimenian);
        case NpcType.LaDimenian2: return npcTypeToUrl(NpcType.LaDimenian);
        case NpcType.SoDimenian2: return npcTypeToUrl(NpcType.SoDimenian);
        case NpcType.DarkBelra2: return npcTypeToUrl(NpcType.DarkBelra);

        // Episode II VR Spaceship

        case NpcType.SavageWolf2: return npcTypeToUrl(NpcType.SavageWolf);
        case NpcType.BarbarousWolf2: return npcTypeToUrl(NpcType.BarbarousWolf);
        case NpcType.PanArms2: return npcTypeToUrl(NpcType.PanArms);
        case NpcType.Dubchic2: return npcTypeToUrl(NpcType.Dubchic);
        case NpcType.Gilchic2: return npcTypeToUrl(NpcType.Gilchic);
        case NpcType.Garanz2: return npcTypeToUrl(NpcType.Garanz);
        case NpcType.Dubswitch2: return npcTypeToUrl(NpcType.Dubswitch);
        case NpcType.Delsaber2: return npcTypeToUrl(NpcType.Delsaber);
        case NpcType.ChaosSorcerer2: return npcTypeToUrl(NpcType.ChaosSorcerer);

        default: return `/npcs/${npcType.code}.nj`;
    }
}

function objectTypeToUrl(objectType: ObjectType): string {
    switch (objectType) {
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
            return `/objects/${String(objectType.pso_id)}.nj`;

        default:
            return `/objects/${String(objectType.pso_id)}.xj`;
    }
}

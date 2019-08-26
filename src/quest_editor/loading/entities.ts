import { BufferGeometry, CylinderBufferGeometry, Texture } from "three";
import Logger from "js-logger";
import { LoadingCache } from "./LoadingCache";
import { Endianness } from "../../core/data_formats/Endianness";
import { ArrayBufferCursor } from "../../core/data_formats/cursor/ArrayBufferCursor";
import { ninja_object_to_buffer_geometry } from "../../core/rendering/conversion/ninja_geometry";
import { parse_nj, parse_xj } from "../../core/data_formats/parsing/ninja";
import { parse_xvm } from "../../core/data_formats/parsing/ninja/texture";
import { xvm_to_textures } from "../../core/rendering/conversion/ninja_textures";
import { load_array_buffer } from "../../core/loading";
import { object_data, ObjectType } from "../../core/data_formats/parsing/quest/object_types";
import { NpcType } from "../../core/data_formats/parsing/quest/npc_types";

const logger = Logger.get("loading/entities");

const DEFAULT_ENTITY = new CylinderBufferGeometry(3, 3, 20);
DEFAULT_ENTITY.translate(0, 10, 0);

const DEFAULT_ENTITY_PROMISE: Promise<BufferGeometry> = new Promise(resolve =>
    resolve(DEFAULT_ENTITY),
);

const DEFAULT_ENTITY_TEX: Texture[] = [];

const DEFAULT_ENTITY_TEX_PROMISE: Promise<Texture[]> = new Promise(resolve =>
    resolve(DEFAULT_ENTITY_TEX),
);

const npc_cache = new LoadingCache<NpcType, Promise<BufferGeometry>>();
npc_cache.set(NpcType.Unknown, DEFAULT_ENTITY_PROMISE);

const npc_tex_cache = new LoadingCache<NpcType, Promise<Texture[]>>();
npc_tex_cache.set(NpcType.Unknown, DEFAULT_ENTITY_TEX_PROMISE);

const object_cache = new LoadingCache<ObjectType, Promise<BufferGeometry>>();
const object_tex_cache = new LoadingCache<ObjectType, Promise<Texture[]>>();

for (const type of [
    ObjectType.Unknown,
    ObjectType.PlayerSet,
    ObjectType.FogCollision,
    ObjectType.EventCollision,
    ObjectType.ObjRoomID,
    ObjectType.ScriptCollision,
    ObjectType.ItemLight,
    ObjectType.FogCollisionSW,
    ObjectType.MenuActivation,
    ObjectType.BoxDetectObject,
    ObjectType.SymbolChatObject,
    ObjectType.TouchPlateObject,
    ObjectType.TargetableObject,
    ObjectType.EffectObject,
    ObjectType.CountDownObject,
    ObjectType.TelepipeLocation,
    ObjectType.Pioneer2InvisibleTouchplate,
    ObjectType.TempleMapDetect,
    ObjectType.LabInvisibleObject,
]) {
    object_cache.set(type, DEFAULT_ENTITY_PROMISE);
    object_tex_cache.set(type, DEFAULT_ENTITY_TEX_PROMISE);
}

export async function load_npc_geometry(npc_type: NpcType): Promise<BufferGeometry> {
    return npc_cache.get_or_set(npc_type, async () => {
        try {
            const { url, data } = await load_npc_data(npc_type, AssetType.Geometry);
            const cursor = new ArrayBufferCursor(data, Endianness.Little);
            const nj_objects = url.endsWith(".nj") ? parse_nj(cursor) : parse_xj(cursor);

            if (nj_objects.length) {
                return ninja_object_to_buffer_geometry(nj_objects[0]);
            } else {
                logger.warn(`Couldn't parse ${url} for ${NpcType[npc_type]}.`);
                return DEFAULT_ENTITY;
            }
        } catch (e) {
            logger.warn(`Couldn't load geometry file for ${NpcType[npc_type]}.`, e);
            return DEFAULT_ENTITY;
        }
    });
}

export async function load_npc_textures(npc_type: NpcType): Promise<Texture[]> {
    return npc_tex_cache.get_or_set(npc_type, async () => {
        try {
            const { data } = await load_npc_data(npc_type, AssetType.Texture);
            const cursor = new ArrayBufferCursor(data, Endianness.Little);
            const xvm = parse_xvm(cursor);
            return xvm_to_textures(xvm);
        } catch (e) {
            logger.warn(`Couldn't load texture file for ${NpcType[npc_type]}.`, e);
            return DEFAULT_ENTITY_TEX;
        }
    });
}

export async function load_object_geometry(object_type: ObjectType): Promise<BufferGeometry> {
    return object_cache.get_or_set(object_type, async () => {
        try {
            const { url, data } = await load_object_data(object_type, AssetType.Geometry);
            const cursor = new ArrayBufferCursor(data, Endianness.Little);
            const nj_objects = url.endsWith(".nj") ? parse_nj(cursor) : parse_xj(cursor);

            if (nj_objects.length) {
                return ninja_object_to_buffer_geometry(nj_objects[0]);
            } else {
                logger.warn(`Couldn't parse ${url} for ${ObjectType[object_type]}.`);
                return DEFAULT_ENTITY;
            }
        } catch (e) {
            logger.warn(`Couldn't load geometry file for ${ObjectType[object_type]}.`, e);
            return DEFAULT_ENTITY;
        }
    });
}

export async function load_object_textures(object_type: ObjectType): Promise<Texture[]> {
    return object_tex_cache.get_or_set(object_type, async () => {
        try {
            const { data } = await load_object_data(object_type, AssetType.Texture);
            const cursor = new ArrayBufferCursor(data, Endianness.Little);
            const xvm = parse_xvm(cursor);
            return xvm_to_textures(xvm);
        } catch (e) {
            logger.warn(`Couldn't load texture file for ${ObjectType[object_type]}.`, e);
            return DEFAULT_ENTITY_TEX;
        }
    });
}

export async function load_npc_data(
    npc_type: NpcType,
    type: AssetType,
): Promise<{ url: string; data: ArrayBuffer }> {
    const url = npc_type_to_url(npc_type, type);
    const data = await load_array_buffer(url);
    return { url, data };
}

export async function load_object_data(
    object_type: ObjectType,
    type: AssetType,
): Promise<{ url: string; data: ArrayBuffer }> {
    const url = object_type_to_url(object_type, type);
    const data = await load_array_buffer(url);
    return { url, data };
}

enum AssetType {
    Geometry,
    Texture,
}

function npc_type_to_url(npc_type: NpcType, type: AssetType): string {
    switch (npc_type) {
        // The dubswitch model is in XJ format.
        case NpcType.Dubswitch:
            return `/npcs/${NpcType[npc_type]}.${type === AssetType.Geometry ? "xj" : "xvm"}`;

        // Episode II VR Temple

        case NpcType.Hildebear2:
            return npc_type_to_url(NpcType.Hildebear, type);
        case NpcType.Hildeblue2:
            return npc_type_to_url(NpcType.Hildeblue, type);
        case NpcType.RagRappy2:
            return npc_type_to_url(NpcType.RagRappy, type);
        case NpcType.Monest2:
            return npc_type_to_url(NpcType.Monest, type);
        case NpcType.PoisonLily2:
            return npc_type_to_url(NpcType.PoisonLily, type);
        case NpcType.NarLily2:
            return npc_type_to_url(NpcType.NarLily, type);
        case NpcType.GrassAssassin2:
            return npc_type_to_url(NpcType.GrassAssassin, type);
        case NpcType.Dimenian2:
            return npc_type_to_url(NpcType.Dimenian, type);
        case NpcType.LaDimenian2:
            return npc_type_to_url(NpcType.LaDimenian, type);
        case NpcType.SoDimenian2:
            return npc_type_to_url(NpcType.SoDimenian, type);
        case NpcType.DarkBelra2:
            return npc_type_to_url(NpcType.DarkBelra, type);

        // Episode II VR Spaceship

        case NpcType.SavageWolf2:
            return npc_type_to_url(NpcType.SavageWolf, type);
        case NpcType.BarbarousWolf2:
            return npc_type_to_url(NpcType.BarbarousWolf, type);
        case NpcType.PanArms2:
            return npc_type_to_url(NpcType.PanArms, type);
        case NpcType.Dubchic2:
            return npc_type_to_url(NpcType.Dubchic, type);
        case NpcType.Gilchic2:
            return npc_type_to_url(NpcType.Gilchic, type);
        case NpcType.Garanz2:
            return npc_type_to_url(NpcType.Garanz, type);
        case NpcType.Dubswitch2:
            return npc_type_to_url(NpcType.Dubswitch, type);
        case NpcType.Delsaber2:
            return npc_type_to_url(NpcType.Delsaber, type);
        case NpcType.ChaosSorcerer2:
            return npc_type_to_url(NpcType.ChaosSorcerer, type);

        default:
            return `/npcs/${NpcType[npc_type]}.${type === AssetType.Geometry ? "nj" : "xvm"}`;
    }
}

function object_type_to_url(object_type: ObjectType, type: AssetType): string {
    if (type === AssetType.Geometry) {
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
                return `/objects/${object_data(object_type).pso_id}.nj`;

            default:
                return `/objects/${object_data(object_type).pso_id}.xj`;
        }
    } else {
        return `/objects/${object_data(object_type).pso_id}.xvm`;
    }
}

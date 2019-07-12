import Logger from "js-logger";
import { BufferGeometry, CylinderBufferGeometry, Texture } from "three";
import { parse_nj, parse_xj } from "../data_formats/parsing/ninja";
import { NpcType, ObjectType } from "../domain";
import { ninja_object_to_buffer_geometry } from "../rendering/models";
import { get_npc_data, get_object_data, AssetType } from "./binary_assets";
import { Endianness } from "../data_formats";
import { ArrayBufferCursor } from "../data_formats/cursor/ArrayBufferCursor";
import { parse_xvm } from "../data_formats/parsing/ninja/texture";
import { xvm_to_textures } from "../rendering/textures";

const logger = Logger.get("stores/EntityStore");

const DEFAULT_ENTITY = new CylinderBufferGeometry(3, 3, 20);
DEFAULT_ENTITY.translate(0, 10, 0);

const DEFAULT_ENTITY_PROMISE: Promise<BufferGeometry> = new Promise(resolve =>
    resolve(DEFAULT_ENTITY)
);

const DEFAULT_ENTITY_TEX: Texture[] = [];

const DEFAULT_ENTITY_TEX_PROMISE: Promise<Texture[]> = new Promise(resolve =>
    resolve(DEFAULT_ENTITY_TEX)
);

const npc_cache: Map<NpcType, Promise<BufferGeometry>> = new Map();
npc_cache.set(NpcType.Unknown, DEFAULT_ENTITY_PROMISE);

const npc_tex_cache: Map<NpcType, Promise<Texture[]>> = new Map();
npc_tex_cache.set(NpcType.Unknown, DEFAULT_ENTITY_TEX_PROMISE);

const object_cache: Map<ObjectType, Promise<BufferGeometry>> = new Map();
object_cache.set(ObjectType.Unknown, DEFAULT_ENTITY_PROMISE);

class EntityStore {
    async get_npc_geometry(npc_type: NpcType): Promise<BufferGeometry> {
        let mesh = npc_cache.get(npc_type);

        if (mesh) {
            return mesh;
        } else {
            mesh = get_npc_data(npc_type, AssetType.Geometry)
                .then(({ url, data }) => {
                    const cursor = new ArrayBufferCursor(data, Endianness.Little);
                    const nj_objects = url.endsWith(".nj") ? parse_nj(cursor) : parse_xj(cursor);

                    if (nj_objects.length) {
                        return ninja_object_to_buffer_geometry(nj_objects[0]);
                    } else {
                        logger.warn(`Could not parse ${url}.`);
                        return DEFAULT_ENTITY;
                    }
                })
                .catch(e => {
                    logger.warn(`Could load geometry file for ${npc_type.code}.`, e);
                    return DEFAULT_ENTITY;
                });

            npc_cache.set(npc_type, mesh);
            return mesh;
        }
    }

    async get_npc_tex(npc_type: NpcType): Promise<Texture[]> {
        let tex = npc_tex_cache.get(npc_type);

        if (tex) {
            return tex;
        } else {
            tex = get_npc_data(npc_type, AssetType.Texture)
                .then(({ data }) => {
                    const cursor = new ArrayBufferCursor(data, Endianness.Little);
                    const xvm = parse_xvm(cursor);
                    return xvm_to_textures(xvm);
                })
                .catch(e => {
                    logger.warn(`Could load texture file for ${npc_type.code}.`, e);
                    return DEFAULT_ENTITY_TEX;
                });

            npc_tex_cache.set(npc_type, tex);
            return tex;
        }
    }

    async get_object_geometry(object_type: ObjectType): Promise<BufferGeometry> {
        let geometry = object_cache.get(object_type);

        if (geometry) {
            return geometry;
        } else {
            geometry = get_object_data(object_type)
                .then(({ url, data }) => {
                    const cursor = new ArrayBufferCursor(data, Endianness.Little);
                    const nj_objects = url.endsWith(".nj") ? parse_nj(cursor) : parse_xj(cursor);

                    if (nj_objects.length) {
                        return ninja_object_to_buffer_geometry(nj_objects[0]);
                    } else {
                        logger.warn(`Could not parse ${url} for ${object_type.name}.`);
                        return DEFAULT_ENTITY;
                    }
                })
                .catch(e => {
                    logger.warn(`Could load geometry file for ${object_type.name}.`, e);
                    return DEFAULT_ENTITY;
                });

            object_cache.set(object_type, geometry);
            return geometry;
        }
    }
}

export const entity_store = new EntityStore();

import { BufferGeometry, CylinderBufferGeometry } from "three";
import { parse_nj, parse_xj } from "../data_formats/parsing/ninja";
import { NpcType, ObjectType } from "../domain";
import { ninja_object_to_buffer_geometry } from "../rendering/models";
import { get_npc_data, get_object_data } from "./binary_assets";
import { Endianness } from "../data_formats";
import { ArrayBufferCursor } from "../data_formats/cursor/ArrayBufferCursor";

const DEFAULT_ENTITY = new CylinderBufferGeometry(3, 3, 20);
DEFAULT_ENTITY.translate(0, 10, 0);

const DEFAULT_ENTITY_PROMISE: Promise<BufferGeometry> = new Promise(resolve =>
    resolve(DEFAULT_ENTITY)
);

const npc_cache: Map<NpcType, Promise<BufferGeometry>> = new Map();
npc_cache.set(NpcType.Unknown, DEFAULT_ENTITY_PROMISE);

const object_cache: Map<ObjectType, Promise<BufferGeometry>> = new Map();
object_cache.set(ObjectType.Unknown, DEFAULT_ENTITY_PROMISE);

class EntityStore {
    async get_npc_geometry(npc_type: NpcType): Promise<BufferGeometry> {
        let mesh = npc_cache.get(npc_type);

        if (mesh) {
            return mesh;
        } else {
            mesh = get_npc_data(npc_type).then(({ url, data }) => {
                const cursor = new ArrayBufferCursor(data, Endianness.Little);
                const nj_objects = url.endsWith(".nj") ? parse_nj(cursor) : parse_xj(cursor);

                if (nj_objects.length) {
                    return ninja_object_to_buffer_geometry(nj_objects[0]);
                } else {
                    throw new Error(`Could not parse ${url}.`);
                }
            });

            npc_cache.set(npc_type, mesh);
            return mesh;
        }
    }

    async get_object_geometry(object_type: ObjectType): Promise<BufferGeometry> {
        let geometry = object_cache.get(object_type);

        if (geometry) {
            return geometry;
        } else {
            geometry = get_object_data(object_type).then(({ url, data }) => {
                const cursor = new ArrayBufferCursor(data, Endianness.Little);
                const nj_objects = url.endsWith(".nj") ? parse_nj(cursor) : parse_xj(cursor);

                if (nj_objects.length) {
                    return ninja_object_to_buffer_geometry(nj_objects[0]);
                } else {
                    throw new Error("File could not be parsed into a BufferGeometry.");
                }
            });

            object_cache.set(object_type, geometry);
            return geometry;
        }
    }
}

export const entity_store = new EntityStore();

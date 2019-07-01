import { BufferGeometry } from "three";
import { NpcType, ObjectType } from "../domain";
import { BufferCursor } from "../data_formats/BufferCursor";
import { get_npc_data, get_object_data } from "./binary_assets";
import { ninja_object_to_buffer_geometry } from "../rendering/models";
import { parse_nj, parse_xj } from "../data_formats/parsing/ninja";

const npc_cache: Map<number, Promise<BufferGeometry>> = new Map();
const object_cache: Map<number, Promise<BufferGeometry>> = new Map();

class EntityStore {
    async get_npc_geometry(npc_type: NpcType): Promise<BufferGeometry> {
        let mesh = npc_cache.get(npc_type.id);

        if (mesh) {
            return mesh;
        } else {
            mesh = get_npc_data(npc_type).then(({ url, data }) => {
                const cursor = new BufferCursor(data, true);
                const nj_objects = url.endsWith('.nj') ? parse_nj(cursor) : parse_xj(cursor);

                if (nj_objects.length) {
                    return ninja_object_to_buffer_geometry(nj_objects[0]);
                } else {
                    throw new Error(`Could not parse ${url}.`);
                }
            });

            npc_cache.set(npc_type.id, mesh);
            return mesh;
        }
    }

    async get_object_geometry(object_type: ObjectType): Promise<BufferGeometry> {
        let geometry = object_cache.get(object_type.id);

        if (geometry) {
            return geometry;
        } else {
            geometry = get_object_data(object_type).then(({ url, data }) => {
                const cursor = new BufferCursor(data, true);
                const nj_objects = url.endsWith('.nj') ? parse_nj(cursor) : parse_xj(cursor);

                if (nj_objects.length) {
                    return ninja_object_to_buffer_geometry(nj_objects[0]);
                } else {
                    throw new Error('File could not be parsed into a BufferGeometry.');
                }
            });

            object_cache.set(object_type.id, geometry);
            return geometry;
        }
    }
}

export const entity_store = new EntityStore();

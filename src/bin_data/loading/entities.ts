import { BufferGeometry } from 'three';
import { NpcType, ObjectType } from '../../domain';
import { ninja_object_to_buffer_geometry } from '../../rendering/models';
import { BufferCursor } from '../BufferCursor';
import { parse_nj, parse_xj } from '../parsing/ninja';
import { getNpcData, getObjectData } from './binaryAssets';

const npc_cache: Map<string, Promise<BufferGeometry>> = new Map();
const object_cache: Map<string, Promise<BufferGeometry>> = new Map();

export function get_npc_geometry(npc_type: NpcType): Promise<BufferGeometry> {
    let mesh = npc_cache.get(String(npc_type.id));

    if (mesh) {
        return mesh;
    } else {
        mesh = getNpcData(npc_type).then(({ url, data }) => {
            const cursor = new BufferCursor(data, true);
            const nj_objects = url.endsWith('.nj') ? parse_nj(cursor) : parse_xj(cursor);

            if (nj_objects.length) {
                return ninja_object_to_buffer_geometry(nj_objects[0]);
            } else {
                throw new Error(`Could not parse ${url}.`);
            }
        });

        npc_cache.set(String(npc_type.id), mesh);
        return mesh;
    }
}

export function get_object_geometry(object_type: ObjectType): Promise<BufferGeometry> {
    let geometry = object_cache.get(String(object_type.id));

    if (geometry) {
        return geometry;
    } else {
        geometry = getObjectData(object_type).then(({ url, data }) => {
            const cursor = new BufferCursor(data, true);
            const nj_objects = url.endsWith('.nj') ? parse_nj(cursor) : parse_xj(cursor);

            if (nj_objects.length) {
                return ninja_object_to_buffer_geometry(nj_objects[0]);
            } else {
                throw new Error('File could not be parsed into a BufferGeometry.');
            }
        });

        object_cache.set(String(object_type.id), geometry);
        return geometry;
    }
}

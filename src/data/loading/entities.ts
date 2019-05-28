import { BufferGeometry } from 'three';
import { NpcType, ObjectType } from '../../domain';
import { get_npc_data, get_object_data } from './assets';
import { ArrayBufferCursor } from '../ArrayBufferCursor';
import { parse_nj, parse_xj } from '../parsing/ninja';

const npc_cache: Map<string, Promise<BufferGeometry>> = new Map();
const object_cache: Map<string, Promise<BufferGeometry>> = new Map();

export function get_npc_geometry(npc_type: NpcType): Promise<BufferGeometry> {
    let geometry = npc_cache.get(String(npc_type.id));

    if (geometry) {
        return geometry;
    } else {
        geometry = get_npc_data(npc_type).then(({ url, data }) => {
            const cursor = new ArrayBufferCursor(data, true);
            const object_3d = url.endsWith('.nj') ? parse_nj(cursor) : parse_xj(cursor);

            if (object_3d) {
                return object_3d;
            } else {
                throw new Error('File could not be parsed into a BufferGeometry.');
            }
        });

        npc_cache.set(String(npc_type.id), geometry);
        return geometry;
    }
}

export function get_object_geometry(object_type: ObjectType): Promise<BufferGeometry> {
    let geometry = object_cache.get(String(object_type.id));

    if (geometry) {
        return geometry;
    } else {
        geometry = get_object_data(object_type).then(({ url, data }) => {
            const cursor = new ArrayBufferCursor(data, true);
            const object_3d = url.endsWith('.nj') ? parse_nj(cursor) : parse_xj(cursor);

            if (object_3d) {
                return object_3d;
            } else {
                throw new Error('File could not be parsed into a BufferGeometry.');
            }
        });

        object_cache.set(String(object_type.id), geometry);
        return geometry;
    }
}

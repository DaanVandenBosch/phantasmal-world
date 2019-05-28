import { BufferGeometry } from 'three';
import { NpcType, ObjectType } from '../../domain';
import { getNpcData, getObjectData } from './assets';
import { ArrayBufferCursor } from '../ArrayBufferCursor';
import { parseNj, parseXj } from '../parsing/ninja';

const npcCache: Map<string, Promise<BufferGeometry>> = new Map();
const objectCache: Map<string, Promise<BufferGeometry>> = new Map();

export function getNpcGeometry(npcType: NpcType): Promise<BufferGeometry> {
    let geometry = npcCache.get(String(npcType.id));

    if (geometry) {
        return geometry;
    } else {
        geometry = getNpcData(npcType).then(({ url, data }) => {
            const cursor = new ArrayBufferCursor(data, true);
            const object3d = url.endsWith('.nj') ? parseNj(cursor) : parseXj(cursor);

            if (object3d) {
                return object3d;
            } else {
                throw new Error('File could not be parsed into a BufferGeometry.');
            }
        });

        npcCache.set(String(npcType.id), geometry);
        return geometry;
    }
}

export function getObjectGeometry(objectType: ObjectType): Promise<BufferGeometry> {
    let geometry = objectCache.get(String(objectType.id));

    if (geometry) {
        return geometry;
    } else {
        geometry = getObjectData(objectType).then(({ url, data }) => {
            const cursor = new ArrayBufferCursor(data, true);
            const object3d = url.endsWith('.nj') ? parseNj(cursor) : parseXj(cursor);

            if (object3d) {
                return object3d;
            } else {
                throw new Error('File could not be parsed into a BufferGeometry.');
            }
        });

        objectCache.set(String(objectType.id), geometry);
        return geometry;
    }
}

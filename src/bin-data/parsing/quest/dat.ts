import { groupBy } from 'lodash';
import { ArrayBufferCursor } from '../../ArrayBufferCursor';
import Logger from 'js-logger';

const logger = Logger.get('bin-data/parsing/quest/dat');

const OBJECT_SIZE = 68;
const NPC_SIZE = 72;

export interface DatFile {
    objs: DatObject[];
    npcs: DatNpc[];
    unknowns: DatUnknown[];
}

interface DatEntity {
    typeId: number;
    sectionId: number;
    position: { x: number, y: number, z: number };
    rotation: { x: number, y: number, z: number };
    areaId: number;
    unknown: number[][];
}

export interface DatObject extends DatEntity {
}

export interface DatNpc extends DatEntity {
    flags: number;
    skin: number;
}

export interface DatUnknown {
    entityType: number;
    totalSize: number;
    areaId: number;
    entitiesSize: number;
    data: number[];
}

export function parseDat(cursor: ArrayBufferCursor): DatFile {
    const objs: DatObject[] = [];
    const npcs: DatNpc[] = [];
    const unknowns: DatUnknown[] = [];

    while (cursor.bytesLeft) {
        const entityType = cursor.u32();
        const totalSize = cursor.u32();
        const areaId = cursor.u32();
        const entitiesSize = cursor.u32();

        if (entityType === 0) {
            break;
        } else {
            if (entitiesSize !== totalSize - 16) {
                throw Error(`Malformed DAT file. Expected an entities size of ${totalSize - 16}, got ${entitiesSize}.`);
            }

            if (entityType === 1) { // Objects
                const objectCount = Math.floor(entitiesSize / OBJECT_SIZE);
                const startPosition = cursor.position;

                for (let i = 0; i < objectCount; ++i) {
                    const typeId = cursor.u16();
                    const unknown1 = cursor.u8Array(10);
                    const sectionId = cursor.u16();
                    const unknown2 = cursor.u8Array(2);
                    const x = cursor.f32();
                    const y = cursor.f32();
                    const z = cursor.f32();
                    const rotationX = cursor.i32() / 0xFFFF * 2 * Math.PI;
                    const rotationY = cursor.i32() / 0xFFFF * 2 * Math.PI;
                    const rotationZ = cursor.i32() / 0xFFFF * 2 * Math.PI;
                    // The next 3 floats seem to be scale values.
                    const unknown3 = cursor.u8Array(28);

                    objs.push({
                        typeId,
                        sectionId,
                        position: { x, y, z },
                        rotation: { x: rotationX, y: rotationY, z: rotationZ },
                        areaId,
                        unknown: [unknown1, unknown2, unknown3]
                    });
                }

                const bytesRead = cursor.position - startPosition;

                if (bytesRead !== entitiesSize) {
                    logger.warn(`Read ${bytesRead} bytes instead of expected ${entitiesSize} for entity type ${entityType} (Object).`);
                    cursor.seek(entitiesSize - bytesRead);
                }
            } else if (entityType === 2) { // NPCs
                const npcCount = Math.floor(entitiesSize / NPC_SIZE);
                const startPosition = cursor.position;

                for (let i = 0; i < npcCount; ++i) {
                    const typeId = cursor.u16();
                    const unknown1 = cursor.u8Array(10);
                    const sectionId = cursor.u16();
                    const unknown2 = cursor.u8Array(6);
                    const x = cursor.f32();
                    const y = cursor.f32();
                    const z = cursor.f32();
                    const rotationX = cursor.i32() / 0xFFFF * 2 * Math.PI;
                    const rotationY = cursor.i32() / 0xFFFF * 2 * Math.PI;
                    const rotationZ = cursor.i32() / 0xFFFF * 2 * Math.PI;
                    const unknown3 = cursor.u8Array(4);
                    const flags = cursor.u32();
                    const unknown4 = cursor.u8Array(12);
                    const skin = cursor.u32();
                    const unknown5 = cursor.u8Array(4);

                    npcs.push({
                        typeId,
                        sectionId,
                        position: { x, y, z },
                        rotation: { x: rotationX, y: rotationY, z: rotationZ },
                        skin,
                        areaId,
                        flags,
                        unknown: [unknown1, unknown2, unknown3, unknown4, unknown5]
                    });
                }

                const bytesRead = cursor.position - startPosition;

                if (bytesRead !== entitiesSize) {
                    logger.warn(`Read ${bytesRead} bytes instead of expected ${entitiesSize} for entity type ${entityType} (NPC).`);
                    cursor.seek(entitiesSize - bytesRead);
                }
            } else {
                // There are also waves (type 3) and unknown entity types 4 and 5.
                unknowns.push({
                    entityType,
                    totalSize,
                    areaId,
                    entitiesSize,
                    data: cursor.u8Array(entitiesSize)
                });
            }
        }
    }

    return { objs, npcs, unknowns };
}

export function writeDat({ objs, npcs, unknowns }: DatFile): ArrayBufferCursor {
    const cursor = new ArrayBufferCursor(
        objs.length * OBJECT_SIZE + npcs.length * NPC_SIZE + unknowns.length * 1000, true);

    const groupedObjs = groupBy(objs, obj => obj.areaId);
    const objAreaIds = Object.keys(groupedObjs)
        .map(key => parseInt(key, 10))
        .sort((a, b) => a - b);

    for (const areaId of objAreaIds) {
        const areaObjs = groupedObjs[areaId];
        const entitiesSize = areaObjs.length * OBJECT_SIZE;
        cursor.writeU32(1); // Entity type
        cursor.writeU32(entitiesSize + 16);
        cursor.writeU32(areaId);
        cursor.writeU32(entitiesSize);

        for (const obj of areaObjs) {
            cursor.writeU16(obj.typeId);
            cursor.writeU8Array(obj.unknown[0]);
            cursor.writeU16(obj.sectionId);
            cursor.writeU8Array(obj.unknown[1]);
            cursor.writeF32(obj.position.x);
            cursor.writeF32(obj.position.y);
            cursor.writeF32(obj.position.z);
            cursor.writeI32(Math.round(obj.rotation.x / (2 * Math.PI) * 0xFFFF));
            cursor.writeI32(Math.round(obj.rotation.y / (2 * Math.PI) * 0xFFFF));
            cursor.writeI32(Math.round(obj.rotation.z / (2 * Math.PI) * 0xFFFF));
            cursor.writeU8Array(obj.unknown[2]);
        }
    }

    const groupedNpcs = groupBy(npcs, npc => npc.areaId);
    const npcAreaIds = Object.keys(groupedNpcs)
        .map(key => parseInt(key, 10))
        .sort((a, b) => a - b);

    for (const areaId of npcAreaIds) {
        const areaNpcs = groupedNpcs[areaId];
        const entitiesSize = areaNpcs.length * NPC_SIZE;
        cursor.writeU32(2); // Entity type
        cursor.writeU32(entitiesSize + 16);
        cursor.writeU32(areaId);
        cursor.writeU32(entitiesSize);

        for (const npc of areaNpcs) {
            cursor.writeU16(npc.typeId);
            cursor.writeU8Array(npc.unknown[0]);
            cursor.writeU16(npc.sectionId);
            cursor.writeU8Array(npc.unknown[1]);
            cursor.writeF32(npc.position.x);
            cursor.writeF32(npc.position.y);
            cursor.writeF32(npc.position.z);
            cursor.writeI32(Math.round(npc.rotation.x / (2 * Math.PI) * 0xFFFF));
            cursor.writeI32(Math.round(npc.rotation.y / (2 * Math.PI) * 0xFFFF));
            cursor.writeI32(Math.round(npc.rotation.z / (2 * Math.PI) * 0xFFFF));
            cursor.writeU8Array(npc.unknown[2]);
            cursor.writeU32(npc.flags);
            cursor.writeU8Array(npc.unknown[3]);
            cursor.writeU32(npc.skin);
            cursor.writeU8Array(npc.unknown[4]);
        }
    }

    for (const unknown of unknowns) {
        cursor.writeU32(unknown.entityType);
        cursor.writeU32(unknown.totalSize);
        cursor.writeU32(unknown.areaId);
        cursor.writeU32(unknown.entitiesSize);
        cursor.writeU8Array(unknown.data);
    }

    // Final header.
    cursor.writeU32(0);
    cursor.writeU32(0);
    cursor.writeU32(0);
    cursor.writeU32(0);

    cursor.seekStart(0);

    return cursor;
}

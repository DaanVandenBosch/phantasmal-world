import { groupBy } from 'lodash';
import { BufferCursor } from '../../BufferCursor';
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

export function parseDat(cursor: BufferCursor): DatFile {
    const objs: DatObject[] = [];
    const npcs: DatNpc[] = [];
    const unknowns: DatUnknown[] = [];

    while (cursor.bytes_left) {
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
                    const unknown1 = cursor.u8_array(10);
                    const sectionId = cursor.u16();
                    const unknown2 = cursor.u8_array(2);
                    const x = cursor.f32();
                    const y = cursor.f32();
                    const z = cursor.f32();
                    const rotationX = cursor.i32() / 0xFFFF * 2 * Math.PI;
                    const rotationY = cursor.i32() / 0xFFFF * 2 * Math.PI;
                    const rotationZ = cursor.i32() / 0xFFFF * 2 * Math.PI;
                    // The next 3 floats seem to be scale values.
                    const unknown3 = cursor.u8_array(28);

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
                    const unknown1 = cursor.u8_array(10);
                    const sectionId = cursor.u16();
                    const unknown2 = cursor.u8_array(6);
                    const x = cursor.f32();
                    const y = cursor.f32();
                    const z = cursor.f32();
                    const rotationX = cursor.i32() / 0xFFFF * 2 * Math.PI;
                    const rotationY = cursor.i32() / 0xFFFF * 2 * Math.PI;
                    const rotationZ = cursor.i32() / 0xFFFF * 2 * Math.PI;
                    const unknown3 = cursor.u8_array(4);
                    const flags = cursor.f32();
                    const unknown4 = cursor.u8_array(12);
                    const skin = cursor.u32();
                    const unknown5 = cursor.u8_array(4);

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
                    data: cursor.u8_array(entitiesSize)
                });
            }
        }
    }

    return { objs, npcs, unknowns };
}

export function writeDat({ objs, npcs, unknowns }: DatFile): BufferCursor {
    const cursor = new BufferCursor(
        objs.length * OBJECT_SIZE + npcs.length * NPC_SIZE + unknowns.length * 1000, true);

    const groupedObjs = groupBy(objs, obj => obj.areaId);
    const objAreaIds = Object.keys(groupedObjs)
        .map(key => parseInt(key, 10))
        .sort((a, b) => a - b);

    for (const areaId of objAreaIds) {
        const areaObjs = groupedObjs[areaId];
        const entitiesSize = areaObjs.length * OBJECT_SIZE;
        cursor.write_u32(1); // Entity type
        cursor.write_u32(entitiesSize + 16);
        cursor.write_u32(areaId);
        cursor.write_u32(entitiesSize);

        for (const obj of areaObjs) {
            cursor.write_u16(obj.typeId);
            cursor.write_u8_array(obj.unknown[0]);
            cursor.write_u16(obj.sectionId);
            cursor.write_u8_array(obj.unknown[1]);
            cursor.write_f32(obj.position.x);
            cursor.write_f32(obj.position.y);
            cursor.write_f32(obj.position.z);
            cursor.write_i32(Math.round(obj.rotation.x / (2 * Math.PI) * 0xFFFF));
            cursor.write_i32(Math.round(obj.rotation.y / (2 * Math.PI) * 0xFFFF));
            cursor.write_i32(Math.round(obj.rotation.z / (2 * Math.PI) * 0xFFFF));
            cursor.write_u8_array(obj.unknown[2]);
        }
    }

    const groupedNpcs = groupBy(npcs, npc => npc.areaId);
    const npcAreaIds = Object.keys(groupedNpcs)
        .map(key => parseInt(key, 10))
        .sort((a, b) => a - b);

    for (const areaId of npcAreaIds) {
        const areaNpcs = groupedNpcs[areaId];
        const entitiesSize = areaNpcs.length * NPC_SIZE;
        cursor.write_u32(2); // Entity type
        cursor.write_u32(entitiesSize + 16);
        cursor.write_u32(areaId);
        cursor.write_u32(entitiesSize);

        for (const npc of areaNpcs) {
            cursor.write_u16(npc.typeId);
            cursor.write_u8_array(npc.unknown[0]);
            cursor.write_u16(npc.sectionId);
            cursor.write_u8_array(npc.unknown[1]);
            cursor.write_f32(npc.position.x);
            cursor.write_f32(npc.position.y);
            cursor.write_f32(npc.position.z);
            cursor.write_i32(Math.round(npc.rotation.x / (2 * Math.PI) * 0xFFFF));
            cursor.write_i32(Math.round(npc.rotation.y / (2 * Math.PI) * 0xFFFF));
            cursor.write_i32(Math.round(npc.rotation.z / (2 * Math.PI) * 0xFFFF));
            cursor.write_u8_array(npc.unknown[2]);
            cursor.write_f32(npc.flags);
            cursor.write_u8_array(npc.unknown[3]);
            cursor.write_u32(npc.skin);
            cursor.write_u8_array(npc.unknown[4]);
        }
    }

    for (const unknown of unknowns) {
        cursor.write_u32(unknown.entityType);
        cursor.write_u32(unknown.totalSize);
        cursor.write_u32(unknown.areaId);
        cursor.write_u32(unknown.entitiesSize);
        cursor.write_u8_array(unknown.data);
    }

    // Final header.
    cursor.write_u32(0);
    cursor.write_u32(0);
    cursor.write_u32(0);
    cursor.write_u32(0);

    cursor.seek_start(0);

    return cursor;
}

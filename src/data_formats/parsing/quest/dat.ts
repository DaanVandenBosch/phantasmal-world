import Logger from "js-logger";
import { groupBy } from "lodash";
import { Endianness } from "../..";
import { Cursor } from "../../cursor/Cursor";
import { WritableResizableBufferCursor } from "../../cursor/WritableResizableBufferCursor";
import { ResizableBuffer } from "../../ResizableBuffer";
import { Vec3 } from "../../vector";

const logger = Logger.get("data_formats/parsing/quest/dat");

const OBJECT_SIZE = 68;
const NPC_SIZE = 72;

export type DatFile = {
    objs: DatObject[];
    npcs: DatNpc[];
    unknowns: DatUnknown[];
};

export type DatEntity = {
    type_id: number;
    section_id: number;
    position: Vec3;
    rotation: Vec3;
    area_id: number;
    unknown: number[][];
};

export type DatObject = DatEntity;

export type DatNpc = DatEntity & {
    flags: number;
    skin: number;
};

export type DatUnknown = {
    entity_type: number;
    total_size: number;
    area_id: number;
    entities_size: number;
    data: number[];
};

export function parse_dat(cursor: Cursor): DatFile {
    const objs: DatObject[] = [];
    const npcs: DatNpc[] = [];
    const unknowns: DatUnknown[] = [];

    while (cursor.bytes_left) {
        const entity_type = cursor.u32();
        const total_size = cursor.u32();
        const area_id = cursor.u32();
        const entities_size = cursor.u32();

        if (entity_type === 0) {
            break;
        } else {
            if (entities_size !== total_size - 16) {
                throw Error(
                    `Malformed DAT file. Expected an entities size of ${total_size -
                        16}, got ${entities_size}.`
                );
            }

            if (entity_type === 1) {
                // Objects
                const object_count = Math.floor(entities_size / OBJECT_SIZE);
                const start_position = cursor.position;

                for (let i = 0; i < object_count; ++i) {
                    const type_id = cursor.u16();
                    const unknown1 = cursor.u8_array(10);
                    const section_id = cursor.u16();
                    const unknown2 = cursor.u8_array(2);
                    const x = cursor.f32();
                    const y = cursor.f32();
                    const z = cursor.f32();
                    const rotation_x = (cursor.i32() / 0xffff) * 2 * Math.PI;
                    const rotation_y = (cursor.i32() / 0xffff) * 2 * Math.PI;
                    const rotation_z = (cursor.i32() / 0xffff) * 2 * Math.PI;
                    // The next 3 floats seem to be scale values.
                    const unknown3 = cursor.u8_array(28);

                    objs.push({
                        type_id,
                        section_id,
                        position: new Vec3(x, y, z),
                        rotation: new Vec3(rotation_x, rotation_y, rotation_z),
                        area_id,
                        unknown: [unknown1, unknown2, unknown3],
                    });
                }

                const bytes_read = cursor.position - start_position;

                if (bytes_read !== entities_size) {
                    logger.warn(
                        `Read ${bytes_read} bytes instead of expected ${entities_size} for entity type ${entity_type} (Object).`
                    );
                    cursor.seek(entities_size - bytes_read);
                }
            } else if (entity_type === 2) {
                // NPCs
                const npc_count = Math.floor(entities_size / NPC_SIZE);
                const start_position = cursor.position;

                for (let i = 0; i < npc_count; ++i) {
                    const type_id = cursor.u16();
                    const unknown1 = cursor.u8_array(10);
                    const section_id = cursor.u16();
                    const unknown2 = cursor.u8_array(6);
                    const x = cursor.f32();
                    const y = cursor.f32();
                    const z = cursor.f32();
                    const rotation_x = (cursor.i32() / 0xffff) * 2 * Math.PI;
                    const rotation_y = (cursor.i32() / 0xffff) * 2 * Math.PI;
                    const rotation_z = (cursor.i32() / 0xffff) * 2 * Math.PI;
                    const unknown3 = cursor.u8_array(4);
                    const flags = cursor.f32();
                    const unknown4 = cursor.u8_array(12);
                    const skin = cursor.u32();
                    const unknown5 = cursor.u8_array(4);

                    npcs.push({
                        type_id,
                        section_id,
                        position: new Vec3(x, y, z),
                        rotation: new Vec3(rotation_x, rotation_y, rotation_z),
                        skin,
                        area_id,
                        flags,
                        unknown: [unknown1, unknown2, unknown3, unknown4, unknown5],
                    });
                }

                const bytes_read = cursor.position - start_position;

                if (bytes_read !== entities_size) {
                    logger.warn(
                        `Read ${bytes_read} bytes instead of expected ${entities_size} for entity type ${entity_type} (NPC).`
                    );
                    cursor.seek(entities_size - bytes_read);
                }
            } else {
                // There are also waves (type 3) and unknown entity types 4 and 5.
                unknowns.push({
                    entity_type,
                    total_size,
                    area_id,
                    entities_size,
                    data: cursor.u8_array(entities_size),
                });
            }
        }
    }

    return { objs, npcs, unknowns };
}

export function write_dat({ objs, npcs, unknowns }: DatFile): ResizableBuffer {
    const buffer = new ResizableBuffer(
        objs.length * (16 + OBJECT_SIZE) +
            npcs.length * (16 + NPC_SIZE) +
            unknowns.reduce((a, b) => a + b.total_size, 0)
    );
    const cursor = new WritableResizableBufferCursor(buffer, Endianness.Little);

    const grouped_objs = groupBy(objs, obj => obj.area_id);
    const obj_area_ids = Object.keys(grouped_objs)
        .map(key => parseInt(key, 10))
        .sort((a, b) => a - b);

    for (const area_id of obj_area_ids) {
        const area_objs = grouped_objs[area_id];
        const entities_size = area_objs.length * OBJECT_SIZE;
        cursor.write_u32(1); // Entity type
        cursor.write_u32(entities_size + 16);
        cursor.write_u32(area_id);
        cursor.write_u32(entities_size);

        for (const obj of area_objs) {
            cursor.write_u16(obj.type_id);
            cursor.write_u8_array(obj.unknown[0]);
            cursor.write_u16(obj.section_id);
            cursor.write_u8_array(obj.unknown[1]);
            cursor.write_f32(obj.position.x);
            cursor.write_f32(obj.position.y);
            cursor.write_f32(obj.position.z);
            cursor.write_i32(Math.round((obj.rotation.x / (2 * Math.PI)) * 0xffff));
            cursor.write_i32(Math.round((obj.rotation.y / (2 * Math.PI)) * 0xffff));
            cursor.write_i32(Math.round((obj.rotation.z / (2 * Math.PI)) * 0xffff));
            cursor.write_u8_array(obj.unknown[2]);
        }
    }

    const grouped_npcs = groupBy(npcs, npc => npc.area_id);
    const npc_area_ids = Object.keys(grouped_npcs)
        .map(key => parseInt(key, 10))
        .sort((a, b) => a - b);

    for (const area_id of npc_area_ids) {
        const area_npcs = grouped_npcs[area_id];
        const entities_size = area_npcs.length * NPC_SIZE;
        cursor.write_u32(2); // Entity type
        cursor.write_u32(entities_size + 16);
        cursor.write_u32(area_id);
        cursor.write_u32(entities_size);

        for (const npc of area_npcs) {
            cursor.write_u16(npc.type_id);
            cursor.write_u8_array(npc.unknown[0]);
            cursor.write_u16(npc.section_id);
            cursor.write_u8_array(npc.unknown[1]);
            cursor.write_f32(npc.position.x);
            cursor.write_f32(npc.position.y);
            cursor.write_f32(npc.position.z);
            cursor.write_i32(Math.round((npc.rotation.x / (2 * Math.PI)) * 0xffff));
            cursor.write_i32(Math.round((npc.rotation.y / (2 * Math.PI)) * 0xffff));
            cursor.write_i32(Math.round((npc.rotation.z / (2 * Math.PI)) * 0xffff));
            cursor.write_u8_array(npc.unknown[2]);
            cursor.write_f32(npc.flags);
            cursor.write_u8_array(npc.unknown[3]);
            cursor.write_u32(npc.skin);
            cursor.write_u8_array(npc.unknown[4]);
        }
    }

    for (const unknown of unknowns) {
        cursor.write_u32(unknown.entity_type);
        cursor.write_u32(unknown.total_size);
        cursor.write_u32(unknown.area_id);
        cursor.write_u32(unknown.entities_size);
        cursor.write_u8_array(unknown.data);
    }

    // Final header.
    cursor.write_u32(0);
    cursor.write_u32(0);
    cursor.write_u32(0);
    cursor.write_u32(0);

    return buffer;
}

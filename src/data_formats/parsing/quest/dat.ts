import Logger from "js-logger";
import { groupBy } from "lodash";
import { Endianness } from "../..";
import { Cursor } from "../../cursor/Cursor";
import { ResizableBufferCursor } from "../../cursor/ResizableBufferCursor";
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
    scale: Vec3;
    area_id: number;
    unknown: number[][];
};

export type DatObject = DatEntity;

export type DatNpc = DatEntity & {
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
                    const position = cursor.vec3_f32();
                    const rotation_x = (cursor.i32() / 0xffff) * 2 * Math.PI;
                    const rotation_y = (cursor.i32() / 0xffff) * 2 * Math.PI;
                    const rotation_z = (cursor.i32() / 0xffff) * 2 * Math.PI;
                    const scale = cursor.vec3_f32();
                    const unknown3 = cursor.u8_array(16);

                    objs.push({
                        type_id,
                        section_id,
                        position,
                        rotation: new Vec3(rotation_x, rotation_y, rotation_z),
                        scale,
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
                    const position = cursor.vec3_f32();
                    const rotation_x = (cursor.i32() / 0xffff) * 2 * Math.PI;
                    const rotation_y = (cursor.i32() / 0xffff) * 2 * Math.PI;
                    const rotation_z = (cursor.i32() / 0xffff) * 2 * Math.PI;
                    const scale = cursor.vec3_f32();
                    const unknown3 = cursor.u8_array(8);
                    const skin = cursor.u32();
                    const unknown4 = cursor.u8_array(4);

                    npcs.push({
                        type_id,
                        section_id,
                        position,
                        rotation: new Vec3(rotation_x, rotation_y, rotation_z),
                        scale,
                        skin,
                        area_id,
                        unknown: [unknown1, unknown2, unknown3, unknown4],
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
    const cursor = new ResizableBufferCursor(buffer, Endianness.Little);

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
            if (obj.unknown.length !== 3)
                throw new Error(`unknown should be of length 3, was ${obj.unknown.length}`);

            cursor.write_u16(obj.type_id);

            if (obj.unknown[0].length !== 10)
                throw new Error(`unknown[0] should be of length 10, was ${obj.unknown[0].length}`);

            cursor.write_u8_array(obj.unknown[0]);
            cursor.write_u16(obj.section_id);

            if (obj.unknown[1].length !== 2)
                throw new Error(`unknown[1] should be of length 2, was ${obj.unknown[1].length}`);

            cursor.write_u8_array(obj.unknown[1]);
            cursor.write_vec3_f32(obj.position);
            cursor.write_i32(Math.round((obj.rotation.x / (2 * Math.PI)) * 0xffff));
            cursor.write_i32(Math.round((obj.rotation.y / (2 * Math.PI)) * 0xffff));
            cursor.write_i32(Math.round((obj.rotation.z / (2 * Math.PI)) * 0xffff));
            cursor.write_vec3_f32(obj.scale);

            if (obj.unknown[2].length !== 16)
                throw new Error(`unknown[2] should be of length 16, was ${obj.unknown[2].length}`);

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
            cursor.write_vec3_f32(npc.position);
            cursor.write_i32(Math.round((npc.rotation.x / (2 * Math.PI)) * 0xffff));
            cursor.write_i32(Math.round((npc.rotation.y / (2 * Math.PI)) * 0xffff));
            cursor.write_i32(Math.round((npc.rotation.z / (2 * Math.PI)) * 0xffff));
            cursor.write_vec3_f32(npc.scale);
            cursor.write_u8_array(npc.unknown[2]);
            cursor.write_u32(npc.skin);
            cursor.write_u8_array(npc.unknown[3]);
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

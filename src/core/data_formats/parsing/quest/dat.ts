import Logger from "js-logger";
import { groupBy } from "lodash";
import { Endianness } from "../../Endianness";
import { Cursor } from "../../cursor/Cursor";
import { ResizableBufferCursor } from "../../cursor/ResizableBufferCursor";
import { ResizableBuffer } from "../../ResizableBuffer";
import { Vec3 } from "../../vector";
import { WritableCursor } from "../../cursor/WritableCursor";

const logger = Logger.get("core/data_formats/parsing/quest/dat");

const OBJECT_SIZE = 68;
const NPC_SIZE = 72;

export type DatFile = {
    readonly objs: readonly DatObject[];
    readonly npcs: readonly DatNpc[];
    readonly waves: readonly DatWave[];
    readonly unknowns: readonly DatUnknown[];
};

export type DatEntity = {
    readonly type_id: number;
    readonly section_id: number;
    readonly position: Vec3;
    readonly rotation: Vec3;
    readonly area_id: number;
    readonly unknown: readonly number[][];
};

export type DatObject = DatEntity & {
    readonly id: number;
    readonly group_id: number;
    readonly properties: readonly number[];
};

export type DatNpc = DatEntity & {
    readonly scale: Vec3;
    readonly npc_id: number;
    readonly script_label: number;
    readonly roaming: number;
};

export type DatWave = {
    readonly id: number;
    readonly section_id: number;
    readonly wave: number;
    readonly delay: number;
    readonly actions: readonly DatWaveAction[];
    readonly area_id: number;
    readonly unknown: number;
};

export enum DatWaveActionType {
    SpawnNpcs = 0x8,
    Unlock = 0xa,
    Lock = 0xb,
    SpawnWave = 0xc,
}

export type DatWaveAction =
    | DatWaveActionSpawnNpcs
    | DatWaveActionUnlock
    | DatWaveActionLock
    | DatWaveActionSpawnWave;

export type DatWaveActionSpawnNpcs = {
    readonly type: DatWaveActionType.SpawnNpcs;
    readonly section_id: number;
    readonly appear_flag: number;
};

export type DatWaveActionUnlock = {
    readonly type: DatWaveActionType.Unlock;
    readonly door_id: number;
};

export type DatWaveActionLock = {
    readonly type: DatWaveActionType.Lock;
    readonly door_id: number;
};

export type DatWaveActionSpawnWave = {
    readonly type: DatWaveActionType.SpawnWave;
    readonly wave_id: number;
};

export type DatUnknown = {
    readonly entity_type: number;
    readonly total_size: number;
    readonly area_id: number;
    readonly entities_size: number;
    readonly data: number[];
};

export function parse_dat(cursor: Cursor): DatFile {
    const objs: DatObject[] = [];
    const npcs: DatNpc[] = [];
    const waves: DatWave[] = [];
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
                        16}, got ${entities_size}.`,
                );
            }

            const entities_cursor = cursor.take(entities_size);

            if (entity_type === 1) {
                parse_objects(entities_cursor, area_id, objs);
            } else if (entity_type === 2) {
                parse_npcs(entities_cursor, area_id, npcs);
            } else if (entity_type === 3) {
                parse_waves(entities_cursor, area_id, waves);
            } else {
                // Unknown entity types 4 and 5.
                unknowns.push({
                    entity_type,
                    total_size,
                    area_id,
                    entities_size,
                    data: cursor.u8_array(entities_size),
                });
            }

            if (entities_cursor.bytes_left) {
                logger.warn(
                    `Read ${entities_cursor.position} bytes instead of expected ${entities_cursor.size} for entity type ${entity_type}.`,
                );
            }
        }
    }

    return { objs, npcs, waves, unknowns };
}

export function write_dat({ objs, npcs, waves, unknowns }: DatFile): ResizableBuffer {
    const buffer = new ResizableBuffer(
        objs.length * (16 + OBJECT_SIZE) +
            npcs.length * (16 + NPC_SIZE) +
            unknowns.reduce((a, b) => a + b.total_size, 0),
    );
    const cursor = new ResizableBufferCursor(buffer, Endianness.Little);

    write_objects(cursor, objs);

    write_npcs(cursor, npcs);

    write_waves(cursor, waves);

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

function parse_objects(cursor: Cursor, area_id: number, objs: DatObject[]): void {
    const object_count = Math.floor(cursor.size / OBJECT_SIZE);

    for (let i = 0; i < object_count; ++i) {
        const type_id = cursor.u16();
        const unknown1 = cursor.u8_array(6);
        const id = cursor.u16();
        const group_id = cursor.u16();
        const section_id = cursor.u16();
        const unknown2 = cursor.u8_array(2);
        const position = cursor.vec3_f32();
        const rotation = {
            x: (cursor.i32() / 0xffff) * 2 * Math.PI,
            y: (cursor.i32() / 0xffff) * 2 * Math.PI,
            z: (cursor.i32() / 0xffff) * 2 * Math.PI,
        };
        const properties = [
            cursor.f32(),
            cursor.f32(),
            cursor.f32(),
            cursor.u32(),
            cursor.u32(),
            cursor.u32(),
            cursor.u32(),
        ];

        objs.push({
            type_id,
            id,
            group_id,
            section_id,
            position,
            rotation,
            properties,
            area_id,
            unknown: [unknown1, unknown2],
        });
    }
}

function parse_npcs(cursor: Cursor, area_id: number, npcs: DatNpc[]): void {
    const npc_count = Math.floor(cursor.size / NPC_SIZE);

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
        const npc_id = cursor.f32();
        const script_label = cursor.f32();
        const roaming = cursor.u32();
        const unknown3 = cursor.u8_array(4);

        npcs.push({
            type_id,
            section_id,
            position,
            rotation: { x: rotation_x, y: rotation_y, z: rotation_z },
            scale,
            npc_id,
            script_label,
            roaming,
            area_id,
            unknown: [unknown1, unknown2, unknown3],
        });
    }
}

function parse_waves(cursor: Cursor, area_id: number, waves: DatWave[]): void {
    const actions_offset = cursor.u32();
    cursor.seek(4); // Always 0x10
    const wave_count = cursor.u32();
    cursor.seek(3); // Always 0
    const wave_type = cursor.u8();

    if (wave_type === 0x32) {
        throw new Error("Can't parse challenge mode quests yet.");
    }

    cursor.seek_start(actions_offset);
    const actions_cursor = cursor.take(cursor.bytes_left);
    cursor.seek_start(16);

    for (let i = 0; i < wave_count; ++i) {
        const id = cursor.u32();
        cursor.seek(4); // Always 0x100
        const section_id = cursor.u16();
        const wave = cursor.u16();
        const delay = cursor.u16();
        const unknown = cursor.u16(); // "wavesetting"?
        const wave_actions_offset = cursor.u32();

        actions_cursor.seek_start(wave_actions_offset);
        const actions = parse_wave_actions(actions_cursor);

        waves.push({
            id,
            section_id,
            wave,
            delay,
            actions,
            area_id,
            unknown,
        });
    }

    if (cursor.position !== actions_offset) {
        logger.warn(
            `Read ${cursor.position - 16} bytes of wave data instead of expected ${actions_offset -
                16}.`,
        );
    }

    let last_u8 = 0xff;

    while (actions_cursor.bytes_left) {
        last_u8 = actions_cursor.u8();

        if (last_u8 !== 0xff) {
            break;
        }
    }

    if (last_u8 !== 0xff) {
        actions_cursor.seek(-1);
    }

    // Make sure the cursor position represents the amount of bytes we've consumed.
    cursor.seek_start(actions_offset + actions_cursor.position);
}

function parse_wave_actions(cursor: Cursor): DatWaveAction[] {
    const actions: DatWaveAction[] = [];

    outer: while (cursor.bytes_left) {
        const type = cursor.u8();

        switch (type) {
            case 1:
                break outer;

            case DatWaveActionType.SpawnNpcs:
                actions.push({
                    type: DatWaveActionType.SpawnNpcs,
                    section_id: cursor.u16(),
                    appear_flag: cursor.u16(),
                });
                break;

            case DatWaveActionType.Unlock:
                actions.push({
                    type: DatWaveActionType.Unlock,
                    door_id: cursor.u16(),
                });
                break;

            case DatWaveActionType.Lock:
                actions.push({
                    type: DatWaveActionType.Lock,
                    door_id: cursor.u16(),
                });
                break;

            case DatWaveActionType.SpawnWave:
                actions.push({
                    type: DatWaveActionType.SpawnWave,
                    wave_id: cursor.u32(),
                });
                break;

            default:
                logger.warn(`Unexpected wave action type ${type}.`);
                break outer;
        }
    }

    return actions;
}

function write_objects(cursor: WritableCursor, objs: readonly DatObject[]): void {
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
            if (obj.unknown.length !== 2)
                throw new Error(`unknown should be of length 2, was ${obj.unknown.length}`);

            cursor.write_u16(obj.type_id);

            if (obj.unknown[0].length !== 6)
                throw new Error(`unknown[0] should be of length 6, was ${obj.unknown[0].length}`);

            cursor.write_u8_array(obj.unknown[0]);
            cursor.write_u16(obj.id);
            cursor.write_u16(obj.group_id);
            cursor.write_u16(obj.section_id);

            if (obj.unknown[1].length !== 2)
                throw new Error(`unknown[1] should be of length 2, was ${obj.unknown[1].length}`);

            cursor.write_u8_array(obj.unknown[1]);
            cursor.write_vec3_f32(obj.position);
            cursor.write_i32(Math.round((obj.rotation.x / (2 * Math.PI)) * 0xffff));
            cursor.write_i32(Math.round((obj.rotation.y / (2 * Math.PI)) * 0xffff));
            cursor.write_i32(Math.round((obj.rotation.z / (2 * Math.PI)) * 0xffff));

            if (obj.properties.length !== 7)
                throw new Error(`properties should be of length 7, was ${obj.properties.length}`);

            cursor.write_f32(obj.properties[0]);
            cursor.write_f32(obj.properties[1]);
            cursor.write_f32(obj.properties[2]);
            cursor.write_u32(obj.properties[3]);
            cursor.write_u32(obj.properties[4]);
            cursor.write_u32(obj.properties[5]);
            cursor.write_u32(obj.properties[6]);
        }
    }
}

function write_npcs(cursor: WritableCursor, npcs: readonly DatNpc[]): void {
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
            if (npc.unknown.length !== 3)
                throw new Error(`unknown should be of length 3, was ${npc.unknown.length}`);

            cursor.write_u16(npc.type_id);

            if (npc.unknown[0].length !== 10)
                throw new Error(`unknown[0] should be of length 10, was ${npc.unknown[0].length}`);

            cursor.write_u8_array(npc.unknown[0]);
            cursor.write_u16(npc.section_id);

            if (npc.unknown[1].length !== 6)
                throw new Error(`unknown[1] should be of length 6, was ${npc.unknown[1].length}`);

            cursor.write_u8_array(npc.unknown[1]);
            cursor.write_vec3_f32(npc.position);
            cursor.write_i32(Math.round((npc.rotation.x / (2 * Math.PI)) * 0xffff));
            cursor.write_i32(Math.round((npc.rotation.y / (2 * Math.PI)) * 0xffff));
            cursor.write_i32(Math.round((npc.rotation.z / (2 * Math.PI)) * 0xffff));
            cursor.write_vec3_f32(npc.scale);
            cursor.write_f32(npc.npc_id);
            cursor.write_f32(npc.script_label);
            cursor.write_u32(npc.roaming);

            if (npc.unknown[2].length !== 4)
                throw new Error(`unknown[2] should be of length 4, was ${npc.unknown[2].length}`);

            cursor.write_u8_array(npc.unknown[2]);
        }
    }
}

function write_waves(cursor: WritableCursor, waves: readonly DatWave[]): void {
    const grouped_waves = groupBy(waves, wave => wave.area_id);
    const wave_area_ids = Object.keys(grouped_waves)
        .map(key => parseInt(key, 10))
        .sort((a, b) => a - b);

    for (const area_id of wave_area_ids) {
        const area_waves = grouped_waves[area_id];

        // Standard header.
        cursor.write_u32(3); // Entity type
        const total_size_offset = cursor.position;
        cursor.write_u32(0); // Placeholder for the total size.
        cursor.write_u32(area_id);
        const entities_size_offset = cursor.position;
        cursor.write_u32(0); // Placeholder for the entities size.

        // Wave header.
        const start_pos = cursor.position;
        // TODO: actual wave size is dependent on the wave type (challenge mode).
        // Absolute offset.
        const actions_offset = start_pos + 16 + 20 * area_waves.length;
        cursor.size = Math.max(actions_offset, cursor.size);

        cursor.write_u32(actions_offset - start_pos);
        cursor.write_u32(0x10);
        cursor.write_u32(area_waves.length);
        cursor.write_u32(0); // TODO: write wave type (challenge mode).

        // Relative offset.
        let wave_actions_offset = 0;

        for (const wave of area_waves) {
            cursor.write_u32(wave.id);
            cursor.write_u32(0x10000);
            cursor.write_u16(wave.section_id);
            cursor.write_u16(wave.wave);
            cursor.write_u16(wave.delay);
            cursor.write_u16(wave.unknown);
            cursor.write_u32(wave_actions_offset);
            const next_wave_pos = cursor.position;

            cursor.seek_start(actions_offset + wave_actions_offset);

            for (const action of wave.actions) {
                cursor.write_u8(action.type);

                switch (action.type) {
                    case DatWaveActionType.SpawnNpcs:
                        cursor.write_u16(action.section_id);
                        cursor.write_u16(action.appear_flag);
                        break;

                    case DatWaveActionType.Unlock:
                        cursor.write_u16(action.door_id);
                        break;

                    case DatWaveActionType.Lock:
                        cursor.write_u16(action.door_id);
                        break;

                    case DatWaveActionType.SpawnWave:
                        cursor.write_u32(action.wave_id);
                        break;

                    default:
                        // Need to cast because TypeScript infers action to be `never`.
                        throw new Error(`Unknown wave action type ${(action as any).type}.`);
                }
            }

            // End of wave actions.
            cursor.write_u8(1);

            wave_actions_offset = cursor.position - actions_offset;

            cursor.seek_start(next_wave_pos);
        }

        cursor.seek_start(actions_offset + wave_actions_offset);

        while ((cursor.position - actions_offset) % 4 !== 0) {
            cursor.write_u8(0xff);
        }

        const end_pos = cursor.position;

        cursor.seek_start(total_size_offset);
        cursor.write_u32(end_pos - start_pos + 16);

        cursor.seek_start(entities_size_offset);
        cursor.write_u32(end_pos - start_pos);

        cursor.seek_start(end_pos);
    }
}

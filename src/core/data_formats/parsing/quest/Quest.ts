import { Vec3 } from "../../vector";
import { npc_data, NpcType, NpcTypeData } from "./npc_types";
import { id_to_object_type, object_data, ObjectType, ObjectTypeData } from "./object_types";
import { DatEvent, DatUnknown, NPC_BYTE_SIZE, OBJECT_BYTE_SIZE } from "./dat";
import { Episode } from "./Episode";
import { Segment } from "../../asm/instructions";
import { get_npc_type } from "./get_npc_type";
import { ArrayBufferBlock } from "../../block/ArrayBufferBlock";
import { assert } from "../../../util";
import { Endianness } from "../../block/Endianness";

const DEFAULT_SCALE: Vec3 = Object.freeze({ x: 1, y: 1, z: 1 });

export class Quest {
    constructor(
        public id: number,
        public language: number,
        public name: string,
        public short_description: string,
        public long_description: string,
        public episode: Episode,
        readonly objects: readonly QuestObject[],
        readonly npcs: readonly QuestNpc[],
        readonly events: QuestEvent[],
        /**
         * (Partial) raw DAT data that can't be parsed yet by Phantasmal.
         */
        readonly dat_unknowns: DatUnknown[],
        readonly object_code: readonly Segment[],
        readonly shop_items: number[],
        readonly map_designations: Map<number, number>,
    ) {}
}

export type EntityTypeData = NpcTypeData | ObjectTypeData;

export type EntityType = NpcType | ObjectType;

export interface QuestEntity<Type extends EntityType = EntityType> {
    area_id: number;
    readonly data: ArrayBufferBlock;
    type: Type;
    section_id: number;
    position: Vec3;
    rotation: Vec3;
}

export class QuestNpc implements QuestEntity<NpcType> {
    episode: Episode;
    area_id: number;
    readonly data: ArrayBufferBlock;

    get type(): NpcType {
        return get_npc_type(this.episode, this.type_id, this.regular, this.skin, this.area_id);
    }

    set type(type: NpcType) {
        const data = npc_data(type);

        if (data.episode != undefined) {
            this.episode = data.episode;
        }

        this.type_id = data.type_id ?? 0;
        this.regular = data.regular ?? true;
        this.skin = data.skin ?? 0;

        if (data.area_ids.length > 0 && !data.area_ids.includes(this.area_id)) {
            this.area_id = data.area_ids[0];
        }
    }

    get type_id(): number {
        return this.data.get_u16(0);
    }

    set type_id(type_id: number) {
        this.data.set_u16(0, type_id);
    }

    get section_id(): number {
        return this.data.get_u16(12);
    }

    set section_id(section_id: number) {
        this.data.set_u16(12, section_id);
    }

    get wave(): number {
        return this.data.get_u16(14);
    }

    set wave(wave: number) {
        this.data.set_u16(14, wave);
    }

    get wave_2(): number {
        return this.data.get_u32(16);
    }

    set wave_2(wave_2: number) {
        this.data.set_u32(16, wave_2);
    }

    /**
     * Section-relative position.
     */
    get position(): Vec3 {
        return {
            x: this.data.get_f32(20),
            y: this.data.get_f32(24),
            z: this.data.get_f32(28),
        };
    }

    set position(position: Vec3) {
        this.data.set_f32(20, position.x);
        this.data.set_f32(24, position.y);
        this.data.set_f32(28, position.z);
    }

    get rotation(): Vec3 {
        return {
            x: (this.data.get_i32(32) / 0xffff) * 2 * Math.PI,
            y: (this.data.get_i32(36) / 0xffff) * 2 * Math.PI,
            z: (this.data.get_i32(40) / 0xffff) * 2 * Math.PI,
        };
    }

    set rotation(rotation: Vec3) {
        this.data.set_i32(32, Math.round((rotation.x / (2 * Math.PI)) * 0xffff));
        this.data.set_i32(36, Math.round((rotation.y / (2 * Math.PI)) * 0xffff));
        this.data.set_i32(40, Math.round((rotation.z / (2 * Math.PI)) * 0xffff));
    }

    /**
     * Seemingly 3 floats, not sure what they represent.
     * The y component is used to help determine what the NpcType is.
     */
    get scale(): Vec3 {
        return {
            x: this.data.get_f32(44),
            y: this.data.get_f32(48),
            z: this.data.get_f32(52),
        };
    }

    set scale(scale: Vec3) {
        this.data.set_f32(44, scale.x);
        this.data.set_f32(48, scale.y);
        this.data.set_f32(52, scale.z);
    }

    get regular(): boolean {
        return Math.abs(this.data.get_f32(48) - 1) > 0.00001;
    }

    set regular(regular: boolean) {
        this.data.set_i32(48, (this.data.get_i32(48) & ~0x800000) | (regular ? 0 : 0x800000));
    }

    get npc_id(): number {
        return this.data.get_f32(56);
    }

    /**
     * Only seems to be valid for non-enemies.
     */
    get script_label(): number {
        return Math.round(this.data.get_f32(60));
    }

    get skin(): number {
        return this.data.get_u32(64);
    }

    set skin(skin: number) {
        this.data.set_u32(64, skin);
    }

    constructor(episode: Episode, area_id: number, data: ArrayBufferBlock) {
        assert(
            data.size === NPC_BYTE_SIZE,
            () => `Data size should be ${NPC_BYTE_SIZE} but was ${data.size}.`,
        );

        this.episode = episode;
        this.area_id = area_id;
        this.data = data;
    }

    static create(type: NpcType, area_id: number, wave: number): QuestNpc {
        const npc = new QuestNpc(
            Episode.I,
            area_id,
            new ArrayBufferBlock(NPC_BYTE_SIZE, Endianness.Little),
        );

        // Set scale before type because type will change it.
        npc.scale = DEFAULT_SCALE;
        npc.type = type;
        // Set area_id after type, because you might want to overwrite the area_id that type has
        // determined.
        npc.area_id = area_id;
        npc.wave = wave;
        npc.wave_2 = wave;

        return npc;
    }
}

export class QuestObject implements QuestEntity<ObjectType> {
    area_id: number;
    readonly data: ArrayBufferBlock;

    get type(): ObjectType {
        return id_to_object_type(this.type_id);
    }

    set type(type: ObjectType) {
        this.type_id = object_data(type).type_id ?? 0;
    }

    get type_id(): number {
        return this.data.get_u16(0);
    }

    set type_id(type_id: number) {
        this.data.set_u16(0, type_id);
    }

    get id(): number {
        return this.data.get_u16(8);
    }

    get group_id(): number {
        return this.data.get_u16(10);
    }

    get section_id(): number {
        return this.data.get_u16(12);
    }

    set section_id(section_id: number) {
        this.data.set_u16(12, section_id);
    }

    /**
     * Section-relative position.
     */
    get position(): Vec3 {
        return {
            x: this.data.get_f32(16),
            y: this.data.get_f32(20),
            z: this.data.get_f32(24),
        };
    }

    set position(position: Vec3) {
        this.data.set_f32(16, position.x);
        this.data.set_f32(20, position.y);
        this.data.set_f32(24, position.z);
    }

    get rotation(): Vec3 {
        return {
            x: (this.data.get_i32(28) / 0xffff) * 2 * Math.PI,
            y: (this.data.get_i32(32) / 0xffff) * 2 * Math.PI,
            z: (this.data.get_i32(36) / 0xffff) * 2 * Math.PI,
        };
    }

    set rotation(rotation: Vec3) {
        this.data.set_i32(28, Math.round((rotation.x / (2 * Math.PI)) * 0xffff));
        this.data.set_i32(32, Math.round((rotation.y / (2 * Math.PI)) * 0xffff));
        this.data.set_i32(36, Math.round((rotation.z / (2 * Math.PI)) * 0xffff));
    }

    get script_label(): number | undefined {
        switch (this.type) {
            case ObjectType.ScriptCollision:
            case ObjectType.ForestConsole:
            case ObjectType.TalkLinkToSupport:
                return this.data.get_u32(52);
            case ObjectType.RicoMessagePod:
                return this.data.get_u32(56);
            default:
                return undefined;
        }
    }

    get script_label_2(): number | undefined {
        switch (this.type) {
            case ObjectType.RicoMessagePod:
                return this.data.get_u32(60);
            default:
                return undefined;
        }
    }

    constructor(area_id: number, data: ArrayBufferBlock) {
        assert(
            data.size === OBJECT_BYTE_SIZE,
            () => `Data size should be ${OBJECT_BYTE_SIZE} but was ${data.size}.`,
        );

        this.area_id = area_id;
        this.data = data;
    }

    static create(type: ObjectType, area_id: number): QuestObject {
        const obj = new QuestObject(
            area_id,
            new ArrayBufferBlock(OBJECT_BYTE_SIZE, Endianness.Little),
        );

        obj.type = type;
        // Set area_id after type, because you might want to overwrite the area_id that type has
        // determined.
        obj.area_id = area_id;

        return obj;
    }
}

export type QuestEvent = DatEvent;

export function entity_type_to_string(type: EntityType): string {
    return (NpcType as any)[type] ?? (ObjectType as any)[type];
}

export function is_npc_type(entity_type: EntityType): entity_type is NpcType {
    return NpcType[entity_type] != undefined;
}

export function entity_data(type: EntityType): EntityTypeData {
    return npc_data(type as NpcType) ?? object_data(type as ObjectType);
}

import { npc_data, NpcType, NpcTypeData } from "./npc_types";
import { object_data, ObjectType, ObjectTypeData } from "./object_types";
import { DatEvent, DatUnknown, NPC_BYTE_SIZE } from "./dat";
import { Episode } from "./Episode";
import { Segment } from "../../asm/instructions";
import { get_npc_type, QuestNpc } from "./QuestNpc";
import { get_object_type, QuestObject } from "./QuestObject";
import { EntityProp, EntityPropType } from "./properties";
import { angle_to_rad, rad_to_angle } from "../ninja/angle";
import { assert, require_finite, require_integer } from "../../../util";

export type Quest = {
    id: number;
    language: number;
    name: string;
    short_description: string;
    long_description: string;
    episode: Episode;
    readonly objects: readonly QuestObject[];
    readonly npcs: readonly QuestNpc[];
    readonly events: QuestEvent[];
    /**
     * (Partial) raw DAT data that can't be parsed yet by Phantasmal.
     */
    readonly dat_unknowns: DatUnknown[];
    readonly object_code: readonly Segment[];
    readonly shop_items: number[];
    readonly map_designations: Map<number, number>;
};

export type EntityTypeData = NpcTypeData | ObjectTypeData;

export type EntityType = NpcType | ObjectType;

export type QuestEntity = QuestNpc | QuestObject;

export type QuestEvent = DatEvent;

export function entity_type_to_string(type: EntityType): string {
    return (NpcType as any)[type] ?? (ObjectType as any)[type];
}

export function is_npc_type(entity_type: EntityType): entity_type is NpcType {
    return NpcType[entity_type] != undefined;
}

export function is_object_type(entity_type: EntityType): entity_type is ObjectType {
    return ObjectType[entity_type] != undefined;
}

export function entity_data(type: EntityType): EntityTypeData {
    return npc_data(type as NpcType) ?? object_data(type as ObjectType);
}

export function get_entity_type(entity: QuestEntity): EntityType {
    return entity.data.byteLength === NPC_BYTE_SIZE
        ? get_npc_type(entity as QuestNpc)
        : get_object_type(entity as QuestObject);
}

export function get_entity_prop_value(entity: QuestEntity, prop: EntityProp): number {
    switch (prop.type) {
        case EntityPropType.U8:
            return entity.view.getUint8(prop.offset);
        case EntityPropType.U16:
            return entity.view.getUint16(prop.offset, true);
        case EntityPropType.U32:
            return entity.view.getUint32(prop.offset, true);
        case EntityPropType.I8:
            return entity.view.getInt8(prop.offset);
        case EntityPropType.I16:
            return entity.view.getInt16(prop.offset, true);
        case EntityPropType.I32:
            return entity.view.getInt32(prop.offset, true);
        case EntityPropType.F32:
            return entity.view.getFloat32(prop.offset, true);
        case EntityPropType.Angle:
            return angle_to_rad(entity.view.getInt32(prop.offset, true));
    }
}

export function set_entity_prop_value(entity: QuestEntity, prop: EntityProp, value: number): void {
    switch (prop.type) {
        case EntityPropType.U8:
            require_in_bounds(value, 0, 0xff);
            entity.view.setUint8(prop.offset, value);
            break;
        case EntityPropType.U16:
            require_in_bounds(value, 0, 0xffff);
            entity.view.setUint16(prop.offset, value, true);
            break;
        case EntityPropType.U32:
            require_in_bounds(value, 0, 0xffffffff);
            entity.view.setUint32(prop.offset, value, true);
            break;
        case EntityPropType.I8:
            require_in_bounds(value, -0x80, 0x7f);
            entity.view.setInt8(prop.offset, value);
            break;
        case EntityPropType.I16:
            require_in_bounds(value, -0x8000, 0x7fff);
            entity.view.setInt16(prop.offset, value, true);
            break;
        case EntityPropType.I32:
            require_in_bounds(value, -0x80000000, 0x7fffffff);
            entity.view.setInt32(prop.offset, value, true);
            break;
        case EntityPropType.F32:
            entity.view.setFloat32(prop.offset, value, true);
            break;
        case EntityPropType.Angle:
            require_finite(value, "value");
            entity.view.setInt32(prop.offset, rad_to_angle(value), true);
            break;
    }
}

function require_in_bounds(value: unknown, min: number, max: number): asserts value is number {
    require_integer(value, "value");
    assert(value >= min, () => `value should be greater than or equal to ${min} but was ${value}.`);
    assert(value <= max, () => `value should be less than or equal to ${max} but was ${value}.`);
}

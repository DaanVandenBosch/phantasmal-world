import { npc_data, NpcType, NpcTypeData } from "./npc_types";
import { object_data, ObjectType, ObjectTypeData } from "./object_types";
import { DatEvent, DatUnknown } from "./dat";
import { Episode } from "./Episode";
import { Segment } from "../../asm/instructions";
import { QuestNpc } from "./QuestNpc";
import { QuestObject } from "./QuestObject";

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

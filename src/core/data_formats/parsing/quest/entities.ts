import { Vec3 } from "../../vector";
import { npc_data, NpcType, NpcTypeData } from "./npc_types";
import { object_data, ObjectType, ObjectTypeData } from "./object_types";

export type QuestEntity = QuestNpc | QuestObject;

export type QuestNpc = {
    readonly type: NpcType;
    readonly area_id: number;
    readonly section_id: number;
    /**
     * Section-relative position
     */
    readonly position: Vec3;
    readonly rotation: Vec3;
    readonly scale: Vec3;
    /**
     * Data of which the purpose hasn't been discovered yet.
     */
    readonly unknown: number[][];
    readonly pso_type_id: number;
    readonly pso_skin: number;
};

export type QuestObject = {
    readonly type: ObjectType;
    readonly area_id: number;
    readonly section_id: number;
    /**
     * Section-relative position
     */
    readonly position: Vec3;
    readonly rotation: Vec3;
    readonly scale: Vec3;
    /**
     * Data of which the purpose hasn't been discovered yet.
     */
    readonly unknown: number[][];
};

export type EntityTypeData = NpcTypeData | ObjectTypeData;

export type EntityType = NpcType | ObjectType;

export function entity_type_to_string(type: EntityType): string {
    return (NpcType as any)[type] || (ObjectType as any)[type];
}

export function entity_data(type: EntityType): EntityTypeData {
    return npc_data(type as NpcType) || object_data(type as ObjectType);
}

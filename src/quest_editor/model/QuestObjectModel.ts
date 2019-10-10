import { QuestEntityModel } from "./QuestEntityModel";
import { ObjectType } from "../../core/data_formats/parsing/quest/object_types";
import { Euler, Vector3 } from "three";

export class QuestObjectModel extends QuestEntityModel<ObjectType> {
    readonly id: number;
    readonly group_id: number;
    readonly properties: Map<string, number>;
    /**
     * Data of which the purpose hasn't been discovered yet.
     */
    readonly unknown: readonly number[][];

    constructor(
        type: ObjectType,
        id: number,
        group_id: number,
        area_id: number,
        section_id: number,
        position: Vector3,
        rotation: Euler,
        properties: Map<string, number>,
        unknown: readonly number[][],
    ) {
        super(type, area_id, section_id, position, rotation);

        this.id = id;
        this.group_id = group_id;
        this.properties = properties;
        this.unknown = unknown;
    }
}

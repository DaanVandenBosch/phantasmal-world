import { QuestEntityModel } from "./QuestEntityModel";
import { ObjectType } from "../../core/data_formats/parsing/quest/object_types";
import { Vec3 } from "../../core/data_formats/vector";

export class QuestObjectModel extends QuestEntityModel<ObjectType> {
    readonly id: number;
    readonly group_id: number;

    constructor(
        type: ObjectType,
        id: number,
        group_id: number,
        area_id: number,
        section_id: number,
        position: Vec3,
        rotation: Vec3,
        properties: Map<string, number>,
        unknown: number[][],
    ) {
        super(type, area_id, section_id, position, rotation);

        this.id = id;
        this.group_id = group_id;
    }
}

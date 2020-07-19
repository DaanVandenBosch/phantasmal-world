import { QuestEntityModel } from "./QuestEntityModel";
import { ObjectType } from "../../core/data_formats/parsing/quest/object_types";
import { defined } from "../../core/util";
import {
    get_object_position,
    get_object_rotation,
    get_object_section_id,
    get_object_type,
    QuestObject,
    set_object_position,
    set_object_rotation,
    set_object_section_id,
} from "../../core/data_formats/parsing/quest/QuestObject";
import { Vec3 } from "../../core/data_formats/vector";

export class QuestObjectModel extends QuestEntityModel<ObjectType, QuestObject> {
    get type(): ObjectType {
        return get_object_type(this.entity);
    }

    constructor(object: QuestObject) {
        defined(object, "object");

        super(object);
    }

    protected get_entity_section_id(): number {
        return get_object_section_id(this.entity);
    }

    protected set_entity_section_id(section_id: number): void {
        set_object_section_id(this.entity, section_id);
    }

    protected get_entity_position(): Vec3 {
        return get_object_position(this.entity);
    }

    protected set_entity_position(position: Vec3): void {
        set_object_position(this.entity, position);
    }

    protected get_entity_rotation(): Vec3 {
        return get_object_rotation(this.entity);
    }

    protected set_entity_rotation(rotation: Vec3): void {
        set_object_rotation(this.entity, rotation);
    }
}

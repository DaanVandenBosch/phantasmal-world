import { WritableProperty } from "../../core/observable/property/WritableProperty";
import { Property } from "../../core/observable/property/Property";
import { EntityProp, EntityPropType } from "../../core/data_formats/parsing/quest/properties";
import { property } from "../../core/observable";
import {
    get_entity_prop_value,
    QuestEntity,
    set_entity_prop_value,
} from "../../core/data_formats/parsing/quest/Quest";

export class QuestEntityPropModel {
    private readonly entity: QuestEntity;
    private readonly prop: EntityProp;
    private readonly _value: WritableProperty<number>;

    readonly name: string;
    readonly type: EntityPropType;
    readonly value: Property<number>;

    constructor(quest_entity: QuestEntity, entity_prop: EntityProp) {
        this.entity = quest_entity;
        this.prop = entity_prop;

        this.name = entity_prop.name;

        this.type = entity_prop.type;

        this._value = property(get_entity_prop_value(quest_entity, entity_prop));
        this.value = this._value;
    }

    set_value(value: number): void {
        set_entity_prop_value(this.entity, this.prop, value);
        this._value.val = value;
    }
}

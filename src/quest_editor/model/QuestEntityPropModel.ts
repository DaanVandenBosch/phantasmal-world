import { WritableProperty } from "../../core/observable/property/WritableProperty";
import { Property } from "../../core/observable/property/Property";
import { EntityProp, EntityPropType } from "../../core/data_formats/parsing/quest/properties";
import { property } from "../../core/observable";
import {
    get_entity_prop_value,
    set_entity_prop_value,
} from "../../core/data_formats/parsing/quest/Quest";
import { QuestEntityModel } from "./QuestEntityModel";
import { ObjectType } from "../../core/data_formats/parsing/quest/object_types";

export class QuestEntityPropModel {
    private readonly entity: QuestEntityModel;
    private readonly prop: EntityProp;
    private readonly _value: WritableProperty<number>;
    private readonly affects_model: boolean;

    readonly name: string;
    readonly offset: number;
    readonly type: EntityPropType;
    readonly value: Property<number>;

    constructor(entity: QuestEntityModel, entity_prop: EntityProp) {
        this.entity = entity;
        this.prop = entity_prop;

        this.name = entity_prop.name;

        this.offset = entity_prop.offset;

        this.type = entity_prop.type;

        this._value = property(get_entity_prop_value(entity.entity, entity_prop));
        this.value = this._value;

        switch (this.entity.type) {
            case ObjectType.Probe:
                this.affects_model = entity_prop.offset === 40;
                break;

            case ObjectType.Saw:
            case ObjectType.LaserDetect:
                this.affects_model = entity_prop.offset === 48;
                break;

            case ObjectType.Sonic:
            case ObjectType.LittleCryotube:
            case ObjectType.Cactus:
            case ObjectType.BigBrownRock:
            case ObjectType.BigBlackRocks:
            case ObjectType.BeeHive:
                this.affects_model = entity_prop.offset === 52;
                break;

            case ObjectType.ForestConsole:
                this.affects_model = entity_prop.offset === 56;
                break;

            case ObjectType.PrincipalWarp:
            case ObjectType.LaserFence:
            case ObjectType.LaserSquareFence:
            case ObjectType.LaserFenceEx:
            case ObjectType.LaserSquareFenceEx:
                this.affects_model = entity_prop.offset === 60;
                break;

            default:
                this.affects_model = false;
                break;
        }
    }

    set_value(value: number, propagate_to_entity: boolean = true): void {
        set_entity_prop_value(this.entity.entity, this.prop, value);
        this._value.val = value;

        if (propagate_to_entity && this.affects_model) {
            this.entity.set_model(value, false);
        }
    }
}

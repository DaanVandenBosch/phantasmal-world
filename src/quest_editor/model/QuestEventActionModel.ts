import { require_non_negative_integer } from "../../core/util";
import { Property } from "../../core/observable/property/Property";
import { WritableProperty } from "../../core/observable/property/WritableProperty";
import { property } from "../../core/observable";
import { enum_values } from "../../core/enums";

export enum QuestEventActionType {
    SpawnNpcs,
    Unlock,
    Lock,
}

export const QuestEventActionTypes: readonly QuestEventActionType[] = enum_values(
    QuestEventActionType,
);

export type QuestEventActionModel =
    | QuestEventActionSpawnNpcsModel
    | QuestEventActionUnlockModel
    | QuestEventActionLockModel;

export class QuestEventActionSpawnNpcsModel {
    readonly type = QuestEventActionType.SpawnNpcs;
    readonly section_id: number;
    readonly appear_flag: number;

    constructor(section_id: number, appear_flag: number) {
        require_non_negative_integer(section_id, "section_id");
        require_non_negative_integer(appear_flag, "appear_flag");

        this.section_id = section_id;
        this.appear_flag = appear_flag;
    }
}

export class QuestEventActionUnlockModel {
    private readonly _door_id: WritableProperty<number>;

    readonly type = QuestEventActionType.Unlock;
    readonly door_id: Property<number>;

    constructor(door_id: number) {
        require_non_negative_integer(door_id, "door_id");

        this._door_id = property(door_id);
        this.door_id = this._door_id;
    }

    set_door_id(door_id: number): void {
        require_non_negative_integer(door_id, "door_id");

        this._door_id.val = door_id;
    }
}

export class QuestEventActionLockModel {
    private readonly _door_id: WritableProperty<number>;

    readonly type = QuestEventActionType.Lock;
    readonly door_id: Property<number>;

    constructor(door_id: number) {
        require_non_negative_integer(door_id, "door_id");

        this._door_id = property(door_id);
        this.door_id = this._door_id;
    }

    set_door_id(door_id: number): void {
        require_non_negative_integer(door_id, "door_id");

        this._door_id.val = door_id;
    }
}

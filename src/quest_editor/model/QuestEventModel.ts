import { QuestEventActionModel } from "./QuestEventActionModel";
import { WritableListProperty } from "../../core/observable/property/list/WritableListProperty";
import { list_property } from "../../core/observable";
import { ListProperty } from "../../core/observable/property/list/ListProperty";

export class QuestEventModel {
    private readonly _actions: WritableListProperty<QuestEventActionModel> = list_property();

    readonly id: number;
    readonly section_id: number;
    readonly wave: number;
    readonly delay: number;
    readonly actions: ListProperty<QuestEventActionModel> = this._actions;
    readonly unknown: number;

    constructor(id: number, section_id: number, wave: number, delay: number, unknown: number) {
        if (!Number.isInteger(id)) throw new Error("id should be an integer.");
        if (!Number.isInteger(section_id)) throw new Error("section_id should be an integer.");
        if (!Number.isInteger(wave)) throw new Error("wave should be an integer.");
        if (!Number.isInteger(delay)) throw new Error("delay should be an integer.");
        if (!Number.isInteger(unknown)) throw new Error("unknown should be an integer.");

        this.id = id;
        this.section_id = section_id;
        this.wave = wave;
        this.delay = delay;
        this.unknown = unknown;
    }

    add_action(action: QuestEventActionModel): void {
        this._actions.push(action);
    }
}

import { QuestEventActionModel } from "./QuestEventActionModel";
import { WritableListProperty } from "../../core/observable/property/list/WritableListProperty";
import { list_property, property } from "../../core/observable";
import { ListProperty } from "../../core/observable/property/list/ListProperty";
import { Property } from "../../core/observable/property/Property";
import { WritableProperty } from "../../core/observable/property/WritableProperty";
import { require_integer, require_non_negative_integer } from "../../core/util";

export class QuestEventModel {
    private readonly _section_id: WritableProperty<number>;
    private readonly _delay: WritableProperty<number>;
    private readonly _actions: WritableListProperty<QuestEventActionModel> = list_property();

    readonly id: number;
    readonly section_id: Property<number>;
    readonly wave: number;
    readonly delay: Property<number>;
    readonly actions: ListProperty<QuestEventActionModel> = this._actions;
    readonly unknown: number;

    constructor(id: number, section_id: number, wave: number, delay: number, unknown: number) {
        require_non_negative_integer(id, "id");
        require_non_negative_integer(section_id, "section_id");
        require_non_negative_integer(wave, "wave");
        require_non_negative_integer(delay, "delay");
        require_integer(unknown, "unknown");

        this.id = id;
        this._section_id = property(section_id);
        this.section_id = this._section_id;
        this.wave = wave;
        this._delay = property(delay);
        this.delay = this._delay;
        this.unknown = unknown;
    }

    set_section_id(section_id: number): void {
        require_non_negative_integer(section_id, "section_id");
        this._section_id.val = section_id;
    }

    set_delay(delay: number): void {
        require_non_negative_integer(delay, "delay");
        this._delay.val = delay;
    }

    add_action(action: QuestEventActionModel): void {
        this._actions.push(action);
    }
}

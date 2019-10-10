import { QuestEventModel } from "./QuestEventModel";
import { ListProperty } from "../../core/observable/property/list/ListProperty";
import { WritableListProperty } from "../../core/observable/property/list/WritableListProperty";
import { list_property } from "../../core/observable";

export class QuestEventChainModel {
    private readonly _events: WritableListProperty<QuestEventModel>;

    readonly events: ListProperty<QuestEventModel>;

    constructor(events: QuestEventModel[]) {
        this._events = list_property(undefined, ...events);
        this.events = this._events;
    }
}

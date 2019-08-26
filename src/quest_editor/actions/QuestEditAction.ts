import { Action } from "../../core/undo/Action";
import { QuestModel } from "../model/QuestModel";
import { PropertyChangeEvent } from "../../core/observable/Property";

export abstract class QuestEditAction<T> implements Action {
    abstract readonly description: string;

    protected new: T;
    protected old: T;

    constructor(protected quest: QuestModel, event: PropertyChangeEvent<T>) {
        this.new = event.value;
        this.old = event.old_value;
    }

    abstract undo(): void;

    abstract redo(): void;
}

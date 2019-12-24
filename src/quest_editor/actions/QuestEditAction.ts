import { Action } from "../../core/undo/Action";
import { QuestModel } from "../model/QuestModel";

export abstract class QuestEditAction<T> implements Action {
    abstract readonly description: string;

    protected readonly old: T;
    protected readonly new: T;

    constructor(protected readonly quest: QuestModel, old_value: T, new_value: T) {
        this.old = old_value;
        this.new = new_value;
    }

    abstract undo(): void;

    abstract redo(): void;
}

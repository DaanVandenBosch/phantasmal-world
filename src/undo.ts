import { computed, observable } from "mobx";

export class Action {
    constructor(
        readonly description: string,
        readonly undo: () => void,
        readonly redo: () => void
    ) {}
}

export class UndoStack {
    @observable.ref private stack: Action[] = [];
    /**
     * The index where new actions are inserted.
     */
    @observable private index = 0;

    @computed get can_undo(): boolean {
        return this.index > 0;
    }

    @computed get can_redo(): boolean {
        return this.index < this.stack.length;
    }

    /**
     * The first action that will be undone when calling undo().
     */
    @computed get first_undo(): Action | undefined {
        return this.stack[this.index - 1];
    }

    /**
     * The first action that will be redone when calling redo().
     */
    @computed get first_redo(): Action | undefined {
        return this.stack[this.index];
    }

    push_action(description: string, undo: () => void, redo: () => void): void {
        this.push(new Action(description, undo, redo));
    }

    push(action: Action): void {
        this.stack.splice(this.index, this.stack.length - this.index, action);
        this.index++;
    }

    undo(): boolean {
        if (this.can_undo) {
            this.stack[--this.index].undo();
            return true;
        } else {
            return false;
        }
    }

    redo(): boolean {
        if (this.can_redo) {
            this.stack[this.index++].redo();
            return true;
        } else {
            return false;
        }
    }

    clear(): void {
        this.stack = [];
        this.index = 0;
    }
}

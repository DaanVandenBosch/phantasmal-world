import { computed, observable, IObservableArray, action } from "mobx";

export class Action {
    constructor(
        readonly description: string,
        readonly undo: () => void,
        readonly redo: () => void
    ) {}
}

export class UndoStack {
    @observable static current?: UndoStack;

    @observable private readonly stack: IObservableArray<Action> = observable.array([], {
        deep: false,
    });
    /**
     * The index where new actions are inserted.
     */
    @observable private index = 0;

    make_current(): void {
        UndoStack.current = this;
    }

    ensure_not_current(): void {
        if (UndoStack.current === this) {
            UndoStack.current = undefined;
        }
    }

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
        return this.can_undo ? this.stack[this.index - 1] : undefined;
    }

    /**
     * The first action that will be redone when calling redo().
     */
    @computed get first_redo(): Action | undefined {
        return this.can_redo ? this.stack[this.index] : undefined;
    }

    @action
    push_action(description: string, undo: () => void, redo: () => void): void {
        this.push(new Action(description, undo, redo));
    }

    @action
    push(action: Action): void {
        this.stack.splice(this.index, this.stack.length - this.index, action);
        this.index++;
    }

    @action
    undo(): boolean {
        if (this.can_undo) {
            this.stack[--this.index].undo();
            return true;
        } else {
            return false;
        }
    }

    @action
    redo(): boolean {
        if (this.can_redo) {
            this.stack[this.index++].redo();
            return true;
        } else {
            return false;
        }
    }

    @action
    clear(): void {
        this.stack.clear();
        this.index = 0;
    }
}

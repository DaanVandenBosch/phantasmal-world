import { computed, observable, IObservableArray, action } from "mobx";

export class Action {
    constructor(
        readonly description: string,
        readonly undo: () => void,
        readonly redo: () => void
    ) {}
}

class UndoManager {
    @observable current?: Undo;

    @computed
    get can_undo(): boolean {
        return this.current ? this.current.can_undo : false;
    }

    @computed
    get can_redo(): boolean {
        return this.current ? this.current.can_redo : false;
    }

    @computed
    get first_undo(): Action | undefined {
        return this.current && this.current.first_undo;
    }

    @computed
    get first_redo(): Action | undefined {
        return this.current && this.current.first_redo;
    }

    undo(): boolean {
        return this.current ? this.current.undo() : false;
    }

    redo(): boolean {
        return this.current ? this.current.redo() : false;
    }
}

export const undo_manager = new UndoManager();

interface Undo {
    make_current(): void;

    ensure_not_current(): void;

    readonly can_undo: boolean;

    readonly can_redo: boolean;

    /**
     * The first action that will be undone when calling undo().
     */
    readonly first_undo: Action | undefined;

    /**
     * The first action that will be redone when calling redo().
     */
    readonly first_redo: Action | undefined;

    undo(): boolean;

    redo(): boolean;

    reset(): void;
}

/**
 * Simply contains a single action. `can_undo` and `can_redo` must be managed manually.
 */
export class SimpleUndo implements Undo {
    @observable.ref action: Action;

    constructor(description: string, undo: () => void, redo: () => void) {
        this.action = new Action(description, undo, redo);
    }

    @action
    make_current(): void {
        undo_manager.current = this;
    }

    @action
    ensure_not_current(): void {
        if (undo_manager.current === this) {
            undo_manager.current = undefined;
        }
    }

    @observable _can_undo = false;

    get can_undo(): boolean {
        return this._can_undo;
    }

    set can_undo(can_undo: boolean) {
        this._can_undo = can_undo;
    }

    @observable _can_redo = false;

    get can_redo(): boolean {
        return this._can_redo;
    }

    set can_redo(can_redo: boolean) {
        this._can_redo = can_redo;
    }

    @computed get first_undo(): Action | undefined {
        return this.can_undo ? this.action : undefined;
    }

    @computed get first_redo(): Action | undefined {
        return this.can_redo ? this.action : undefined;
    }

    @action
    undo(): boolean {
        if (this.can_undo) {
            this.action.undo();
            return true;
        } else {
            return false;
        }
    }

    @action
    redo(): boolean {
        if (this.can_redo) {
            this.action.redo();
            return true;
        } else {
            return false;
        }
    }

    @action
    reset(): void {
        this._can_undo = false;
        this._can_redo = false;
    }
}

/**
 * Full-fledged linear undo/redo implementation.
 */
export class UndoStack implements Undo {
    @observable private readonly stack: IObservableArray<Action> = observable.array([], {
        deep: false,
    });
    /**
     * The index where new actions are inserted.
     */
    @observable private index = 0;

    @action
    make_current(): void {
        undo_manager.current = this;
    }

    @action
    ensure_not_current(): void {
        if (undo_manager.current === this) {
            undo_manager.current = undefined;
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

    /**
     * Pop an action off the stack without undoing.
     */
    @action
    pop(): Action | undefined {
        return this.stack.splice(--this.index, 1)[0];
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
    reset(): void {
        this.stack.clear();
        this.index = 0;
    }
}

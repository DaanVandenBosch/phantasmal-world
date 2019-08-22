import { Undo } from "./Undo";
import { WritableArrayProperty } from "../observable/WritableArrayProperty";
import { Action } from "./Action";
import { array_property, map, property } from "../observable";
import { NOOP_UNDO } from "./noop_undo";
import { undo_manager } from "./UndoManager";

/**
 * Full-fledged linear undo/redo implementation.
 */
export class UndoStack implements Undo {
    private readonly stack: WritableArrayProperty<Action> = array_property();

    /**
     * The index where new actions are inserted.
     */
    private readonly index = property(0);

    make_current(): void {
        undo_manager.current.val = this;
    }

    ensure_not_current(): void {
        if (undo_manager.current.val === this) {
            undo_manager.current.val = NOOP_UNDO;
        }
    }

    readonly can_undo = this.index.map(index => index > 0);

    readonly can_redo = map((stack, index) => index < stack.length, this.stack, this.index);

    readonly first_undo = this.can_undo.map(can_undo => {
        return can_undo ? this.stack.get(this.index.val - 1) : undefined;
    });

    readonly first_redo = this.can_redo.map(can_redo => {
        return can_redo ? this.stack.get(this.index.val) : undefined;
    });

    push_action(description: string, undo: () => void, redo: () => void): void {
        this.push(new Action(description, undo, redo));
    }

    push(action: Action): void {
        this.stack.splice(this.index.val, this.stack.length.val - this.index.val, action);
        this.index.update(i => i + 1);
    }

    /**
     * Pop an action off the stack without undoing.
     */
    pop(): Action | undefined {
        this.index.update(i => i - 1);
        return this.stack.splice(this.index.val, 1)[0];
    }

    undo(): boolean {
        if (this.can_undo) {
            this.index.update(i => i - 1);
            this.stack.get(this.index.val).undo();
            return true;
        } else {
            return false;
        }
    }

    redo(): boolean {
        if (this.can_redo) {
            this.stack.get(this.index.val).redo();
            this.index.update(i => i + 1);
            return true;
        } else {
            return false;
        }
    }

    reset(): void {
        this.stack.clear();
        this.index.val = 0;
    }
}

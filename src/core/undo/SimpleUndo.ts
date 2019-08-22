import { Undo } from "./Undo";
import { Action } from "./Action";
import { Property } from "../observable/Property";
import { property } from "../observable";
import { NOOP_UNDO } from "./noop_undo";
import { undo_manager } from "./UndoManager";

/**
 * Simply contains a single action. `can_undo` and `can_redo` must be managed manually.
 */
export class SimpleUndo implements Undo {
    private readonly _action: Action;
    readonly action: Property<Action>;

    constructor(description: string, undo: () => void, redo: () => void) {
        this._action = new Action(description, undo, redo);
        this.action = property(this._action);
    }

    make_current(): void {
        undo_manager.current.val = this;
    }

    ensure_not_current(): void {
        if (undo_manager.current.val === this) {
            undo_manager.current.val = NOOP_UNDO;
        }
    }

    readonly can_undo = property(false);

    readonly can_redo = property(false);

    readonly first_undo: Property<Action | undefined> = this.can_undo.map(can_undo =>
        can_undo ? this._action : undefined,
    );

    readonly first_redo: Property<Action | undefined> = this.can_redo.map(can_redo =>
        can_redo ? this._action : undefined,
    );

    undo(): boolean {
        if (this.can_undo) {
            this._action.undo();
            return true;
        } else {
            return false;
        }
    }

    redo(): boolean {
        if (this.can_redo) {
            this._action.redo();
            return true;
        } else {
            return false;
        }
    }

    reset(): void {
        this.can_undo.val = false;
        this.can_redo.val = false;
    }
}

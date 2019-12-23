import { Undo } from "./Undo";
import { Action } from "./Action";
import { Property } from "../observable/property/Property";
import { map, property } from "../observable";
import { undo_manager } from "./UndoManager";
import { WritableProperty } from "../observable/property/WritableProperty";

/**
 * Simply contains a single action. `can_undo` and `can_redo` must be managed manually.
 */
export class SimpleUndo implements Undo {
    readonly action: WritableProperty<Action>;

    constructor(description: string, undo: () => void, redo: () => void) {
        this.action = property({ description, undo, redo });

        this.first_undo = map(
            (action, can_undo) => (can_undo ? action : undefined),
            this.action,
            this.can_undo,
        );

        this.first_redo = map(
            (action, can_redo) => (can_redo ? action : undefined),
            this.action,
            this.can_redo,
        );
    }

    make_current(): void {
        undo_manager.current.val = this;
    }

    readonly can_undo = property(false);

    readonly can_redo = property(false);

    readonly first_undo: Property<Action | undefined>;

    readonly first_redo: Property<Action | undefined>;

    undo(): boolean {
        if (this.can_undo) {
            this.action.val.undo();
            return true;
        } else {
            return false;
        }
    }

    redo(): boolean {
        if (this.can_redo) {
            this.action.val.redo();
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

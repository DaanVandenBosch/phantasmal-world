import { property } from "../observable";
import { Undo } from "./Undo";

const NOOP_UNDO: Undo = {
    can_redo: property(false),
    can_undo: property(false),
    first_redo: property(undefined),
    first_undo: property(undefined),

    make_current() {
        undo_manager.current.val = this;
    },

    redo() {
        return false;
    },

    reset() {
        // Do nothing.
    },

    undo() {
        return false;
    },
};

export class UndoManager {
    readonly current = property<Undo>(NOOP_UNDO);

    can_undo = this.current.flat_map(c => c.can_undo);

    can_redo = this.current.flat_map(c => c.can_redo);

    first_undo = this.current.flat_map(c => c.first_undo);

    first_redo = this.current.flat_map(c => c.first_redo);

    undo(): boolean {
        return this.current.val.undo();
    }

    redo(): boolean {
        return this.current.val.redo();
    }

    make_noop_current(): void {
        undo_manager.current.val = NOOP_UNDO;
    }
}

export const undo_manager = new UndoManager();

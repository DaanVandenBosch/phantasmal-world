import { Undo } from "./Undo";
import { property } from "../observable";
import { undo_manager } from "./UndoManager";

export const NOOP_UNDO: Undo = {
    can_redo: property(false),
    can_undo: property(false),
    first_redo: property(undefined),
    first_undo: property(undefined),

    ensure_not_current() {
        // This is the default Undo, so it can't ensure it's not the current Undo.
    },

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

import { Undo } from "./Undo";
import { property } from "../observable";
import { undo_manager } from "./UndoManager";

export const NOOP_UNDO: Undo = {
    can_redo: property(false),
    can_undo: property(false),
    first_redo: property(undefined),
    first_undo: property(undefined),

    ensure_not_current() {},

    make_current() {
        undo_manager.current.val = this;
    },

    redo() {
        return false;
    },

    reset() {},

    undo() {
        return false;
    },
};

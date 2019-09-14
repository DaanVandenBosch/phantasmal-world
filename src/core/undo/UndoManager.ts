import { property } from "../observable";
import { Undo } from "./Undo";
import { NOOP_UNDO } from "./noop_undo";

class UndoManager {
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
}

export const undo_manager = new UndoManager();

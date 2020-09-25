import { property } from "../observable";
import { Undo } from "./Undo";
import { Property } from "../observable/property/Property";
import { Action } from "./Action";
import { WritableProperty } from "../observable/property/WritableProperty";

class NoopUndo implements Undo {
    readonly can_redo = property(false);
    readonly can_undo = property(false);
    readonly first_redo = property(undefined);
    readonly first_undo = property(undefined);

    constructor(private readonly manager: UndoManager) {}

    make_current(): void {
        this.manager.current.val = this;
    }

    redo(): boolean {
        return false;
    }

    reset(): void {
        // Do nothing.
    }

    undo(): boolean {
        return false;
    }
}

export class UndoManager {
    private readonly noop_undo = new NoopUndo(this);

    readonly current: WritableProperty<Undo> = property<Undo>(this.noop_undo);

    readonly can_undo: Property<boolean> = this.current.flat_map(c => c.can_undo);

    readonly can_redo: Property<boolean> = this.current.flat_map(c => c.can_redo);

    readonly first_undo: Property<Action | undefined> = this.current.flat_map(c => c.first_undo);

    readonly first_redo: Property<Action | undefined> = this.current.flat_map(c => c.first_redo);

    undo(): boolean {
        return this.current.val.undo();
    }

    redo(): boolean {
        return this.current.val.redo();
    }

    make_noop_current(): void {
        this.current.val = this.noop_undo;
    }
}

export const undo_manager = new UndoManager();

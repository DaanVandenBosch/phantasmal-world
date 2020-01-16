import { Undo } from "./Undo";
import { WritableListProperty } from "../observable/property/list/WritableListProperty";
import { Action } from "./Action";
import { list_property, map, property } from "../observable";
import { undo_manager } from "./UndoManager";
import { LogManager } from "../Logger";

const logger = LogManager.get("core/undo/UndoStack");

/**
 * Full-fledged linear undo/redo implementation.
 */
export class UndoStack implements Undo {
    private readonly stack: WritableListProperty<Action> = list_property();

    /**
     * The index where new actions are inserted.
     */
    private readonly index = property(0);

    readonly can_undo = this.index.map(index => index > 0);

    readonly can_redo = map((stack, index) => index < stack.length, this.stack, this.index);

    readonly first_undo = this.can_undo.map(can_undo => {
        return can_undo ? this.stack.get(this.index.val - 1) : undefined;
    });

    readonly first_redo = this.can_redo.map(can_redo => {
        return can_redo ? this.stack.get(this.index.val) : undefined;
    });

    private undoing_or_redoing = false;

    make_current(): void {
        undo_manager.current.val = this;
    }

    push(action: Action): Action {
        if (!this.undoing_or_redoing) {
            this.stack.splice(this.index.val, Infinity, action);
            this.index.update(i => i + 1);
        }

        return action;
    }

    /**
     * Pop an action off the stack without undoing.
     */
    pop(): Action | undefined {
        this.index.update(i => i - 1);
        return this.stack.splice(this.index.val, 1)[0];
    }

    undo(): boolean {
        if (this.can_undo.val && !this.undoing_or_redoing) {
            try {
                this.undoing_or_redoing = true;
                this.index.update(i => i - 1);
                this.stack.get(this.index.val).undo();
            } catch (e) {
                logger.warn("Error while undoing action.", e);
            } finally {
                this.undoing_or_redoing = false;
            }

            return true;
        } else {
            return false;
        }
    }

    redo(): boolean {
        if (this.can_redo.val && !this.undoing_or_redoing) {
            try {
                this.undoing_or_redoing = true;
                this.stack.get(this.index.val).redo();
                this.index.update(i => i + 1);
            } catch (e) {
                logger.warn("Error while redoing action.", e);
            } finally {
                this.undoing_or_redoing = false;
            }

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

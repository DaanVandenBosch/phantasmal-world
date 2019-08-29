import { Property } from "../observable/property/Property";
import { Action } from "./Action";

export interface Undo {
    make_current(): void;

    ensure_not_current(): void;

    readonly can_undo: Property<boolean>;

    readonly can_redo: Property<boolean>;

    /**
     * The first action that will be undone when calling undo().
     */
    readonly first_undo: Property<Action | undefined>;

    /**
     * The first action that will be redone when calling redo().
     */
    readonly first_redo: Property<Action | undefined>;

    undo(): boolean;

    redo(): boolean;

    reset(): void;
}

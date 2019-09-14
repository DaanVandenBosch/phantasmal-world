export interface Action {
    readonly description: string;
    readonly undo: () => void;
    readonly redo: () => void;
}

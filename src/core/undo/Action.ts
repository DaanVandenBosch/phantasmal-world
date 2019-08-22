export class Action {
    constructor(
        readonly description: string,
        readonly undo: () => void,
        readonly redo: () => void,
    ) {}
}

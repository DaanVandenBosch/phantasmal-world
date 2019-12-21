import { UndoStack } from "./UndoStack";

test("simple properties and invariants", () => {
    const stack = new UndoStack();

    expect(stack.can_undo.val).toBe(false);
    expect(stack.can_redo.val).toBe(false);

    /* eslint-disable */
    stack.push({ description: "", undo: () => {}, redo: () => {} });
    stack.push({ description: "", undo: () => {}, redo: () => {} });
    stack.push({ description: "", undo: () => {}, redo: () => {} });
    /* eslint-enable */

    expect(stack.can_undo.val).toBe(true);
    expect(stack.can_redo.val).toBe(false);

    stack.undo();

    expect(stack.can_undo.val).toBe(true);
    expect(stack.can_redo.val).toBe(true);

    stack.undo();
    stack.undo();

    expect(stack.can_undo.val).toBe(false);
    expect(stack.can_redo.val).toBe(true);
});

test("undo", () => {
    const stack = new UndoStack();

    // Pretend value started and 3 and we've set it to 7 and then 13.
    let value = 13;

    stack.push({ description: "X", undo: () => (value = 3), redo: () => (value = 7) });
    stack.push({ description: "Y", undo: () => (value = 7), redo: () => (value = 13) });

    expect(stack.undo()).toBe(true);
    expect(value).toBe(7);

    expect(stack.undo()).toBe(true);
    expect(value).toBe(3);

    expect(stack.undo()).toBe(false);
    expect(value).toBe(3);
});

test("redo", () => {
    const stack = new UndoStack();

    // Pretend value started and 3 and we've set it to 7 and then 13.
    let value = 13;

    stack.push({ description: "X", undo: () => (value = 3), redo: () => (value = 7) });
    stack.push({ description: "Y", undo: () => (value = 7), redo: () => (value = 13) });

    stack.undo();
    stack.undo();

    expect(value).toBe(3);

    expect(stack.redo()).toBe(true);
    expect(value).toBe(7);

    expect(stack.redo()).toBe(true);
    expect(value).toBe(13);

    expect(stack.redo()).toBe(false);
    expect(value).toBe(13);
});

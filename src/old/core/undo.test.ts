import { UndoStack, Action } from "./undo";

test("simple properties and invariants", () => {
    const stack = new UndoStack();

    expect(stack.can_undo).toBe(false);
    expect(stack.can_redo).toBe(false);

    stack.push(new Action("", () => {}, () => {}));
    stack.push(new Action("", () => {}, () => {}));
    stack.push(new Action("", () => {}, () => {}));

    expect(stack.can_undo).toBe(true);
    expect(stack.can_redo).toBe(false);

    stack.undo();

    expect(stack.can_undo).toBe(true);
    expect(stack.can_redo).toBe(true);

    stack.undo();
    stack.undo();

    expect(stack.can_undo).toBe(false);
    expect(stack.can_redo).toBe(true);
});

test("undo", () => {
    const stack = new UndoStack();

    // Pretend value started and 3 and we've set it to 7 and then 13.
    let value = 13;

    stack.push(new Action("X", () => (value = 3), () => (value = 7)));
    stack.push(new Action("Y", () => (value = 7), () => (value = 13)));

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

    stack.push(new Action("X", () => (value = 3), () => (value = 7)));
    stack.push(new Action("Y", () => (value = 7), () => (value = 13)));

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

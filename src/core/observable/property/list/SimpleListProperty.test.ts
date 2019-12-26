import { SimpleListProperty } from "./SimpleListProperty";
import { ListChangeType, ListChangeEvent } from "./ListProperty";

test("constructor", () => {
    const list = new SimpleListProperty<number>(undefined, 1, 2, 3);

    expect(list.val).toEqual([1, 2, 3]);
    expect(list.length.val).toBe(3);
});

test("push", () => {
    const changes: ListChangeEvent<number>[] = [];
    const list = new SimpleListProperty<number>();

    list.observe_list(change => changes.push(change));

    list.push(9);

    expect(list.val).toEqual([9]);
    expect(changes.length).toBe(1);
    expect(changes[0]).toEqual({
        type: ListChangeType.ListChange,
        index: 0,
        removed: [],
        inserted: [9],
    });

    list.push(1, 2, 3);

    expect(list.val).toEqual([9, 1, 2, 3]);
    expect(changes.length).toBe(2);
    expect(changes[1]).toEqual({
        type: ListChangeType.ListChange,
        index: 1,
        removed: [],
        inserted: [1, 2, 3],
    });
});

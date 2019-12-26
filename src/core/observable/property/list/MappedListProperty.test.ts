import { list_property } from "../../index";

test("filtered list should contain only values that match the predicate", () => {
    const list = list_property(undefined, 1, 2, 3, 4, 5);
    const filtered = list.filtered(v => v > 1);

    expect(filtered.val).toEqual([2, 3, 4, 5]);

    list.push(6);

    expect(filtered.val).toEqual([2, 3, 4, 5, 6]);

    list.push(0);

    expect(filtered.val).toEqual([2, 3, 4, 5, 6]);
});

test("length", () => {
    const list = list_property(undefined, 1, 2, 3, 4, 5);
    const filtered = list.filtered(v => v > 1);

    expect(filtered.length.val).toBe(4);

    list.push(6);

    expect(filtered.length.val).toBe(5);

    list.push(0);

    expect(filtered.length.val).toBe(5);
});

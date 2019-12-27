import { flat_map_to_list, list_property, property } from "./index";
import { ListChangeEvent, ListChangeType } from "./property/list/ListProperty";

test("flat_map_to_list filtered", () => {
    const prop = property({ list: list_property(undefined, 1, 2, 3) });
    const mapped = flat_map_to_list(p => p.list.filtered(e => e % 2 === 0), prop);
    const change_events: ListChangeEvent<number>[] = [];

    mapped.observe_list(change => change_events.push(change));

    expect(mapped.val).toEqual([2]);
    expect(change_events).toEqual([]);

    prop.val.list.push(4);

    expect(mapped.val).toEqual([2, 4]);
    expect(change_events).toEqual([
        {
            type: ListChangeType.ListChange,
            index: 0,
            removed: [2],
            inserted: [2, 4],
        },
    ]);
});

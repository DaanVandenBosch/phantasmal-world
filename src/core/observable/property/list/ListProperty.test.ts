import {
    is_list_property,
    ListChangeType,
    ListProperty,
    ListPropertyChangeEvent,
} from "./ListProperty";
import { SimpleListProperty } from "./SimpleListProperty";
import { DependentListProperty } from "./DependentListProperty";
import { list_property } from "../../index";

// This suite tests every implementation of ListProperty.

function test_list_property(
    name: string,
    create: () => {
        property: ListProperty<any>;
        emit_list_change: () => void;
    },
): void {
    test(`${name} should be a list property according to is_list_property`, () => {
        const { property } = create();

        expect(is_list_property(property)).toBe(true);
    });

    test(`${name} should propagate list changes to a filtered list`, () => {
        const { property, emit_list_change } = create();
        const filtered = property.filtered(() => true);
        const events: ListPropertyChangeEvent<any>[] = [];

        filtered.observe_list(event => events.push(event));

        emit_list_change();

        expect(events.length).toBe(1);
        expect(events[0].type).toBe(ListChangeType.ListChange);
    });
}

test_list_property(SimpleListProperty.name, () => {
    const property = new SimpleListProperty<string>();
    return {
        property,
        emit_list_change: () => property.push("test"),
    };
});

test_list_property(DependentListProperty.name, () => {
    const list = list_property<number>();
    const property = new DependentListProperty(list, x => x.map(v => 2 * v));
    return {
        property,
        emit_list_change: () => list.push(10),
    };
});

import { SimpleProperty } from "./SimpleProperty";
import { SimpleListProperty } from "./list/SimpleListProperty";
import { PropertyChangeEvent } from "./Property";
import { WritableProperty } from "./WritableProperty";

// This suite tests every implementation of WritableProperty.

function test_writable_property<T>(
    name: string,
    create: () => {
        property: WritableProperty<T>;
        emit: () => void;
        create_val: () => T;
    },
): void {
    test(`${name} should emit a PropertyChangeEvent when val is modified`, () => {
        const { property, create_val } = create();
        const events: PropertyChangeEvent<T>[] = [];

        property.observe(event => events.push(event));

        const new_val = create_val();
        property.val = new_val;

        expect(events.length).toBe(1);
        expect(events[0].value).toEqual(new_val);
    });
}

test_writable_property(SimpleProperty.name, () => {
    const property = new SimpleProperty(1);
    return {
        property,
        emit: () => (property.val += 1),
        create_val: () => property.val + 1,
    };
});

test_writable_property(SimpleListProperty.name, () => {
    const property = new SimpleListProperty<string>();
    return {
        property,
        emit: () => property.push("test"),
        create_val: () => ["test"],
    };
});

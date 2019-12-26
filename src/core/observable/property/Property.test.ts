import { SimpleProperty } from "./SimpleProperty";
import { DependentProperty } from "./DependentProperty";
import { list_property } from "../index";
import { FlatMappedProperty } from "./FlatMappedProperty";
import { SimpleListProperty } from "./list/SimpleListProperty";
import { DependentListProperty } from "./list/DependentListProperty";
import { is_property, Property, PropertyChangeEvent } from "./Property";
import { is_list_property } from "./list/ListProperty";

// This suite tests every implementation of Property.

function test_property(
    name: string,
    create: () => {
        property: Property<any>;
        emit: () => void;
    },
): void {
    test(`${name} should be a property according to is_property`, () => {
        const { property } = create();

        expect(is_property(property)).toBe(true);
    });

    test(`${name} should call observers immediately if added with call_now set to true`, () => {
        const { property } = create();
        const events: PropertyChangeEvent<any>[] = [];

        property.observe(event => events.push(event), { call_now: true });

        expect(events.length).toBe(1);
    });

    test(`${name} should propagate updates to mapped properties`, () => {
        const { property, emit } = create();
        let i = 0;
        const mapped = property.map(() => i++);
        const events: PropertyChangeEvent<any>[] = [];

        mapped.observe(event => events.push(event));

        emit();

        expect(events.length).toBe(1);
    });

    test(`${name} should propagate updates to flat mapped properties`, () => {
        const { property, emit } = create();
        let i = 0;
        const flat_mapped = property.flat_map(() => new SimpleProperty(i++));
        const events: PropertyChangeEvent<any>[] = [];

        flat_mapped.observe(event => events.push(event));

        emit();

        expect(events.length).toBe(1);
    });

    test(`${name} should correctly set value and old_value in emitted PropertyChangeEvents`, () => {
        const { property, emit } = create();

        const events: PropertyChangeEvent<any>[] = [];

        property.observe(event => events.push(event));

        const initial_value = property.val;

        emit();

        expect(events.length).toBe(1);
        expect(events[0].value).toBe(property.val);

        if (!is_list_property(property)) {
            expect(events[0].old_value).toBe(initial_value);
        }

        emit();

        expect(events.length).toBe(2);
        expect(events[1].value).toBe(property.val);

        if (!is_list_property(property)) {
            expect(events[1].old_value).toBe(events[0].value);
        }
    });
}

test_property(SimpleProperty.name, () => {
    const property = new SimpleProperty(1);
    return {
        property,
        emit: () => (property.val += 1),
    };
});

test_property(DependentProperty.name, () => {
    const p = new SimpleProperty(0);
    const property = new DependentProperty([p], () => 2 * p.val);
    return {
        property,
        emit: () => (p.val += 2),
    };
});

test_property(`${FlatMappedProperty.name} (dependent property emits)`, () => {
    const p = new SimpleProperty({ x: new SimpleProperty(5) });
    const property = new FlatMappedProperty([p], () => p.val.x);
    return {
        property,
        emit: () => (p.val = { x: new SimpleProperty(p.val.x.val + 5) }),
    };
});

test_property(`${FlatMappedProperty.name} (nested property emits)`, () => {
    const p = new SimpleProperty({ x: new SimpleProperty(5) });
    const property = new FlatMappedProperty([p], () => p.val.x);
    return {
        property,
        emit: () => (p.val.x.val += 5),
    };
});

test_property(SimpleListProperty.name, () => {
    const property = new SimpleListProperty<string>();
    return {
        property,
        emit: () => property.push("test"),
    };
});

test_property(DependentListProperty.name, () => {
    const list = list_property<number>();
    const property = new DependentListProperty(list, x => x.map(v => 2 * v));
    return {
        property,
        emit: () => list.push(10),
    };
});

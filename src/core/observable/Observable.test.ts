import { ChangeEvent, Observable } from "./Observable";
import { SimpleEmitter } from "./SimpleEmitter";
import { SimpleProperty } from "./property/SimpleProperty";
import { DependentProperty } from "./property/DependentProperty";
import { list_property, property } from "./index";
import { FlatMappedProperty } from "./property/FlatMappedProperty";
import { SimpleListProperty } from "./property/list/SimpleListProperty";
import { DependentListProperty } from "./property/list/DependentListProperty";

// This suite tests every implementation of Observable.

function test_observable(
    name: string,
    create: () => {
        observable: Observable<any>;
        emit: () => void;
    },
): void {
    test(`${name} should call observers when events are emitted`, () => {
        const { observable, emit } = create();
        const changes: ChangeEvent<any>[] = [];

        observable.observe(c => {
            changes.push(c);
        });

        emit();

        expect(changes.length).toBe(1);

        emit();
        emit();
        emit();

        expect(changes.length).toBe(4);
    });

    test(`${name} should not call observers after they are disposed`, () => {
        const { observable, emit } = create();
        const changes: ChangeEvent<any>[] = [];

        const observer = observable.observe(c => {
            changes.push(c);
        });

        emit();

        expect(changes.length).toBe(1);

        observer.dispose();

        emit();
        emit();
        emit();

        expect(changes.length).toBe(1);
    });
}

test_observable(SimpleEmitter.name, () => {
    const observable = new SimpleEmitter();
    return {
        observable,
        emit: () => observable.emit({ value: 1 }),
    };
});

test_observable(SimpleProperty.name, () => {
    const observable = new SimpleProperty(1);
    return {
        observable,
        emit: () => (observable.val += 1),
    };
});

test_observable(DependentProperty.name, () => {
    const p = property(0);
    const observable = new DependentProperty([p], () => 2 * p.val);
    return {
        observable,
        emit: () => (p.val += 2),
    };
});

test_observable(`${FlatMappedProperty.name} (dependent property emits)`, () => {
    const p = property({ x: property(5) });
    const observable = new FlatMappedProperty([p], () => p.val.x);
    return {
        observable,
        emit: () => (p.val = { x: property(p.val.x.val + 5) }),
    };
});

test_observable(`${FlatMappedProperty.name} (nested property emits)`, () => {
    const p = property({ x: property(5) });
    const observable = new FlatMappedProperty([p], () => p.val.x);
    return {
        observable,
        emit: () => (p.val.x.val += 5),
    };
});

test_observable(SimpleListProperty.name, () => {
    const observable = new SimpleListProperty<string>();
    return {
        observable,
        emit: () => observable.push("test"),
    };
});

test_observable(DependentListProperty.name, () => {
    const list = list_property<number>();
    const observable = new DependentListProperty(list, x => x.map(v => 2 * v));
    return {
        observable,
        emit: () => list.push(10),
    };
});

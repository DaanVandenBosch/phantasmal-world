import { SimpleEmitter } from "./SimpleEmitter";
import { WritableProperty } from "./property/WritableProperty";
import { SimpleProperty } from "./property/SimpleProperty";
import { Emitter } from "./Emitter";
import { Property } from "./property/Property";
import { DependentProperty } from "./property/DependentProperty";
import { WritableListProperty } from "./property/list/WritableListProperty";
import { SimpleWritableListProperty } from "./property/list/SimpleWritableListProperty";

export function emitter<E>(): Emitter<E> {
    return new SimpleEmitter();
}

export function property<T>(value: T): WritableProperty<T> {
    return new SimpleProperty(value);
}

export function array_property<T>(...values: T[]): WritableListProperty<T> {
    return new SimpleWritableListProperty(...values);
}

export function add(left: Property<number>, right: number): Property<number> {
    return left.map(l => l + right);
}

export function sub(left: Property<number>, right: number): Property<number> {
    return left.map(l => l - right);
}

export function map<R, S, T>(
    f: (prop_1: S, prop_2: T) => R,
    prop_1: Property<S>,
    prop_2: Property<T>,
): Property<R> {
    return new DependentProperty([prop_1, prop_2], () => f(prop_1.val, prop_2.val));
}

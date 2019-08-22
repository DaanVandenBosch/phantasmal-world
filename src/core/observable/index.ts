import { SimpleEmitter } from "./SimpleEmitter";
import { WritableProperty } from "./WritableProperty";
import { SimpleProperty } from "./SimpleProperty";
import { Emitter } from "./Emitter";
import { Property } from "./Property";
import { DependentProperty } from "./DependentProperty";
import { WritableArrayProperty } from "./WritableArrayProperty";
import { SimpleWritableArrayProperty } from "./SimpleWritableArrayProperty";

export function emitter<E>(): Emitter<E> {
    return new SimpleEmitter();
}

export function property<T>(value: T): WritableProperty<T> {
    return new SimpleProperty(value);
}

export function array_property<T>(...values: T[]): WritableArrayProperty<T> {
    return new SimpleWritableArrayProperty(...values);
}

export function if_defined<S, T>(
    property: Property<S | undefined>,
    f: (value: S) => T,
    default_value: T,
): T {
    const val = property.val;
    return val == undefined ? default_value : f(val);
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

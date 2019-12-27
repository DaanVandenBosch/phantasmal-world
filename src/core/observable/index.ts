import { SimpleEmitter } from "./SimpleEmitter";
import { WritableProperty } from "./property/WritableProperty";
import { SimpleProperty } from "./property/SimpleProperty";
import { Emitter } from "./Emitter";
import { Property } from "./property/Property";
import { MappedProperty } from "./property/MappedProperty";
import { WritableListProperty } from "./property/list/WritableListProperty";
import { SimpleListProperty } from "./property/list/SimpleListProperty";
import { Observable } from "./Observable";
import { FlatMappedProperty } from "./property/FlatMappedProperty";
import { ListProperty } from "./property/list/ListProperty";
import { FlatMappedListProperty } from "./property/list/FlatMappedListProperty";

export function emitter<E>(): Emitter<E> {
    return new SimpleEmitter();
}

export function property<T>(value: T): WritableProperty<T> {
    return new SimpleProperty(value);
}

export function list_property<T>(
    extract_observables?: (element: T) => Observable<any>[],
    ...elements: T[]
): WritableListProperty<T> {
    return new SimpleListProperty(extract_observables, ...elements);
}

export function add(left: Property<number>, right: number): Property<number> {
    return left.map(l => l + right);
}

export function sub(left: Property<number>, right: number): Property<number> {
    return left.map(l => l - right);
}

export function map<R, P1>(transform: (prop_1: P1) => R, prop_1: Property<P1>): Property<R>;
export function map<R, P1, P2>(
    transform: (prop_1: P1, prop_2: P2) => R,
    prop_1: Property<P1>,
    prop_2: Property<P2>,
): Property<R>;
export function map<R, P1, P2, P3>(
    transform: (prop_1: P1, prop_2: P2, prop_3: P3) => R,
    prop_1: Property<P1>,
    prop_2: Property<P2>,
    prop_3: Property<P3>,
): Property<R>;
export function map<R, P1, P2, P3, P4>(
    transform: (prop_1: P1, prop_2: P2, prop_3: P3, prop_4: P4) => R,
    prop_1: Property<P1>,
    prop_2: Property<P2>,
    prop_3: Property<P3>,
    prop_4: Property<P4>,
): Property<R>;
export function map<R, P1, P2, P3, P4, P5>(
    transform: (prop_1: P1, prop_2: P2, prop_3: P3, prop_4: P4, prop_5: P5) => R,
    prop_1: Property<P1>,
    prop_2: Property<P2>,
    prop_3: Property<P3>,
    prop_4: Property<P4>,
    prop_5: Property<P5>,
): Property<R>;
export function map<R>(transform: (...props: any[]) => R, ...props: Property<any>[]): Property<R> {
    return new MappedProperty(props, () => transform(...props.map(p => p.val)));
}

export function flat_map<R, P1>(
    transform: (prop_1: P1) => Property<R>,
    prop_1: Property<P1>,
): Property<R>;
export function flat_map<R, P1, P2>(
    transform: (prop_1: P1, prop_2: P2) => Property<R>,
    prop_1: Property<P1>,
    prop_2: Property<P2>,
): Property<R>;
export function flat_map<R, P1, P2, P3>(
    transform: (prop_1: P1, prop_2: P2, prop_3: P3) => Property<R>,
    prop_1: Property<P1>,
    prop_2: Property<P2>,
    prop_3: Property<P3>,
): Property<R>;
export function flat_map<R, P1, P2, P3, P4>(
    transform: (prop_1: P1, prop_2: P2, prop_3: P3, prop_4: P4) => Property<R>,
    prop_1: Property<P1>,
    prop_2: Property<P2>,
    prop_3: Property<P3>,
    prop_4: Property<P4>,
): Property<R>;
export function flat_map<R, P1, P2, P3, P4, P5>(
    transform: (prop_1: P1, prop_2: P2, prop_3: P3, prop_4: P4, prop_5: P5) => Property<R>,
    prop_1: Property<P1>,
    prop_2: Property<P2>,
    prop_3: Property<P3>,
    prop_4: Property<P4>,
    prop_5: Property<P5>,
): Property<R>;
export function flat_map<R>(
    transform: (...props: any[]) => Property<R>,
    ...props: Property<any>[]
): Property<R> {
    return new FlatMappedProperty(props, () => transform(...props.map(p => p.val)));
}

export function flat_map_to_list<R, P1>(
    transform: (prop_1: P1) => ListProperty<R>,
    prop_1: Property<P1>,
): ListProperty<R>;
export function flat_map_to_list<R, P1, P2>(
    transform: (prop_1: P1, prop_2: P2) => ListProperty<R>,
    prop_1: Property<P1>,
    prop_2: Property<P2>,
): ListProperty<R>;
export function flat_map_to_list<R, P1, P2, P3>(
    transform: (prop_1: P1, prop_2: P2, prop_3: P3) => ListProperty<R>,
    prop_1: Property<P1>,
    prop_2: Property<P2>,
    prop_3: Property<P3>,
): ListProperty<R>;
export function flat_map_to_list<R, P1, P2, P3, P4>(
    transform: (prop_1: P1, prop_2: P2, prop_3: P3, prop_4: P4) => ListProperty<R>,
    prop_1: Property<P1>,
    prop_2: Property<P2>,
    prop_3: Property<P3>,
    prop_4: Property<P4>,
): ListProperty<R>;
export function flat_map_to_list<R, P1, P2, P3, P4, P5>(
    transform: (prop_1: P1, prop_2: P2, prop_3: P3, prop_4: P4, prop_5: P5) => ListProperty<R>,
    prop_1: Property<P1>,
    prop_2: Property<P2>,
    prop_3: Property<P3>,
    prop_4: Property<P4>,
    prop_5: Property<P5>,
): ListProperty<R>;
export function flat_map_to_list<R>(
    transform: (...props: any[]) => ListProperty<R>,
    ...props: Property<any>[]
): ListProperty<R> {
    return new FlatMappedListProperty(props, () => transform(...props.map(p => p.val)));
}

import { SimpleEmitter } from "./SimpleEmitter";
import { WritableProperty } from "./WritableProperty";
import { SimpleProperty } from "./SimpleProperty";
import { Emitter } from "./Emitter";

export function emitter<E, M = undefined>(): Emitter<E, M> {
    return new SimpleEmitter();
}

export function property<T>(value: T): WritableProperty<T> {
    return new SimpleProperty(value);
}

import { Property } from "./Property";
import { Observable } from "./Observable";
import { Disposable } from "./Disposable";

export interface WritableProperty<T> extends Property<T> {
    is_writable_property: true;

    set(value: T): void;

    bind(observable: Observable<T, any>): Disposable;

    bind_bi(property: WritableProperty<T>): Disposable;
}

export function is_writable_property<T>(
    observable: Observable<T, any>,
): observable is WritableProperty<T> {
    return (observable as any).is_writable_property;
}

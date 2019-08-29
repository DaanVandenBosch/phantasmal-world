import { ChangeEvent, Observable } from "../Observable";
import { Disposable } from "../Disposable";

export interface PropertyChangeEvent<T> extends ChangeEvent<T> {
    old_value: T;
}

export interface Property<T> extends Observable<T> {
    readonly is_property: true;

    readonly val: T;

    get_val(): T;

    observe(
        observer: (event: PropertyChangeEvent<T>) => void,
        options?: { call_now?: boolean },
    ): Disposable;

    map<U>(f: (element: T) => U): Property<U>;

    flat_map<U>(f: (element: T) => Property<U>): Property<U>;
}

export function is_property<T>(observable: Observable<T>): observable is Property<T> {
    return (observable as any).is_property;
}

export function is_any_property(observable: any): observable is Property<any> {
    return observable && (observable as any).is_property;
}

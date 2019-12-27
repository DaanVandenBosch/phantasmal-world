import { ChangeEvent, Observable } from "../Observable";
import { Disposable } from "../Disposable";

export interface Property<T> extends Observable<T> {
    readonly is_property: true;

    readonly val: T;

    get_val(): T;

    observe(
        observer: (event: ChangeEvent<T>) => void,
        options?: { call_now?: boolean },
    ): Disposable;

    map<U>(transform: (value: T) => U): Property<U>;

    flat_map<U>(transform: (value: T) => Property<U>): Property<U>;
}

export function is_property<T>(observable: any): observable is Property<T> {
    return observable != undefined && (observable as any).is_property;
}

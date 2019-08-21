import { Observable } from "./Observable";

export interface Property<T> extends Observable<T, PropertyMeta<T>> {
    readonly is_property: true;

    get(): T;

    map<U>(f: (element: T) => U): Property<U>;
}

export type PropertyMeta<T> = { old_value: T };

export function is_property<T>(observable: any): observable is Property<T> {
    return (observable as any).is_property;
}

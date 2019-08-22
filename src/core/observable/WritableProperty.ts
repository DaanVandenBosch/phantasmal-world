import { Property } from "./Property";
import { Observable } from "./Observable";
import { Disposable } from "./Disposable";

export interface WritableProperty<T> extends Property<T> {
    readonly is_writable_property: true;

    val: T;

    update(f: (value: T) => T): void;

    /**
     * Bind the value of this property to the given observable.
     *
     * @param observable the observable who's events will be propagated to this property.
     */
    bind(observable: Observable<T>): Disposable;

    bind_bi(property: WritableProperty<T>): Disposable;
}

export function is_writable_property<T>(
    observable: Observable<T>,
): observable is WritableProperty<T> {
    return (observable as any).is_writable_property;
}

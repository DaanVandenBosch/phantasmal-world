import { Property } from "./Property";
import { Observable } from "./Observable";
import { Disposable } from "./Disposable";

export interface WritableProperty<T> extends Property<T> {
    val: T;

    set_val(value: T, options?: { silent?: boolean }): void;

    update(f: (value: T) => T): void;

    /**
     * Bind the value of this property to the given observable.
     *
     * @param observable the observable who's events will be propagated to this property.
     */
    bind_to(observable: Observable<T>): Disposable;

    bind_bi(property: WritableProperty<T>): Disposable;
}

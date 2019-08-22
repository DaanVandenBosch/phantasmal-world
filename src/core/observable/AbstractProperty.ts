import { Property } from "./Property";
import { DependentProperty } from "./DependentProperty";
import { FlatMappedProperty } from "./FlatMappedProperty";
import { AbstractMinimalProperty } from "./AbstractMinimalProperty";

export abstract class AbstractProperty<T> extends AbstractMinimalProperty<T> {
    map<U>(f: (element: T) => U): Property<U> {
        return new DependentProperty([this], () => f(this.val));
    }

    flat_map<U>(f: (element: T) => Property<U>): Property<U> {
        return new FlatMappedProperty(this, value => f(value));
    }
}

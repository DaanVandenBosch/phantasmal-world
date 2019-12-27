import { MappedProperty } from "./MappedProperty";
import { FlatMappedProperty } from "./FlatMappedProperty";
import { AbstractMinimalProperty } from "./AbstractMinimalProperty";
import { Property } from "./Property";

export abstract class AbstractProperty<T> extends AbstractMinimalProperty<T> {
    map<U>(transform: (value: T) => U): Property<U> {
        return new MappedProperty([this], () => transform(this.val));
    }

    flat_map<U>(transform: (value: T) => Property<U>): Property<U> {
        return new FlatMappedProperty([this], () => transform(this.val));
    }
}

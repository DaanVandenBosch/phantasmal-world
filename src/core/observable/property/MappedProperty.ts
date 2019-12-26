import { FlatMappedProperty } from "./FlatMappedProperty";
import { Property } from "./Property";
import { DependentProperty } from "./DependentProperty";

export class MappedProperty<T> extends DependentProperty<T> {
    constructor(dependencies: readonly Property<any>[], protected compute_value: () => T) {
        super(dependencies);
    }

    map<U>(transform: (element: T) => U): Property<U> {
        return new MappedProperty([this], () => transform(this.val));
    }

    flat_map<U>(transform: (element: T) => Property<U>): Property<U> {
        return new FlatMappedProperty([this], () => transform(this.val));
    }
}

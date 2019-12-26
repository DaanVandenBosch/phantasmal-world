import { ListProperty } from "./ListProperty";
import { is_property, Property } from "../Property";
import { DependentListProperty } from "./DependentListProperty";

export class MappedListProperty<T> extends DependentListProperty<T> {
    constructor(
        dependencies: readonly Property<any>[],
        protected readonly compute_values: () => readonly T[],
    ) {
        super(dependencies);
    }

    filtered(
        predicate: ((value: T) => boolean) | Property<(value: T) => boolean>,
    ): ListProperty<T> {
        if (is_property(predicate)) {
            return new MappedListProperty([this, predicate], () => this.val.filter(predicate.val));
        } else {
            return new MappedListProperty([this], () => this.val.filter(predicate));
        }
    }
}

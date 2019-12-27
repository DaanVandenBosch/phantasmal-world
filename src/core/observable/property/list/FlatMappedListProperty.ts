import { Disposable } from "../../Disposable";
import { MappedProperty } from "../MappedProperty";
import { is_property, Property } from "../Property";
import { ListProperty, ListChangeEvent } from "./ListProperty";
import { FlatMappedProperty } from "../FlatMappedProperty";
import { DependentListProperty } from "./DependentListProperty";
import { MappedListProperty } from "./MappedListProperty";
import { ChangeEvent } from "../../Observable";

export class FlatMappedListProperty<T> extends DependentListProperty<T> {
    private computed_property?: ListProperty<T>;
    private computed_disposable?: Disposable;

    get_val(): readonly T[] {
        if (this.should_recompute() || !this.computed_property) {
            return super.get_val();
        } else {
            return this.computed_property.val;
        }
    }

    constructor(
        dependencies: readonly Property<any>[],
        private readonly compute: () => ListProperty<T>,
    ) {
        super(dependencies);
    }

    observe(
        observer: (event: ChangeEvent<readonly T[]>) => void,
        options?: { call_now?: boolean },
    ): Disposable {
        const super_disposable = super.observe(observer, options);

        return {
            dispose: () => {
                super_disposable.dispose();

                if (this.observers.length === 0) {
                    this.computed_disposable?.dispose();
                    this.computed_disposable = undefined;
                    this.computed_property = undefined;
                }
            },
        };
    }

    observe_list(
        observer: (change: ListChangeEvent<T>) => void,
        options?: { call_now?: boolean },
    ): Disposable {
        const super_disposable = super.observe_list(observer, options);

        return {
            dispose: () => {
                super_disposable.dispose();
            },
        };
    }

    map<U>(f: (element: readonly T[]) => U): Property<U> {
        return new MappedProperty([this], () => f(this.val));
    }

    flat_map<U>(f: (element: readonly T[]) => Property<U>): Property<U> {
        return new FlatMappedProperty([this], () => f(this.val));
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

    protected compute_values(): readonly T[] {
        this.computed_disposable?.dispose();

        this.computed_property = this.compute();

        this.computed_disposable = this.computed_property.observe(() => {
            this.emit();
        });

        return this.computed_property.val;
    }
}

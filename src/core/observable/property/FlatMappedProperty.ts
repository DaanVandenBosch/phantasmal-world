import { Disposable } from "../Disposable";
import { MappedProperty } from "./MappedProperty";
import { Property } from "./Property";
import { DependentProperty } from "./DependentProperty";
import { ChangeEvent } from "../Observable";

export class FlatMappedProperty<T> extends DependentProperty<T> {
    private computed_property?: Property<T>;
    private computed_disposable?: Disposable;

    get_val(): T {
        if (this.should_recompute()) {
            return super.get_val();
        } else {
            return this.computed_property!.val;
        }
    }

    constructor(
        dependencies: readonly Property<any>[],
        private readonly compute: () => Property<T>,
    ) {
        super(dependencies);
    }

    observe(
        observer: (event: ChangeEvent<T>) => void,
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

    map<U>(transform: (value: T) => U): Property<U> {
        return new MappedProperty([this], () => transform(this.val));
    }

    flat_map<U>(transform: (value: T) => Property<U>): Property<U> {
        return new FlatMappedProperty([this], () => transform(this.val));
    }

    protected compute_value(has_observers: boolean): T {
        this.computed_property = this.compute();

        this.computed_disposable?.dispose();

        if (has_observers) {
            this.computed_disposable = this.computed_property.observe(() => {
                this._val = this.computed_property!.val;
                this.emit();
            });
        }

        return this.computed_property.val;
    }
}

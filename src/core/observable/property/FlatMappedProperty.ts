import { Disposable } from "../Disposable";
import { MappedProperty } from "./MappedProperty";
import { Property, PropertyChangeEvent } from "./Property";
import { DependentProperty } from "./DependentProperty";

export class FlatMappedProperty<T> extends DependentProperty<T> {
    private computed_property?: Property<T>;
    private computed_disposable?: Disposable;

    constructor(
        dependencies: readonly Property<any>[],
        private readonly compute: () => Property<T>,
    ) {
        super(dependencies);
    }

    observe(
        observer: (event: PropertyChangeEvent<T>) => void,
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

    map<U>(transform: (element: T) => U): Property<U> {
        return new MappedProperty([this], () => transform(this.val));
    }

    flat_map<U>(transform: (element: T) => Property<U>): Property<U> {
        return new FlatMappedProperty([this], () => transform(this.val));
    }

    protected compute_value(): T {
        if (this.computed_disposable) this.computed_disposable.dispose();

        this.computed_property = this.compute();

        const old_value = this.computed_property.val;

        this.computed_disposable = this.computed_property.observe(() => {
            this.emit(old_value);
        });

        return this.computed_property.val;
    }
}

import { Disposable } from "../Disposable";
import { AbstractMinimalProperty } from "./AbstractMinimalProperty";
import { DependentProperty } from "./DependentProperty";
import { Property, PropertyChangeEvent } from "./Property";

/**
 * Starts observing its dependency when the first observer on this property is registered.
 * Stops observing its dependency when the last observer on this property is disposed.
 * This way no extra disposables need to be managed when {@link Property.flat_map} is used.
 */
export class FlatMappedProperty<T, U> extends AbstractMinimalProperty<U> implements Property<U> {
    get val(): U {
        return this.get_val();
    }

    get_val(): U {
        return this.computed_property
            ? this.computed_property.val
            : this.f(this.dependency.val).val;
    }

    private dependency_disposable?: Disposable;
    private computed_property?: Property<U>;
    private computed_disposable?: Disposable;

    constructor(private dependency: Property<T>, private f: (value: T) => Property<U>) {
        super();
    }

    observe(
        observer: (event: PropertyChangeEvent<U>) => void,
        options?: { call_now?: boolean },
    ): Disposable {
        const super_disposable = super.observe(observer, options);

        if (this.dependency_disposable == undefined) {
            this.dependency_disposable = this.dependency.observe(() => {
                const old_value = this.val;
                this.compute_and_observe();
                this.emit(old_value);
            });

            this.compute_and_observe();
        }

        return {
            dispose: () => {
                super_disposable.dispose();

                if (this.observers.length === 0) {
                    this.dependency_disposable!.dispose();
                    this.dependency_disposable = undefined;
                    this.computed_disposable!.dispose();
                    this.computed_disposable = undefined;
                    this.computed_property = undefined;
                }
            },
        };
    }

    map<V>(f: (element: U) => V): Property<V> {
        return new DependentProperty([this], () => f(this.val));
    }

    flat_map<V>(f: (element: U) => Property<V>): Property<V> {
        return new FlatMappedProperty(this, value => f(value));
    }

    private compute_and_observe(): void {
        if (this.computed_disposable) this.computed_disposable.dispose();

        this.computed_property = this.f(this.dependency.val);

        let old_value = this.computed_property.val;

        this.computed_disposable = this.computed_property.observe(() => {
            const ov = old_value;
            old_value = this.val;
            this.emit(ov);
        });
    }
}

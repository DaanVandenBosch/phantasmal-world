import { Disposable } from "../Disposable";
import { Disposer } from "../Disposer";
import { AbstractMinimalProperty } from "./AbstractMinimalProperty";
import { FlatMappedProperty } from "./FlatMappedProperty";
import { Property, PropertyChangeEvent } from "./Property";

/**
 * Starts observing its dependencies when the first observer on this property is registered.
 * Stops observing its dependencies when the last observer on this property is disposed.
 * This way no extra disposables need to be managed when e.g. {@link Property.map} is used.
 */
export class DependentProperty<T> extends AbstractMinimalProperty<T> implements Property<T> {
    private _val?: T;

    get val(): T {
        return this.get_val();
    }

    get_val(): T {
        if (this.dependency_disposables.length) {
            return this._val as T;
        } else {
            return this.f();
        }
    }

    private dependency_disposables = new Disposer();

    constructor(private dependencies: Property<any>[], private f: () => T) {
        super();
    }

    observe(
        observer: (event: PropertyChangeEvent<T>) => void,
        options: { call_now?: boolean } = {},
    ): Disposable {
        const super_disposable = super.observe(observer, options);

        if (this.dependency_disposables.length === 0) {
            this._val = this.f();

            this.dependency_disposables.add_all(
                ...this.dependencies.map(dependency =>
                    dependency.observe(() => {
                        const old_value = this._val!;
                        this._val = this.f();
                        this.emit(old_value);
                    }),
                ),
            );
        }

        this.emit(this._val!);

        return {
            dispose: () => {
                super_disposable.dispose();

                if (this.observers.length === 0) {
                    this.dependency_disposables.dispose_all();
                }
            },
        };
    }

    map<U>(f: (element: T) => U): Property<U> {
        return new DependentProperty([this], () => f(this.val));
    }

    flat_map<U>(f: (element: T) => Property<U>): Property<U> {
        return new FlatMappedProperty(this, value => f(value));
    }
}

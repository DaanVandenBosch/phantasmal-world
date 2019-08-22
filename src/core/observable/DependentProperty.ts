import { Disposable } from "./Disposable";
import { Property } from "./Property";
import { Disposer } from "./Disposer";
import { AbstractMinimalProperty } from "./AbstractMinimalProperty";
import { FlatMappedProperty } from "./FlatMappedProperty";

/**
 * Starts observing its dependencies when the first observer on this property is registered.
 * Stops observing its dependencies when the last observer on this property is disposed.
 * This way no extra disposables need to be managed when e.g. {@link Property.map} is used.
 */
export class DependentProperty<T> extends AbstractMinimalProperty<T> implements Property<T> {
    readonly is_property = true;

    private _val?: T;

    get val(): T {
        if (this.dependency_disposables) {
            return this._val as T;
        } else {
            return this.f();
        }
    }

    private dependency_disposables = new Disposer();

    constructor(private dependencies: Property<any>[], private f: () => T) {
        super();
    }

    observe(observer: (event: T) => void): Disposable {
        const super_disposable = super.observe(observer);

        if (this.dependency_disposables.length === 0) {
            this._val = this.f();

            this.dependency_disposables.add_all(
                ...this.dependencies.map(dependency =>
                    dependency.observe(() => {
                        this._val = this.f();
                        this.emit();
                    }),
                ),
            );
        }

        return {
            dispose: () => {
                super_disposable.dispose();

                if (this.observers.length === 0) {
                    this.dependency_disposables.dispose();
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

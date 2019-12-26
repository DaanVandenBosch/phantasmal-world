import { Disposable } from "../Disposable";
import { Disposer } from "../Disposer";
import { AbstractMinimalProperty } from "./AbstractMinimalProperty";
import { Property, PropertyChangeEvent } from "./Property";

/**
 * Starts observing its dependencies when the first observer on this property is registered.
 * Stops observing its dependencies when the last observer on this property is disposed.
 * This way no extra disposables need to be managed when e.g. {@link Property.map} is used.
 */
export abstract class DependentProperty<T> extends AbstractMinimalProperty<T> {
    private dependency_disposer = new Disposer();
    private _val?: T;

    get val(): T {
        return this.get_val();
    }

    get_val(): T {
        if (this.dependency_disposer.length === 0) {
            this._val = this.compute_value();
        }

        return this._val as T;
    }

    protected constructor(private dependencies: readonly Property<any>[]) {
        super();
    }

    observe(
        observer: (event: PropertyChangeEvent<T>) => void,
        options: { call_now?: boolean } = {},
    ): Disposable {
        const super_disposable = super.observe(observer, options);

        if (this.dependency_disposer.length === 0) {
            this.dependency_disposer.add_all(
                ...this.dependencies.map(dependency =>
                    dependency.observe(() => {
                        const old_value = this._val!;
                        this._val = this.compute_value();

                        if (this._val !== old_value) {
                            this.emit(old_value);
                        }
                    }),
                ),
            );

            this._val = this.compute_value();
        }

        return {
            dispose: () => {
                super_disposable.dispose();

                if (this.observers.length === 0) {
                    this.dependency_disposer.dispose_all();
                }
            },
        };
    }

    protected abstract compute_value(): T;
}

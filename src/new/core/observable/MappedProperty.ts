import { SimpleEmitter } from "./SimpleEmitter";
import { Disposable } from "./Disposable";
import { Property, PropertyMeta } from "./Property";

/**
 * Starts observing its origin when the first observer on this property is registered.
 * Stops observing its origin when the last observer on this property is disposed.
 * This way no extra disposables need to be managed when {@link Property.map} is used.
 */
export class MappedProperty<S, T> extends SimpleEmitter<T, PropertyMeta<T>> implements Property<T> {
    readonly is_property = true;

    private origin_disposable?: Disposable;
    private value?: T;

    constructor(private origin: Property<S>, private f: (value: S) => T) {
        super();
    }

    observe(observer: (event: T, meta: PropertyMeta<T>) => void): Disposable {
        const disposable = super.observe(observer);

        if (this.origin_disposable == undefined) {
            this.value = this.f(this.origin.get());

            this.origin_disposable = this.origin.observe(origin_value => {
                const old_value = this.value as T;
                this.value = this.f(origin_value);

                this.emit(this.value, { old_value });
            });
        }

        return {
            dispose: () => {
                disposable.dispose();

                if (this.observers.length === 0) {
                    this.origin_disposable!.dispose();
                    this.origin_disposable = undefined;
                }
            },
        };
    }

    get(): T {
        if (this.origin_disposable) {
            return this.value as T;
        } else {
            return this.f(this.origin.get());
        }
    }

    map<U>(f: (element: T) => U): Property<U> {
        return new MappedProperty(this, f);
    }
}

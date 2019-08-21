import { SimpleEmitter } from "./SimpleEmitter";
import { Disposable } from "./Disposable";
import { Observable } from "./Observable";
import { WritableProperty } from "./WritableProperty";
import { Property, PropertyMeta, is_property } from "./Property";
import { MappedProperty } from "./MappedProperty";

export class SimpleProperty<T> extends SimpleEmitter<T, PropertyMeta<T>>
    implements WritableProperty<T> {
    readonly is_property = true;
    readonly is_writable_property = true;

    private value: T;

    constructor(value: T) {
        super();
        this.value = value;
    }

    get(): T {
        return this.value;
    }

    set(value: T): void {
        if (value !== this.value) {
            const old_value = this.value;
            this.value = value;
            this.emit(value, { old_value });
        }
    }

    bind(observable: Observable<T, any>): Disposable {
        if (is_property(observable)) {
            this.set(observable.get());
        }

        return observable.observe(v => this.set(v));
    }

    bind_bi(property: WritableProperty<T>): Disposable {
        const bind_1 = this.bind(property);
        const bind_2 = property.bind(this);
        return {
            dispose(): void {
                bind_1.dispose();
                bind_2.dispose();
            },
        };
    }

    map<U>(f: (element: T) => U): Property<U> {
        return new MappedProperty(this, f);
    }
}

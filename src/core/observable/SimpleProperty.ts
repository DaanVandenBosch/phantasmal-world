import { Disposable } from "./Disposable";
import { Observable } from "./Observable";
import { WritableProperty } from "./WritableProperty";
import { is_property } from "./Property";
import { AbstractProperty } from "./AbstractProperty";

export class SimpleProperty<T> extends AbstractProperty<T> implements WritableProperty<T> {
    readonly is_writable_property = true;

    constructor(private _val: T) {
        super();
    }

    get val(): T {
        return this._val;
    }

    set val(val: T) {
        if (val !== this._val) {
            this._val = val;
            this.emit();
        }
    }

    update(f: (value: T) => T): void {
        this.val = f(this.val);
    }

    bind(observable: Observable<T>): Disposable {
        if (is_property(observable)) {
            this.val = observable.val;
        }

        return observable.observe(v => (this.val = v));
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
}

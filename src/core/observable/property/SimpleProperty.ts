import { Disposable } from "../Disposable";
import { Observable } from "../Observable";
import { WritableProperty } from "./WritableProperty";
import { AbstractProperty } from "./AbstractProperty";
import { is_property } from "./Property";

export class SimpleProperty<T> extends AbstractProperty<T> implements WritableProperty<T> {
    constructor(private _val: T) {
        super();
    }

    get val(): T {
        return this.get_val();
    }

    set val(value: T) {
        this.set_val(value);
    }

    get_val(): T {
        return this._val;
    }

    set_val(val: T, options: { silent?: boolean } = {}): void {
        if (val !== this._val) {
            const old_value = this._val;
            this._val = val;

            if (!options.silent) {
                this.emit(old_value);
            }
        }
    }

    update(f: (value: T) => T): void {
        this.val = f(this.val);
    }

    bind_to(observable: Observable<T>): Disposable {
        if (is_property(observable)) {
            this.val = observable.val;
        }

        return observable.observe(event => (this.val = event.value));
    }

    bind_bi(property: WritableProperty<T>): Disposable {
        const bind_1 = this.bind_to(property);
        const bind_2 = property.bind_to(this);
        return {
            dispose(): void {
                bind_1.dispose();
                bind_2.dispose();
            },
        };
    }
}

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
            this._val = val;

            if (!options.silent) {
                this.emit();
            }
        }
    }

    update(f: (value: T) => T): void {
        this.val = f(this.val);
    }

    bind_to(observable: Observable<T>): Disposable {
        if (is_property<T>(observable)) {
            this.val = observable.val;
        }

        return observable.observe(event => (this.val = event.value));
    }
}

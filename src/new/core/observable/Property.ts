import { Observable } from "./Observable";

export class Property<T> extends Observable<T, { old_value: T }> {
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
            this.fire(value, { old_value });
        }
    }
}

import { Disposable } from "../gui/Disposable";

export class Observable<T> {
    private value: T;
    private readonly observers: ((new_value: T, old_value: T) => void)[] = [];

    constructor(value: T) {
        this.value = value;
    }

    get(): T {
        return this.value;
    }

    set(value: T): void {
        if (value !== this.value) {
            const old_value = this.value;
            this.value = value;

            for (const observer of this.observers) {
                try {
                    observer(value, old_value);
                } catch (e) {
                    console.error(e);
                }
            }
        }
    }

    observe(observer: (new_value: T, old_value: T) => void): Disposable {
        if (!this.observers.includes(observer)) {
            this.observers.push(observer);
        }

        return {
            dispose: () => {
                const index = this.observers.indexOf(observer);

                if (index !== -1) {
                    this.observers.splice(index, 1);
                }
            },
        };
    }
}

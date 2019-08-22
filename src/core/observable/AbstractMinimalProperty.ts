import { Property } from "./Property";
import { Disposable } from "./Disposable";
import Logger from "js-logger";

const logger = Logger.get("core/observable/AbstractMinimalProperty");

// This class exists purely because otherwise the resulting cyclic dependency graph would trip up commonjs.
// The dependency graph is still cyclic but for some reason it's not a problem this way.
export abstract class AbstractMinimalProperty<T> implements Property<T> {
    readonly is_property = true;

    abstract readonly val: T;

    protected readonly observers: ((value: T) => void)[] = [];

    observe(observer: (value: T) => void): Disposable {
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

    abstract map<U>(f: (element: T) => U): Property<U>;

    abstract flat_map<U>(f: (element: T) => Property<U>): Property<U>;

    protected emit(): void {
        for (const observer of this.observers) {
            try {
                observer(this.val);
            } catch (e) {
                logger.error("Observer threw error.", e);
            }
        }
    }
}

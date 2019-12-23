import { Disposable } from "../Disposable";
import Logger from "js-logger";
import { Property, PropertyChangeEvent } from "./Property";

const logger = Logger.get("core/observable/property/AbstractMinimalProperty");

// This class exists purely because otherwise the resulting cyclic dependency graph would trip up commonjs.
// The dependency graph is still cyclic but for some reason it's not a problem this way.
export abstract class AbstractMinimalProperty<T> implements Property<T> {
    readonly is_property = true;

    abstract readonly val: T;

    abstract get_val(): T;

    protected readonly observers: ((change: PropertyChangeEvent<T>) => void)[] = [];

    observe(
        observer: (change: PropertyChangeEvent<T>) => void,
        options?: { call_now?: boolean },
    ): Disposable {
        this.observers.push(observer);

        if (options && options.call_now) {
            this.call_observer(observer, this.val, this.val);
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

    protected emit(old_value: T): void {
        const value = this.val;

        for (const observer of this.observers) {
            this.call_observer(observer, value, old_value);
        }
    }

    private call_observer(
        observer: (event: PropertyChangeEvent<T>) => void,
        value: T,
        old_value: T,
    ): void {
        try {
            observer({ value, old_value });
        } catch (e) {
            logger.error("Observer threw error.", e);
        }
    }
}

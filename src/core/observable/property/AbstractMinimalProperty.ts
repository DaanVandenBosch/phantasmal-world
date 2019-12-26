import { Disposable } from "../Disposable";
import { Property } from "./Property";
import { LogManager } from "../../Logger";
import { ChangeEvent } from "../Observable";

const logger = LogManager.get("core/observable/property/AbstractMinimalProperty");

// This class exists purely because otherwise the resulting cyclic dependency graph would trip up
// webpack. The dependency graph is still cyclic but for some reason it's not a problem this way.
export abstract class AbstractMinimalProperty<T> implements Property<T> {
    readonly is_property = true;

    abstract readonly val: T;

    abstract get_val(): T;

    protected readonly observers: ((change: ChangeEvent<T>) => void)[] = [];

    observe(
        observer: (change: ChangeEvent<T>) => void,
        options?: { call_now?: boolean },
    ): Disposable {
        this.observers.push(observer);

        if (options && options.call_now) {
            this.call_observer(observer, this.val);
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
        const value = this.val;

        for (const observer of this.observers) {
            this.call_observer(observer, value);
        }
    }

    private call_observer(observer: (event: ChangeEvent<T>) => void, value: T): void {
        try {
            observer({ value });
        } catch (e) {
            logger.error("Observer threw error.", e);
        }
    }
}

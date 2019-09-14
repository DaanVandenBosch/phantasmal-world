import { Disposable } from "./Disposable";
import Logger from "js-logger";
import { Emitter } from "./Emitter";
import { ChangeEvent } from "./Observable";

const logger = Logger.get("core/observable/SimpleEmitter");

export class SimpleEmitter<T> implements Emitter<T> {
    protected readonly observers: ((event: ChangeEvent<T>) => void)[] = [];

    emit(event: ChangeEvent<T>): void {
        for (const observer of this.observers) {
            try {
                observer(event);
            } catch (e) {
                logger.error("Observer threw error.", e);
            }
        }
    }

    observe(observer: (event: ChangeEvent<T>) => void): Disposable {
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

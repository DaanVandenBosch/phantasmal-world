import { Disposable } from "./Disposable";
import Logger from "js-logger";

const logger = Logger.get("core/observable/SimpleEmitter");

export class SimpleEmitter<E> {
    protected readonly observers: ((event: E) => void)[] = [];

    emit(event: E): void {
        for (const observer of this.observers) {
            try {
                observer(event);
            } catch (e) {
                logger.error("Observer threw error.", e);
            }
        }
    }

    observe(observer: (event: E) => void): Disposable {
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

import { Disposable } from "../gui/Disposable";

export class Observable<E, M = undefined> {
    private readonly observers: ((event: E, meta: M) => void)[] = [];

    fire(event: E, meta: M): void {
        for (const observer of this.observers) {
            try {
                observer(event, meta);
            } catch (e) {
                console.error(e);
            }
        }
    }

    observe(observer: (event: E, meta: M) => void): Disposable {
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

import { Disposable } from "./Disposable";
import Logger = require("js-logger");

const logger = Logger.get("core/observable/Disposer");

export class Disposer implements Disposable {
    private readonly disposables: Disposable[] = [];

    add<T extends Disposable>(disposable: T): T {
        this.disposables.push(disposable);
        return disposable;
    }

    add_all(...disposable: Disposable[]): this {
        this.disposables.push(...disposable);
        return this;
    }

    dispose(): void {
        for (const disposable of this.disposables.splice(0, this.disposables.length)) {
            try {
                disposable.dispose();
            } catch (e) {
                logger.warn("Error while disposing.", e);
            }
        }
    }
}

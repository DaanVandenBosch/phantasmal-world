import { Disposable } from "./Disposable";
import Logger = require("js-logger");

const logger = Logger.get("core/observable/Disposer");

/**
 * Container for disposables.
 */
export class Disposer implements Disposable {
    private readonly disposables: Disposable[] = [];
    private disposed = false;

    /**
     * The amount of disposables contained in this disposer.
     */
    get length(): number {
        return this.disposables.length;
    }

    /**
     * Add a single disposable and return the given disposable.
     */
    add<T extends Disposable>(disposable: T): T {
        this.check_not_disposed();
        this.disposables.push(disposable);
        return disposable;
    }

    /**
     * Add 0 or more disposables.
     */
    add_all(...disposable: Disposable[]): this {
        this.check_not_disposed();
        this.disposables.push(...disposable);
        return this;
    }

    /**
     * Disposes all held disposables.
     */
    dispose_all(): void {
        for (const disposable of this.disposables.splice(0, this.disposables.length)) {
            try {
                disposable.dispose();
            } catch (e) {
                logger.warn("Error while disposing.", e);
            }
        }
    }

    /**
     * Disposes all held disposables.
     */
    dispose(): void {
        this.dispose_all();
        this.disposed = true;
    }

    private check_not_disposed(): void {
        if (this.disposed) {
            throw new Error("This disposer has been disposed.");
        }
    }
}

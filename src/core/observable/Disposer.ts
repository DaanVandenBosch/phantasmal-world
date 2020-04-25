import { Disposable } from "./Disposable";
import { LogManager } from "../Logger";
import { array_remove } from "../util";

const logger = LogManager.get("core/observable/Disposer");

/**
 * Container for disposables.
 */
export class Disposer implements Disposable {
    private _disposed = false;
    private readonly disposables: Disposable[];

    /**
     * The amount of disposables contained in this disposer.
     */
    get length(): number {
        return this.disposables.length;
    }

    get disposed(): boolean {
        return this._disposed;
    }

    constructor(...disposables: Disposable[]) {
        this.disposables = disposables;
    }

    /**
     * Add a single disposable and return the given disposable.
     */
    add<T extends Disposable>(disposable: T): T {
        if (this.disposed) {
            disposable.dispose();
        } else {
            this.disposables.push(disposable);
        }

        return disposable;
    }

    /**
     * Insert a single disposable at the given index and return the given disposable.
     */
    insert<T extends Disposable>(index: number, disposable: T): T {
        if (this._disposed) {
            disposable.dispose();
        } else {
            this.disposables.splice(index, 0, disposable);
        }

        return disposable;
    }

    /**
     * Add 0 or more disposables.
     */
    add_all(...disposables: Disposable[]): this {
        if (this._disposed) {
            for (const disposable of disposables) {
                disposable.dispose();
            }
        } else {
            this.disposables.push(...disposables);
        }

        return this;
    }

    /**
     * Removes and disposes the given disposable.
     */
    remove(disposable: Disposable): void {
        array_remove(this.disposables, disposable);
        disposable.dispose();
    }

    /**
     * Disposes all held disposables.
     */
    dispose_all(): void {
        this.dispose_at(0, this.disposables.length);
    }

    /**
     * Disposes all held disposables.
     */
    dispose(): void {
        this.dispose_all();
        this._disposed = true;
    }

    dispose_at(index: number, amount = 1): void {
        for (const disposable of this.disposables.splice(index, amount)) {
            try {
                disposable.dispose();
            } catch (e) {
                logger.warn("Error while disposing.", e);
            }
        }
    }
}

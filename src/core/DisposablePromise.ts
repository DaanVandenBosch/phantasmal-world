import { Disposable } from "./observable/Disposable";

enum State {
    Initializing,
    Resolving,
    Resolved,
    Rejected,
    Disposed,
}

export class DisposablePromise<T> implements Promise<T>, Disposable {
    static resolve<S, T extends S = S>(value: T | PromiseLike<T>): DisposablePromise<S> {
        return new DisposablePromise((resolve, reject) => {
            if ("then" in value) {
                value.then(resolve, reject);
            } else {
                resolve(value);
            }
        });
    }

    static wrap<T>(promise: Promise<T>): DisposablePromise<T> {
        return new DisposablePromise((resolve, reject) => {
            promise.then(resolve).catch(reject);
        });
    }

    private state = State.Initializing;
    private value?: T;
    private error?: Error;

    private readonly init_handler: (
        resolve: (value: T) => void,
        reject: (error: Error) => void,
    ) => void;
    private readonly disposal_handler?: () => void;

    private resolution_listeners: ((value: T) => void)[] = [];
    private rejection_listeners: ((error: Error) => void)[] = [];
    private settlement_listeners: (() => void)[] = [];

    private get settled(): boolean {
        return this.state !== State.Initializing && this.state !== State.Resolving;
    }

    readonly [Symbol.toStringTag]: string = "DisposablePromise";

    constructor(
        init: (resolve: (value: T) => void, reject: (error: Error) => void) => void,
        dispose?: () => void,
    ) {
        this.init_handler = init;
        this.disposal_handler = dispose;
        setTimeout(this.initialize, 0);
    }

    then<TResult1 = T, TResult2 = never>(
        onfulfilled?: ((value: T) => PromiseLike<TResult1> | TResult1) | undefined | null,
        onrejected?: ((reason: any) => PromiseLike<TResult2> | TResult2) | undefined | null,
    ): DisposablePromise<TResult1 | TResult2> {
        return new DisposablePromise(
            (resolve, reject) => {
                if (onfulfilled != undefined) {
                    this.add_resolution_listener(async value => resolve(await onfulfilled(value)));
                } else {
                    this.add_resolution_listener(resolve as any);
                }

                if (onrejected != undefined) {
                    this.add_rejection_listener(async error => resolve(await onrejected(error)));
                } else {
                    this.add_rejection_listener(reject);
                }
            },
            () => this.dispose(),
        );
    }

    catch<TResult = never>(
        onrejected?: ((reason: any) => PromiseLike<TResult> | TResult) | undefined | null,
    ): DisposablePromise<T | TResult> {
        return new DisposablePromise(
            resolve => {
                this.add_resolution_listener(resolve);

                if (onrejected != undefined) {
                    this.add_rejection_listener(async error => resolve(await onrejected(error)));
                }
            },
            () => this.dispose(),
        );
    }

    finally(onfinally?: (() => void) | undefined | null): DisposablePromise<T> {
        return new DisposablePromise(
            () => {
                if (onfinally != undefined) {
                    this.add_settlement_listener(this.finally);
                }
            },
            () => this.dispose(),
        );
    }

    /**
     * Cancels future resolution. After calling this method, any then, catch or finally handlers
     * will not be called.
     */
    dispose(): void {
        if (!this.settled) {
            this.state = State.Disposed;

            // Remove listeners without calling them.
            this.resolution_listeners.splice(0);
            this.rejection_listeners.splice(0);
            this.settlement_listeners.splice(0);

            this.disposal_handler?.();
        } else {
            this.state = State.Disposed;
        }
    }

    private initialize = (): void => {
        if (this.state === State.Initializing) {
            this.init_handler(this.resolve, this.reject);
        }
    };

    private resolve = (value: T): void => {
        if (!this.settled) {
            this.state = State.Resolved;
            this.value = value;

            // Remove listeners and call the resolution and settlement listeners.
            this.rejection_listeners.splice(0);
            this.settlement_listeners.splice(0);

            for (const listener of this.resolution_listeners.splice(0)) {
                listener(value);
            }

            for (const listener of this.settlement_listeners.splice(0)) {
                listener();
            }
        }
    };

    private reject = (error: Error): void => {
        if (!this.settled) {
            this.state = State.Rejected;
            this.error = error;

            // Remove listeners and call the rejection and settlement listeners.
            this.resolution_listeners.splice(0);
            this.settlement_listeners.splice(0);

            for (const listener of this.rejection_listeners.splice(0)) {
                listener(error);
            }

            for (const listener of this.settlement_listeners.splice(0)) {
                listener();
            }
        }
    };

    private add_resolution_listener(listener: (value: T) => void): void {
        if (this.state === State.Resolved) {
            // Just call the listener without adding it to the list.
            setTimeout(() => listener(this.value!), 0);
        } else if (!this.settled) {
            this.resolution_listeners.push(listener);
        }
    }

    private add_rejection_listener(listener: (error: Error) => void): void {
        if (this.state === State.Rejected) {
            // Just call the listener without adding it to the list.
            setTimeout(() => listener(this.error!), 0);
        } else if (!this.settled) {
            this.rejection_listeners.push(listener);
        }
    }

    private add_settlement_listener(listener: () => void): void {
        if (this.settled) {
            // Just call the listener without adding it to the list.
            setTimeout(() => listener(), 0);
        } else if (this.state !== State.Disposed) {
            this.settlement_listeners.push(listener);
        }
    }
}

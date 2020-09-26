import { Disposable } from "./observable/Disposable";
import { is_promise_like } from "./util";

enum State {
    Pending,
    Fulfilled,
    Rejected,
    Disposed,
}

export class DisposablePromise<T> implements Promise<T>, Disposable {
    static all<T>(values: Iterable<T | PromiseLike<T>>): DisposablePromise<T[]> {
        return new DisposablePromise(
            (resolve, reject) => {
                const results: T[] = [];
                let len = 0;

                function add_result(r: T): void {
                    results.push(r);

                    if (results.length === len) {
                        resolve(results);
                    }
                }

                for (const value of values) {
                    len++;

                    if (is_promise_like(value)) {
                        value.then(add_result, reject);
                    } else {
                        add_result(value);
                    }
                }
            },
            () => {
                for (const value of values) {
                    if (value instanceof DisposablePromise) {
                        value.dispose();
                    }
                }
            },
        );
    }

    static resolve<T>(value: T | PromiseLike<T>, dispose?: () => void): DisposablePromise<T> {
        if (is_promise_like(value)) {
            return new DisposablePromise((resolve, reject) => {
                value.then(resolve, reject);
            }, dispose);
        } else {
            return new DisposablePromise(resolve => {
                resolve(value);
            }, dispose);
        }
    }

    private state: State = State.Pending;
    private value?: T;
    private reason?: any;

    private readonly fulfillment_listeners: ((value: T) => unknown)[] = [];
    private readonly rejection_listeners: ((reason: any) => unknown)[] = [];
    private readonly disposal_handler?: () => void;

    [Symbol.toStringTag] = "DisposablePromise";

    constructor(
        executor: (
            resolve: (value: T | PromiseLike<T>) => void,
            reject: (reason?: any) => void,
        ) => void,
        dispose?: () => void,
    ) {
        this.disposal_handler = dispose;

        executor(this.executor_resolve, this.executor_reject);
    }

    private executor_resolve = (value: T | PromiseLike<T>): void => {
        if (is_promise_like(value)) {
            if (this.state !== State.Pending) return;

            value.then(
                p_value => {
                    this.fulfilled(p_value);
                },
                p_reason => {
                    this.rejected(p_reason);
                },
            );
        } else {
            this.fulfilled(value);
        }
    };

    private executor_reject = (reason?: any): void => {
        this.rejected(reason);
    };

    private fulfilled(value: T): void {
        if (this.state !== State.Pending) return;

        this.state = State.Fulfilled;
        this.value = value;

        for (const listener of this.fulfillment_listeners) {
            listener(value);
        }

        this.fulfillment_listeners.splice(0);
        this.rejection_listeners.splice(0);
    }

    private rejected(reason?: any): void {
        if (this.state !== State.Pending) return;

        this.state = State.Rejected;
        this.reason = reason;

        for (const listener of this.rejection_listeners) {
            listener(reason);
        }

        this.fulfillment_listeners.splice(0);
        this.rejection_listeners.splice(0);
    }

    then<TResult1 = T, TResult2 = never>(
        onfulfilled?: ((value: T) => TResult1 | PromiseLike<TResult1>) | undefined | null,
        onrejected?: ((reason: any) => TResult2 | PromiseLike<TResult2>) | undefined | null,
    ): DisposablePromise<TResult1 | TResult2> {
        return new DisposablePromise(
            (resolve, reject) => {
                if (onfulfilled == undefined) {
                    this.add_fulfillment_listener(resolve as any);
                } else {
                    this.add_fulfillment_listener(value => {
                        try {
                            resolve(onfulfilled(value));
                        } catch (e) {
                            reject(e);
                        }
                    });
                }

                if (onrejected == undefined) {
                    this.add_rejection_listener(reject);
                } else {
                    this.add_rejection_listener(reason => {
                        try {
                            resolve(onrejected(reason));
                        } catch (e) {
                            reject(e);
                        }
                    });
                }
            },
            () => this.dispose(),
        );
    }

    catch<TResult = never>(
        onrejected?: ((reason: any) => PromiseLike<TResult> | TResult) | undefined | null,
    ): DisposablePromise<T | TResult> {
        return new DisposablePromise(
            (resolve, reject) => {
                this.add_fulfillment_listener(resolve as any);

                if (onrejected == undefined) {
                    this.add_rejection_listener(reject);
                } else {
                    this.add_rejection_listener(reason => {
                        try {
                            resolve(onrejected(reason));
                        } catch (e) {
                            reject(e);
                        }
                    });
                }
            },
            () => this.dispose(),
        );
    }

    finally(onfinally?: (() => void) | undefined | null): DisposablePromise<T> {
        if (onfinally == undefined) {
            return this;
        } else {
            return new DisposablePromise(
                (resolve, reject) => {
                    this.add_fulfillment_listener(value => {
                        try {
                            onfinally();
                            resolve(value);
                        } catch (e) {
                            reject(e);
                        }
                    });

                    this.add_rejection_listener(value => {
                        try {
                            onfinally();
                            reject(value);
                        } catch (e) {
                            reject(e);
                        }
                    });
                },
                () => this.dispose(),
            );
        }
    }

    /**
     * Cancels the promise. After calling this method, any then, catch or finally handlers will not
     * be called.
     */
    dispose(): void {
        if (this.state !== State.Disposed) {
            this.state = State.Disposed;
            this.disposal_handler?.();
        }
    }

    private add_fulfillment_listener(listener: (value: T) => unknown): void {
        switch (this.state) {
            case State.Pending:
                this.fulfillment_listeners.push(listener);
                break;
            case State.Fulfilled:
                listener(this.value!);
                break;
            case State.Rejected:
            case State.Disposed:
                break;
        }
    }

    private add_rejection_listener(listener: (reason: any) => unknown): void {
        switch (this.state) {
            case State.Pending:
                this.rejection_listeners.push(listener);
                break;
            case State.Rejected:
                listener(this.reason);
                break;
            case State.Fulfilled:
            case State.Disposed:
                break;
        }
    }
}

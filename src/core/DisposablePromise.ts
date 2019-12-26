import { Disposable } from "./observable/Disposable";

export class DisposablePromise<T> extends Promise<T> implements Disposable {
    static resolve<T>(value?: T | PromiseLike<T>): DisposablePromise<T> {
        return new DisposablePromise((resolve, reject) => {
            if (value === undefined) {
                new DisposablePromise(() => undefined);
            } else if ("then" in value) {
                value.then(resolve, reject);
            } else {
                resolve(value);
            }
        });
    }

    static wrap<T>(promise: Promise<T>, dispose?: () => void): DisposablePromise<T> {
        if (promise instanceof DisposablePromise) {
            return promise;
        } else {
            return new DisposablePromise((resolve, reject) => {
                promise.then(resolve).catch(reject);
            }, dispose);
        }
    }

    private disposed: boolean;

    private readonly disposal_handler?: () => void;

    constructor(
        executor: (
            resolve: (value?: T | PromiseLike<T>) => void,
            reject: (reason?: any) => void,
        ) => void,
        dispose?: () => void,
    ) {
        let resolve_fn: (value?: T | PromiseLike<T> | undefined) => void;
        let reject_fn: (value?: T | PromiseLike<T> | undefined) => void;

        super((resolve, reject) => {
            resolve_fn = resolve;
            reject_fn = reject;
        });

        this.disposed = false;
        this.disposal_handler = dispose;

        executor(
            value => {
                if (!this.disposed) {
                    resolve_fn(value);
                }
            },
            reason => {
                if (!this.disposed) {
                    reject_fn(reason);
                }
            },
        );
    }

    then<TResult1 = T, TResult2 = never>(
        onfulfilled?: ((value: T) => PromiseLike<TResult1> | TResult1) | undefined | null,
        onrejected?: ((reason: any) => PromiseLike<TResult2> | TResult2) | undefined | null,
    ): DisposablePromise<TResult1 | TResult2> {
        return DisposablePromise.wrap(super.then(onfulfilled, onrejected), () => this.dispose());
    }

    catch<TResult = never>(
        onrejected?: ((reason: any) => PromiseLike<TResult> | TResult) | undefined | null,
    ): DisposablePromise<T | TResult> {
        return DisposablePromise.wrap(super.catch(onrejected), () => this.dispose());
    }

    finally(onfinally?: (() => void) | undefined | null): DisposablePromise<T> {
        return DisposablePromise.wrap(super.finally(onfinally), () => this.dispose());
    }

    /**
     * Cancels the promise. After calling this method, any then, catch or finally handlers will not
     * be called.
     */
    dispose(): void {
        if (!this.disposed) {
            this.disposed = true;
            this.disposal_handler?.();
        }
    }
}

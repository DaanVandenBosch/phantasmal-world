import { Disposable } from "../../../../src/core/observable/Disposable";
import { Disposer } from "../../../../src/core/observable/Disposer";

export function with_disposable<D extends Disposable, T>(
    disposable: D,
    f: (disposable: D) => T | Promise<T>,
): T {
    let is_promise = false;

    try {
        const value = f(disposable);

        if (value != undefined && "then" in value && "finally" in value) {
            is_promise = true;
            return (value.finally(() => disposable.dispose()) as any) as T;
        } else {
            return value;
        }
    } finally {
        if (!is_promise) {
            disposable.dispose();
        }
    }
}

export function with_disposer<T>(f: (disposer: Disposer) => T | Promise<T>): T {
    return with_disposable(new Disposer(), f);
}

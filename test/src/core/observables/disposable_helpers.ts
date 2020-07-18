import { Disposable } from "../../../../src/core/observable/Disposable";
import { Disposer } from "../../../../src/core/observable/Disposer";
import { is_promise } from "../../../../src/core/util";

export function with_disposable<D extends Disposable, T>(
    disposable: D,
    f: (disposable: D) => T,
): T {
    let return_promise = false;

    try {
        const value = f(disposable);

        if (is_promise(value)) {
            return_promise = true;
            return (value.finally(() => disposable.dispose()) as unknown) as T;
        } else {
            return value;
        }
    } finally {
        if (!return_promise) {
            disposable.dispose();
        }
    }
}

export function with_disposer<T>(f: (disposer: Disposer) => T): T {
    return with_disposable(new Disposer(), f);
}

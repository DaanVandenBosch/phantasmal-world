import { Disposable } from "./Disposable";

export interface Observable<E, M = undefined> {
    observe(observer: (event: E, meta: M) => void): Disposable;
}

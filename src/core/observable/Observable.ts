import { Disposable } from "./Disposable";

export interface Observable<E> {
    observe(observer: (event: E) => void): Disposable;
}

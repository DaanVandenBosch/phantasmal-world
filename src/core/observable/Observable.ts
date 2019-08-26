import { Disposable } from "./Disposable";

export interface ChangeEvent<T> {
    value: T;
}

export interface Observable<T> {
    observe(observer: (event: ChangeEvent<T>) => void): Disposable;
}

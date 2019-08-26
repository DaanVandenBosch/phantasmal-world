import { ChangeEvent, Observable } from "./Observable";

export interface Emitter<T> extends Observable<T> {
    emit(event: ChangeEvent<T>): void;
}

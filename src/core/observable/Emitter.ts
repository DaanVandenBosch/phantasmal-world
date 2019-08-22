import { Observable } from "./Observable";

export interface Emitter<E> extends Observable<E> {
    emit(event: E): void;
}

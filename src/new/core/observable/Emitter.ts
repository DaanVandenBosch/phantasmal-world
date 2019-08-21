import { Observable } from "./Observable";

export interface Emitter<E, M> extends Observable<E, M> {
    emit(event: E, meta: M): void;
}

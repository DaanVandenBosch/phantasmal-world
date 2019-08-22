import { Property } from "./Property";

export interface ArrayProperty<T> extends Property<T[]> {
    get(index: number): T;

    readonly length: Property<number>;
}

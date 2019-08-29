import { Property } from "../Property";

export interface ListProperty<T> extends Property<T[]> {
    readonly length: Property<number>;

    get(index: number): T;
}

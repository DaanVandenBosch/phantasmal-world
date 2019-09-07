import { Property } from "../Property";
import { Disposable } from "../../Disposable";

export enum ListChangeType {
    ListChange,
    ValueChange,
}

export type ListPropertyChangeEvent<T> = ListChange<T> | ListValueChange<T>;

export type ListChange<T> = {
    readonly type: ListChangeType.ListChange;
    readonly index: number;
    readonly removed: T[];
    readonly inserted: T[];
};

export type ListValueChange<T> = {
    readonly type: ListChangeType.ValueChange;
    readonly index: number;
    readonly updated: T[];
};

export interface ListProperty<T> extends Property<T[]> {
    readonly length: Property<number>;

    get(index: number): T;

    observe_list(
        observer: (change: ListPropertyChangeEvent<T>) => void,
        options?: { call_now?: boolean },
    ): Disposable;
}

import { Property } from "../Property";
import { Disposable } from "../../Disposable";

export enum ListChangeType {
    Insertion,
    Removal,
    Replacement,
    Update,
}

export type ListPropertyChangeEvent<T> =
    | ListInsertion<T>
    | ListRemoval<T>
    | ListReplacement<T>
    | ListUpdate<T>;

export type ListInsertion<T> = {
    readonly type: ListChangeType.Insertion;
    readonly inserted: T[];
    readonly from: number;
    readonly to: number;
};

export type ListRemoval<T> = {
    readonly type: ListChangeType.Removal;
    readonly removed: T[];
    readonly from: number;
    readonly to: number;
};

export type ListReplacement<T> = {
    readonly type: ListChangeType.Replacement;
    readonly removed: T[];
    readonly inserted: T[];
    readonly from: number;
    readonly removed_to: number;
    readonly inserted_to: number;
};

export type ListUpdate<T> = {
    readonly type: ListChangeType.Update;
    readonly updated: T[];
    readonly index: number;
};

export interface ListProperty<T> extends Property<T[]> {
    readonly length: Property<number>;

    get(index: number): T;

    observe_list(
        observer: (change: ListPropertyChangeEvent<T>) => void,
        options?: { call_now?: boolean },
    ): Disposable;
}

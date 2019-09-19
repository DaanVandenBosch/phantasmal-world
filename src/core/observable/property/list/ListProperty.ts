import { Property } from "../Property";
import { Disposable } from "../../Disposable";
import { Observable } from "../../Observable";

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
    readonly is_list_property: true;

    readonly length: Property<number>;

    get(index: number): T;

    observe_list(observer: (change: ListPropertyChangeEvent<T>) => void): Disposable;
}

export function is_list_property<T>(observable: Observable<T[]>): observable is ListProperty<T> {
    return (observable as any).is_list_property;
}

export function is_any_list_property(observable: any): observable is ListProperty<any> {
    return observable && observable.is_list_property;
}

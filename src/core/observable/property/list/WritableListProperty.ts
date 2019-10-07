import { ListProperty } from "./ListProperty";
import { WritableProperty } from "../WritableProperty";

export interface WritableListProperty<T> extends ListProperty<T>, WritableProperty<readonly T[]> {
    val: readonly T[];

    set(index: number, value: T): void;

    push(...values: T[]): number;

    splice(index: number, delete_count?: number): T[];
    splice(index: number, delete_count: number, ...values: readonly T[]): T[];

    remove(...values: readonly T[]): void;

    clear(): void;

    sort(compare: (a: T, b: T) => number): void;
}

import { WritableProperty } from "./WritableProperty";
import { ArrayProperty } from "./ArrayProperty";

export interface WritableArrayProperty<T> extends ArrayProperty<T>, WritableProperty<T[]> {
    val: T[];

    set(index: number, value: T): void;

    splice(index: number, delete_count?: number, ...items: T[]): T[];

    clear(): void;
}

import { ListProperty } from "./ListProperty";
import { WritableProperty } from "../WritableProperty";

export interface WritableListProperty<T> extends ListProperty<T>, WritableProperty<T[]> {
    val: T[];

    set(index: number, value: T): void;

    splice(index: number, delete_count?: number, ...items: T[]): T[];

    clear(): void;
}

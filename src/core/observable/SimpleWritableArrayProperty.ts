/* eslint-disable no-dupe-class-members */
import { WritableArrayProperty } from "./WritableArrayProperty";
import { Disposable } from "./Disposable";
import { WritableProperty } from "./WritableProperty";
import { Observable } from "./Observable";
import { property } from "./index";
import { AbstractProperty } from "./AbstractProperty";

export class SimpleWritableArrayProperty<T> extends AbstractProperty<T[]>
    implements WritableArrayProperty<T> {
    readonly is_property = true;

    readonly is_writable_property = true;

    private readonly _length = property(0);
    readonly length = this._length;

    private readonly values: T[];

    get val(): T[] {
        return this.values;
    }

    constructor(...values: T[]) {
        super();
        this.values = values;
    }

    bind(observable: Observable<T[]>): Disposable {
        /* TODO */ throw new Error("not implemented");
    }

    bind_bi(property: WritableProperty<T[]>): Disposable {
        /* TODO */ throw new Error("not implemented");
    }

    update(f: (value: T[]) => T[]): void {
        this.values.splice(0, this.values.length, ...f(this.values));
    }

    get(index: number): T {
        return this.values[index];
    }

    set(index: number, value: T): void {
        this.values[index] = value;
        this.emit();
    }

    clear(): void {
        this.values.splice(0, this.values.length);
        this.emit();
    }

    splice(index: number, delete_count?: number): T[];
    splice(index: number, delete_count: number, ...items: T[]): T[];
    splice(index: number, delete_count?: number, ...items: T[]): T[] {
        let ret: T[];

        if (delete_count == undefined) {
            ret = this.values.splice(index);
        } else {
            ret = this.values.splice(index, delete_count, ...items);
        }

        this.emit();

        return ret;
    }
}

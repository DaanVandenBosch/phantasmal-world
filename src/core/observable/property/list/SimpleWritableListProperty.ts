/* eslint-disable no-dupe-class-members */
import { WritableListProperty } from "./WritableListProperty";
import { Disposable } from "../../Disposable";
import { WritableProperty } from "../WritableProperty";
import { Observable } from "../../Observable";
import { property } from "../../index";
import { AbstractProperty } from "../AbstractProperty";
import { Property } from "../Property";

export class SimpleWritableListProperty<T> extends AbstractProperty<T[]>
    implements WritableListProperty<T> {
    readonly length: Property<number>;

    get val(): T[] {
        return this.get_val();
    }

    set val(values: T[]) {
        this.set_val(values);
    }

    get_val(): T[] {
        return this.values;
    }

    set_val(values: T[]): T[] {
        const replaced_values = this.values.splice(0, this.values.length, ...values);
        this.emit(this.values);
        return replaced_values;
    }

    private readonly _length = property(0);
    private readonly values: T[];

    constructor(...values: T[]) {
        super();

        this.length = this._length;
        this.values = values;
    }

    bind_to(observable: Observable<T[]>): Disposable {
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
        this.emit(this.values);
    }

    clear(): void {
        this.values.splice(0, this.values.length);
        this.emit(this.values);
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

        this.emit(this.values);

        return ret;
    }
}

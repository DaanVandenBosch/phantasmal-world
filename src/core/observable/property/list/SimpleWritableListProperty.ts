import { WritableListProperty } from "./WritableListProperty";
import { Disposable } from "../../Disposable";
import { WritableProperty } from "../WritableProperty";
import { Observable } from "../../Observable";
import { property } from "../../index";
import { AbstractProperty } from "../AbstractProperty";
import { Property } from "../Property";
import { ListChangeType, ListPropertyChangeEvent } from "./ListProperty";
import Logger from "js-logger";

const logger = Logger.get("core/observable/property/list/SimpleWritableListProperty");

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
        const removed = this.values.splice(0, this.values.length, ...values);
        this.emit_list({
            type: ListChangeType.Replacement,
            removed,
            inserted: values,
            from: 0,
            removed_to: removed.length,
            inserted_to: values.length,
        });
        return removed;
    }

    private readonly _length = property(0);
    private readonly values: T[];
    private readonly list_observers: ((change: ListPropertyChangeEvent<T>) => void)[] = [];

    constructor(...values: T[]) {
        super();

        this.length = this._length;
        this.values = values;
    }

    observe_list(observer: (change: ListPropertyChangeEvent<T>) => void): Disposable {
        if (!this.list_observers.includes(observer)) {
            this.list_observers.push(observer);
        }

        return {
            dispose: () => {
                const index = this.list_observers.indexOf(observer);

                if (index !== -1) {
                    this.list_observers.splice(index, 1);
                }
            },
        };
    }

    bind_to(observable: Observable<T[]>): Disposable {
        /* TODO */ throw new Error("not implemented");
    }

    bind_bi(property: WritableProperty<T[]>): Disposable {
        /* TODO */ throw new Error("not implemented");
    }

    update(f: (value: T[]) => T[]): void {
        this.splice(0, this.values.length, ...f(this.values));
    }

    get(index: number): T {
        return this.values[index];
    }

    set(index: number, value: T): void {
        const removed = [this.values[index]];
        this.values[index] = value;
        this.emit_list({
            type: ListChangeType.Replacement,
            removed,
            inserted: [value],
            from: index,
            removed_to: index + 1,
            inserted_to: index + 1,
        });
    }

    clear(): void {
        const removed = this.values.splice(0, this.values.length);
        this.emit_list({
            type: ListChangeType.Replacement,
            removed,
            inserted: [],
            from: 0,
            removed_to: removed.length,
            inserted_to: 0,
        });
    }

    splice(index: number, delete_count?: number, ...items: T[]): T[] {
        let removed: T[];

        if (delete_count == undefined) {
            removed = this.values.splice(index);
        } else {
            removed = this.values.splice(index, delete_count, ...items);
        }

        this.emit_list({
            type: ListChangeType.Replacement,
            removed,
            inserted: items,
            from: index,
            removed_to: index + removed.length,
            inserted_to: index + items.length,
        });

        return removed;
    }

    protected emit_list(change: ListPropertyChangeEvent<T>): void {
        for (const observer of this.list_observers) {
            try {
                observer(change);
            } catch (e) {
                logger.error("Observer threw error.", e);
            }
        }

        this.emit(this.values);
    }
}

import { WritableListProperty } from "./WritableListProperty";
import { Disposable } from "../../Disposable";
import { WritableProperty } from "../WritableProperty";
import { Observable } from "../../Observable";
import { property } from "../../index";
import { AbstractProperty } from "../AbstractProperty";
import { Property } from "../Property";
import { ListChangeType, ListPropertyChangeEvent } from "./ListProperty";
import Logger from "js-logger";

const logger = Logger.get("core/observable/property/list/SimpleListProperty");

export class SimpleListProperty<T> extends AbstractProperty<T[]>
    implements WritableListProperty<T> {
    readonly length: Property<number>;

    get val(): T[] {
        return this.get_val();
    }

    set val(elements: T[]) {
        this.set_val(elements);
    }

    get_val(): T[] {
        return this.values;
    }

    set_val(elements: T[]): T[] {
        const removed = this.values.splice(0, this.values.length, ...elements);
        this.finalize_update({
            type: ListChangeType.ListChange,
            index: 0,
            removed,
            inserted: elements,
        });
        return removed;
    }

    private readonly _length = property(0);
    private readonly values: T[];
    private readonly extract_observables?: (element: T) => Observable<any>[];
    /**
     * Internal observers which observe observables related to this list's values so that their
     * changes can be propagated via update events.
     */
    private readonly value_observers: { index: number; disposables: Disposable[] }[] = [];
    /**
     * External observers which are observing this list.
     */
    private readonly list_observers: ((change: ListPropertyChangeEvent<T>) => void)[] = [];

    /**
     * @param extract_observables - Extractor function called on each value in this list. Changes
     * to the returned observables will be propagated via update events.
     * @param values - Initial values of this list.
     */
    constructor(extract_observables?: (element: T) => Observable<any>[], ...values: T[]) {
        super();

        this.length = this._length;
        this.values = values;
        this.extract_observables = extract_observables;
    }

    observe_list(
        observer: (change: ListPropertyChangeEvent<T>) => void,
        options?: { call_now?: true },
    ): Disposable {
        if (this.value_observers.length === 0 && this.extract_observables) {
            this.replace_element_observers(0, Infinity, this.values);
        }

        if (!this.list_observers.includes(observer)) {
            this.list_observers.push(observer);
        }

        if (options && options.call_now) {
            this.call_list_observer(observer, {
                type: ListChangeType.ListChange,
                index: 0,
                removed: [],
                inserted: this.values,
            });
        }

        return {
            dispose: () => {
                const index = this.list_observers.indexOf(observer);

                if (index !== -1) {
                    this.list_observers.splice(index, 1);
                }

                if (this.list_observers.length === 0) {
                    for (const { disposables } of this.value_observers) {
                        for (const disposable of disposables) {
                            disposable.dispose();
                        }
                    }

                    this.value_observers.splice(0, Infinity);
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

    update(f: (element: T[]) => T[]): void {
        this.splice(0, this.values.length, ...f(this.values));
    }

    get(index: number): T {
        return this.values[index];
    }

    set(index: number, element: T): void {
        const removed = [this.values[index]];
        this.values[index] = element;
        this.finalize_update({
            type: ListChangeType.ListChange,
            index,
            removed,
            inserted: [element],
        });
    }

    clear(): void {
        const removed = this.values.splice(0, this.values.length);
        this.finalize_update({
            type: ListChangeType.ListChange,
            index: 0,
            removed,
            inserted: [],
        });
    }

    splice(index: number, delete_count?: number, ...items: T[]): T[] {
        let removed: T[];

        if (delete_count == undefined) {
            removed = this.values.splice(index);
        } else {
            removed = this.values.splice(index, delete_count, ...items);
        }

        this.finalize_update({
            type: ListChangeType.ListChange,
            index,
            removed,
            inserted: items,
        });

        return removed;
    }

    /**
     * Does the following in the given order:
     * - Updates value observers
     * - Emits ListPropertyChangeEvent
     * - Emits PropertyChangeEvent
     * - Sets length
     */
    protected finalize_update(change: ListPropertyChangeEvent<T>): void {
        if (
            this.list_observers.length &&
            this.extract_observables &&
            change.type === ListChangeType.ListChange
        ) {
            this.replace_element_observers(change.index, change.removed.length, change.inserted);
        }

        for (const observer of this.list_observers) {
            this.call_list_observer(observer, change);
        }

        this.emit(this.values);

        this._length.val = this.values.length;
    }

    private call_list_observer(
        observer: (change: ListPropertyChangeEvent<T>) => void,
        change: ListPropertyChangeEvent<T>,
    ): void {
        try {
            observer(change);
        } catch (e) {
            logger.error("Observer threw error.", e);
        }
    }

    private replace_element_observers(from: number, amount: number, new_elements: T[]): void {
        let index = from;

        const removed = this.value_observers.splice(
            from,
            amount,
            ...new_elements.map(element => {
                const obj = {
                    index,
                    disposables: this.extract_observables!(element).map(observable =>
                        observable.observe(() => {
                            this.finalize_update({
                                type: ListChangeType.ValueChange,
                                updated: [element],
                                index: obj.index,
                            });
                        }),
                    ),
                };
                index++;
                return obj;
            }),
        );

        for (const { disposables } of removed) {
            for (const disposable of disposables) {
                disposable.dispose();
            }
        }

        while (index < this.value_observers.length) {
            this.value_observers[index].index += index;
        }
    }
}

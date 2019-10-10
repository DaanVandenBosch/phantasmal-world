import { WritableListProperty } from "./WritableListProperty";
import { Disposable } from "../../Disposable";
import { WritableProperty } from "../WritableProperty";
import { Observable } from "../../Observable";
import { property } from "../../index";
import { AbstractProperty } from "../AbstractProperty";
import { is_property, Property } from "../Property";
import { is_list_property, ListChangeType, ListPropertyChangeEvent } from "./ListProperty";
import Logger from "js-logger";

const logger = Logger.get("core/observable/property/list/SimpleListProperty");

export class SimpleListProperty<T> extends AbstractProperty<readonly T[]>
    implements WritableListProperty<T> {
    readonly is_list_property = true;

    readonly length: Property<number>;

    get val(): readonly T[] {
        return this.get_val();
    }

    set val(values: readonly T[]) {
        this.set_val(values);
    }

    get_val(): T[] {
        return this.values;
    }

    set_val(values: readonly T[]): T[] {
        const removed = this.values.splice(0, this.values.length, ...values);
        this.finalize_update({
            type: ListChangeType.ListChange,
            index: 0,
            removed,
            inserted: values,
        });
        return removed;
    }

    private readonly _length: WritableProperty<number>;
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

        this._length = property(values.length);
        this.length = this._length;
        this.values = values;
        this.extract_observables = extract_observables;
    }

    observe_list(observer: (change: ListPropertyChangeEvent<T>) => void): Disposable {
        if (this.value_observers.length === 0 && this.extract_observables) {
            this.replace_element_observers(0, Infinity, this.values);
        }

        if (!this.list_observers.includes(observer)) {
            this.list_observers.push(observer);
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

    bind_to(observable: Observable<readonly T[]>): Disposable {
        if (is_list_property(observable)) {
            this.set_val(observable.val);

            return observable.observe_list(change => {
                if (change.type === ListChangeType.ListChange) {
                    this.splice(change.index, change.removed.length, ...change.inserted);
                }
            });
        } else {
            if (is_property(observable)) {
                this.set_val(observable.val);
            }

            return observable.observe(({ value }) => this.set_val(value));
        }
    }

    bind_bi(property: WritableProperty<readonly T[]>): Disposable {
        const bind_1 = this.bind_to(property);
        const bind_2 = property.bind_to(this);
        return {
            dispose(): void {
                bind_1.dispose();
                bind_2.dispose();
            },
        };
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

    push(...values: T[]): number {
        const index = this.values.length;
        this.values.push(...values);

        this.finalize_update({
            type: ListChangeType.ListChange,
            index,
            removed: [],
            inserted: values,
        });

        return this.length.val;
    }

    remove(...values: T[]): void {
        for (const value of values) {
            const index = this.values.indexOf(value);
            this.values.splice(index, 1);

            this.finalize_update({
                type: ListChangeType.ListChange,
                index,
                removed: [value],
                inserted: [],
            });
        }
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

    splice(index: number, delete_count?: number, ...values: T[]): T[] {
        let removed: T[];

        if (delete_count == undefined) {
            removed = this.values.splice(index);
        } else {
            removed = this.values.splice(index, delete_count, ...values);
        }

        this.finalize_update({
            type: ListChangeType.ListChange,
            index,
            removed,
            inserted: values,
        });

        return removed;
    }

    sort(compare: (a: T, b: T) => number): void {
        this.values.sort(compare);

        this.finalize_update({
            type: ListChangeType.ListChange,
            index: 0,
            removed: this.values,
            inserted: this.values,
        });
    }

    /**
     * Does the following in the given order:
     * - Updates value observers
     * - Sets length silently
     * - Emits ListPropertyChangeEvent
     * - Emits PropertyChangeEvent
     * - Emits length PropertyChangeEvent if necessary
     */
    protected finalize_update(change: ListPropertyChangeEvent<T>): void {
        if (
            this.list_observers.length &&
            this.extract_observables &&
            change.type === ListChangeType.ListChange
        ) {
            this.replace_element_observers(change.index, change.removed.length, change.inserted);
        }

        const old_length = this._length.val;
        this._length.set_val(this.values.length, { silent: true });

        for (const observer of this.list_observers) {
            this.call_list_observer(observer, change);
        }

        this.emit(this.values);

        // Set length to old length first to ensure an event is emitted.
        this._length.set_val(old_length, { silent: true });
        this._length.set_val(this.values.length, { silent: false });
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

    private replace_element_observers(
        from: number,
        amount: number,
        new_elements: readonly T[],
    ): void {
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

        const shift = new_elements.length - amount;

        while (index < this.value_observers.length) {
            this.value_observers[index++].index += shift;
        }
    }
}

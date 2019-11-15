import { WritableListProperty } from "./WritableListProperty";
import { Disposable } from "../../Disposable";
import { Observable } from "../../Observable";
import { is_property } from "../Property";
import { is_list_property, ListChangeType, ListProperty } from "./ListProperty";
import { AbstractListProperty } from "./AbstractListProperty";
import { DependentListProperty } from "./DependentListProperty";

export class SimpleListProperty<T> extends AbstractListProperty<T>
    implements WritableListProperty<T> {
    get val(): readonly T[] {
        return this.get_val();
    }

    set val(values: readonly T[]) {
        this.set_val(values);
    }

    private readonly values: T[];

    /**
     * @param extract_observables - Extractor function called on each value in this list. Changes
     * to the returned observables will be propagated via ValueChange events.
     * @param values - Initial values of this list.
     */
    constructor(extract_observables?: (element: T) => Observable<any>[], ...values: T[]) {
        super(extract_observables);

        this.values = values || [];
    }

    get_val(): readonly T[] {
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

    filtered(predicate: (value: T) => boolean): ListProperty<T> {
        return new DependentListProperty(this, values => values.filter(predicate));
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

    update(f: (element: T[]) => T[]): void {
        this.splice(0, this.values.length, ...f(this.values));
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

            if (index > -1) {
                this.values.splice(index, 1);

                this.finalize_update({
                    type: ListChangeType.ListChange,
                    index,
                    removed: [value],
                    inserted: [],
                });
            }
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

    protected compute_length(): number {
        return this.values.length;
    }
}

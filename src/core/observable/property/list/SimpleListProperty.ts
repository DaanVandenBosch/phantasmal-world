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
        return this.elements;
    }

    set_val(elements: T[]): T[] {
        const removed = this.elements.splice(0, this.elements.length, ...elements);
        this.finalize_update({
            type: ListChangeType.Replacement,
            removed,
            inserted: elements,
            from: 0,
            removed_to: removed.length,
            inserted_to: elements.length,
        });
        return removed;
    }

    private readonly _length = property(0);
    private readonly elements: T[];
    private readonly extract_observables?: (element: T) => Observable<any>[];
    /**
     * Internal observers which observe observables related to this list's elements so that their
     * changes can be propagated via update events.
     */
    private readonly element_observers: { index: number; disposables: Disposable[] }[] = [];
    /**
     * External observers which are observing this list.
     */
    private readonly list_observers: ((change: ListPropertyChangeEvent<T>) => void)[] = [];

    /**
     * @param extract_observables - Extractor function called on each element in this list. Changes
     * to the returned observables will be propagated via update events.
     * @param elements - Initial elements of this list.
     */
    constructor(extract_observables?: (element: T) => Observable<any>[], ...elements: T[]) {
        super();

        this.length = this._length;
        this.elements = elements;
        this.extract_observables = extract_observables;
    }

    observe_list(
        observer: (change: ListPropertyChangeEvent<T>) => void,
        options?: { call_now?: true },
    ): Disposable {
        if (this.element_observers.length === 0 && this.extract_observables) {
            this.replace_element_observers(this.elements, 0, Infinity);
        }

        if (!this.list_observers.includes(observer)) {
            this.list_observers.push(observer);
        }

        if (options && options.call_now) {
            this.call_list_observer(observer, {
                type: ListChangeType.Insertion,
                inserted: this.elements,
                from: 0,
                to: this.elements.length,
            });
        }

        return {
            dispose: () => {
                const index = this.list_observers.indexOf(observer);

                if (index !== -1) {
                    this.list_observers.splice(index, 1);
                }

                if (this.list_observers.length === 0) {
                    for (const { disposables } of this.element_observers) {
                        for (const disposable of disposables) {
                            disposable.dispose();
                        }
                    }

                    this.element_observers.splice(0, Infinity);
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
        this.splice(0, this.elements.length, ...f(this.elements));
    }

    get(index: number): T {
        return this.elements[index];
    }

    set(index: number, element: T): void {
        const removed = [this.elements[index]];
        this.elements[index] = element;
        this.finalize_update({
            type: ListChangeType.Replacement,
            removed,
            inserted: [element],
            from: index,
            removed_to: index + 1,
            inserted_to: index + 1,
        });
    }

    clear(): void {
        const removed = this.elements.splice(0, this.elements.length);
        this.finalize_update({
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
            removed = this.elements.splice(index);
        } else {
            removed = this.elements.splice(index, delete_count, ...items);
        }

        this.finalize_update({
            type: ListChangeType.Replacement,
            removed,
            inserted: items,
            from: index,
            removed_to: index + removed.length,
            inserted_to: index + items.length,
        });

        return removed;
    }

    /**
     * Does the following in the given order:
     * - Updates element observers
     * - Emits ListPropertyChangeEvent
     * - Emits PropertyChangeEvent
     * - Sets length
     */
    protected finalize_update(change: ListPropertyChangeEvent<T>): void {
        if (this.list_observers.length && this.extract_observables) {
            switch (change.type) {
                case ListChangeType.Insertion:
                    this.replace_element_observers(change.inserted, change.from, 0);
                    break;

                case ListChangeType.Removal:
                    this.replace_element_observers([], change.from, change.removed.length);
                    break;

                case ListChangeType.Replacement:
                    this.replace_element_observers(
                        change.inserted,
                        change.from,
                        change.removed.length,
                    );
                    break;
            }
        }

        for (const observer of this.list_observers) {
            this.call_list_observer(observer, change);
        }

        this.emit(this.elements);

        this._length.val = this.elements.length;
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

    private replace_element_observers(new_elements: T[], from: number, amount: number): void {
        let index = from;

        const removed = this.element_observers.splice(
            from,
            amount,
            ...new_elements.map(element => {
                const obj = {
                    index,
                    disposables: this.extract_observables!(element).map(observable =>
                        observable.observe(() => {
                            this.finalize_update({
                                type: ListChangeType.Update,
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

        while (index < this.element_observers.length) {
            this.element_observers[index].index += index;
        }
    }
}

import { ListChangeType, ListProperty, ListPropertyChangeEvent } from "./ListProperty";
import { AbstractProperty } from "../AbstractProperty";
import { Disposable } from "../../Disposable";
import { Observable } from "../../Observable";
import { Property } from "../Property";
import { LogManager } from "../../../Logger";

const logger = LogManager.get("core/observable/property/list/AbstractListProperty");

class LengthProperty extends AbstractProperty<number> {
    private length = 0;

    get val(): number {
        return this.get_val();
    }

    get_val(): number {
        return (this.length = this.compute_length());
    }

    constructor(private compute_length: () => number) {
        super();
    }

    update(): void {
        const old_length = this.length;
        const length = this.compute_length();

        if (old_length !== length) {
            this.length = length;
            this.emit(old_length);
        }
    }
}

export abstract class AbstractListProperty<T> extends AbstractProperty<readonly T[]>
    implements ListProperty<T> {
    readonly is_list_property = true;

    readonly length: Property<number>;

    private readonly _length: LengthProperty;
    private readonly extract_observables?: (element: T) => Observable<any>[];
    /**
     * Internal observers which observe observables related to this list's values so that their
     * changes can be propagated via update events.
     */
    private readonly value_observers: { index: number; disposables: Disposable[] }[] = [];
    /**
     * External observers which are observing this list.
     */
    protected readonly list_observers: ((change: ListPropertyChangeEvent<T>) => void)[] = [];

    protected constructor(extract_observables?: (element: T) => Observable<any>[]) {
        super();

        this._length = new LengthProperty(() => this.compute_length());
        this.length = this._length;
        this.extract_observables = extract_observables;
    }

    get(index: number): T {
        return this.val[index];
    }

    observe_list(
        observer: (change: ListPropertyChangeEvent<T>) => void,
        options?: { call_now?: boolean },
    ): Disposable {
        if (this.value_observers.length === 0 && this.extract_observables) {
            this.replace_element_observers(0, Infinity, this.val);
        }

        if (!this.list_observers.includes(observer)) {
            this.list_observers.push(observer);
        }

        if (options && options.call_now) {
            this.call_list_observer(observer, {
                type: ListChangeType.ListChange,
                index: 0,
                removed: [],
                inserted: this.val,
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

    abstract filtered(
        predicate: ((value: T) => boolean) | Property<(value: T) => boolean>,
    ): ListProperty<T>;

    protected abstract compute_length(): number;

    /**
     * Does the following in the given order:
     * - Updates value observers
     * - Emits length PropertyChangeEvent if necessary
     * - Emits ListPropertyChangeEvent
     * - Emits PropertyChangeEvent
     */
    protected finalize_update(change: ListPropertyChangeEvent<T>): void {
        if (
            this.list_observers.length &&
            this.extract_observables &&
            change.type === ListChangeType.ListChange
        ) {
            this.replace_element_observers(change.index, change.removed.length, change.inserted);
        }

        this._length.update();

        for (const observer of this.list_observers) {
            this.call_list_observer(observer, change);
        }

        this.emit(this.val);
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

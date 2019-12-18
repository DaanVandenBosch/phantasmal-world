import { ListChangeType, ListProperty, ListPropertyChangeEvent } from "./ListProperty";
import { is_any_property, Property, PropertyChangeEvent } from "../Property";
import { Disposable } from "../../Disposable";
import { AbstractListProperty } from "./AbstractListProperty";
import { Disposer } from "../../Disposer";
import { property } from "../../index";

export class DependentListProperty<T> extends AbstractListProperty<T> {
    private readonly dependency: ListProperty<T>;
    private readonly transform: Property<(values: readonly T[]) => T[]>;
    private dependency_disposer = new Disposer();
    private values?: T[];

    get val(): readonly T[] {
        return this.get_val();
    }

    constructor(
        dependency: ListProperty<T>,
        transform: ((values: readonly T[]) => T[]) | Property<(values: readonly T[]) => T[]>,
    ) {
        super();

        this.dependency = dependency;

        if (is_any_property(transform)) {
            this.transform = transform;
        } else {
            this.transform = property(transform);
        }
    }

    get_val(): readonly T[] {
        if (this.values) {
            return this.values;
        } else {
            return this.transform.val(this.dependency.val);
        }
    }

    observe(
        observer: (event: PropertyChangeEvent<readonly T[]>) => void,
        options: { call_now?: boolean } = {},
    ): Disposable {
        const super_disposable = super.observe(observer, options);

        this.init_dependency_disposables();

        return {
            dispose: () => {
                super_disposable.dispose();
                this.dispose_dependency_disposables();
            },
        };
    }

    observe_list(
        observer: (change: ListPropertyChangeEvent<T>) => void,
        options?: { call_now?: boolean },
    ): Disposable {
        const super_disposable = super.observe_list(observer, options);

        this.init_dependency_disposables();

        return {
            dispose: () => {
                super_disposable.dispose();
                this.dispose_dependency_disposables();
            },
        };
    }

    filtered(
        predicate: ((value: T) => boolean) | Property<(value: T) => boolean>,
    ): ListProperty<T> {
        if (is_any_property(predicate)) {
            return new DependentListProperty(
                this,
                predicate.map(p => values => values.filter(p)),
            );
        } else {
            return new DependentListProperty(this, values => values.filter(predicate));
        }
    }

    protected compute_length(): number {
        if (this.values) {
            return this.values.length;
        } else {
            return this.transform.val(this.dependency.val).length;
        }
    }

    private init_dependency_disposables(): void {
        if (this.dependency_disposer.length === 0) {
            const observer = (): void => {
                const removed = this.values!;
                this.values = this.transform.val(this.dependency.val);

                this.finalize_update({
                    type: ListChangeType.ListChange,
                    index: 0,
                    removed,
                    inserted: this.values,
                });
            };

            this.values = this.transform.val(this.dependency.val);

            this.dependency_disposer.add_all(
                this.dependency.observe(observer),
                this.transform.observe(observer),
            );
        }
    }

    private dispose_dependency_disposables(): void {
        if (this.observers.length === 0 && this.list_observers.length === 0) {
            this.dependency_disposer.dispose_all();
        }
    }
}

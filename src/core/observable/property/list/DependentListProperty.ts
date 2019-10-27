import { ListChangeType, ListProperty, ListPropertyChangeEvent } from "./ListProperty";
import { PropertyChangeEvent } from "../Property";
import { Disposable } from "../../Disposable";
import { AbstractListProperty } from "./AbstractListProperty";

export class DependentListProperty<T> extends AbstractListProperty<T> {
    private readonly dependency: ListProperty<T>;
    private readonly transform: (values: readonly T[]) => T[];
    private dependency_disposable?: Disposable;
    private values?: T[];

    get val(): readonly T[] {
        return this.get_val();
    }

    constructor(dependency: ListProperty<T>, transform: (values: readonly T[]) => T[]) {
        super();

        this.dependency = dependency;
        this.transform = transform;
    }

    get_val(): readonly T[] {
        if (this.values) {
            return this.values;
        } else {
            return this.transform(this.dependency.val);
        }
    }

    observe(
        observer: (event: PropertyChangeEvent<readonly T[]>) => void,
        options: { call_now?: boolean } = {},
    ): Disposable {
        const super_disposable = super.observe(observer, options);

        this.init_dependency_disposable();

        return {
            dispose: () => {
                super_disposable.dispose();
                this.dispose_dependency_disposable();
            },
        };
    }

    observe_list(observer: (change: ListPropertyChangeEvent<T>) => void): Disposable {
        const super_disposable = super.observe_list(observer);

        this.init_dependency_disposable();

        return {
            dispose: () => {
                super_disposable.dispose();
                this.dispose_dependency_disposable();
            },
        };
    }

    filtered(predicate: (value: T) => boolean): ListProperty<T> {
        return new DependentListProperty(this, values => values.filter(predicate));
    }

    protected compute_length(): number {
        if (this.values) {
            return this.values.length;
        } else {
            return this.transform(this.dependency.val).length;
        }
    }

    private init_dependency_disposable(): void {
        if (this.dependency_disposable == undefined) {
            this.values = this.transform(this.dependency.val);

            this.dependency_disposable = this.dependency.observe(() => {
                const removed = this.values!;
                this.values = this.transform(this.dependency.val);

                this.finalize_update({
                    type: ListChangeType.ListChange,
                    index: 0,
                    removed,
                    inserted: this.values,
                });
            });
        }
    }

    private dispose_dependency_disposable(): void {
        if (
            this.observers.length === 0 &&
            this.list_observers.length === 0 &&
            this.dependency_disposable
        ) {
            this.dependency_disposable.dispose();
        }
    }
}

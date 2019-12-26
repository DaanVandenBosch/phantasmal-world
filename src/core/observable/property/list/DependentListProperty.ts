import { ListChangeType, ListChangeEvent } from "./ListProperty";
import { Property } from "../Property";
import { Disposable } from "../../Disposable";
import { AbstractListProperty } from "./AbstractListProperty";
import { Disposer } from "../../Disposer";
import { ChangeEvent } from "../../Observable";

/**
 * Starts observing its dependencies when the first observer on this property is registered.
 * Stops observing its dependencies when the last observer on this property is disposed.
 * This way no extra disposables need to be managed when e.g. {@link Property.map} is used.
 */
export abstract class DependentListProperty<T> extends AbstractListProperty<T> {
    private dependency_disposer = new Disposer();
    private values?: readonly T[];

    get val(): readonly T[] {
        return this.get_val();
    }

    get_val(): readonly T[] {
        if (this.dependency_disposer.length === 0) {
            this.values = this.compute_values();
        }

        return this.values as T[];
    }

    protected constructor(private dependencies: readonly Property<any>[]) {
        super();
    }

    observe(
        observer: (event: ChangeEvent<readonly T[]>) => void,
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
        observer: (change: ListChangeEvent<T>) => void,
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

    protected compute_length(): number {
        if (this.dependency_disposer.length === 0) {
            this.values = this.compute_values();
        }

        return this.values!.length;
    }

    protected abstract compute_values(): readonly T[];

    private init_dependency_disposables(): void {
        if (this.dependency_disposer.length === 0) {
            this.values = this.compute_values();

            this.dependency_disposer.add_all(
                ...this.dependencies.map(dependency =>
                    dependency.observe(() => {
                        const removed = this.values!;
                        this.values = this.compute_values();

                        this.finalize_update({
                            type: ListChangeType.ListChange,
                            index: 0,
                            removed,
                            inserted: this.values,
                        });
                    }),
                ),
            );
        }
    }

    private dispose_dependency_disposables(): void {
        if (this.observers.length === 0 && this.list_observers.length === 0) {
            this.dependency_disposer.dispose_all();
        }
    }
}
